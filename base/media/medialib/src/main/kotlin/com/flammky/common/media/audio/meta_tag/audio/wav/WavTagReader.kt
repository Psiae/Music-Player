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
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.readFileDataIntoBufferLE
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.*
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.IffHeaderChunk.ensureOnEqualBoundary
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav.chunk.WavCorruptChunkType
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav.chunk.WavId3Chunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav.chunk.WavListChunk
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.Hex
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagOptionSingleton
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.wav.WavInfoTag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.wav.WavTag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.wav.WavTag.Companion.createDefaultID3Tag
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
class WavTagReader(private val loggingName: String) {
	/**
	 * Read file and return tag metadata
	 *
	 * @param path
	 * @return
	 * @throws CannotReadException
	 * @throws IOException
	 */
	@Throws(CannotReadException::class, IOException::class)
	fun read(path: Path?): WavTag {
		return if (VersionHelper.hasOreo()) {
			FileChannel.open(path).use { read(it) }
		} else {
			RandomAccessFile(path?.toFile(), "r").use { read(it.channel) }
		}
	}

	@Throws(CannotReadException::class, IOException::class)
	fun read(fc: FileChannel): WavTag {
		val tag = WavTag(TagOptionSingleton.instance.wavOptions)
		if (WavRIFFHeader.isValidHeader(
				loggingName, fc
			)
		) {
			while (fc.position() < fc.size()) {
				if (!readChunk(fc, tag)) {
					break
				}
			}
		} else {
			throw CannotReadException(
				"$loggingName Wav RIFF Header not valid"
			)
		}
		createDefaultMetadataTagsIfMissing(tag)
		return tag
	}

	/**
	 * So if the file doesn't contain (both) types of metadata we construct them so data can be
	 * added and written back to file on save
	 *
	 * @param tag
	 */
	private fun createDefaultMetadataTagsIfMissing(tag: WavTag) {
		if (!tag.isExistingId3Tag) {
			tag.iD3Tag = createDefaultID3Tag()
		}
		if (!tag.isExistingInfoTag) {
			tag.infoTag = WavInfoTag()
		}
	}

