/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Rapha�l Slinckx <raphael@slinckx.net>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav

import com.flammky.android.core.sdk.VersionHelper
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.SupportedFileFormat
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.GenericAudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.readFileDataIntoBufferLE
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.Chunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.IffHeaderChunk.ensureOnEqualBoundary
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav.chunk.WavCorruptChunkType
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav.chunk.WavFactChunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav.chunk.WavFormatChunk
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.Hex
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.util.logging.Logger

/**
 * Read the Wav file chunks, until finds WavFormatChunk and then generates AudioHeader from it
 */
class WavInfoReader(private val loggingName: String) {
	//So if we encounter bad chunk we know if we have managed to find good audio chunks first
	private var isFoundAudio = false
	private var isFoundFormat = false

	@Throws(CannotReadException::class, IOException::class)
	fun read(path: Path?): GenericAudioHeader {
		return if (VersionHelper.hasOreo()) {
			FileChannel.open(path).use { read(it) }
		} else {
			RandomAccessFile(path?.toFile(), "r").use { read(it.channel) }
		}
	}

	fun read(fc: FileChannel): GenericAudioHeader {
		val info = GenericAudioHeader()
		if (WavRIFFHeader.isValidHeader(
				loggingName, fc
			)
		) {
			while (fc.position() < fc.size()) {
				//Problem reading chunk and no way to workround it so exit loop
				if (!readChunk(fc, info)) {
					break
				}
			}
		} else {
			throw CannotReadException(
				"$loggingName Wav RIFF Header not valid"
			)
		}
		return if (isFoundFormat && isFoundAudio) {
			info.format = (SupportedFileFormat.WAV.displayName)
			info.isLossless = true
			calculateTrackLength(info)
			info
		} else {
			throw CannotReadException(
				"$loggingName Unable to safetly read chunks for this file, appears to be corrupt"
			)
		}
	}

	/**
	 * Calculate track length, done it here because requires data from multiple chunks
	 *
	 * @param info
	 * @throws CannotReadException
	 */
	@Throws(CannotReadException::class)
	private fun calculateTrackLength(info: GenericAudioHeader) {
		//If we have fact chunk we can calculate accurately by taking total of samples (per channel) divided by the number
		//of samples taken per second (per channel)
		if (info.noOfSamples != null) {
			if (info.sampleRateAsNumber!! > 0) {
				info.preciseTrackLength =
					(info.noOfSamples!!.toFloat() / info.sampleRateAsNumber!!).toDouble()
			}
		} else if (info.audioDataLength!! > 0) {
			info.preciseTrackLength = ((info.audioDataLength!!.toFloat() / info.byteRate!!).toDouble())
		} else {
			throw CannotReadException(
				"$loggingName Wav Data Header Missing"
			)
		}
	}

