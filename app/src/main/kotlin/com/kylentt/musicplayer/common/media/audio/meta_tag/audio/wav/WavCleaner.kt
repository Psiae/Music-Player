package com.kylentt.musicplayer.common.media.audio.meta_tag.audio.wav

import com.kylentt.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.kylentt.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkHeader
import com.kylentt.musicplayer.common.media.audio.meta_tag.audio.iff.IffHeaderChunk.ensureOnEqualBoundary
import com.kylentt.musicplayer.common.media.audio.meta_tag.logging.Hex
import com.kylentt.musicplayer.core.sdk.VersionHelper
import java.io.IOException
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.logging.Logger

/**
 * Experimental, reads the length of data chiunk and removes all data after that, useful for removing screwed up tags at end of file, but
 * use with care, not very robust.
 */
class WavCleaner(private val path: Path) {
	private var loggingName: String = ""

	init {
		if (VersionHelper.hasOreo()) loggingName = path.fileName.toString()
	}

	/**
	 * Delete all data after data chunk and print new length
	 *
	 * @throws Exception
	 */
	@Throws(Exception::class)
	fun clean() {
		println("EndOfDataChunk:" + Hex.asHex(findEndOfDataChunk()))
	}

	/**
	 * If find data chunk delete al data after it
	 *
	 * @return
	 * @throws Exception
	 */
	@Throws(Exception::class)
	private fun findEndOfDataChunk(): Int {
		if (!VersionHelper.hasOreo()) TODO("Implement API < 26")

		FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.READ).use { fc ->
			if (WavRIFFHeader.isValidHeader(loggingName, fc)) {
				while (fc.position() < fc.size()) {
					val endOfChunk = readChunk(fc)
					if (endOfChunk > 0) {
						fc.truncate(fc.position())
						return endOfChunk
					}
				}
			}
		}
		return 0
	}

	/**
	 *
	 * @param fc
	 * @return location of end of data chunk when chunk found
	 *
	 * @throws IOException
	 * @throws CannotReadException
	 */
	@Throws(IOException::class, CannotReadException::class)
	private fun readChunk(fc: FileChannel): Int {
		val chunkHeader = ChunkHeader(ByteOrder.LITTLE_ENDIAN)
		if (!chunkHeader.readHeader(fc)) {
			return 0
		}
		val id = chunkHeader.id
		logger.config(
			loggingName + " Reading Chunk:" + id
				+ ":starting at:" + Hex.asDecAndHex(chunkHeader.startLocationInFile)
				+ ":sizeIncHeader:" + (chunkHeader.size + ChunkHeader.CHUNK_HEADER_SIZE)
		)
		val chunkType: WavChunkType? = WavChunkType.Companion.get(id)

		//If known chunkType
		if (chunkType != null) {
			when (chunkType) {
				WavChunkType.DATA -> {
					fc.position(fc.position() + chunkHeader.size)
					return fc.position().toInt()
				}
				else -> {
					logger.config(loggingName + " Skipping chunk bytes:" + chunkHeader.size)
					fc.position(fc.position() + chunkHeader.size)
				}
			}
		} else {
			if (chunkHeader.size < 0) {
				val msg =
					(loggingName + " Not a valid header, unable to read a sensible size:Header"
						+ chunkHeader.id + "Size:" + chunkHeader.size)
				logger.severe(msg)
				throw CannotReadException(msg)
			}
			logger.severe(loggingName + " Skipping chunk bytes:" + chunkHeader.size + " for" + chunkHeader.id)
			fc.position(fc.position() + chunkHeader.size)
		}
		ensureOnEqualBoundary(fc, chunkHeader)
		return 0
	}

	companion object {
		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.audio.wav")

		@Throws(Exception::class)
		@JvmStatic
		fun main(args: Array<String>) {
			if (!VersionHelper.hasOreo()) TODO("Implement API < 26")
			val path = Paths.get("E:\\MQ\\Schubert, F\\The Last Six Years, vol 4-Imogen Cooper")
			recursiveDelete(path)
		}

		@Throws(Exception::class)
		private fun recursiveDelete(path: Path) {
			if (!VersionHelper.hasOreo()) TODO("Implement API < 26")

			for (next in path.toFile().listFiles()) {
				if (next.isFile && (next.name.endsWith(".WAV") || next.name.endsWith(".wav"))) {
					val wc = WavCleaner(next.toPath())
					wc.clean()
				} else if (next.isDirectory) {
					recursiveDelete(next.toPath())
				}
			}
		}
	}
}
