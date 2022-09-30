package com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidDataTypeException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagOptionSingleton
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.AbstractTagFrameBody
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetEncoder
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.logging.Level

/**
 * Represents a String which is not delimited by null character.
 *
 * This type of String will usually only be used when it is the last field within a frame, when reading the remainder of
 * the byte array will be read, when writing the frame will be accommodate the required size for the String. The String
 * will be encoded based upon the text encoding of the frame that it belongs to.
 *
 * All TextInformation frames support multiple strings, stored as a null separated list, where null is represented by
 * the termination code for the character encoding. This functionality is only officially support in ID3v24.
 *
 * Most applications will ignore any but the first value, but some such as Foobar2000 will decode them properly
 *
 * iTunes write null terminators characters after the String even though it only writes a single value.
 *
 *
 */
open class TextEncodedStringSizeTerminated : AbstractString {
	/**
	 * Creates a new empty TextEncodedStringSizeTerminated datatype.
	 *
	 * @param identifier identifies the frame type
	 * @param frameBody
	 */
	constructor(identifier: String?, frameBody: AbstractTagFrameBody?) : super(
		identifier,
		frameBody
	)

	/**
	 * Copy constructor
	 *
	 * @param object
	 */
	constructor(`object`: TextEncodedStringSizeTerminated?) : super(`object`)

	override fun equals(other: Any?): Boolean {
		return if (this === other) {
			true
		} else other is TextEncodedStringSizeTerminated && super.equals(
			other
		)
	}

	/**
	 * Read a 'n' bytes from buffer into a String where n is the framesize - offset
	 * so therefore cannot use this if there are other objects after it because it has no
	 * delimiter.
	 *
	 * Must take into account the text encoding defined in the Encoding Object
	 * ID3 Text Frames often allow multiple strings seperated by the null char
	 * appropriate for the encoding.
	 *
	 * @param arr    this is the buffer for the frame
	 * @param offset this is where to start reading in the buffer for this field
	 * @throws NullPointerException
	 * @throws IndexOutOfBoundsException
	 */
	@Throws(InvalidDataTypeException::class)
	override fun readByteArray(arr: ByteArray?, offset: Int) {
		//Decode sliced inBuffer
		val inBuffer: ByteBuffer
		inBuffer =
			if (TagOptionSingleton.instance.isAndroid) {
				//#302 [dallen] truncating array manually since the decoder.decode() does not honor the offset in the in buffer
				val truncArr = ByteArray(arr!!.size - offset)
				System.arraycopy(arr, offset, truncArr, 0, truncArr.size)
				ByteBuffer.wrap(truncArr)
			} else {
				ByteBuffer.wrap(arr, offset, arr!!.size - offset).slice()
			}
		val outBuffer = CharBuffer.allocate(arr.size - offset)
		val decoder = getCorrectDecoder(inBuffer)
		val coderResult = decoder!!.decode(inBuffer, outBuffer, true)
		if (coderResult.isError) {
			AbstractDataType.Companion.logger.warning(
				"Decoding error:$coderResult"
			)
		}
		decoder.flush(outBuffer)
		outBuffer.flip()

		//If using UTF16 with BOM we then search through the text removing any BOMs that could exist
		//for multiple values, BOM could be Big Endian or Little Endian
		if (StandardCharsets.UTF_16 == textEncodingCharSet) {
			//Remove addtional bom
			value = outBuffer.toString().replace("\ufeff", "").replace("\ufffe", "")
			//Remove unmappable chars caused by problem with decoding
			value = (value as String).replace("\ufdff", "").replace("\ufffd", "")
		} else {
			value = outBuffer.toString()
		}
		//SetSize, important this is correct for finding the next datatype
		this.size = arr.size - offset
		if (AbstractDataType.Companion.logger.isLoggable(Level.FINEST)) {
			AbstractDataType.Companion.logger.finest(
				"Read SizeTerminatedString:$value size:$size"
			)
		}
	}

