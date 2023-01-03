package com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidDataTypeException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.AbstractTagFrameBody
import com.flammky.musicplayer.common.media.audio.meta_tag.utils.EqualsUtil.areEqual
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.logging.Level

/**
 * Represents a data type that allow multiple Strings but they should be paired as key values, i.e should be 2,4,6..
 * But keys are not unique so we don't store as a map, so could have same key pointing to two different values
 * such as two ENGINEER keys
 *
 */
class PairedTextEncodedStringNullTerminated : AbstractDataType {

	override var value: Any? = super.value

	override var size: Int = super.size

	constructor(identifier: String?, frameBody: AbstractTagFrameBody?) : super(
		identifier,
		frameBody
	) {
		value = ValuePairs()
	}

	constructor(`object`: TextEncodedStringSizeTerminated?) : super(`object`) {
		value = ValuePairs()
	}

	constructor(`object`: PairedTextEncodedStringNullTerminated?) : super(`object`)

	override fun equals(other: Any?): Boolean {
		if (other === this) {
			return true
		}
		if (other !is PairedTextEncodedStringNullTerminated) {
			return false
		}
		return areEqual(value, other.value)
	}


	/**
	 * Check the value can be encoded with the specified encoding
	 *
	 * @return
	 */
	fun canBeEncoded(): Boolean {
		for (entry in (value as ValuePairs).getMapping()) {
			val next = TextEncodedStringNullTerminated(identifier, body, entry.value)
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
			"Reading PairTextEncodedStringNullTerminated from array from offset:$offset"
		)
		//Continue until unable to read a null terminated String

		while (true) {
			try {
				//Read Key
				val key = TextEncodedStringNullTerminated(identifier, body)
				key.readByteArray(arr, offset)
				size += key.size
				offset += key.size
				if (key.size == 0) {
					break
				}
				try {
					//Read Value
					val result = TextEncodedStringNullTerminated(identifier, body)
					result.readByteArray(arr, offset)
					size += result.size
					offset += result.size
					if (result.size == 0) {
						break
					}
					//Add to value
					(value as ValuePairs).add(key.value as String, result.value as String)
				} catch (idte: InvalidDataTypeException) {
					//Value may not be null terminated if it is the last value
					//Read Value
					if (offset >= arr!!.size) {
						break
					}
					val result = TextEncodedStringSizeTerminated(identifier, body)
					result.readByteArray(arr, offset)
					size += result.size
					offset += result.size
					if (result.size == 0) {
						break
					}
					//Add to value
					(value as ValuePairs).add(key.value as String, result.value as String)
					break
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
			"Read  PairTextEncodedStringNullTerminated:$value size:$size"
		)
	}

	/**
	 * For every String write to byteBuffer
	 *
	 * @return byteBuffer that should be written to file to persist this dataType.
	 */
	override fun writeByteArray(): ByteArray? {
		logger.finer("Writing PairTextEncodedStringNullTerminated")
		var localSize = 0
		val buffer = ByteArrayOutputStream()
		try {
			for (pair in (value as ValuePairs).getMapping()) {
				run {
					val next = TextEncodedStringNullTerminated(identifier, body, pair.key)
					buffer.write(next.writeByteArray())
					localSize += next.size
				}
				run {
					val next = TextEncodedStringNullTerminated(identifier, body, pair.value)
					buffer.write(next.writeByteArray())
					localSize += next.size
				}
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
		logger.finer("Written PairTextEncodedStringNullTerminated")
		return buffer.toByteArray()
	}

	override fun toString(): String {
		return value.toString()
	}

	/**
	 * This holds the values held by this PairedTextEncodedDataType, always held as pairs of values
	 */
	class ValuePairs {

		private val mapping: MutableList<Pair> = ArrayList()

		fun add(pair: Pair) {
			mapping.add(pair)
		}

		/**
		 * Add String Data type to the value list
		 *
		 * @param value to add to the list
		 */
		fun add(key: String?, value: String?) {
			mapping.add(Pair(key, value))
		}

		/**
		 * Return the list of values
		 *
		 * @return the list of values
		 */
		fun getMapping(): MutableList<Pair> {
			return mapping
		}

		/**
		 * @return no of values
		 */
		fun getNumberOfValues(): Int {
			return mapping.size
		}

		/**
		 * Return the list of values as a single string separated by a colon,comma
		 *
		 * @return a string representation of the value
		 */
		override fun toString(): String {
			val sb = StringBuffer()
			for (next in mapping) {
				sb.append(next.key + ':' + next.value + ',')
			}
			if (sb.length > 0) {
				sb.setLength(sb.length - 1)
			}
			return sb.toString()
		}

		/**
		 * @return no of values
		 */
		fun getNumberOfPairs(): Int {
			return mapping.size
		}

		override fun equals(obj: Any?): Boolean {
			if (obj === this) {
				return true
			}
			if (obj !is ValuePairs) {
				return false
			}
			return areEqual(getNumberOfValues().toLong(), obj.getNumberOfValues().toLong())
		}
	}

	fun getValue(): ValuePairs? {
		return value as? ValuePairs
	}
}
