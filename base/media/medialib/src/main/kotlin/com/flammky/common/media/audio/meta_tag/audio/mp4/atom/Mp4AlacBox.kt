package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.u
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * AlacBox ( Apple Lossless Codec information description box),
 *
 * Normally occurs twice, the first ALAC contains the default  values, the second ALAC within contains the real
 * values for this audio.
 */
class Mp4AlacBox(header: Mp4BoxHeader?, dataBuffer: ByteBuffer?) : AbstractMp4Box() {
	var maxSamplePerFrame // 32bit
		= 0
		private set
	var unknown1 // 8bit
		= 0
		private set
	var sampleSize // 8bit
		= 0
		private set
	var historyMult // 8bit
		= 0
		private set
	var initialHistory // 8bit
		= 0
		private set
	var kModifier // 8bit
		= 0
		private set
	var channels // 8bit
		= 0
		private set
	var unknown2 // 16bit
		= 0
		private set
	var maxCodedFrameSize // 32bit
		= 0
		private set
	var bitRate // 32bit
		= 0
		private set
	var sampleRate // 32bit
		= 0
		private set

	/**
	 * DataBuffer must start from from the start of the body
	 *
	 * @param header     header info
	 * @param dataBuffer data of box (doesnt include header data)
	 */
	init {
		this.header = header
		this.data = dataBuffer
	}

	@Throws(CannotReadException::class)
	fun processData() {
		//Skip version/other flags
		val data = data!!
		data.position(data.position() + OTHER_FLAG_LENGTH)
		data.order(ByteOrder.BIG_ENDIAN)
		maxSamplePerFrame = data.int
		unknown1 = u(data.get())
		sampleSize = u(data.get())
		historyMult = u(data.get())
		initialHistory = u(data.get())
		kModifier = u(data.get())
		channels = u(data.get())
		unknown2 = data.short.toInt()
		maxCodedFrameSize = data.int
		bitRate = data.int
		sampleRate = data.int
	}

	override fun toString(): String {
		return ("maxSamplePerFrame:" + maxSamplePerFrame
			+ "unknown1:" + unknown1
			+ "sampleSize:" + sampleSize
			+ "historyMult:" + historyMult
			+ "initialHistory:" + initialHistory
			+ "kModifier:" + kModifier
			+ "channels:" + channels
			+ "unknown2 :" + unknown2
			+ "maxCodedFrameSize:" + maxCodedFrameSize
			+ "bitRate:" + bitRate
			+ "sampleRate:" + sampleRate)
	}

	companion object {
		const val OTHER_FLAG_LENGTH = 4
	}
}