	/**
	 * Write String using specified encoding
	 *
	 * When this is called multiple times, all but the last value has a trailing null
	 *
	 * @param encoder
	 * @param next
	 * @param i
	 * @param noOfValues
	 * @return
	 * @throws CharacterCodingException
	 */
	@Throws(CharacterCodingException::class)
	protected fun writeString(
		encoder: CharsetEncoder,
		next: String,
		i: Int,
		noOfValues: Int
	): ByteBuffer {
		val bb: ByteBuffer
		bb = if (i + 1 == noOfValues) {
			encoder.encode(CharBuffer.wrap(next))
		} else {
			encoder.encode(CharBuffer.wrap(next + '\u0000'))
		}
		bb.rewind()
		return bb
	}

	/**
	 * Write String in UTF-LEBOM format
	 *
	 * When this is called multiple times, all but the last value has a trailing null
	 *
	 * Remember we are using this charset because the charset that writes BOM does it the wrong way for us
	 * so we use this none and then manually add the BOM ourselves.
	 *
	 * @param next
	 * @param i
	 * @param noOfValues
	 * @return
	 * @throws CharacterCodingException
	 */
	@Throws(CharacterCodingException::class)
	protected fun writeStringUTF16LEBOM(next: String, i: Int, noOfValues: Int): ByteBuffer {
		val encoder = StandardCharsets.UTF_16LE.newEncoder()
		encoder.onMalformedInput(CodingErrorAction.IGNORE)
		encoder.onUnmappableCharacter(CodingErrorAction.IGNORE)
		val bb: ByteBuffer
		//Note remember LE BOM is ff fe but this is handled by encoder Unicode char is fe ff
		bb = if (i + 1 == noOfValues) {
			encoder.encode(CharBuffer.wrap('\ufeff'.toString() + next))
		} else {
			encoder.encode(CharBuffer.wrap('\ufeff'.toString() + next + '\u0000'))
		}
		bb.rewind()
		return bb
	}

	/**
	 * Write String in UTF-BEBOM format
	 *
	 * When this is called multiple times, all but the last value has a trailing null
	 *
	 * @param next
	 * @param i
	 * @param noOfValues
	 * @return
	 * @throws CharacterCodingException
	 */
	@Throws(CharacterCodingException::class)
	protected fun writeStringUTF16BEBOM(next: String, i: Int, noOfValues: Int): ByteBuffer {
		val encoder = StandardCharsets.UTF_16BE.newEncoder()
		encoder.onMalformedInput(CodingErrorAction.IGNORE)
		encoder.onUnmappableCharacter(CodingErrorAction.IGNORE)
		val bb: ByteBuffer
		//Add BOM
		bb = if (i + 1 == noOfValues) {
			encoder.encode(CharBuffer.wrap('\ufeff'.toString() + next))
		} else {
			encoder.encode(CharBuffer.wrap('\ufeff'.toString() + next + '\u0000'))
		}
		bb.rewind()
		return bb
	}

	/**
	 * Removing trailing null from end of String, this should not be there but some applications continue to write
	 * this unnecessary null char.
	 */
	protected fun stripTrailingNull() {
		if (TagOptionSingleton.instance.isRemoveTrailingTerminatorOnWrite) {
			var stringValue = value as String
			if (stringValue.length > 0) {
				if (stringValue[stringValue.length - 1] == '\u0000') {
					stringValue = stringValue.substring(0, stringValue.length - 1)
					value = stringValue
				}
			}
		}
	}

	/**
	 * Because nulls are stripped we need to check if not removing trailing nulls whether the original
	 * value ended with a null and if so add it back in.
	 * @param values
	 * @param stringValue
	 */
	protected fun checkTrailingNull(values: MutableList<String>, stringValue: String) {
		if (!TagOptionSingleton.instance.isRemoveTrailingTerminatorOnWrite) {
			if (stringValue.length > 0 && stringValue[stringValue.length - 1] == '\u0000') {
				val lastVal = values[values.size - 1]
				val newLastVal = lastVal + '\u0000'
				values[values.size - 1] = newLastVal
			}
		}
	}

