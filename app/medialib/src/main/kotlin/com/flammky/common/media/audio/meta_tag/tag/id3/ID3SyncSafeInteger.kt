package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3

import java.nio.ByteBuffer

/**
 * Peforms encoding/decoding of an syncsafe integer
 *
 *
 * Syncsafe integers are used for the size in the tag header of v23 and v24 tags, and in the frame size in
 * the frame header of v24 frames.
 *
 *
 * In some parts of the tag it is inconvenient to use the
 * unsychronisation scheme because the size of unsynchronised data is
 * not known in advance, which is particularly problematic with size
 * descriptors. The solution in ID3v2 is to use synchsafe integers, in
 * which there can never be any false synchs. Synchsafe integers are
 * integers that keep its highest bit (bit 7) zeroed, making seven bits
 * out of eight available. Thus a 32 bit synchsafe integer can store 28
 * bits of information.
 *
 * Example:
 *
 * 255 (%11111111) encoded as a 16 bit synchsafe integer is 383
 * (%00000001 01111111).
 */
object ID3SyncSafeInteger {
	const val INTEGRAL_SIZE = 4

	/**
	 * Sizes equal or smaller than this are the same whether held as sync safe integer or normal integer so
	 * it doesnt matter.
	 */
	const val MAX_SAFE_SIZE = 127

	/**
	 * Read syncsafe value from byteArray in format specified in spec and convert to int.
	 *
	 * @param buffer syncsafe integer
	 * @return decoded int
	 */
	fun bufferToValue(buffer: ByteArray): Int {
		//Note Need to && with 0xff otherwise if value is greater than 128 we get a negative number
		//when cast byte to int
		return (buffer[0].toInt() and 0xff shl 21) +
			(buffer[1].toInt() and 0xff shl 14) +
			(buffer[2].toInt() and 0xff shl 7) +
			(buffer[3].toInt() and 0xff)
	}

	/**
	 * Read syncsafe value from buffer in format specified in spec and convert to int.
	 *
	 * The buffers position is moved to just after the location of the syncsafe integer
	 *
	 * @param buffer syncsafe integer
	 * @return decoded int
	 */
	@JvmStatic
	fun bufferToValue(buffer: ByteBuffer): Int {
		val byteBuffer = ByteArray(INTEGRAL_SIZE)
		buffer[byteBuffer, 0, INTEGRAL_SIZE]
		return bufferToValue(byteBuffer)
	}

	/**
	 * Is buffer holding a value that is definently not syncsafe
	 *
	 * We cannot guarantee a buffer is holding a syncsafe integer but there are some checks
	 * we can do to show that it definently is not.
	 *
	 * The buffer is NOT moved after reading.
	 *
	 * This function is useful for reading ID3v24 frames created in iTunes because iTunes does not use syncsafe
	 * integers in  its frames.
	 *
	 * @param buffer
	 * @return true if this buffer is definently not holding a syncsafe integer
	 */
	@JvmStatic
	fun isBufferNotSyncSafe(buffer: ByteBuffer): Boolean {
		val position = buffer.position()

		//Check Bit7 not set
		for (i in 0 until INTEGRAL_SIZE) {
			val nextByte = buffer[position + i]
			if (nextByte.toInt() and 0x80 > 0) {
				return true
			}
		}
		return false
	}

	/**
	 * Checks if the buffer just contains zeros
	 *
	 * This can be used to identify when accessing padding of a tag
	 *
	 * @param buffer
	 * @return true if buffer only contains zeros
	 */
	@JvmStatic
	fun isBufferEmpty(buffer: ByteArray): Boolean {
		for (aBuffer in buffer) {
			if (aBuffer.toInt() != 0) {
				return false
			}
		}
		return true
	}

	/**
	 * Convert int value to syncsafe value held in bytearray
	 *
	 * @param size
	 * @return buffer syncsafe integer
	 */
	@JvmStatic
	fun valueToBuffer(size: Int): ByteArray {
		val buffer = ByteArray(4)
		buffer[0] = (size and 0x0FE00000 shr 21).toByte()
		buffer[1] = (size and 0x001FC000 shr 14).toByte()
		buffer[2] = (size and 0x00003F80 shr 7).toByte()
		buffer[3] = (size and 0x0000007F).toByte()
		return buffer
	}
}