	/**
	 * Reads a Wav Chunk.
	 */
	@Throws(IOException::class, CannotReadException::class)
	protected fun readChunk(fc: FileChannel, info: GenericAudioHeader): Boolean {
		val chunk: Chunk
		val chunkHeader = ChunkHeader(ByteOrder.LITTLE_ENDIAN)
		if (!chunkHeader.readHeader(fc)) {
			return false
		}
		val id = chunkHeader.id
		logger.info(
			loggingName + " Reading Chunk:" + id + ":starting at:" + Hex.asDecAndHex(chunkHeader.startLocationInFile) + ":sizeIncHeader:" + (chunkHeader.size + ChunkHeader.CHUNK_HEADER_SIZE)
		)
		val chunkType: WavChunkType? = WavChunkType.Companion.get(id)

		//If known chunkType
		if (chunkType != null) {
			when (chunkType) {
				WavChunkType.FACT -> {
					val fmtChunkData = readFileDataIntoBufferLE(fc, chunkHeader.size.toInt())
					chunk = WavFactChunk(fmtChunkData, chunkHeader, info)
					if (!chunk.readChunk()) {
						return false
					}
				}
				WavChunkType.DATA -> {

					//We just need this value from header dont actually need to read data itself
					info.audioDataLength = chunkHeader.size
					info.audioDataStartPosition = (fc.position())
					info.audioDataEndPosition = (fc.position() + chunkHeader.size)
					fc.position(fc.position() + chunkHeader.size)
					isFoundAudio = true
				}
				WavChunkType.FORMAT -> {
					val fmtChunkData = readFileDataIntoBufferLE(fc, chunkHeader.size.toInt())
					chunk = WavFormatChunk(fmtChunkData, chunkHeader, info)
					if (!chunk.readChunk()) {
						return false
					}
					isFoundFormat = true
				}
				else -> if (fc.position() + chunkHeader.size <= fc.size()) {
					fc.position(fc.position() + chunkHeader.size)
				} else {
					if (isFoundAudio && isFoundFormat) {
						logger.severe(
							loggingName + " Size of Chunk Header larger than data, skipping to file end:" + id + ":starting at:" + Hex.asDecAndHex(
								chunkHeader.startLocationInFile
							) + ":sizeIncHeader:" + (chunkHeader.size + ChunkHeader.CHUNK_HEADER_SIZE)
						)
						fc.position(fc.size())
					} else {
						logger.severe(
							"$loggingName Size of Chunk Header larger than data, cannot read file"
						)
						throw CannotReadException(
							"$loggingName Size of Chunk Header larger than data, cannot read file"
						)
					}
				}
			}
		} else if (id!!.substring(1, 3) == WavCorruptChunkType.CORRUPT_LIST_EARLY.code) {
			logger.severe(
				loggingName + " Found Corrupt LIST Chunk, starting at Odd Location:" + chunkHeader.id + ":" + chunkHeader.size
			)
			fc.position(fc.position() - (ChunkHeader.CHUNK_HEADER_SIZE - 1))
			return true
		} else if (id.substring(0, 3) == WavCorruptChunkType.CORRUPT_LIST_LATE.code) {
			logger.severe(
				loggingName + " Found Corrupt LIST Chunk (2), starting at Odd Location:" + chunkHeader.id + ":" + chunkHeader.size
			)
			fc.position(fc.position() - (ChunkHeader.CHUNK_HEADER_SIZE + 1))
			return true
		} else if (id == "\u0000\u0000\u0000\u0000" && chunkHeader.size == 0L) {
			//Carry on reading until not null (TODO check not long)
			val fileRemainder = (fc.size() - fc.position()).toInt()
			val restOfFile = ByteBuffer.allocate(fileRemainder)
			fc.read(restOfFile)
			restOfFile.flip()
			while (restOfFile.hasRemaining() && restOfFile.get().toInt() == 0) {
			}
			logger.severe(
				loggingName + "Found Null Padding, starting at " + chunkHeader.startLocationInFile + ", size:" + restOfFile.position() + ChunkHeader.CHUNK_HEADER_SIZE
			)
			fc.position(chunkHeader.startLocationInFile + restOfFile.position() + ChunkHeader.CHUNK_HEADER_SIZE - 1)
			return true
		} else {
			if (chunkHeader.size < 0) {
				//As long as we have found audio data and info we can just skip to the end
				if (isFoundAudio && isFoundFormat) {
					logger.severe(
						loggingName + " Size of Chunk Header is negative, skipping to file end:" + id + ":starting at:" + Hex.asDecAndHex(
							chunkHeader.startLocationInFile
						) + ":sizeIncHeader:" + (chunkHeader.size + ChunkHeader.CHUNK_HEADER_SIZE)
					)
					fc.position(fc.size())
				} else {
					val msg =
						(loggingName + " Not a valid header, unable to read a sensible size:Header"
							+ chunkHeader.id + "Size:" + chunkHeader.size)
					logger.severe(msg)
					throw CannotReadException(msg)
				}
			} else if (fc.position() + chunkHeader.size <= fc.size()) {
				logger.severe(
					loggingName + " Skipping chunk bytes:" + chunkHeader.size + " for " + chunkHeader.id
				)
				fc.position(fc.position() + chunkHeader.size)
			} else {
				//As long as we have found audio data and info we can just skip to the end
				if (isFoundAudio && isFoundFormat) {
					logger.severe(
						loggingName + " Size of Chunk Header larger than data, skipping to file end:" + id + ":starting at:" + Hex.asDecAndHex(
							chunkHeader.startLocationInFile
						) + ":sizeIncHeader:" + (chunkHeader.size + ChunkHeader.CHUNK_HEADER_SIZE)
					)
					fc.position(fc.size())
				} else {
					logger.severe(
						"$loggingName Size of Chunk Header larger than data, cannot read file"
					)
					throw CannotReadException(
						"$loggingName Size of Chunk Header larger than data, cannot read file"
					)
				}
			}
		}
		ensureOnEqualBoundary(fc, chunkHeader)
		return true
	}

	companion object {
		var logger = Logger.getLogger("org.jaudiotagger.audio.wav")
	}
}
