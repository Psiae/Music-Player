package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import java.nio.ByteBuffer

/**
 * Mp4aBox ( sample (frame encoding) description box)
 *
 * At first glance appears to hold no of channels but actually always returns 2 even for mono recordings
 * so just need to skip over data in order to get to child atom esds
 *
 *
 * 4 bytes version/flags = byte hex version + 24-bit hex flags
 * (current = 0)
 *
 * 6 bytes reserved = 48-bit value set to zero
 * 2 bytes data reference index
 * = short unsigned index from 'dref' box
 * 2 bytes QUICKTIME audio encoding version = short hex version
 * - default = 0 ; audio data size before decompression = 1
 * 2 bytes QUICKTIME audio encoding revision level
 * = byte hex version
 * - default = 0 ; video can revise this value
 * 4 bytes QUICKTIME audio encoding vendor
 * = long ASCII text string
 * - default = 0
 * 2 bytes audio channels = short unsigned count
 * (mono = 1 ; stereo = 2)
 * 2 bytes audio sample size = short unsigned value
 * (8 or 16)
 * 2 bytes QUICKTIME audio compression id = short integer value
 * - default = 0
 * 2 bytes QUICKTIME audio packet size = short value set to zero
 * 4 bytes audio sample rate = long unsigned fixed point rate
 */
class Mp4Mp4aBox(header: Mp4BoxHeader?, dataBuffer: ByteBuffer) : AbstractMp4Box() {
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
		dataBuffer.position(dataBuffer.position() + TOTAL_LENGTH)
	}

	companion object {
		const val RESERVED_POS = 0
		const val REFERENCE_INDEX_POS = 6
		const val AUDIO_ENCODING_POS = 8
		const val AUDIO_REVISION_POS = 10
		const val AUDIO_ENCODING_VENDOR_POS = 12
		const val CHANNELS_POS = 16
		const val AUDIO_SAMPLE_SIZE_POS = 18
		const val AUDIO_COMPRESSION_ID_POS = 20
		const val AUDIO_PACKET_SIZE_POS = 22
		const val AUDIO_SAMPLE_RATE_POS = 24
		const val RESERVED_LENGTH = 6
		const val REFERENCE_INDEX_LENGTH = 2
		const val AUDIO_ENCODING_LENGTH = 2
		const val AUDIO_REVISION_LENGTH = 2
		const val AUDIO_ENCODING_VENDOR_LENGTH = 4
		const val CHANNELS_LENGTH = 2
		const val AUDIO_SAMPLE_SIZE_LENGTH = 2
		const val AUDIO_COMPRESSION_ID_LENGTH = 2
		const val AUDIO_PACKET_SIZE_LENGTH = 2
		const val AUDIO_SAMPLE_RATE_LENGTH = 4
		const val TOTAL_LENGTH =
			RESERVED_LENGTH + REFERENCE_INDEX_LENGTH + AUDIO_ENCODING_LENGTH + AUDIO_REVISION_LENGTH + AUDIO_ENCODING_VENDOR_LENGTH + CHANNELS_LENGTH + AUDIO_SAMPLE_SIZE_LENGTH + AUDIO_COMPRESSION_ID_LENGTH + AUDIO_PACKET_SIZE_LENGTH + AUDIO_SAMPLE_RATE_LENGTH
	}
}
