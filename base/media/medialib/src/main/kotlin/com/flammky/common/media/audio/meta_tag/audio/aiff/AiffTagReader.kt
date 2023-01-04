package com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff

import com.flammky.android.core.sdk.VersionHelper
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.chunk.AiffChunkReader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.chunk.AiffChunkType
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.chunk.ID3Chunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.Chunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkSummary
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.IffHeaderChunk
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.Hex
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.aiff.AiffTag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.aiff.AiffTag.Companion.createDefaultID3Tag
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.util.logging.Logger

/**
 * Read the AIff file chunks, until finds Aiff Common chunk and then generates AudioHeader from it
 */
class AiffTagReader(private val loggingName: String) : AiffChunkReader() {
	/**
	 * Read editable Metadata
	 *
	 * @param file
	 * @return
	 * @throws CannotReadException
	 * @throws IOException
	 */
	@Throws(CannotReadException::class, IOException::class, UnsupportedOperationException::class)
	fun read(file: Path): AiffTag {
		return if (VersionHelper.hasOreo()) {
			FileChannel.open(file).use { read(it)	}
		} else {
			RandomAccessFile(file.toFile(), "r").use { read(it.channel) }
		}
	}

	fun read(fc: FileChannel): AiffTag {
		val aiffAudioHeader = AiffAudioHeader()
		val aiffTag = AiffTag()
		val fileHeader = AiffFileHeader(fc.toString())
		val overallChunkSize = fileHeader.readHeader(fc, aiffAudioHeader)
		aiffTag.formSize = overallChunkSize
		aiffTag.fileSize = fc.size()
		val endLocationOfAiffData = overallChunkSize + ChunkHeader.CHUNK_HEADER_SIZE
		while (fc.position() < endLocationOfAiffData && fc.position() < fc.size()) {
			if (!readChunk(fc, aiffTag)) {
				break
			}
		}
		if (aiffTag.iD3Tag == null) {
			aiffTag.iD3Tag = createDefaultID3Tag()
		}
		if (fc.position() > endLocationOfAiffData) {
			aiffTag.isLastChunkSizeExtendsPastFormSize = true
		}
		return aiffTag
	}

	/**
	 * Reads an AIFF ID3 Chunk.
	 *
	 * @return `false`, if we were not able to read a valid chunk id
	 */
	@Throws(IOException::class)
	private fun readChunk(fc: FileChannel, aiffTag: AiffTag): Boolean {
		val chunkHeader = ChunkHeader(ByteOrder.BIG_ENDIAN)
		if (!chunkHeader.readHeader(fc)) {
			return false
		}
		logger.config(
			loggingName + ":Reading Chunk:" + chunkHeader.id + ":starting at:"
				+ Hex.asDecAndHex(chunkHeader.startLocationInFile)
				+ ":sizeIncHeader:" + (chunkHeader.size + ChunkHeader.CHUNK_HEADER_SIZE)
		)
		val startLocationOfId3TagInFile = fc.position()
		val chunkType = AiffChunkType[chunkHeader.id!!]
		if (chunkType != null && chunkType === AiffChunkType.TAG && chunkHeader.size > 0) {
			val chunkData = readChunkDataIntoBuffer(fc, chunkHeader)
			aiffTag.addChunkSummary(
				ChunkSummary(
					chunkHeader.id!!,
					chunkHeader.startLocationInFile,
					chunkHeader.size
				)
			)

			//If we havent already for an ID3 Tag
			if (aiffTag.iD3Tag == null) {
				val chunk: Chunk = ID3Chunk(chunkHeader, chunkData, aiffTag, loggingName)
				chunk.readChunk()
				aiffTag.isExistingId3Tag = true
				aiffTag.iD3Tag!!.setStartLocationInFile(startLocationOfId3TagInFile)
				aiffTag.iD3Tag!!.setEndLocationInFile(fc.position())
			} else {
				logger.warning(
					loggingName + ":Ignoring ID3Tag because already have one:"
						+ chunkHeader.id + ":"
						+ chunkHeader.startLocationInFile + ":"
						+ Hex.asDecAndHex(chunkHeader.startLocationInFile - 1)
						+ ":sizeIncHeader:" + (chunkHeader.size + ChunkHeader.CHUNK_HEADER_SIZE)
				)
			}
		} else if (chunkType != null && chunkType === AiffChunkType.CORRUPT_TAG_LATE) {
			logger.warning(
				loggingName + ":Found Corrupt ID3 Chunk, starting at Odd Location:" + chunkHeader.id + ":"
					+ Hex.asDecAndHex(chunkHeader.startLocationInFile - 1)
					+ ":sizeIncHeader:" + (chunkHeader.size + ChunkHeader.CHUNK_HEADER_SIZE)
			)

			//We only want to know if first metadata tag is misaligned
			if (aiffTag.iD3Tag == null) {
				aiffTag.isIncorrectlyAlignedTag = true
			}
			fc.position(fc.position() - (ChunkHeader.CHUNK_HEADER_SIZE + 1))
			return true
		} else if (chunkType != null && chunkType === AiffChunkType.CORRUPT_TAG_EARLY) {
			logger.warning(
				loggingName + ":Found Corrupt ID3 Chunk, starting at Odd Location:" + chunkHeader.id
					+ ":" + Hex.asDecAndHex(chunkHeader.startLocationInFile)
					+ ":sizeIncHeader:" + (chunkHeader.size + ChunkHeader.CHUNK_HEADER_SIZE)
			)

			//We only want to know if first metadata tag is misaligned
			if (aiffTag.iD3Tag == null) {
				aiffTag.isIncorrectlyAlignedTag = true
			}
			fc.position(fc.position() - (ChunkHeader.CHUNK_HEADER_SIZE - 1))
			return true
		} else {
			logger.config(
				loggingName + ":Skipping Chunk:" + chunkHeader.id + ":" + chunkHeader.size
			)
			aiffTag.addChunkSummary(
				ChunkSummary(
					chunkHeader.id!!,
					chunkHeader.startLocationInFile,
					chunkHeader.size
				)
			)
			fc.position(fc.position() + chunkHeader.size)
		}
		IffHeaderChunk.ensureOnEqualBoundary(fc, chunkHeader)
		return true
	}

	companion object {
		var logger = Logger.getLogger("org.jaudiotagger.audio.aiff")
	}
}
