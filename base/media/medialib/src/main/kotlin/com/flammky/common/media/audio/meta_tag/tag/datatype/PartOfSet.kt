package com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidDataTypeException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagOptionSingleton
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.AbstractTagFrameBody
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.options.PadNumberOption
import com.flammky.musicplayer.common.media.audio.meta_tag.utils.EqualsUtil.areEqual
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetEncoder
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.logging.Level
import java.util.regex.Pattern

/**
 * Represents the form 01/10 whereby the second part is optional. This is used by frame such as TRCK and TPOS and MVNM
 *
 * Some applications like to prepend the count with a zero to aid sorting, (i.e 02 comes before 10)
 *
 * If TagOptionSingleton.getInstance().isPadNumbers() is enabled then all fields will be written to file padded
 * depending on the value of agOptionSingleton.getInstance().getPadNumberTotalLength(). Additionally fields returned
 * from file will be returned as padded even if they are not currently stored as padded in the file.
 *
 * If TagOptionSingleton.getInstance().isPadNumbers() is disabled then count and track are written to file as they
 * are provided, i.e if provided pre-padded they will be stored pre-padded, if not they will not. Values read from
 * file will be returned as they are currently stored in file.
 *
 *
 */
class PartOfSet : AbstractString {
	/**
	 * Creates a new empty  PartOfSet datatype.
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
	constructor(`object`: PartOfSet?) : super(`object`)

	override fun equals(other: Any?): Boolean {
		if (other === this) {
			return true
		}
		if (other !is PartOfSet) {
			return false
		}
		return areEqual(value, other.value)
	}

	/**
	 * Read a 'n' bytes from buffer into a String where n is the frameSize - offset
	 * so therefore cannot use this if there are other objects after it because it has no
	 * delimiter.
	 *
	 * Must take into account the text encoding defined in the Encoding Object
	 * ID3 Text Frames often allow multiple strings separated by the null char
	 * appropriate for the encoding.
	 *
	 * @param arr    this is the buffer for the frame
	 * @param offset this is where to start reading in the buffer for this field
	 * @throws NullPointerException
	 * @throws IndexOutOfBoundsException
	 */
	@Throws(InvalidDataTypeException::class)
	override fun readByteArray(arr: ByteArray?, offset: Int) {
		//Get the Specified Decoder
		val decoder = textEncodingCharSet!!.newDecoder()

		//Decode sliced inBuffer
		val inBuffer = ByteBuffer.wrap(arr, offset, arr!!.size - offset).slice()
		val outBuffer = CharBuffer.allocate(arr.size - offset)
		decoder.reset()
		val coderResult = decoder.decode(inBuffer, outBuffer, true)
		if (coderResult.isError) {
			logger.warning(
				"Decoding error:$coderResult"
			)
		}
		decoder.flush(outBuffer)
		outBuffer.flip()

		//Store value
		val stringValue = outBuffer.toString()
		value = PartOfSetValue(stringValue)

		//SetSize, important this is correct for finding the next datatype
		this.size = (arr.size - offset)
		if (logger.isLoggable(Level.CONFIG)) {
			logger.config(
				"Read SizeTerminatedString:$value size:$size"
			)
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
		var value = this.value.toString()
		val data: ByteArray
		//Try and write to buffer using the CharSet defined by getTextEncodingCharSet()
		try {
			if (TagOptionSingleton.instance.isRemoveTrailingTerminatorOnWrite) {
				if (value.length > 0) {
					if (value[value.length - 1] == '\u0000') {
						value = value.substring(0, value.length - 1)
					}
				}
			}
			val charset = textEncodingCharSet
			val valueWithBOM: String
			val encoder: CharsetEncoder
			if (StandardCharsets.UTF_16 == charset) {
				encoder = StandardCharsets.UTF_16LE.newEncoder()
				//Note remember LE BOM is ff fe but this is handled by encoder Unicode char is fe ff
				valueWithBOM = '\ufeff'.toString() + value
			} else {
				encoder = charset!!.newEncoder()
				valueWithBOM = value
			}
			encoder.onMalformedInput(CodingErrorAction.IGNORE)
			encoder.onUnmappableCharacter(CodingErrorAction.IGNORE)
			val bb = encoder.encode(CharBuffer.wrap(valueWithBOM))
			data = ByteArray(bb.limit())
			bb[data, 0, bb.limit()]
		} //Should never happen so if does throw a RuntimeException
		catch (ce: CharacterCodingException) {
			logger.severe(ce.message)
			throw RuntimeException(ce)
		}
		this.size = data.size
		return data
	}

	/**
	 * Get the text encoding being used.
	 *
	 * The text encoding is defined by the frame body that the text field belongs to.
	 *
	 * @return the text encoding charset
	 */
	override val textEncodingCharSet: Charset?
		get() {
			val textEncoding = this.body?.textEncoding ?: return null
			val charset = TextEncoding.instanceOf.getCharsetForId(textEncoding.toInt())
			logger.finest("text encoding:" + textEncoding + " charset:" + charset!!.name())
			return charset
		}

	/**
	 * Holds data
	 */
	class PartOfSetValue {
		var count: Int? = null
		var total: Int? = null
		private var extra //Any extraneous info such as null chars
			: String? = null
		private var rawText // raw text representation used to actually save the data IF !TagOptionSingleton.getInstance().isPadNumbers()
			: String? = null
		private var rawCount //count value as provided
			: String? = null
		private var rawTotal //total value as provided
			: String? = null

		constructor() {
			rawText = ""
		}

		/**
		 * When constructing from data
		 *
		 * @param value
		 */
		constructor(value: String?) {
			rawText = value
			initFromValue(value)
		}

		/**
		 * Newly created
		 *
		 * @param count
		 * @param total
		 */
		constructor(count: Int?, total: Int?) {
			this.count = count
			rawCount = count.toString()
			this.total = total
			rawTotal = total.toString()
			resetValueFromCounts()
		}

		/**
		 * Given a raw value that could contain both a count and total and extra stuff (but needdnt contain
		 * anything tries to parse it)
		 *
		 * @param value
		 */
		private fun initFromValue(value: String?) {
			try {
				var m = trackNoPatternWithTotalCount.matcher(value)
				if (m.matches()) {
					extra = m.group(3)
					count = m.group(1).toInt()
					rawCount = m.group(1)
					total = m.group(2).toInt()
					rawTotal = m.group(2)
					return
				}
				m = trackNoPattern.matcher(value)
				if (m.matches()) {
					extra = m.group(2)
					count = m.group(1).toInt()
					rawCount = m.group(1)
				}
			} catch (nfe: NumberFormatException) {
				//#JAUDIOTAGGER-366 Could occur if actually value is a long not an int
				count = 0
			}
		}

		private fun resetValueFromCounts() {
			val sb = StringBuffer()
			if (rawCount != null) {
				sb.append(rawCount)
			} else {
				sb.append("0")
			}
			if (rawTotal != null) {
				sb.append(SEPARATOR + rawTotal)
			}
			if (extra != null) {
				sb.append(extra)
			}
			rawText = sb.toString()
		}

		fun setCount(count: Int) {
			this.count = count
			rawCount = count.toString()
			resetValueFromCounts()
		}

		fun setTotal(total: Int) {
			this.total = total
			rawTotal = total.toString()
			resetValueFromCounts()
		}

		fun setCount(count: String) {
			try {
				this.count = count.toInt()
				rawCount = count
				resetValueFromCounts()
			} catch (nfe: NumberFormatException) {
			}
		}

		fun setTotal(total: String) {
			try {
				this.total = total.toInt()
				rawTotal = total
				resetValueFromCounts()
			} catch (nfe: NumberFormatException) {
			}
		}

		var rawValue: String?
			get() = rawText
			set(value) {
				rawText = value
				initFromValue(value)
			}//Don't Pad

		/**
		 * Get Count including padded if padding is enabled
		 *
		 * @return
		 */
		val countAsText: String?
			get() {
				//Don't Pad
				val sb = StringBuffer()
				if (!TagOptionSingleton.instance.isPadNumbers) {
					return rawCount
				} else {
					padNumber(sb, count, TagOptionSingleton.instance.padNumberTotalLength)
				}
				return sb.toString()
			}

		/**
		 * Pad number so number is defined as long as length
		 *
		 * @param sb
		 * @param count
		 * @param padNumberLength
		 */
		private fun padNumber(sb: StringBuffer, count: Int?, padNumberLength: PadNumberOption) {
			if (count != null) {
				if (padNumberLength == PadNumberOption.PAD_ONE_ZERO) {
					if (count > 0 && count < 10) {
						sb.append("0").append(count)
					} else {
						sb.append(count.toInt())
					}
				} else if (padNumberLength == PadNumberOption.PAD_TWO_ZERO) {
					if (count > 0 && count < 10) {
						sb.append("00").append(count)
					} else if (count > 9 && count < 100) {
						sb.append("0").append(count)
					} else {
						sb.append(count.toInt())
					}
				} else if (padNumberLength == PadNumberOption.PAD_THREE_ZERO) {
					if (count > 0 && count < 10) {
						sb.append("000").append(count)
					} else if (count > 9 && count < 100) {
						sb.append("00").append(count)
					} else if (count > 99 && count < 1000) {
						sb.append("0").append(count)
					} else {
						sb.append(count.toInt())
					}
				}
			}
		}//Don't Pad

		/**
		 * Get Total padded
		 *
		 * @return
		 */
		val totalAsText: String?
			get() {
				//Don't Pad
				val sb = StringBuffer()
				if (!TagOptionSingleton.instance.isPadNumbers) {
					return rawTotal
				} else {
					padNumber(sb, total, TagOptionSingleton.instance.padNumberTotalLength)
				}
				return sb.toString()
			}

		override fun toString(): String {

			//Don't Pad
			val sb = StringBuffer()
			if (!TagOptionSingleton.instance.isPadNumbers) {
				return rawText!!
			} else {
				if (count != null) {
					padNumber(sb, count, TagOptionSingleton.instance.padNumberTotalLength)
				} else if (total != null) {
					padNumber(sb, 0, TagOptionSingleton.instance.padNumberTotalLength)
				}
				if (total != null) {
					sb.append(SEPARATOR)
					padNumber(sb, total, TagOptionSingleton.instance.padNumberTotalLength)
				}
				if (extra != null) {
					sb.append(extra)
				}
			}
			return sb.toString()
		}

		override fun equals(obj: Any?): Boolean {
			if (obj === this) {
				return true
			}
			if (obj !is PartOfSetValue) {
				return false
			}
			val that = obj
			return (areEqual(
				count, that.count
			)
				&& areEqual(
				total, that.total
			))
		}

		companion object {
			//Match track/total pattern allowing for extraneous nulls ecetera at the end
			private val trackNoPatternWithTotalCount: Pattern =
				Pattern.compile("([0-9]+)/([0-9]+)(.*)", Pattern.CASE_INSENSITIVE)
			private val trackNoPattern: Pattern =
				Pattern.compile("([0-9]+)(.*)", Pattern.CASE_INSENSITIVE)
			private const val SEPARATOR = "/"
		}
	}

	override var value: Any? = super.value
		get() = field as? PartOfSetValue

	override fun toString(): String {
		return value.toString()
	}
}
