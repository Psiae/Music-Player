package com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff

import com.flammky.android.core.sdk.VersionHelper
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.chunk.AiffChunkSummary.getChunkBeforeStartingMetadataTag
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.chunk.AiffChunkSummary.isOnlyMetadataTagsAfterStartingMetadataTag
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.chunk.AiffChunkType
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotWriteException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.NoWritePermissionsException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.IffHeaderChunk
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.Hex
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagOptionSingleton
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.aiff.AiffTag
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.AccessDeniedException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.logging.Logger

/**
 * Write Aiff Tag.
 */
class AiffTagWriter {
	/**
	 * Read existing metadata
	 *
	 * @param file
	 * @return tags within Tag wrapper
	 * @throws IOException
	 * @throws CannotWriteException
	 */
	@Throws(IOException::class, CannotWriteException::class)
	private fun getExistingMetadata(file: Path): AiffTag? {
		return try {
			//Find AiffTag (if any)
			val im = AiffTagReader(file.toString())
			im.read(file)
		} catch (ex: CannotReadException) {
			throw CannotWriteException(
				"$file Failed to read file"
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
	private fun seekToStartOfMetadata(
		fc: FileChannel,
		existingTag: AiffTag?,
		fileName: String
	): ChunkHeader {
		fc.position(existingTag!!.startLocationInFileOfId3Chunk)
		val chunkHeader = ChunkHeader(ByteOrder.BIG_ENDIAN)
		chunkHeader.readHeader(fc)
		fc.position(fc.position() - ChunkHeader.CHUNK_HEADER_SIZE)
		if (AiffChunkType.TAG.code != chunkHeader.id) {
			throw CannotWriteException(fileName + ":Unable to find ID3 chunk at expected location:" + existingTag.startLocationInFileOfId3Chunk)
		}
		return chunkHeader
	}

	/**
	 *
	 * @param existingTag
	 * @param fc
	 * @return true if at end of file (also take into account padding byte), also allows for the header size being
	 * reported in ID3 tag is larger than the boundary returned by the FORM header
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun isAtEndOfFileAllowingForPaddingByte(
		existingTag: AiffTag?,
		fc: FileChannel
	): Boolean {
		return (existingTag!!.iD3Tag!!.endLocationInFile!! >= fc.size()
			||
			(Utils.isOddLength(
				existingTag.iD3Tag!!.endLocationInFile!!
			)
				&&
				existingTag.iD3Tag!!.endLocationInFile!! + 1 == fc.size()))
	}

	/**
	 * Delete given [Tag] from file.
	 *
	 * @param tag tag, must be instance of [AiffTag]
	 * @param file
	 * @throws IOException
	 * @throws CannotWriteException
	 */
	@Throws(CannotWriteException::class, UnsupportedOperationException::class)
	fun delete(tag: Tag, file: Path) {
		if (!VersionHelper.hasOreo()) throw UnsupportedOperationException()

		try {
			FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.READ).use { fc ->
				logger.severe(
					"$file:Deleting tag from file"
				)
				val existingTag = getExistingMetadata(file)
				if (existingTag!!.isExistingId3Tag && existingTag.iD3Tag!!.startLocationInFile != null) {
					val chunkHeader = seekToStartOfMetadata(fc, existingTag, file.toString())
					if (isAtEndOfFileAllowingForPaddingByte(existingTag, fc)) {
						logger.config(file.toString() + ":Setting new length to:" + existingTag.startLocationInFileOfId3Chunk)
						fc.truncate(existingTag.startLocationInFileOfId3Chunk)
					} else {
						logger.config("$file:Deleting tag chunk")
						deleteTagChunk(fc, existingTag, chunkHeader, file.toString())
					}
					rewriteRiffHeaderSize(fc)
				}
				logger.config(
					"$file:Deleted tag from file"
				)
			}
		} catch (ioe: IOException) {
			throw CannotWriteException(file.toString() + ":" + ioe.message)
		}
	}

	/**
	 *
	 * Deletes the given ID3-[Tag]/[Chunk] from the file by moving all following chunks up.
	 * <pre>
	 * [chunk][-id3-][chunk][chunk]
	 * [chunk] &lt;&lt;--- [chunk][chunk]
	 * [chunk][chunk][chunk]
	</pre> *
	 *
	 * @param fc, filechannel
	 * @param existingTag existing tag
	 * @param tagChunkHeader existing chunk header for the tag
	 * @throws IOException if something goes wrong
	 */
	@Throws(IOException::class)
	private fun deleteTagChunk(
		fc: FileChannel,
		existingTag: AiffTag?,
		tagChunkHeader: ChunkHeader,
		fileName: String
	) {
		var lengthTagChunk = tagChunkHeader.size.toInt() + ChunkHeader.CHUNK_HEADER_SIZE
		if (Utils.isOddLength(lengthTagChunk.toLong())) {
			if (existingTag!!.startLocationInFileOfId3Chunk + lengthTagChunk < fc.size()) {
				lengthTagChunk++
			}
		}
		val newLength = fc.size() - lengthTagChunk
		logger.config(
			fileName
				+ ":Size of id3 chunk to delete is:" + Hex.asDecAndHex(lengthTagChunk.toLong())
				+ ":Location:" + Hex.asDecAndHex(
				existingTag!!.startLocationInFileOfId3Chunk
			)
		)

		//Position for reading after the id3 tag
		fc.position(existingTag.startLocationInFileOfId3Chunk + lengthTagChunk)
		logger.severe(fileName + ":Moved location to:" + Hex.asDecAndHex(newLength))
		deleteTagChunkUsingSmallByteBufferSegments(
			existingTag,
			fc,
			newLength,
			lengthTagChunk.toLong()
		)

		//Truncate the file after the last chunk
		logger.config(fileName + ":Setting new length to:" + Hex.asDecAndHex(newLength))
		fc.truncate(newLength)
	}

	/** If Metadata tags are corrupted and no other tags later in the file then just truncate ID3 tags and start again
	 *
	 * @param fc
	 * @param existingTag
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun deleteRemainderOfFile(fc: FileChannel, existingTag: AiffTag?, fileName: String) {
		val precedingChunk = getChunkBeforeStartingMetadataTag(
			existingTag!!
		)
		if (!Utils.isOddLength(
				precedingChunk!!.endLocation
			)
		) {
			logger.config(fileName + ":Truncating corrupted ID3 tags from:" + (existingTag.startLocationInFileOfId3Chunk - 1))
			fc.truncate(existingTag.startLocationInFileOfId3Chunk - 1)
		} else {
			logger.config(fileName + ":Truncating corrupted ID3 tags from:" + existingTag.startLocationInFileOfId3Chunk)
			fc.truncate(existingTag.startLocationInFileOfId3Chunk)
		}
	}

	/**
	 * Use ByteBuffers to copy a chunk, write the chunk and repeat until the rest of the file after the ID3 tag
	 * is rewritten
	 *
	 * @param existingTag existing tag
	 * @param channel channel
	 * @param newLength new length
	 * @param lengthTagChunk length tag chunk
	 * @throws IOException if something goes wrong
	 */
	// TODO: arguments are not used, position is implicit
	@Throws(IOException::class)
	private fun deleteTagChunkUsingSmallByteBufferSegments(
		existingTag: AiffTag?,
		channel: FileChannel,
		newLength: Long,
		lengthTagChunk: Long
	) {
		val buffer = ByteBuffer.allocateDirect(TagOptionSingleton.instance.writeChunkSize.toInt())
		while (channel.read(buffer) >= 0 || buffer.position() != 0) {
			buffer.flip()
			val readPosition = channel.position()
			channel.position(readPosition - lengthTagChunk - buffer.limit())
			channel.write(buffer)
			channel.position(readPosition)
			buffer.compact()
		}
	}

	/**
	 *
	 * @param tag
	 * @param file
	 * @throws CannotWriteException
	 * @throws IOException
	 */
	@Throws(CannotWriteException::class, UnsupportedOperationException::class)
	fun write(tag: Tag, file: Path) {
		if (!VersionHelper.hasOreo()) throw UnsupportedOperationException()

		logger.severe(
			"$file:Writing Aiff tag to file"
		)
		var existingTag: AiffTag? = null
		existingTag = try {
			getExistingMetadata(file)
		} catch (ioe: IOException) {
			throw CannotWriteException(file.toString() + ":" + ioe.message)
		}
		try {
			FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.READ).use { fc ->
				//Issue 227:HDtracks issue, if crap at end of file after length according to FORM then delete it
				val formFileLength = existingTag!!.formSize + ChunkHeader.CHUNK_HEADER_SIZE
				val currentPos = fc.position()
				if (formFileLength < fc.size() && !existingTag.isLastChunkSizeExtendsPastFormSize) {
					logger.warning(file.toString() + ":Extra Non Chunk Data after end of FORM data length:" + (fc.size() - formFileLength))
					fc.position(formFileLength)
					fc.truncate(formFileLength)
					fc.position(currentPos)
				}
				val aiffTag = tag as AiffTag
				val bb = convert(aiffTag, existingTag)

				//Replacing ID3 tag
				if (existingTag.isExistingId3Tag && existingTag.iD3Tag!!.startLocationInFile != null) {
					//Usual case
					if (!existingTag.isIncorrectlyAlignedTag) {
						val chunkHeader = seekToStartOfMetadata(fc, existingTag, file.toString())
						logger.config(file.toString() + ":Current Space allocated:" + existingTag.sizeOfID3TagOnly + ":NewTagRequires:" + bb.limit())

						//Usual case ID3 is last chunk
						if (isAtEndOfFileAllowingForPaddingByte(existingTag, fc)) {
							writeDataToFile(fc, bb)
						} else {
							deleteTagChunk(fc, existingTag, chunkHeader, file.toString())
							fc.position(fc.size())
							writeExtraByteIfChunkOddSize(fc, fc.size())
							writeDataToFile(fc, bb)
						}
					} else if (isOnlyMetadataTagsAfterStartingMetadataTag(
							existingTag
						)
					) {
						deleteRemainderOfFile(fc, existingTag, file.toString())
						fc.position(fc.size())
						writeExtraByteIfChunkOddSize(fc, fc.size())
						writeDataToFile(fc, bb)
					} else {
						throw CannotWriteException(
							"$file:Metadata tags are corrupted and not at end of file so cannot be fixed"
						)
					}
				} else {
					fc.position(fc.size())
					if (Utils.isOddLength(fc.size())) {
						fc.write(ByteBuffer.allocateDirect(1))
					}
					writeDataToFile(fc, bb)
				}

				//Always rewrite header
				rewriteRiffHeaderSize(fc)
			}
		} catch (ade: AccessDeniedException) {
			throw NoWritePermissionsException(file.toString() + ":" + ade.message)
		} catch (ioe: IOException) {
			throw CannotWriteException(file.toString() + ":" + ioe.message)
		}
	}

	/**
	 * Rewrite FORM header to reflect new file length
	 *
	 * @param fc
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun rewriteRiffHeaderSize(fc: FileChannel) {
		fc.position(IffHeaderChunk.SIGNATURE_LENGTH.toLong())
		val bb = ByteBuffer.allocateDirect(IffHeaderChunk.SIZE_LENGTH)
		bb.order(ByteOrder.BIG_ENDIAN)
		val size = fc.size().toInt() - ChunkHeader.CHUNK_HEADER_SIZE
		bb.putInt(size)
		bb.flip()
		fc.write(bb)
	}

	/**
	 * Writes data as a [AiffChunkType.TAG] chunk to the file.
	 *
	 * @param fc filechannel
	 * @param bb data to write
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun writeDataToFile(fc: FileChannel, bb: ByteBuffer) {
		val ch = ChunkHeader(ByteOrder.BIG_ENDIAN)
		ch.id =
			AiffChunkType.TAG.code
		ch.size = bb.limit().toLong()
		fc.write(ch.writeHeader())
		fc.write(bb)
		writeExtraByteIfChunkOddSize(fc, bb.limit().toLong())
	}

	/**
	 * Chunk must also start on an even byte so if our chunksize is odd we need
	 * to write another byte. This should never happen as ID3Tag is now amended
	 * to ensure always write padding byte if needed to stop it being odd sized
	 * but we keep check in just incase.
	 *
	 * @param fc
	 * @param size
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun writeExtraByteIfChunkOddSize(fc: FileChannel, size: Long) {
		if (Utils.isOddLength(size)) {
			fc.write(ByteBuffer.allocateDirect(1))
		}
	}

	/**
	 * Converts tag to [ByteBuffer].
	 *
	 * @param tag tag
	 * @param existingTag
	 * @return byte buffer containing the tag data
	 * @throws UnsupportedEncodingException
	 */
	@Throws(UnsupportedEncodingException::class)
	fun convert(tag: AiffTag, existingTag: AiffTag?): ByteBuffer {
		return try {
			var baos = ByteArrayOutputStream()
			var existingTagSize = existingTag!!.sizeOfID3TagOnly

			//If existingTag is uneven size lets make it even
			if (existingTagSize > 0) {
				if (Utils.isOddLength(existingTagSize)) {
					existingTagSize++
				}
			}

			//Write Tag to buffer
			tag.iD3Tag!!.write(baos, existingTagSize.toInt())

			//If the tag is now odd because we needed to increase size and the data made it odd sized
			//we redo adding a padding byte to make it even
			if (Utils.isOddLength(baos.toByteArray().size.toLong())) {
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

	companion object {
		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.audio.aiff")
	}
}
