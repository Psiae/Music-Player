package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import java.nio.ByteBuffer

/**
 * StsdBox ( sample (frame encoding) description box)
 *
 *
 * 4 bytes version/flags = byte hex version + 24-bit hex flags
 * (current = 0)
 * 4 bytes number of descriptions = long unsigned total
 * (default = 1)
 * Then if audio contains mp4a,alac or drms box
 */
class Mp4StsdBox(header: Mp4BoxHeader?, dataBuffer: ByteBuffer) : AbstractMp4Box() {
	/**
	 * @param header     header info
	 * @param dataBuffer data of box (doesnt include header data)
	 */
	init {
		this.header = header
		this.data = dataBuffer
	}

	@Throws(CannotReadException::class)
	fun processData() {
		val dataBuffer = data!!
		//Skip the data
		dataBuffer.position(dataBuffer.position() + VERSION_FLAG_LENGTH + OTHER_FLAG_LENGTH + NO_OF_DESCRIPTIONS_POS_LENGTH)
	}

	companion object {
		const val VERSION_FLAG_POS = 0
		const val OTHER_FLAG_POS = 1
		const val NO_OF_DESCRIPTIONS_POS = 4
		const val VERSION_FLAG_LENGTH = 1
		const val OTHER_FLAG_LENGTH = 3
		const val NO_OF_DESCRIPTIONS_POS_LENGTH = 4
	}
}