	/**
	 * Reads Wavs Chunk that contain tag metadata
	 *
	 * If the same chunk exists more than once in the file we would just use the last occurence
	 *
	 * @param tag
	 * @return
	 * @throws IOException
	 */
	@Throws(IOException::class, CannotReadException::class)
	protected fun readChunk(fc: FileChannel, tag: WavTag): Boolean {
		val chunk: Chunk
		val chunkHeader = ChunkHeader(ByteOrder.LITTLE_ENDIAN)
		val cs: ChunkSummary
		if (!chunkHeader.readHeader(fc)) {
			return false
		}
		val id = chunkHeader.id
		logger.info(
			loggingName + " Reading Chunk:" + id + ":starting at:" + Hex.asDecAndHex(chunkHeader.startLocationInFile) + ":sizeIncHeader:" + (chunkHeader.size + ChunkHeader.CHUNK_HEADER_SIZE)
		)
		val chunkType: WavChunkType? = WavChunkType.Companion.get(id)
		if (chunkType != null) {
			when (chunkType) {
				WavChunkType.LIST -> {
					cs = ChunkSummary(
						chunkHeader.id!!, chunkHeader.startLocationInFile, chunkHeader.size
					)
					tag.addChunkSummary(cs)
					tag.addMetadataChunkSummary(cs)
					if (tag.infoTag == null) {
						chunk = WavListChunk(
							loggingName,
							readFileDataIntoBufferLE(fc, chunkHeader.size.toInt()),
							chunkHeader,
							tag
						)
						if (!chunk.readChunk()) {
							logger.severe(
								"$loggingName LIST readChunkFailed"
							)
							return false
						}
					} else {
						fc.position(fc.position() + chunkHeader.size)
						logger.warning(
							loggingName + " Ignoring LIST chunk because already have one:" + chunkHeader.id
								+ ":" + Hex.asDecAndHex(chunkHeader.startLocationInFile - 1)
								+ ":sizeIncHeader:" + (chunkHeader.size + ChunkHeader.CHUNK_HEADER_SIZE)
						)
					}
				}
				WavChunkType.ID3_UPPERCASE -> {
					cs = ChunkSummary(
						chunkHeader.id!!, chunkHeader.startLocationInFile, chunkHeader.size
					)
					tag.addChunkSummary(cs)
					tag.addMetadataChunkSummary(cs)
					if (tag.iD3Tag == null) {
						chunk = WavId3Chunk(
							readFileDataIntoBufferLE(fc, chunkHeader.size.toInt()),
							chunkHeader,
							tag,
							loggingName
						)
						if (!chunk.readChunk()) {
							logger.severe(
								"$loggingName ID3 readChunkFailed"
							)
							return false
						}
						logger.severe(
							loggingName + " ID3 chunk should be id3:" + chunkHeader.id + ":"
								+ Hex.asDecAndHex(chunkHeader.startLocationInFile)
								+ ":sizeIncHeader:" + (chunkHeader.size + ChunkHeader.CHUNK_HEADER_SIZE)
						)
					} else {
						fc.position(fc.position() + chunkHeader.size)
						logger.warning(
							loggingName + " Ignoring id3 chunk because already have one:" + chunkHeader.id + ":"
								+ Hex.asDecAndHex(chunkHeader.startLocationInFile)
								+ ":sizeIncHeader:" + (chunkHeader.size + ChunkHeader.CHUNK_HEADER_SIZE)
						)
					}
				}
				WavChunkType.ID3 -> {
					cs = ChunkSummary(
						chunkHeader.id!!, chunkHeader.startLocationInFile, chunkHeader.size
					)
					tag.addChunkSummary(cs)
					tag.addMetadataChunkSummary(cs)
					if (tag.iD3Tag == null) {
						chunk = WavId3Chunk(
							readFileDataIntoBufferLE(fc, chunkHeader.size.toInt()),
							chunkHeader,
							tag,
							loggingName
						)
						if (!chunk.readChunk()) {
							logger.severe(
								"$loggingName id3 readChunkFailed"
							)
							return false
						}
					} else {
						fc.position(fc.position() + chunkHeader.size)
						logger.warning(
							loggingName + " Ignoring id3 chunk because already have one:" + chunkHeader.id + ":"
								+ Hex.asDecAndHex(chunkHeader.startLocationInFile)
								+ ":sizeIncHeader:" + (chunkHeader.size + ChunkHeader.CHUNK_HEADER_SIZE)
						)
					}
				}
				else -> {
					tag.addChunkSummary(
						ChunkSummary(
							chunkHeader.id!!, chunkHeader.startLocationInFile, chunkHeader.size
						)
					)
					fc.position(fc.position() + chunkHeader.size)
				}
			}
		} else if (id!!.substring(1, 3) == WavCorruptChunkType.CORRUPT_ID3_EARLY.code) {
			logger.severe(
				loggingName + " Found Corrupt id3 chunk, starting at Odd Location:" + chunkHeader.id + ":" + chunkHeader.size
			)
			if (tag.infoTag == null && tag.iD3Tag == null) {
				tag.isIncorrectlyAlignedTag = true
			}
			fc.position(fc.position() - (ChunkHeader.CHUNK_HEADER_SIZE - 1))
			return true
		} else if (id.substring(0, 3) == WavCorruptChunkType.CORRUPT_ID3_LATE.code) {
			logger.severe(
				loggingName + " Found Corrupt id3 chunk, starting at Odd Location:" + chunkHeader.id + ":" + chunkHeader.size
			)
			if (tag.infoTag == null && tag.iD3Tag == null) {
				tag.isIncorrectlyAlignedTag = true
			}
			fc.position(fc.position() - (ChunkHeader.CHUNK_HEADER_SIZE + 1))
			return true
		} else if (id.substring(1, 3) == WavCorruptChunkType.CORRUPT_LIST_EARLY.code) {
			logger.severe(
				loggingName + " Found Corrupt LIST Chunk, starting at Odd Location:" + chunkHeader.id + ":" + chunkHeader.size
			)
			if (tag.infoTag == null && tag.iD3Tag == null) {
				tag.isIncorrectlyAlignedTag = true
			}
			fc.position(fc.position() - (ChunkHeader.CHUNK_HEADER_SIZE - 1))
			return true
		} else if (id.substring(0, 3) == WavCorruptChunkType.CORRUPT_LIST_LATE.code) {
			logger.severe(
				loggingName + " Found Corrupt LIST Chunk (2), starting at Odd Location:" + chunkHeader.id + ":" + chunkHeader.size
			)
			if (tag.infoTag == null && tag.iD3Tag == null) {
				tag.isIncorrectlyAlignedTag = true
			}
			fc.position(fc.position() - (ChunkHeader.CHUNK_HEADER_SIZE + 1))
			return true
		} else if (id == "\u0000\u0000\u0000\u0000" && chunkHeader.size == 0L) {
			//Carry on reading until not null (TODO check not long)
			val fileRemainder = (fc.size() - fc.position()).toInt()
			val restOfFile = ByteBuffer.allocate(fileRemainder)
			fc.read(restOfFile)
			restOfFile.flip()
			while (restOfFile.get().toInt() == 0) {
			}
			logger.severe(
				loggingName + "Found Null Padding, starting at " + chunkHeader.startLocationInFile + ", size:" + restOfFile.position() + ChunkHeader.CHUNK_HEADER_SIZE
			)
			fc.position(chunkHeader.startLocationInFile + restOfFile.position() + ChunkHeader.CHUNK_HEADER_SIZE - 1)
			tag.addChunkSummary(
				PaddingChunkSummary(
					chunkHeader.startLocationInFile,
					(restOfFile.position() - 1).toLong()
				)
			)
			tag.isNonStandardPadding = true
			return true
		} else {
			if (chunkHeader.size < 0) {
				logger.severe(
					loggingName + " Size of Chunk Header is negative, skipping to file end:" + id + ":starting at:" + Hex.asDecAndHex(
						chunkHeader.startLocationInFile
					) + ":sizeIncHeader:" + (chunkHeader.size + ChunkHeader.CHUNK_HEADER_SIZE)
				)
				tag.addChunkSummary(
					BadChunkSummary(
						chunkHeader.startLocationInFile,
						fc.size() - fc.position()
					)
				)
				tag.isBadChunkData = true
				fc.position(fc.size())
			} else if (fc.position() + chunkHeader.size <= fc.size()) {
				logger.severe(
					loggingName + " Skipping chunk bytes:" + chunkHeader.size + " for " + chunkHeader.id
				)
				tag.addChunkSummary(
					ChunkSummary(
						chunkHeader.id!!, chunkHeader.startLocationInFile, chunkHeader.size
					)
				)
				fc.position(fc.position() + chunkHeader.size)
			} else {
				logger.severe(
					loggingName + " Size of Chunk Header larger than data, skipping to file end:" + id + ":starting at:" + Hex.asDecAndHex(
						chunkHeader.startLocationInFile
					) + ":sizeIncHeader:" + (chunkHeader.size + ChunkHeader.CHUNK_HEADER_SIZE)
				)
				tag.addChunkSummary(
					BadChunkSummary(
						chunkHeader.startLocationInFile,
						fc.size() - fc.position()
					)
				)
				tag.isBadChunkData = true
				fc.position(fc.size())
			}
		}
		ensureOnEqualBoundary(fc, chunkHeader)
		return true
	}

	companion object {
		var logger = Logger.getLogger("org.jaudiotagger.audio.wav")
	}
}
