package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getSizeBEInt32
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.Mp4AtomIdentifier
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * FreeBox ( padding)
 *
 *
 * There are usually two free boxes, one beneath the meta atom and one toplevel atom
 */
class Mp4FreeBox(datasize: Int) : AbstractMp4Box() {
	/**
	 * Construct a new FreeBox containing datasize padding (i.e doesnt include header size)
	 *
	 * @param datasize padding size
	 */
	init {
		try {
			//Header
			header = Mp4BoxHeader()
			val headerBaos = ByteArrayOutputStream()
			headerBaos.write(getSizeBEInt32(Mp4BoxHeader.Companion.HEADER_LENGTH + datasize))
			headerBaos.write(Mp4AtomIdentifier.FREE.fieldName.toByteArray(StandardCharsets.ISO_8859_1))
			header!!.update(ByteBuffer.wrap(headerBaos.toByteArray()))

			//Body
			val freeBaos = ByteArrayOutputStream()
			for (i in 0 until datasize) {
				freeBaos.write(0x0)
			}
			data = ByteBuffer.wrap(freeBaos.toByteArray())
		} catch (ioe: IOException) {
			//This should never happen as were not actually writing to/from a file
			throw RuntimeException(ioe)
		}
	}
}
