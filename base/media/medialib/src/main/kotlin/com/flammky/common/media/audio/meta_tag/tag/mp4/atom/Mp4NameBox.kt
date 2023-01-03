package com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.atom

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getString
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.AbstractMp4Box
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4BoxHeader
import java.nio.ByteBuffer

/**
 * This box is used within ---- boxes to hold the data name/descriptor
 */
class Mp4NameBox(header: Mp4BoxHeader, dataBuffer: ByteBuffer) : AbstractMp4Box() {
	val name: String

	/**
	 * @param header     parentHeader info
	 * @param dataBuffer data of box (doesnt include parentHeader data)
	 */
	init {
		this.header = header

		//Double check
		if (header.id != IDENTIFIER) {
			throw RuntimeException("Unable to process name box because identifier is:" + header.id)
		}

		//Make slice so operations here don't effect position of main buffer
		data = dataBuffer.slice()
		val data = data!!

		//issuer
		name = getString(data, PRE_DATA_LENGTH, header.dataLength - PRE_DATA_LENGTH, header.encoding)
	}

	companion object {
		const val IDENTIFIER = "name"

		//TODO Are these misnamed, are these version flag bytes or just null bytes
		const val VERSION_LENGTH = 1
		const val FLAGS_LENGTH = 3
		const val PRE_DATA_LENGTH = VERSION_LENGTH + FLAGS_LENGTH
	}
}
