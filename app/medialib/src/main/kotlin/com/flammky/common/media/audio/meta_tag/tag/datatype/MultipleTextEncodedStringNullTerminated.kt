package com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidDataTypeException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.AbstractTagFrameBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.logging.Level

/**
 * Represents a data type that supports multiple terminated Strings (there may only be one)
 */
class MultipleTextEncodedStringNullTerminated : AbstractDataType {

	override var size: Int = super.size

	/**
	 * Creates a new ObjectStringSizeTerminated datatype.
	 *
	 * @param identifier identifies the frame type
	 * @param frameBody
	 */
	constructor(identifier: String?, frameBody: AbstractTagFrameBody?) : super(
		identifier,
		frameBody
	) {
		value = Values()
	}

	constructor(`object`: TextEncodedStringSizeTerminated?) : super(`object`) {
		value = Values()
	}

	constructor(`object`: MultipleTextEncodedStringNullTerminated?) : super(`object`)

	override fun equals(other: Any?): Boolean {
		return other is MultipleTextEncodedStringNullTerminated && super.equals(other)
	}

	/**
	 * Check the value can be encoded with the specified encoding
	 * @return
	 */
	fun canBeEncoded(): Boolean {
		val li = (value as Values).getList().listIterator()
		while (li.hasNext()) {
			val next = TextEncodedStringNullTerminated(identifier, body, li.next())
			if (!next.canBeEncoded()) {
				return false
			}
		}
		return true
	}

	/**
	 * Read Null Terminated Strings from the array starting at offset, continue until unable to find any null terminated
	 * Strings or until reached the end of the array. The offset should be set to byte after the last null terminated
	 * String found.
	 *
	 * @param arr    to read the Strings from
	 * @param offset in the array to start reading from
	 * @throws InvalidDataTypeException if unable to find any null terminated Strings
	 */
	@Throws(InvalidDataTypeException::class)
	override fun readByteArray(arr: ByteArray?, offset: Int) {
		var offset = offset
		logger.finer(
			"Reading MultipleTextEncodedStringNullTerminated from array from offset:$offset"
		)
		//Continue until unable to read a null terminated String
		while (true) {
			try {
				//Read String
				val next = TextEncodedStringNullTerminated(identifier, body)
				next.readByteArray(arr, offset)
				if (next.size == 0) {
					break
				} else {
					//Add to value
					(value as Values).add(next.value as String)

					//Add to size calculation
					size += next.size

					//Increment Offset to start of next datatype.
					offset += next.size
				}
			} catch (idte: InvalidDataTypeException) {
				break
			}
			if (size == 0) {
				logger.warning("No null terminated Strings found")
				throw InvalidDataTypeException("No null terminated Strings found")
			}
		}
		logger.finer(
			"Read  MultipleTextEncodedStringNullTerminated:$value size:$size"
		)
	}

	/**
	 * For every String write to bytebuffer
	 *
	 * @return bytebuffer that should be written to file to persist this datatype.
	 */
	override fun writeByteArray(): ByteArray? {
		logger.finer("Writing MultipleTextEncodedStringNullTerminated")
		var localSize = 0
		val buffer = ByteArrayOutputStream()
		try {
			val li = (value as Values).getList().listIterator()
			while (li.hasNext()) {
				val next = TextEncodedStringNullTerminated(identifier, body, li.next())
				buffer.write(next.writeByteArray())
				localSize += next.size
			}
		} catch (ioe: IOException) {
			//This should never happen because the write is internal with the JVM it is not to a file
			logger.log(
				Level.SEVERE,
				"IOException in MultipleTextEncodedStringNullTerminated when writing byte array",
				ioe
			)
			throw RuntimeException(ioe)
		}

		//Update size member variable
		size = localSize
		logger.finer("Written MultipleTextEncodedStringNullTerminated")
		return buffer.toByteArray()
	}

	/**
	 * This holds the values held by a MultipleTextEncodedData type
	 */
	class Values {
		private val valueList: MutableList<String?> = ArrayList()

		/**
		 * Add String Data type to the value list
		 *
		 * @param value to add to the list
		 */
		fun add(value: String?) {
			valueList.add(value)
		}

		/**
		 * Return the list of values
		 *
		 * @return the list of values
		 */
		fun getList(): List<String?> {
			return valueList
		}

		/**
		 *
		 * @return no of values
		 */
		fun getNumberOfValues(): Int {
			return valueList.size
		}

		/**
		 * Return the list of values as a single string separated by a comma
		 *
		 * @return a string representation of the value
		 */
		override fun toString(): String {
			val sb = StringBuffer()
			val li: ListIterator<String?> = valueList.listIterator()
			while (li.hasNext()) {
				val next = li.next()
				sb.append(next)
				if (li.hasNext()) {
					sb.append(",")
				}
			}
			return sb.toString()
		}
	}
}
