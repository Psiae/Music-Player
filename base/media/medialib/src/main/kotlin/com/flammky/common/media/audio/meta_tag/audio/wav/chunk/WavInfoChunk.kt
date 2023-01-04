package com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav.chunk

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getString
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.isOddLength
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.readFourBytesAsChars
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.IffHeaderChunk
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.FieldDataInvalidException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.wav.WavInfoTag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.wav.WavTag
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Stores basic only metadata but only exists as part of a LIST chunk, doesn't have its own size field
 * instead contains a number of name,size, value tuples. So for this reason we do not subclass the Chunk class
 */
class WavInfoChunk(tag: WavTag, private val loggingName: String) {

	private val wavInfoTag: WavInfoTag = WavInfoTag()

	init {
		tag.infoTag = wavInfoTag
	}

	/**
	 * Read Info chunk
	 * @param chunkData
	 */
	fun readChunks(chunkData: ByteBuffer): Boolean {
		while (chunkData.remaining() >= IffHeaderChunk.TYPE_LENGTH) {
			val id = readFourBytesAsChars(chunkData)
			//Padding
			if (id.trim { it <= ' ' }.isEmpty()) {
				return true
			}
			val size = chunkData.int
			if (!Character.isAlphabetic(id[0].code) ||
				!Character.isAlphabetic(id[1].code) ||
				!Character.isAlphabetic(id[2].code) ||
				!Character.isAlphabetic(id[3].code)
			) {
				logger.severe(
					loggingName + "LISTINFO appears corrupt, ignoring:" + id + ":" + size
				)
				return false
			}
			var value: String? = null
			value = try {
				getString(chunkData, 0, size, StandardCharsets.UTF_8)
			} catch (bue: BufferUnderflowException) {
				logger.log(
					Level.SEVERE,
					loggingName + "LISTINFO appears corrupt, ignoring:" + bue.message,
					bue
				)
				return false
			}
			logger.config(
				loggingName + "Result:" + id + ":" + size + ":" + value + ":"
			)
			val wii: WavInfoIdentifier? = WavInfoIdentifier.Companion.getByCode(id)
			if (wii?.fieldKey != null) {
				try {
					wavInfoTag.setField(wii.fieldKey, value)
				} catch (fdie: FieldDataInvalidException) {
					logger.log(Level.SEVERE, loggingName + fdie.message, fdie)
				}
			} else if (id != null && id.trim { it <= ' ' }.isNotEmpty()) {
				wavInfoTag.addUnRecognizedField(id, value)
			}

			//Each tuple aligned on even byte boundary
			if (isOddLength(size.toLong()) && chunkData.hasRemaining()) {
				chunkData.get()
			}
		}
		return true
	}

	companion object {
		var logger = Logger.getLogger("org.jaudiotagger.audio.wav.WavInfoChunk")
	}
}
