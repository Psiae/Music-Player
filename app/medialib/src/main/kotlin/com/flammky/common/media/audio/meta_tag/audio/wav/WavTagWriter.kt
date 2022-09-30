/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Raphaël Slinckx <raphael@slinckx.net>
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
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotWriteException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.NoWritePermissionsException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getSizeLEInt32
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.isOddLength
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkSummary
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.IffHeaderChunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.PaddingChunkSummary
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav.chunk.WavChunkSummary.getChunkBeforeFirstMetadataTag
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav.chunk.WavChunkSummary.isOnlyMetadataTagsAfterStartingMetadataTag
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav.chunk.WavInfoIdentifier
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav.chunk.WavInfoIdentifier.Companion.getByFieldKey
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.wav.WavInfoTag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.wav.WavTag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.wav.WavTag.Companion.createDefaultID3Tag
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.AccessDeniedException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.logging.Logger

/**
 * Write Wav Tag.
 */
class WavTagWriter(  //For logging
	private val loggingName: String
) {
	/**
	 * Read existing metadata
	 *
	 * @param path
	 * @return tags within Tag wrapper
	 * @throws IOException
	 * @throws CannotWriteException
	 */
	@Throws(IOException::class, CannotWriteException::class)
	fun getExistingMetadata(path: Path?): WavTag? {
		return try {
			//Find WavTag (if any)
			val im = WavTagReader(
				loggingName
			)
			im.read(path)
		} catch (ex: CannotReadException) {
			throw CannotWriteException(
				"Failed to read file $path"
			)
		}
	}

	/**
	 * Seek in file to start of LIST Metadata chunk
	 *
	 * @param fc
	 * @param existingTag
	 * @throws IOException
	 * @throws CannotWriteException
	 */
	@Throws(IOException::class, CannotWriteException::class)
	fun seekToStartOfListInfoMetadata(fc: FileChannel, existingTag: WavTag?): ChunkHeader {
		fc.position(existingTag!!.infoTag!!.startLocationInFile!!)
		val chunkHeader = ChunkHeader(ByteOrder.LITTLE_ENDIAN)
		chunkHeader.readHeader(fc)
		fc.position(fc.position() - ChunkHeader.CHUNK_HEADER_SIZE)
		if (WavChunkType.LIST.code != chunkHeader.id) {
			throw CannotWriteException(
				"$loggingName Unable to find List chunk at original location has file been modified externally"
			)
		}
		return chunkHeader
	}

	@Throws(IOException::class, CannotWriteException::class)
	fun seekToStartOfListInfoMetadataForChunkSummaryHeader(
		fc: FileChannel,
		cs: ChunkSummary
	): ChunkHeader {
		fc.position(cs.fileStartLocation)
		val chunkHeader = ChunkHeader(ByteOrder.LITTLE_ENDIAN)
		chunkHeader.readHeader(fc)
		fc.position(fc.position() - ChunkHeader.CHUNK_HEADER_SIZE)
		if (WavChunkType.LIST.code != chunkHeader.id) {
			throw CannotWriteException(
				"$loggingName Unable to find List chunk at original location has file been modified externally"
			)
		}
		return chunkHeader
	}

	/**
	 * Seek in file to start of Id3 Metadata chunk
	 *
	 * @param fc
	 * @param existingTag
	 * @throws IOException
	 * @throws CannotWriteException
	 */
	@Throws(IOException::class, CannotWriteException::class)
	fun seekToStartOfId3MetadataForChunkSummaryHeader(
		fc: FileChannel,
		existingTag: WavTag?
	): ChunkHeader {
		logger.info(
			loggingName + ":seekToStartOfIdMetadata:" + existingTag!!.startLocationInFileOfId3Chunk
		)
		fc.position(existingTag.startLocationInFileOfId3Chunk)
		val chunkHeader = ChunkHeader(ByteOrder.LITTLE_ENDIAN)
		chunkHeader.readHeader(fc)
		fc.position(fc.position() - ChunkHeader.CHUNK_HEADER_SIZE)
		if (WavChunkType.ID3.code != chunkHeader.id && WavChunkType.ID3_UPPERCASE.code != chunkHeader.id) {
			throw CannotWriteException(
				loggingName + " Unable to find ID3 chunk at original location has file been modified externally:" + chunkHeader.id
			)
		}
		if (WavChunkType.ID3_UPPERCASE.code == chunkHeader.id) {
			logger.severe(
				"$loggingName:on save ID3 chunk will be correctly set with id3 id"
			)
		}
		return chunkHeader
	}

	@Throws(IOException::class, CannotWriteException::class)
	fun seekToStartOfId3MetadataForChunkSummaryHeader(
		fc: FileChannel,
		chunkSummary: ChunkSummary
	): ChunkHeader {
		logger.severe(
			loggingName + ":seekToStartOfIdMetadata:" + chunkSummary.fileStartLocation
		)
		fc.position(chunkSummary.fileStartLocation)
		val chunkHeader = ChunkHeader(ByteOrder.LITTLE_ENDIAN)
		chunkHeader.readHeader(fc)
		fc.position(fc.position() - ChunkHeader.CHUNK_HEADER_SIZE)
		if (WavChunkType.ID3.code != chunkHeader.id && WavChunkType.ID3_UPPERCASE.code != chunkHeader.id) {
			throw CannotWriteException(
				loggingName + " Unable to find ID3 chunk at original location has file been modified externally:" + chunkHeader.id
			)
		}
		if (WavChunkType.ID3_UPPERCASE.code == chunkHeader.id) {
			logger.severe(
				"$loggingName:on save ID3 chunk will be correctly set with id3 id"
			)
		}
		return chunkHeader
	}

	/**
	 * Delete any existing metadata tags from files
	 *
	 * @param tag
	 * @param file
	 * @throws IOException
	 * @throws CannotWriteException
	 */
	@Throws(CannotWriteException::class)
	fun delete(tag: Tag?, file: Path?) {
		logger.info(
			"$loggingName:Deleting metadata from file"
		)
		try {
			if (!VersionHelper.hasOreo()) TODO("Implement API < 26")

			FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.READ).use { fc ->
				val existingTag = getExistingMetadata(file)

				//have both tags
				if (existingTag!!.isExistingId3Tag && existingTag.isExistingInfoTag) {
					val fs = checkExistingLocations(existingTag, fc)
					//We can delete both chunks in one go
					if (fs.isContiguous) {
						//Quick method
						if (fs.isAtEnd) {
							if (fs.isInfoTagFirst) {
								fc.truncate(existingTag.infoTag!!.startLocationInFile!!)
							} else {
								fc.truncate(existingTag.startLocationInFileOfId3Chunk)
							}
						} else {
							if (fs.isInfoTagFirst) {
								val lengthTagChunk =
									(existingTag.endLocationInFileOfId3Chunk - existingTag.infoTag!!.startLocationInFile!!).toInt()
								deleteTagChunk(
									fc,
									existingTag.endLocationInFileOfId3Chunk.toInt(),
									lengthTagChunk
								)
							} else {
								val lengthTagChunk =
									(existingTag.infoTag!!.endLocationInFile!!.toInt() - existingTag.startLocationInFileOfId3Chunk).toInt()
								deleteTagChunk(
									fc,
									existingTag.infoTag!!.endLocationInFile!!.toInt(),
									lengthTagChunk
								)
							}
						}
					} else {
						val existingInfoTag = existingTag.infoTag
						val infoChunkHeader = seekToStartOfListInfoMetadata(fc, existingTag)
						val id3ChunkHeader =
							seekToStartOfId3MetadataForChunkSummaryHeader(fc, existingTag)

						//If one of these two at end of file delete first then remove the other as a chunk
						if (isInfoTagAtEndOfFileAllowingForPaddingByte(existingTag, fc)) {
							fc.truncate(existingInfoTag!!.startLocationInFile!!)
							deleteId3TagChunk(fc, existingTag, id3ChunkHeader)
						} else if (isID3TagAtEndOfFileAllowingForPaddingByte(existingTag, fc)) {
							fc.truncate(existingTag.startLocationInFileOfId3Chunk)
							deleteInfoTagChunk(fc, existingTag, infoChunkHeader)
						} else if (existingTag.infoTag!!.startLocationInFile!! > existingTag.startLocationInFileOfId3Chunk) {
							deleteInfoTagChunk(fc, existingTag, infoChunkHeader)
							deleteId3TagChunk(fc, existingTag, id3ChunkHeader)
						} else {
							deleteId3TagChunk(fc, existingTag, id3ChunkHeader)
							deleteInfoTagChunk(fc, existingTag, infoChunkHeader)
						}
					}
				} else if (existingTag.isExistingInfoTag) {
					val existingInfoTag = existingTag.infoTag
					val chunkHeader = seekToStartOfListInfoMetadata(fc, existingTag)
					//and it is at end of the file
					if (existingInfoTag!!.endLocationInFile == fc.size()) {
						fc.truncate(existingInfoTag.startLocationInFile!!)
					} else {
						deleteInfoTagChunk(fc, existingTag, chunkHeader)
					}
				} else if (existingTag.isExistingId3Tag) {
					val chunkHeader = seekToStartOfId3MetadataForChunkSummaryHeader(fc, existingTag)
					//and it is at end of the file
					if (isID3TagAtEndOfFileAllowingForPaddingByte(existingTag, fc)) {
						fc.truncate(existingTag.startLocationInFileOfId3Chunk)
					} else {
						deleteId3TagChunk(fc, existingTag, chunkHeader)
					}
				} else {
					//Nothing to delete
				}
				rewriteRiffHeaderSize(fc)
			}
		} catch (ioe: IOException) {
			throw CannotWriteException(file.toString() + ":" + ioe.message)
		}
	}

	/**
	 * Delete existing Info Tag
	 *
	 * @param fc
	 * @param existingTag
	 * @param chunkHeader
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun deleteInfoTagChunk(
		fc: FileChannel,
		existingTag: WavTag?,
		chunkHeader: ChunkHeader
	) {
		val existingInfoTag = existingTag!!.infoTag
		val lengthTagChunk = chunkHeader.size.toInt() + ChunkHeader.CHUNK_HEADER_SIZE
		deleteTagChunk(fc, existingInfoTag!!.endLocationInFile!!.toInt(), lengthTagChunk)
	}

	/**
	 * Delete existing Id3 Tag
	 *
	 * @param fc
	 * @param existingTag
	 * @param chunkHeader
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun deleteId3TagChunk(fc: FileChannel, existingTag: WavTag?, chunkHeader: ChunkHeader) {
		val lengthTagChunk = chunkHeader.size.toInt() + ChunkHeader.CHUNK_HEADER_SIZE
		if (isOddLength(
				existingTag!!.endLocationInFileOfId3Chunk
			)
		) {
			deleteTagChunk(
				fc,
				existingTag.endLocationInFileOfId3Chunk.toInt() + 1,
				lengthTagChunk + 1
			)
		} else {
			deleteTagChunk(fc, existingTag.endLocationInFileOfId3Chunk.toInt(), lengthTagChunk)
		}
	}

	/**
	 * Delete Tag Chunk
	 *
	 *
	 * Can be used when chunk is not the last chunk
	 *
	 *
	 * Continually copy a 4mb chunk, write the chunk and repeat until the rest of the file after the tag
	 * is rewritten
	 *
	 * @param fc
	 * @param endOfExistingChunk
	 * @param lengthTagChunk
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun deleteTagChunk(fc: FileChannel, endOfExistingChunk: Int, lengthTagChunk: Int) {
		//Position for reading after the tag
		fc.position(endOfExistingChunk.toLong())
		val buffer =
			ByteBuffer.allocate(TagOptionSingleton.instance.writeChunkSize.toInt())
		while (fc.read(buffer) >= 0 || buffer.position() != 0) {
			buffer.flip()
			val readPosition = fc.position()
			fc.position(readPosition - lengthTagChunk - buffer.limit())
			fc.write(buffer)
			fc.position(readPosition)
			buffer.compact()
		}
		//Truncate the file after the last chunk
		val newLength = fc.size() - lengthTagChunk
		logger.severe(
			loggingName + "Shortening by:" + lengthTagChunk + " Setting new length to:" + newLength
		)
		fc.truncate(newLength)
	}

	/**
	 *
	 * @param tag
	 * @param file
	 * @throws CannotWriteException
	 */
	@Throws(CannotWriteException::class)
	fun write(tag: Tag?, file: Path?) {
		if (!VersionHelper.hasOreo()) TODO("Implement API < 26")

		logger.config(
			"$loggingName Writing tag to file:start"
		)
		val wso: WavSaveOptions = TagOptionSingleton.instance.wavSaveOptions
		var existingTag: WavTag? = null
		existingTag = try {
			getExistingMetadata(file)
		} catch (ioe: IOException) {
			throw CannotWriteException(file.toString() + ":" + ioe.message)
		}

		//TODO in some case we can fix the files, as we can only open the file if we have successfully
		//retrieved audio data
		if (existingTag!!.isBadChunkData) {
			throw CannotWriteException("Unable to make changes to this file because contains bad chunk data")
		}
		try {
			FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.READ).use { fc ->
				val wavTag = tag as WavTag?
				if (wso == WavSaveOptions.SAVE_BOTH) {
					saveBoth(wavTag, fc, existingTag)
				} else if (wso == WavSaveOptions.SAVE_ACTIVE) {
					saveActive(wavTag, fc, existingTag)
				} else if (wso == WavSaveOptions.SAVE_EXISTING_AND_ACTIVE) {
					saveActiveExisting(wavTag, fc, existingTag)
				} else if (wso == WavSaveOptions.SAVE_BOTH_AND_SYNC) {
					wavTag!!.syncTagBeforeWrite()
					saveBoth(wavTag, fc, existingTag)
				} else if (wso == WavSaveOptions.SAVE_EXISTING_AND_ACTIVE_AND_SYNC) {
					wavTag!!.syncTagBeforeWrite()
					saveActiveExisting(wavTag, fc, existingTag)
				} else {
					throw RuntimeException("$loggingName No setting for:WavSaveOptions")
				}

				//If we had non-standard padding check it still exists and if so remove it
				if (existingTag.isNonStandardPadding) {
					for (cs in existingTag.getChunkSummaryList()) {
						//Note, can only delete a single padding section
						if (cs is PaddingChunkSummary) {
							var isPaddingData = true
							fc.position(cs.fileStartLocation)
							val paddingData = ByteBuffer.allocate(cs.chunkSize.toInt())
							fc.read(paddingData)
							paddingData.flip()
							while (paddingData.position() < paddingData.limit()) {
								if (paddingData.get().toInt() != 0) {
									isPaddingData = false
								}
							}
							if (isPaddingData) {
								fc.position(cs.fileStartLocation)
								deletePaddingChunk(
									fc,
									cs.endLocation.toInt(),
									cs.chunkSize.toInt() + ChunkHeader.CHUNK_HEADER_SIZE
								)
							}
							break
						}
					}
				}
				rewriteRiffHeaderSize(fc)
			}
		} catch (ade: AccessDeniedException) {
			throw NoWritePermissionsException(file.toString() + ":" + ade.message)
		} catch (ioe: IOException) {
			throw CannotWriteException(file.toString() + ":" + ioe.message)
		}
		logger.severe(
			"$loggingName Writing tag to file:Done"
		)
	}

	@Throws(IOException::class)
	private fun deletePaddingChunk(fc: FileChannel, endOfExistingChunk: Int, lengthTagChunk: Int) {
		//Position for reading after the tag
		fc.position(endOfExistingChunk.toLong())
		val buffer =
			ByteBuffer.allocate(TagOptionSingleton.instance.writeChunkSize.toInt())
		while (fc.read(buffer) >= 0 || buffer.position() != 0) {
			buffer.flip()
			val readPosition = fc.position()
			fc.position(readPosition - lengthTagChunk - buffer.limit())
			fc.write(buffer)
			fc.position(readPosition)
			buffer.compact()
		}
		//Truncate the file after the last chunk
		val newLength = fc.size() - lengthTagChunk
		logger.config(
			"$loggingName-------------Setting new length to:$newLength"
		)
		fc.truncate(newLength)
	}

	/**
	 * Rewrite RAF header to reflect new file size
	 *
	 * @param fc
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun rewriteRiffHeaderSize(fc: FileChannel) {
		fc.position(IffHeaderChunk.SIGNATURE_LENGTH.toLong())
		val bb = ByteBuffer.allocateDirect(IffHeaderChunk.SIZE_LENGTH)
		bb.order(ByteOrder.LITTLE_ENDIAN)
		val size = fc.size().toInt() - IffHeaderChunk.SIGNATURE_LENGTH - IffHeaderChunk.SIZE_LENGTH
		bb.putInt(size)
		bb.flip()
		fc.write(bb)
	}
	/**
	 * Write LISTINFOChunk of specified size to current file location
	 * ensuring it is on even file boundary
	 *
	 * @param fc       random access file
	 * @param bb        data to write
	 * @param chunkSize chunk size
	 * @throws IOException
	 */
	/**
	 * Write new Info chunk and dont worry about the size of existing chunk just use size of new chunk
	 *
	 * @param fc
	 * @param bb
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun writeInfoDataToFile(
		fc: FileChannel,
		bb: ByteBuffer,
		chunkSize: Long = bb.limit().toLong()
	) {
		if (isOddLength(fc.position())) {
			writePaddingToFile(fc, 1)
		}
		//Write LIST header
		val listHeaderBuffer = ByteBuffer.allocate(ChunkHeader.CHUNK_HEADER_SIZE)
		listHeaderBuffer.order(ByteOrder.LITTLE_ENDIAN)
		listHeaderBuffer.put(WavChunkType.LIST.code.toByteArray(StandardCharsets.US_ASCII))
		listHeaderBuffer.putInt(chunkSize.toInt())
		listHeaderBuffer.flip()
		fc.write(listHeaderBuffer)

		//Now write actual data
		fc.write(bb)
		writeExtraByteIfChunkOddSize(fc, chunkSize)
	}

	/**
	 * Write Id3Chunk of specified size to current file location
	 * ensuring it is on even file boundary
	 *
	 * @param fc       random access file
	 * @param bb        data to write
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun writeId3DataToFile(fc: FileChannel, bb: ByteBuffer) {
		if (isOddLength(fc.position())) {
			writePaddingToFile(fc, 1)
		}

		//Write ID3Data header
		val listBuffer = ByteBuffer.allocate(ChunkHeader.CHUNK_HEADER_SIZE)
		listBuffer.order(ByteOrder.LITTLE_ENDIAN)
		listBuffer.put(WavChunkType.ID3.code.toByteArray(StandardCharsets.US_ASCII))
		listBuffer.putInt(bb.limit())
		listBuffer.flip()
		fc.write(listBuffer)

		//Now write actual data
		fc.write(bb)
	}

	/**
	 * Write Padding bytes
	 *
	 * @param fc
	 * @param paddingSize
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun writePaddingToFile(fc: FileChannel, paddingSize: Int) {
		fc.write(ByteBuffer.allocateDirect(paddingSize))
	}

	internal inner class InfoFieldWriterOrderComparator : Comparator<TagField?> {
		override fun compare(field1: TagField?, field2: TagField?): Int {
			if (field1 == null && field2 == null) return 0
			if (field1?.id == null && field2?.id == null) return 0

			val key1: FieldKey? = field1?.id?.let { FieldKey.valueOf(it) }
			val key2: FieldKey? = field2?.id?.let { FieldKey.valueOf(it) }

			val code1 = getByFieldKey(key1)
			val code2 = getByFieldKey(key2)
			var order1 = Int.MAX_VALUE
			var order2 = Int.MAX_VALUE
			if (code1 != null) {
				order1 = code1.preferredWriteOrder
			}
			if (code2 != null) {
				order2 = code2.preferredWriteOrder
			}
			return order1 - order2
		}
	}

	/**
	 * Converts INfO tag to [ByteBuffer].
	 *
	 * @param tag tag
	 * @return byte buffer containing the tag data
	 * @throws UnsupportedEncodingException
	 */
	@Throws(UnsupportedEncodingException::class)
	fun convertInfoChunk(tag: WavTag?): ByteBuffer {
		return try {
			val baos = ByteArrayOutputStream()
			val wif = tag!!.infoTag

			//Write the Info chunks
			val fields = wif!!.all
			Collections.sort(fields, InfoFieldWriterOrderComparator())
			var isTrackRewritten = false
			for (nextField in fields) {
				val next = nextField as TagTextField?
				val wii = getByFieldKey(
					FieldKey.valueOf(
						next!!.id!!
					)
				)
				baos.write(wii!!.code.toByteArray(StandardCharsets.US_ASCII))
				logger.config(
					loggingName + " Writing:" + wii.code + ":" + next.content
				)

				//TODO Is UTF8 allowed format
				val contentConvertedToBytes = next.content!!.toByteArray(StandardCharsets.UTF_8)
				baos.write(getSizeLEInt32(contentConvertedToBytes.size))
				baos.write(contentConvertedToBytes)

				//Write extra byte if data length not equal
				if (isOddLength(contentConvertedToBytes.size.toLong())) {
					baos.write(0)
				}

				//Add a duplicated record for Twonky
				if (wii === WavInfoIdentifier.TRACKNO) {
					isTrackRewritten = true
					if (TagOptionSingleton.instance.isWriteWavForTwonky) {
						baos.write(
							WavInfoIdentifier.TWONKY_TRACKNO.code.toByteArray(
								StandardCharsets.US_ASCII
							)
						)
						logger.config(
							loggingName + " Writing:" + WavInfoIdentifier.TWONKY_TRACKNO.code + ":" + next.content
						)
						baos.write(getSizeLEInt32(contentConvertedToBytes.size))
						baos.write(contentConvertedToBytes)

						//Write extra byte if data length not equal
						if (isOddLength(contentConvertedToBytes.size.toLong())) {
							baos.write(0)
						}
					}
				}
			}

			//Write any existing unrecognized tuples
			val ti = wif.getUnrecognisedFields().iterator()
			while (ti.hasNext()) {
				val next = ti.next()
				/**
				 * There may be an existing Twonky itrk field we don't want to re-add this because above we already
				 * add based on value of TRACK if user has enabled the isWriteWavForTwonky option
				 *
				 * And if we dont
				 */
				if (next.id != WavInfoIdentifier.TWONKY_TRACKNO.code || !isTrackRewritten && TagOptionSingleton.instance.isWriteWavForTwonky) {
					baos.write(next.id!!.toByteArray(StandardCharsets.US_ASCII))
					logger.config(
						loggingName + " Writing:" + next.id + ":" + next.content
					)
					val contentConvertedToBytes = next.content!!.toByteArray(StandardCharsets.UTF_8)
					baos.write(getSizeLEInt32(contentConvertedToBytes.size))
					baos.write(contentConvertedToBytes)

					//Write extra byte if data length not equal
					if (isOddLength(contentConvertedToBytes.size.toLong())) {
						baos.write(0)
					}
				}
			}
			val infoBuffer = ByteBuffer.wrap(baos.toByteArray())
			infoBuffer.rewind()

			//Now Write INFO header
			val infoHeaderBuffer = ByteBuffer.allocate(IffHeaderChunk.SIGNATURE_LENGTH)
			infoHeaderBuffer.put(WavChunkType.INFO.code.toByteArray(StandardCharsets.US_ASCII))
			infoHeaderBuffer.flip()


			//Construct a single ByteBuffer from both
			val listInfoBuffer =
				ByteBuffer.allocateDirect(infoHeaderBuffer.limit() + infoBuffer.limit())
			listInfoBuffer.put(infoHeaderBuffer)
			listInfoBuffer.put(infoBuffer)
			listInfoBuffer.flip()
			listInfoBuffer
		} catch (ioe: IOException) {
			//Should never happen as not writing to file at this point
			throw RuntimeException(ioe)
		}
	}

	/**
	 * Converts ID3tag to [ByteBuffer].
	 *
	 * @param tag tag containing ID3tag
	 * @return byte buffer containing the tag data
	 * @throws UnsupportedEncodingException
	 */
	@Throws(UnsupportedEncodingException::class)
	fun convertID3Chunk(tag: WavTag?, existingTag: WavTag?): ByteBuffer {
		return try {
			var baos = ByteArrayOutputStream()
			var existingTagSize = existingTag!!.sizeOfID3TagOnly

			//If existingTag is uneven size lets make it even
			if (existingTagSize > 0) {
				if (existingTagSize and 1L != 0L) {
					existingTagSize++
				}
			}

			//#270
			if (tag!!.iD3Tag == null) {
				tag.iD3Tag = createDefaultID3Tag()
			}

			//Write Tag to buffer
			tag.iD3Tag!!.write(baos, existingTagSize.toInt())

			//If the tag is now odd because we needed to increase size and the data made it odd sized
			//we redo adding a padding byte to make it even
			if (baos.toByteArray().size and 1 != 0) {
				val newSize = baos.toByteArray().size + 1
				baos = ByteArrayOutputStream()
				tag.iD3Tag!!.write(baos, newSize)
			}
			val buf = ByteBuffer.wrap(baos.toByteArray())
			buf.rewind()
			buf
		} catch (ioe: IOException) {
			//Should never happen as not writing to file at this point
			throw RuntimeException(ioe)
		}
	}

	/**
	 * Used when writing both tags to work out the best way to do it
	 */
	internal inner class BothTagsFileStructure {
		var isInfoTagFirst = false
		var isContiguous = false
		var isAtEnd = false
		override fun toString(): String {
			return ("IsInfoTagFirst:" + isInfoTagFirst
				+ ":isContiguous:" + isContiguous
				+ ":isAtEnd:" + isAtEnd)
		}
	}

	/**
	 * Identify where both metadata chunks are in relation to each other and other chunks
	 * @param wavTag
	 * @param fc
	 * @return
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun checkExistingLocations(wavTag: WavTag?, fc: FileChannel): BothTagsFileStructure {
		val fs: BothTagsFileStructure = BothTagsFileStructure()
		if (wavTag!!.infoTag!!.startLocationInFile!! < wavTag.iD3Tag!!.startLocationInFile!!) {
			fs.isInfoTagFirst = true
			//Must allow for odd size chunks
			if (Math.abs(wavTag.infoTag!!.endLocationInFile!! - wavTag.startLocationInFileOfId3Chunk) <= 1) {
				fs.isContiguous = true
				if (isID3TagAtEndOfFileAllowingForPaddingByte(wavTag, fc)) {
					fs.isAtEnd = true
				}
			}
		} else {
			//Must allow for odd size chunks
			if (Math.abs(wavTag.iD3Tag!!.endLocationInFile!! - wavTag.infoTag!!.startLocationInFile!!) <= 1) {
				fs.isContiguous = true
				if (isInfoTagAtEndOfFileAllowingForPaddingByte(wavTag, fc)) {
					fs.isAtEnd = true
				}
			}
		}
		return fs
	}

	/**
	 * Write Info chunk to current location which is last chunk of file
	 *
	 * @param fc
	 * @param existingInfoTag
	 * @param newTagBuffer
	 * @throws CannotWriteException
	 * @throws IOException
	 */
	@Throws(CannotWriteException::class, IOException::class)
	private fun writeInfoChunk(
		fc: FileChannel,
		existingInfoTag: WavInfoTag?,
		newTagBuffer: ByteBuffer
	) {
		val newInfoTagSize = newTagBuffer.limit().toLong()
		//We have enough existing space in chunk so just keep existing chunk size
		if (existingInfoTag!!.sizeOfTag >= newInfoTagSize) {
			writeInfoDataToFile(fc, newTagBuffer, existingInfoTag.sizeOfTag)
			//To ensure old data from previous tag are erased
			if (existingInfoTag.sizeOfTag > newInfoTagSize) {
				writePaddingToFile(fc, (existingInfoTag.sizeOfTag - newInfoTagSize).toInt())
			}
		} else {
			writeInfoDataToFile(fc, newTagBuffer, newInfoTagSize)
		}
	}

	/**
	 * Chunk must also start on an even byte so if our chinksize is odd we need
	 * to write another byte
	 *
	 * @param fc
	 * @param size
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun writeExtraByteIfChunkOddSize(fc: FileChannel, size: Long) {
		if (isOddLength(size)) {
			writePaddingToFile(fc, 1)
		}
	}

	/**
	 *
	 * @param existingTag
	 * @param fc
	 * @return trueif ID3Tag at end of the file
	 *
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun isID3TagAtEndOfFileAllowingForPaddingByte(
		existingTag: WavTag?,
		fc: FileChannel
	): Boolean {
		return existingTag!!.iD3Tag!!.endLocationInFile == fc.size() || existingTag.iD3Tag!!.endLocationInFile!! and 1L != 0L && existingTag.iD3Tag!!.endLocationInFile!! + 1 == fc.size()
	}

	/**
	 *
	 * @param existingTag
	 * @param fc
	 * @return
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun isInfoTagAtEndOfFileAllowingForPaddingByte(
		existingTag: WavTag?,
		fc: FileChannel
	): Boolean {
		return existingTag!!.infoTag!!.endLocationInFile == fc.size() || existingTag.infoTag!!.endLocationInFile!! and 1L != 0L && existingTag.infoTag!!.endLocationInFile!! + 1 == fc.size()
	}

	/**
	 * Save both Info and ID3 chunk
	 *
	 * @param wavTag
	 * @param fc
	 * @param existingTag
	 * @throws CannotWriteException
	 * @throws IOException
	 */
	@Throws(CannotWriteException::class, IOException::class)
	private fun saveBoth(wavTag: WavTag?, fc: FileChannel, existingTag: WavTag?) {
		val infoTagBuffer = convertInfoChunk(wavTag)
		val newInfoTagSize = infoTagBuffer.limit().toLong()
		val id3TagBuffer = convertID3Chunk(wavTag, existingTag)

		//Easiest just to delete all metadata (gets rid of duplicates)
		if (isOnlyMetadataTagsAfterStartingMetadataTag(
				existingTag!!
			)
		) {
			deleteExistingMetadataTagsToEndOfFile(fc, existingTag)
			if (TagOptionSingleton.instance.wavSaveOrder === WavSaveOrder.INFO_THEN_ID3) {
				writeInfoChunkAtFileEnd(fc, infoTagBuffer, newInfoTagSize)
				writeId3ChunkAtFileEnd(fc, id3TagBuffer)
			} else {
				writeId3ChunkAtFileEnd(fc, id3TagBuffer)
				writeInfoChunkAtFileEnd(fc, infoTagBuffer, newInfoTagSize)
			}
		} else if (!existingTag.isIncorrectlyAlignedTag) {
			if (existingTag.metadataChunkSummaryList.size > 0) {
				val li = existingTag.metadataChunkSummaryList.listIterator(
					existingTag.metadataChunkSummaryList.size
				)
				while (li.hasPrevious()) {
					val next = li.previous()
					logger.config(">>>>Deleting--" + next.chunkId + "---" + next.fileStartLocation + "--" + next.endLocation)
					if (isOddLength(next.endLocation)) {
						deleteTagChunk(
							fc,
							next.endLocation.toInt(),
							(next.endLocation + 1 - next.fileStartLocation).toInt()
						)
					} else {
						deleteTagChunk(
							fc,
							next.endLocation.toInt(),
							(next.endLocation - next.fileStartLocation).toInt()
						)
					}
				}
			}
			if (TagOptionSingleton.instance.wavSaveOrder === WavSaveOrder.INFO_THEN_ID3) {
				writeInfoChunkAtFileEnd(fc, infoTagBuffer, newInfoTagSize)
				writeId3ChunkAtFileEnd(fc, id3TagBuffer)
			} else {
				writeId3ChunkAtFileEnd(fc, id3TagBuffer)
				writeInfoChunkAtFileEnd(fc, infoTagBuffer, newInfoTagSize)
			}
		} else {
			throw CannotWriteException(
				"$loggingName Metadata tags are corrupted and not at end of file so cannot be fixed"
			)
		}
	}

	/**
	 * Remove id3 and list chunk if exist
	 *
	 * TODO What about if file has multiple id3 or list chunks
	 *
	 * @param fc
	 * @param existingTag
	 * @throws CannotWriteException
	 * @throws IOException
	 */
	@Throws(CannotWriteException::class, IOException::class)
	fun removeAllMetadata(fc: FileChannel, existingTag: WavTag) {
		if (existingTag.startLocationInFileOfId3Chunk > existingTag.infoTag!!.startLocationInFile!!) {
			val id3ChunkHeader = seekToStartOfId3MetadataForChunkSummaryHeader(fc, existingTag)
			deleteId3TagChunk(fc, existingTag, id3ChunkHeader)
			val infoChunkHeader = seekToStartOfListInfoMetadata(fc, existingTag)
			deleteInfoTagChunk(fc, existingTag, infoChunkHeader)
		} else if (existingTag.infoTag!!.startLocationInFile!! > existingTag.startLocationInFileOfId3Chunk) {
			val infoChunkHeader = seekToStartOfListInfoMetadata(fc, existingTag)
			deleteInfoTagChunk(fc, existingTag, infoChunkHeader)
			val id3ChunkHeader = seekToStartOfId3MetadataForChunkSummaryHeader(fc, existingTag)
			deleteId3TagChunk(fc, existingTag, id3ChunkHeader)
		}
	}

	/**
	 * Write both tags in the order preferred by the options
	 *
	 * @param fc
	 * @param infoTagBuffer
	 * @param id3TagBuffer
	 * @throws IOException
	 */
	@Throws(IOException::class)
	fun writeBothTags(fc: FileChannel, infoTagBuffer: ByteBuffer, id3TagBuffer: ByteBuffer) {
		if (TagOptionSingleton.instance.wavSaveOrder === WavSaveOrder.INFO_THEN_ID3) {
			writeInfoDataToFile(fc, infoTagBuffer)
			writeId3DataToFile(fc, id3TagBuffer)
		} else {
			writeId3DataToFile(fc, id3TagBuffer)
			writeInfoDataToFile(fc, infoTagBuffer)
		}
	}

	/**
	 * Find existing ID3 tag, remove and write new ID3 tag at end of file
	 *
	 * @param fc
	 * @param existingTag
	 * @param infoTagBuffer
	 * @throws CannotWriteException
	 * @throws IOException
	 */
	@Throws(CannotWriteException::class, IOException::class)
	fun replaceInfoChunkAtFileEnd(fc: FileChannel, existingTag: WavTag, infoTagBuffer: ByteBuffer) {
		val infoChunkHeader = seekToStartOfListInfoMetadata(fc, existingTag)
		if (isInfoTagAtEndOfFileAllowingForPaddingByte(existingTag, fc)) {
			logger.severe("writinginfo")
			writeInfoChunk(fc, existingTag.infoTag, infoTagBuffer)
		} else {
			deleteInfoChunkAndCreateNewOneAtFileEnd(fc, existingTag, infoChunkHeader, infoTagBuffer)
		}
	}

	/**
	 * Remove existing INFO tag wherever it is
	 *
	 * @param fc
	 * @param existingTag
	 * @throws IOException
	 */
	@Throws(CannotWriteException::class, IOException::class)
	fun deleteOrTruncateId3Tag(fc: FileChannel, existingTag: WavTag) {
		if (isID3TagAtEndOfFileAllowingForPaddingByte(existingTag, fc)) {
			fc.truncate(existingTag.startLocationInFileOfId3Chunk)
		} else {
			val id3ChunkHeader = seekToStartOfId3MetadataForChunkSummaryHeader(fc, existingTag)
			deleteId3TagChunk(fc, existingTag, id3ChunkHeader)
		}
	}

	/**
	 * Delete Existing Id3 chunk wherever it is , then write Id3 chunk at end of file
	 *
	 * @param fc
	 * @param existingTag
	 * @param id3ChunkHeader
	 * @param infoTagBuffer
	 * @throws IOException
	 */
	@Throws(IOException::class)
	fun deleteInfoChunkAndCreateNewOneAtFileEnd(
		fc: FileChannel,
		existingTag: WavTag?,
		id3ChunkHeader: ChunkHeader,
		infoTagBuffer: ByteBuffer
	) {
		deleteInfoTagChunk(fc, existingTag, id3ChunkHeader)
		fc.position(fc.size())
		writeInfoDataToFile(fc, infoTagBuffer)
	}

	/**
	 *
	 * @param wavTag
	 * @param fc
	 * @param existingTag
	 * @throws CannotWriteException
	 * @throws IOException
	 */
	@Throws(CannotWriteException::class, IOException::class)
	fun saveInfo(wavTag: WavTag?, fc: FileChannel, existingTag: WavTag?) {
		val infoTagBuffer = convertInfoChunk(wavTag)
		val newInfoTagSize = infoTagBuffer.limit().toLong()

		//Easiest just to delete all metadata (gets rid of duplicates)
		if (isOnlyMetadataTagsAfterStartingMetadataTag(
				existingTag!!
			)
		) {
			deleteExistingMetadataTagsToEndOfFile(fc, existingTag)
			writeInfoChunkAtFileEnd(fc, infoTagBuffer, newInfoTagSize)
		} else if (!existingTag.isIncorrectlyAlignedTag) {
			if (existingTag.metadataChunkSummaryList.size > 0) {
				val li = existingTag.metadataChunkSummaryList.listIterator(
					existingTag.metadataChunkSummaryList.size
				)
				while (li.hasPrevious()) {
					val next = li.previous()
					logger.config(">>>>Deleting--" + next.chunkId + "---" + next.fileStartLocation + "--" + next.endLocation)
					if (isOddLength(next.endLocation)) {
						deleteTagChunk(
							fc,
							next.endLocation.toInt(),
							(next.endLocation + 1 - next.fileStartLocation).toInt()
						)
					} else {
						deleteTagChunk(
							fc,
							next.endLocation.toInt(),
							(next.endLocation - next.fileStartLocation).toInt()
						)
					}
				}
			}
			writeInfoChunkAtFileEnd(fc, infoTagBuffer, newInfoTagSize)
		} else {
			throw CannotWriteException(
				"$loggingName Metadata tags are corrupted and not at end of file so cannot be fixed"
			)
		}
	}

	/**
	 * Write Info chunk at end of file
	 *
	 * @param fc
	 * @param infoTagBuffer
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun writeInfoChunkAtFileEnd(
		fc: FileChannel,
		infoTagBuffer: ByteBuffer,
		newInfoTagSize: Long
	) {
		fc.position(fc.size())
		writeInfoDataToFile(fc, infoTagBuffer, newInfoTagSize)
	}

	/**
	 *
	 * @param wavTag
	 * @param fc
	 * @param existingTag
	 * @throws CannotWriteException
	 * @throws IOException
	 */
	@Throws(CannotWriteException::class, IOException::class)
	private fun saveId3(wavTag: WavTag?, fc: FileChannel, existingTag: WavTag?) {
		val id3TagBuffer = convertID3Chunk(wavTag, existingTag)

		//Easiest just to delete all metadata (gets rid of duplicates)
		if (isOnlyMetadataTagsAfterStartingMetadataTag(
				existingTag!!
			)
		) {
			deleteExistingMetadataTagsToEndOfFile(fc, existingTag)
			writeId3ChunkAtFileEnd(fc, id3TagBuffer)
		} else if (!existingTag.isIncorrectlyAlignedTag) {
			if (existingTag.metadataChunkSummaryList.size > 0) {
				val li = existingTag.metadataChunkSummaryList.listIterator(
					existingTag.metadataChunkSummaryList.size
				)
				while (li.hasPrevious()) {
					val next = li.previous()
					logger.config(">>>>Deleting--" + next.chunkId + "---" + next.fileStartLocation + "--" + next.endLocation)
					if (isOddLength(next.endLocation)) {
						deleteTagChunk(
							fc,
							next.endLocation.toInt(),
							(next.endLocation + 1 - next.fileStartLocation).toInt()
						)
					} else {
						deleteTagChunk(
							fc,
							next.endLocation.toInt(),
							(next.endLocation - next.fileStartLocation).toInt()
						)
					}
				}
			}
			writeId3ChunkAtFileEnd(fc, id3TagBuffer)
		} else {
			throw CannotWriteException(
				"$loggingName Metadata tags are corrupted and not at end of file so cannot be fixed"
			)
		}
	}

	/**
	 * Find existing ID3 tag, remove and write new ID3 tag at end of file
	 *
	 * @param fc
	 * @param existingTag
	 * @param id3TagBuffer
	 * @throws CannotWriteException
	 * @throws IOException
	 */
	@Throws(CannotWriteException::class, IOException::class)
	fun replaceId3ChunkAtFileEnd(fc: FileChannel, existingTag: WavTag?, id3TagBuffer: ByteBuffer) {
		val id3ChunkHeader = seekToStartOfId3MetadataForChunkSummaryHeader(fc, existingTag)
		if (isID3TagAtEndOfFileAllowingForPaddingByte(existingTag, fc)) {
			writeId3DataToFile(fc, id3TagBuffer)
		} else {
			deleteId3ChunkAndCreateNewOneAtFileEnd(fc, existingTag, id3ChunkHeader, id3TagBuffer)
		}
	}

	/**
	 * Remove existing INFO tag wherever it is
	 *
	 * @param fc
	 * @param existingTag
	 * @throws IOException
	 */
	@Throws(CannotWriteException::class, IOException::class)
	fun deleteOrTruncateInfoTag(fc: FileChannel, existingTag: WavTag) {
		val infoChunkHeader = seekToStartOfListInfoMetadata(fc, existingTag)
		if (isInfoTagAtEndOfFileAllowingForPaddingByte(existingTag, fc)) {
			fc.truncate(existingTag.infoTag!!.startLocationInFile!!)
		} else {
			deleteInfoTagChunk(fc, existingTag, infoChunkHeader)
		}
	}

	/**
	 * Write Id3 chunk at end of file
	 *
	 * @param fc
	 * @param id3TagBuffer
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun writeId3ChunkAtFileEnd(fc: FileChannel, id3TagBuffer: ByteBuffer) {
		fc.position(fc.size())
		writeId3DataToFile(fc, id3TagBuffer)
	}

	/**
	 * Delete Existing Id3 chunk wherever it is , then write Id3 chunk at end of file
	 *
	 * @param fc
	 * @param existingTag
	 * @param id3ChunkHeader
	 * @param id3TagBuffer
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun deleteId3ChunkAndCreateNewOneAtFileEnd(
		fc: FileChannel,
		existingTag: WavTag?,
		id3ChunkHeader: ChunkHeader,
		id3TagBuffer: ByteBuffer
	) {
		deleteId3TagChunk(fc, existingTag, id3ChunkHeader)
		fc.position(fc.size())
		writeId3DataToFile(fc, id3TagBuffer)
	}

	/**
	 * Save Active chunk only, if a non-active metadata chunk exists will be removed
	 *
	 * @param wavTag
	 * @param fc
	 * @param existingTag
	 * @throws CannotWriteException
	 * @throws IOException
	 */
	@Throws(CannotWriteException::class, IOException::class)
	private fun saveActive(wavTag: WavTag?, fc: FileChannel, existingTag: WavTag?) {
		//Info is Active Tag
		if (wavTag!!.activeTag is WavInfoTag) {
			saveInfo(wavTag, fc, existingTag)
		} else {
			saveId3(wavTag, fc, existingTag)
		}
	}

	/**
	 * Save Active chunk and existing chunks even if not the active chunk
	 *
	 * @param wavTag
	 * @param fc
	 * @param existingTag
	 * @throws CannotWriteException
	 * @throws IOException
	 */
	@Throws(CannotWriteException::class, IOException::class)
	private fun saveActiveExisting(wavTag: WavTag?, fc: FileChannel, existingTag: WavTag?) {
		if (wavTag!!.activeTag is WavInfoTag) {
			if (existingTag!!.isExistingId3Tag) {
				saveBoth(wavTag, fc, existingTag)
			} else {
				saveActive(wavTag, fc, existingTag)
			}
		} else {
			if (existingTag!!.isExistingInfoTag) {
				saveBoth(wavTag, fc, existingTag)
			} else {
				saveActive(wavTag, fc, existingTag)
			}
		}
	}

	/** If Info/ID3 Metadata tags are corrupted and only metadata tags later in the file then just truncate metadata tags and start again
	 *
	 * @param fc
	 * @param existingTag
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun deleteExistingMetadataTagsToEndOfFile(fc: FileChannel, existingTag: WavTag?) {
		val precedingChunk = getChunkBeforeFirstMetadataTag(
			existingTag!!
		)
		//Preceding chunk ends on odd boundary
		if (!isOddLength(
				precedingChunk!!.endLocation
			)
		) {
			fc.truncate(precedingChunk.endLocation)
		} else {
			fc.truncate(precedingChunk.endLocation + 1)
		}
	}

	companion object {
		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.audio.wav")
	}
}