	/**
	 * Write String into byte array
	 *
	 * It will remove a trailing null terminator if exists if the option
	 * RemoveTrailingTerminatorOnWrite has been set.
	 *
	 * @return the data as a byte array in format to write to file
	 */
	override fun writeByteArray(): ByteArray? {
		val data: ByteArray
		//Try and write to buffer using the CharSet defined by getTextEncodingCharSet()
		val charset = textEncodingCharSet
		try {
			stripTrailingNull()

			//Special Handling because there is no UTF16 BOM LE charset
			val stringValue = value as String
			var actualCharSet: Charset? = null
			if (StandardCharsets.UTF_16 == charset) {
				actualCharSet =
					if (TagOptionSingleton.instance.isEncodeUTF16BomAsLittleEndian) {
						StandardCharsets.UTF_16LE
					} else {
						StandardCharsets.UTF_16BE
					}
			}

			//Ensure large enough for any encoding
			val outputBuffer = ByteBuffer.allocate((stringValue.length + 3) * 3)

			//Ensure each string (if multiple values) is written with BOM by writing separately
			val values = splitByNullSeperator(stringValue)
			checkTrailingNull(values, stringValue)

			//For each value
			for (i in values.indices) {
				val next = values[i]
				if (StandardCharsets.UTF_16LE == actualCharSet) {
					outputBuffer.put(writeStringUTF16LEBOM(next, i, values.size))
				} else if (StandardCharsets.UTF_16BE == actualCharSet) {
					outputBuffer.put(writeStringUTF16BEBOM(next, i, values.size))
				} else {
					val charsetEncoder = charset!!.newEncoder()
					charsetEncoder.onMalformedInput(CodingErrorAction.IGNORE)
					charsetEncoder.onUnmappableCharacter(CodingErrorAction.IGNORE)
					outputBuffer.put(writeString(charsetEncoder, next, i, values.size))
				}
			}
			outputBuffer.flip()
			data = ByteArray(outputBuffer.limit())
			outputBuffer.rewind()
			outputBuffer[data, 0, outputBuffer.limit()]
			this.size = data.size
		} //https://bitbucket.org/ijabz/jaudiotagger/issue/1/encoding-metadata-to-utf-16-can-fail-if
		catch (ce: CharacterCodingException) {
			AbstractDataType.Companion.logger.severe(ce.message + ":" + charset + ":" + value)
			throw RuntimeException(ce)
		}
		return data
	}

	/**
	 * Add an additional String to the current String value
	 *
	 * @param value
	 */
	open fun addValue(value: String) {
		this.value = this.value.toString() + "\u0000" + value
	}

	/**
	 * How many values are held, each value is separated by a null terminator
	 *
	 * @return number of values held, usually this will be one.
	 */
	open val numberOfValues: Int
		get() = splitByNullSeperator(
			value as String
		).size

	/**
	 * Get the nth value
	 *
	 * @param index
	 * @return the nth value
	 * @throws IndexOutOfBoundsException if value does not exist
	 */
	open fun getValueAtIndex(index: Int): String {
		//Split String into separate components
		val values: List<String> = splitByNullSeperator(value as String)
		return values[index]
	}

	/**
	 *
	 * @return list of all values
	 */
	open val values: List<String>
		get() = splitByNullSeperator(value as String)

	/**
	 * Get value(s) whilst removing any trailing nulls
	 *
	 * @return
	 */
	open val valueWithoutTrailingNull: String
		get() {
			val values: List<String> = splitByNullSeperator(value as String)
			val sb = StringBuffer()
			for (i in values.indices) {
				if (i != 0) {
					sb.append("\u0000")
				}
				sb.append(values[i])
			}
			return sb.toString()
		}

	companion object {
		/**
		 * Split the values separated by null character
		 *
		 * @param value the raw value
		 * @return list of values, guaranteed to be at least one value
		 */
		fun splitByNullSeperator(value: String): MutableList<String> {
			val valuesarray =
				value.split("\\u0000".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			var values = Arrays.asList(*valuesarray)
			//Read only list so if empty have to create new list
			if (values.size == 0) {
				values = ArrayList(1)
				values.add("")
			}
			return values
		}
	}
}
