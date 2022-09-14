package com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidDataTypeException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagOptionSingleton
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.AbstractTagFrameBody
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.logging.Level

/**
 * Represents a String whose size is determined by finding of a null character at the end of the String.
 *
 * The String itself might be of length zero (i.e just consist of the null character). The String will be encoded based
 * upon the text encoding of the frame that it belongs to.
 */
open class TextEncodedStringNullTerminated : AbstractString {
	/**
	 * Creates a new TextEncodedStringNullTerminated datatype.
	 *
	 * @param identifier identifies the frame type
	 * @param frameBody
	 */
	constructor(identifier: String?, frameBody: AbstractTagFrameBody?) : super(
		identifier,
		frameBody
	)

	/**
	 * Creates a new TextEncodedStringNullTerminated datatype, with value
	 *
	 * @param identifier
	 * @param frameBody
	 * @param value
	 */
	constructor(identifier: String?, frameBody: AbstractTagFrameBody?, value: String?) : super(
		identifier,
		frameBody,
		value
	)

	constructor(`object`: TextEncodedStringNullTerminated?) : super(`object`)

	override fun equals(other: Any?): Boolean {
		return other is TextEncodedStringNullTerminated && super.equals(other)
	}

	/**
	 * Read a string from buffer upto null character (if exists)
	 *
	 * Must take into account the text encoding defined in the Encoding Object
	 * ID3 Text Frames often allow multiple strings separated by the null char
	 * appropriate for the encoding.
	 *
	 * @param arr    this is the buffer for the frame
	 * @param offset this is where to start reading in the buffer for this field
	 */
	@Throws(InvalidDataTypeException::class)
	override fun readByteArray(arr: ByteArray?, offset: Int) {
		if (offset >= arr!!.size) {
			throw InvalidDataTypeException("Unable to find null terminated string")
		}
		val bufferSize: Int
		var size: Int

		//Get the Specified Decoder
		val charset = textEncodingCharSet


		//We only want to load up to null terminator, data after this is part of different
		//field and it may not be possible to decode it so do the check before we do
		//do the decoding,encoding dependent.
		val buffer = ByteBuffer.wrap(arr, offset, arr.size - offset)
		var endPosition = 0

		//Latin-1 and UTF-8 strings are terminated by a single-byte null,
		//while UTF-16 and its variants need two bytes for the null terminator.
		val nullIsOneByte =
			StandardCharsets.ISO_8859_1 === charset || StandardCharsets.UTF_8 === charset
		var isNullTerminatorFound = false
		while (buffer.hasRemaining()) {
			var nextByte = buffer.get()
			if (nextByte.toInt() == 0x00) {
				if (nullIsOneByte) {
					buffer.mark()
					buffer.reset()
					endPosition = buffer.position() - 1
					isNullTerminatorFound = true
					break
				} else {
					// Looking for two-byte null
					if (buffer.hasRemaining()) {
						nextByte = buffer.get()
						if (nextByte.toInt() == 0x00) {
							buffer.mark()
							buffer.reset()
							endPosition = buffer.position() - 2
							isNullTerminatorFound = true
							break
						} else {
							//Nothing to do, we have checked 2nd value of pair it was not a null terminator
							//so will just start looking again in next invocation of loop
						}
					} else {
						buffer.mark()
						buffer.reset()
						endPosition = buffer.position() - 1
						isNullTerminatorFound = true
						break
					}
				}
			} else {
				//If UTF16, we should only be looking on 2 byte boundaries
				if (!nullIsOneByte) {
					if (buffer.hasRemaining()) {
						buffer.get()
					}
				}
			}
		}
		if (!isNullTerminatorFound) {
			throw InvalidDataTypeException("Unable to find null terminated string")
		}

		//Set Size so offset is ready for next field (includes the null terminator)
		size = endPosition - offset
		size++
		if (!nullIsOneByte) {
			size++
		}
		this.size = size

		//Decode buffer if runs into problems should throw exception which we
		//catch and then set value to empty string. (We don't read the null terminator
		//because we dont want to display this)
		bufferSize = endPosition - offset
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest(
				"Text size is:$bufferSize"
			)
		}
		value = if (bufferSize == 0) {
			""
		} else {
			//Decode sliced inBuffer
			val inBuffer = ByteBuffer.wrap(arr, offset, bufferSize).slice()
			val outBuffer = CharBuffer.allocate(bufferSize)
			val decoder = getCorrectDecoder(inBuffer)
			val coderResult = decoder!!.decode(inBuffer, outBuffer, true)
			if (coderResult.isError) {
				logger.warning(
					"Problem decoding text encoded null terminated string:$coderResult"
				)
			}
			decoder.flush(outBuffer)
			outBuffer.flip()
			outBuffer.toString()
		}
		//Set Size so offset is ready for next field (includes the null terminator)
		if (logger.isLoggable(Level.CONFIG)) {
			logger.config(
				"Read NullTerminatedString:$value size inc terminator:$size"
			)
		}
	}

	/**
	 * Write String into byte array, adding a null character to the end of the String
	 *
	 * @return the data as a byte array in format to write to file
	 */
	override fun writeByteArray(): ByteArray? {
		logger.config(
			"Writing NullTerminatedString.$value"
		)
		val data: ByteArray
		//Write to buffer using the CharSet defined by getTextEncodingCharSet()
		//Add a null terminator which will be encoded based on encoding.
		val charset = textEncodingCharSet
		try {
			if (StandardCharsets.UTF_16 == charset) {
				if (TagOptionSingleton.instance.isEncodeUTF16BomAsLittleEndian) {
					val encoder = StandardCharsets.UTF_16LE.newEncoder()
					encoder.onMalformedInput(CodingErrorAction.IGNORE)
					encoder.onUnmappableCharacter(CodingErrorAction.IGNORE)

					//Note remember LE BOM is ff fe but this is handled by encoder Unicode char is fe ff
					val bb =
						encoder.encode(CharBuffer.wrap('\ufeff'.toString() + value as String + '\u0000'))
					data = ByteArray(bb.limit())
					bb[data, 0, bb.limit()]
				} else {
					val encoder = StandardCharsets.UTF_16BE.newEncoder()
					encoder.onMalformedInput(CodingErrorAction.IGNORE)
					encoder.onUnmappableCharacter(CodingErrorAction.IGNORE)

					//Note  BE BOM will leave as fe ff
					val bb =
						encoder.encode(CharBuffer.wrap('\ufeff'.toString() + value as String + '\u0000'))
					data = ByteArray(bb.limit())
					bb[data, 0, bb.limit()]
				}
			} else {
				val encoder = charset!!.newEncoder()
				encoder.onMalformedInput(CodingErrorAction.IGNORE)
				encoder.onUnmappableCharacter(CodingErrorAction.IGNORE)
				val bb = encoder.encode(CharBuffer.wrap(value as String + '\u0000'))
				data = ByteArray(bb.limit())
				bb[data, 0, bb.limit()]
			}
		} //https://bitbucket.org/ijabz/jaudiotagger/issue/1/encoding-metadata-to-utf-16-can-fail-if
		catch (ce: CharacterCodingException) {
			logger.severe(ce.message + ":" + charset!!.name() + ":" + value)
			throw RuntimeException(ce)
		}
		size = data.size
		return data
	}


	override val textEncodingCharSet: Charset?
		get() {
			val textEncoding = this.body?.textEncoding ?: return null
			val charset = TextEncoding.instanceOf.getCharsetForId(textEncoding.toInt())
			logger.finest("text encoding:" + textEncoding + " charset:" + charset!!.name())
			return charset
		}
}
