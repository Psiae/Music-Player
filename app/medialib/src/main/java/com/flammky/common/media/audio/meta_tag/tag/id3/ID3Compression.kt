package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3

import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidFrameException
import java.nio.ByteBuffer
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.DataFormatException
import java.util.zip.Inflater

/**
 * compresses frame data
 *
 * Is currently required for V23Frames and V24Frames
 *
 */
//TODO also need to support compress framedata
object ID3Compression {
	//Logger
	var logger = Logger.getLogger("org.jaudiotagger.tag.id3")

	/**
	 * Decompress realFrameSize bytes to decompressedFrameSize bytes and return as ByteBuffer
	 *
	 * @param byteBuffer
	 * @param decompressedFrameSize
	 * @param realFrameSize
	 * @return
	 * @throws InvalidFrameException
	 */
	@JvmStatic
	@Throws(InvalidFrameException::class)
	fun uncompress(
		identifier: String,
		filename: String,
		byteBuffer: ByteBuffer,
		decompressedFrameSize: Int,
		realFrameSize: Int
	): ByteBuffer {
		logger.config("$filename:About to decompress $realFrameSize bytes, expect result to be:$decompressedFrameSize bytes")
		// Decompress the bytes into this buffer, size initialized from header field
		val result = ByteArray(decompressedFrameSize)
		val input = ByteArray(realFrameSize)

		//Store position ( just after frame header and any extra bits)
		//Read frame data into array, and then put buffer back to where it was
		val position = byteBuffer.position()
		byteBuffer[input, 0, realFrameSize]
		byteBuffer.position(position)
		val decompresser = Inflater()
		decompresser.setInput(input)
		try {
			val inflatedTo = decompresser.inflate(result)
			logger.config(
				"$filename:Decompressed to $inflatedTo bytes"
			)
		} catch (dfe: DataFormatException) {
			logger.log(Level.CONFIG, "Unable to decompress this frame:$identifier", dfe)

			//Update position of main buffer, so no attempt is made to reread these bytes
			byteBuffer.position(byteBuffer.position() + realFrameSize)
			throw InvalidFrameException(
				ErrorMessage.ID3_UNABLE_TO_DECOMPRESS_FRAME.getMsg(
					identifier,
					filename,
					dfe.message
				)
			)
		}
		decompresser.end()
		return ByteBuffer.wrap(result)
	}
}
