package com.flammky.musicplayer.common.media.audio.meta_tag.audio.flac

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v2TagBase.Companion.isId3Tag
import java.io.IOException
import java.nio.channels.FileChannel
import java.util.logging.Logger

/**
 * Flac Stream
 *
 * Reader files and identifies if this is in fact a flac stream
 */
class FlacStreamReader
/**
 * Create instance for holding stream info
 * @param fc
 * @param loggingName
 */(private val fc: FileChannel, private val loggingName: String) {
	/**
	 * Usually flac header is at start of file, but unofficially an ID3 tag is allowed at the start of the file.
	 *
	 * @return the start of the Flac within file
	 */
	var startOfFlacInFile = 0
		private set

	/**
	 * Reads the stream block to ensure it is a flac file
	 *
	 * @throws IOException
	 * @throws CannotReadException
	 */
	@Throws(IOException::class, CannotReadException::class)
	fun findStream() {
		//Begins tag parsing
		if (fc.size() == 0L) {
			//Empty File
			throw CannotReadException(
				"Error: File empty $loggingName"
			)
		}
		fc.position(0)

		//FLAC Stream at start
		if (isFlacHeader) {
			startOfFlacInFile = 0
			return
		}

		//Ok maybe there is an ID3v24tag first
		if (isId3v2Tag) {
			startOfFlacInFile = (fc.position() - FLAC_STREAM_IDENTIFIER_LENGTH).toInt()
			return
		}
		throw CannotReadException(
			loggingName + ErrorMessage.FLAC_NO_FLAC_HEADER_FOUND.msg
		)
	}

	//FLAC Stream immediately after end of id3 tag
	@get:Throws(IOException::class)
	private val isId3v2Tag: Boolean
		private get() {
			fc.position(0)
			if (isId3Tag(
					fc
				)
			) {
				logger.warning(
					loggingName + ErrorMessage.FLAC_CONTAINS_ID3TAG.getMsg(
						fc.position()
					)
				)
				//FLAC Stream immediately after end of id3 tag
				if (isFlacHeader) {
					return true
				}
			}
			return false
		}

	@get:Throws(IOException::class)
	private val isFlacHeader: Boolean
		private get() {
			val headerBuffer = Utils.readFileDataIntoBufferBE(
				fc, FLAC_STREAM_IDENTIFIER_LENGTH
			)
			return Utils.readFourBytesAsChars(headerBuffer) == FLAC_STREAM_IDENTIFIER
		}

	companion object {
		var logger = Logger.getLogger("org.jaudiotagger.audio.flac")
		const val FLAC_STREAM_IDENTIFIER_LENGTH = 4
		const val FLAC_STREAM_IDENTIFIER = "fLaC"
	}
}
