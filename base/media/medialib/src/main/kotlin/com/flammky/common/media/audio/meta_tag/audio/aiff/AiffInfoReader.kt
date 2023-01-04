package com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff

import com.flammky.android.core.sdk.VersionHelper
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.SupportedFileFormat
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.chunk.*
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.GenericAudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.Chunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.IffHeaderChunk
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.Hex
import java.io.IOException
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.math.roundToInt

/**
 * Read Aiff chunks, except the ID3 chunk.
 */
class AiffInfoReader(private val loggingName: String) : AiffChunkReader() {
	@Throws(CannotReadException::class, IOException::class)

	fun read(file: Path): GenericAudioHeader {
		if (!VersionHelper.hasOreo()) throw CannotReadException()

		FileChannel.open(file).use { fc ->
			logger.config(
				loggingName + ":Reading AIFF file size:" + Hex.asDecAndHex(fc.size())
			)
			val info = AiffAudioHeader()
			val fileHeader = AiffFileHeader(
				loggingName
			)
			val noOfBytes = fileHeader.readHeader(fc, info)
			while (fc.position() < noOfBytes + ChunkHeader.CHUNK_HEADER_SIZE && fc.position() < fc.size()) {
				val result = readChunk(fc, info)
				if (!result) {
					logger.severe(
						"$file:UnableToReadProcessChunk"
					)
					break
				}
			}
			if (info.fileType == AiffType.AIFC) {
				info.format = SupportedFileFormat.AIF.displayName
			} else {
				info.format = SupportedFileFormat.AIF.displayName
			}
			calculateBitRate(info)
			return info
		}
	}

	fun read(fc: FileChannel): GenericAudioHeader {
		val audioHeader = AiffAudioHeader()
		val fileHeader = AiffFileHeader(loggingName)
		val noOfBytes = fileHeader.readHeader(fc, audioHeader)
		while (fc.position() < noOfBytes + ChunkHeader.CHUNK_HEADER_SIZE && fc.position() < fc.size()) {
			val result = readChunk(fc, audioHeader)
			if (!result) {
				break
			}
		}
		if (audioHeader.fileType == AiffType.AIFC) {
			audioHeader.format = SupportedFileFormat.AIF.displayName
		} else {
			audioHeader.format = SupportedFileFormat.AIF.displayName
		}
		calculateBitRate(audioHeader)
		return audioHeader
	}

	/**
	 * Calculate bitrate, done it here because requires data from multiple chunks
	 *
	 * @param info
	 * @throws CannotReadException
	 */
	@Throws(CannotReadException::class)
	private fun calculateBitRate(info: GenericAudioHeader) {
		if (info.audioDataLength != null) {
			info.setBitRate(
				(info.audioDataLength!! * Utils.BITS_IN_BYTE_MULTIPLIER
					/ (info.preciseTrackLength!! * Utils.KILOBYTE_MULTIPLIER)).roundToInt()
			)
		}
	}

	/**
	 * Reads an AIFF Chunk.
	 *
	 * @return `false`, if we were not able to read a valid chunk id
	 */
	@Throws(IOException::class, CannotReadException::class)
	private fun readChunk(fc: FileChannel, aiffAudioHeader: AiffAudioHeader): Boolean {
		val chunk: Chunk?
		val chunkHeader = ChunkHeader(ByteOrder.BIG_ENDIAN)
		if (!chunkHeader.readHeader(fc)) {
			return false
		}
		logger.config(
			loggingName + ":Reading Next Chunk:" + chunkHeader.id
				+ ":starting at:" + Hex.asDecAndHex(chunkHeader.startLocationInFile)
				+ ":sizeIncHeader:" + Hex.asDecAndHex(
				chunkHeader.size + ChunkHeader.CHUNK_HEADER_SIZE
			)
				+ ":ending at:" + Hex.asDecAndHex(chunkHeader.startLocationInFile + chunkHeader.size + ChunkHeader.CHUNK_HEADER_SIZE)
		)
		chunk = createChunk(fc, chunkHeader, aiffAudioHeader)
		if (chunk != null) {
			if (!chunk.readChunk()) {
				logger.severe(
					loggingName + ":ChunkReadFail:" + chunkHeader.id
				)
				return false
			}
		} else {
			if (chunkHeader.size <= 0) {
				val msg =
					(loggingName + ":Not a valid header, unable to read a sensible size:Header"
						+ chunkHeader.id + "Size:" + chunkHeader.size)
				logger.severe(msg)
				throw CannotReadException(msg)
			}
			fc.position(fc.position() + chunkHeader.size)
		}
		IffHeaderChunk.ensureOnEqualBoundary(fc, chunkHeader)
		return true
	}

	/**
	 * Create a chunk. May return `null`, if the chunk is not of a valid type.
	 *
	 * @param fc
	 * @param chunkHeader
	 * @param aiffAudioHeader
	 * @return
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun createChunk(
		fc: FileChannel,
		chunkHeader: ChunkHeader,
		aiffAudioHeader: AiffAudioHeader
	): Chunk? {
		val chunkType = AiffChunkType[chunkHeader.id!!]
		val chunk: Chunk?
		if (chunkType != null) {
			when (chunkType) {
				AiffChunkType.FORMAT_VERSION -> chunk = FormatVersionChunk(
					chunkHeader,
					readChunkDataIntoBuffer(fc, chunkHeader),
					aiffAudioHeader
				)
				AiffChunkType.APPLICATION -> chunk = ApplicationChunk(
					chunkHeader,
					readChunkDataIntoBuffer(fc, chunkHeader),
					aiffAudioHeader
				)
				AiffChunkType.COMMON -> chunk = CommonChunk(
					chunkHeader,
					readChunkDataIntoBuffer(fc, chunkHeader),
					aiffAudioHeader
				)
				AiffChunkType.COMMENTS -> chunk = CommentsChunk(
					chunkHeader,
					readChunkDataIntoBuffer(fc, chunkHeader),
					aiffAudioHeader
				)
				AiffChunkType.NAME -> chunk = NameChunk(
					chunkHeader,
					readChunkDataIntoBuffer(fc, chunkHeader),
					aiffAudioHeader
				)
				AiffChunkType.AUTHOR -> chunk = AuthorChunk(
					chunkHeader,
					readChunkDataIntoBuffer(fc, chunkHeader),
					aiffAudioHeader
				)
				AiffChunkType.COPYRIGHT -> chunk = CopyrightChunk(
					chunkHeader,
					readChunkDataIntoBuffer(fc, chunkHeader),
					aiffAudioHeader
				)
				AiffChunkType.ANNOTATION -> chunk = AnnotationChunk(
					chunkHeader,
					readChunkDataIntoBuffer(fc, chunkHeader),
					aiffAudioHeader
				)
				AiffChunkType.SOUND -> {
					//Dont need to read chunk itself just need size
					aiffAudioHeader.audioDataLength = chunkHeader.size
					aiffAudioHeader.audioDataStartPosition = fc.position()
					aiffAudioHeader.audioDataEndPosition = fc.position() + chunkHeader.size
					chunk = null
				}
				else -> chunk = null
			}
		} else {
			chunk = null
		}
		return chunk
	}

	companion object {
		var logger = Logger.getLogger("org.jaudiotagger.audio.aiff")
	}
}
