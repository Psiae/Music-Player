package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import java.nio.ByteBuffer

/**
 * DrmsBox Replaces mp4a box on drm files
 *
 * Need to skip over data in order to find esds atom
 *
 * Specification not known, so just look for byte by byte 'esds' and then step back four bytes for size
 */
class Mp4DrmsBox(header: Mp4BoxHeader?, dataBuffer: ByteBuffer) : AbstractMp4Box() {
	/**
	 * @param header     header info
	 * @param dataBuffer data of box (doesnt include header data)
	 */
	init {
		this.header = header
		this.data = dataBuffer
	}

	/**
	 * Process direct data
	 *
	 * @throws CannotReadException
	 */
	@Throws(CannotReadException::class)
	fun processData() {
		val dataBuffer = data!!
		while (dataBuffer.hasRemaining()) {
			val next = dataBuffer.get()
			if (next != 'e'.code.toByte()) {
				continue
			}

			//Have we found esds identifier, if so adjust buffer to start of esds atom
			val tempBuffer = dataBuffer.slice()
			if ((tempBuffer.get() == 's'.code.toByte()) and (tempBuffer.get() == 'd'.code.toByte()) and (tempBuffer.get() == 's'.code.toByte())) {
				dataBuffer.position(dataBuffer.position() - 1 - Mp4BoxHeader.Companion.OFFSET_LENGTH)
				return
			}
		}
	}
}
