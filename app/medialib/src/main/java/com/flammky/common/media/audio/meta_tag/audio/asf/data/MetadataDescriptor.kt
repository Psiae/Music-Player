/*
 * Entagged Audio Tag library
 * Copyright (c) 2004-2005 Christian Laireiter <liree@web.de>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.GUID.Companion.parseGUID
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagOptionSingleton
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.math.BigInteger
import java.nio.charset.Charset
import java.util.*
import java.util.logging.Logger

/**
 * This structure represents metadata objects in ASF [MetadataContainer].<br></br>
 * The values are
 * [ checked][ContainerType.assertConstraints] against the capability introduced by the given
 * [ContainerType] at construction.<br></br>
 * <br></br>
 * **Limitation**: Even though some container types do not restrict the data
 * size to [Integer.MAX_VALUE], this implementation does it (due to java
 * nature).<br></br>
 * 2 GiB of data should suffice, and even be to large for normal java heap.
 *
 * @author Christian Laireiter
 */
class MetadataDescriptor @JvmOverloads constructor(
	type: ContainerType?,
	propName: String,
	propType: Int,
	stream: Int = 0,
	language: Int = 0
) : Comparable<MetadataDescriptor>, Cloneable {
	/**
	 * Stores the containerType of the descriptor.
	 */
	private val containerType: ContainerType?

	/**
	 * The binary representation of the value.
	 */
	/*
	 * Note: The maximum data length could be up to a 64-Bit number (unsigned),
	 * but java for now handles just int sized byte[]. Since this class stores
	 * all data in primitive byte[] this size restriction is cascaded to all
	 * dependent implementations.
	 */
	private var content = ByteArray(0)

	/**
	 * This field shows the type of the metadata descriptor. <br></br>
	 *
	 * @see .TYPE_BINARY
	 *
	 * @see .TYPE_BOOLEAN
	 *
	 * @see .TYPE_DWORD
	 *
	 * @see .TYPE_GUID
	 *
	 * @see .TYPE_QWORD
	 *
	 * @see .TYPE_STRING
	 *
	 * @see .TYPE_WORD
	 */
	private var descriptorType: Int

	/**
	 * The name of the metadata descriptor.
	 */
	val name: String

	/**
	 * Copy of the content of the descriptor. <br></br>
	 *
	 * The content in binary representation, as it would be written to asf file.
	 */
	val rawData: ByteArray
		get() {
			val copy = ByteArray(content.size)
			System.arraycopy(content, 0, copy, 0, content.size)
			return copy
		}

	/**
	 * The size (in bytes) the binary representation of the content
	 * uses. (length of [.getRawData])<br></br>
	 *
	 * Size of binary representation of the content.
	 */
	val rawDataSize: Int
		get() {
			return content.size
		}

	/**
	 * The type of the metadata descriptor. <br></br>
	 *
	 * The value of [.descriptorType]
	 *
	 * @see .TYPE_BINARY
	 *
	 * @see .TYPE_BOOLEAN
	 *
	 * @see .TYPE_DWORD
	 *
	 * @see .TYPE_GUID
	 *
	 * @see .TYPE_QWORD
	 *
	 * @see .TYPE_STRING
	 *
	 * @see .TYPE_WORD
	 */
	val type: Int
		get() {
			return descriptorType
		}

	/**
	 * the index of the language in the [language list][LanguageList]
	 * this descriptor applies to.<br></br>
	 */
	var languageIndex = 0
		set(value) {
			containerType!!.assertConstraints(name, content, descriptorType, streamNumber, value)
			field = value
		}

	/**
	 * The number of the stream, this descriptor applies to.<br></br>
	 */
	var streamNumber = 0
		set(value) {
			containerType!!.assertConstraints(name, content, descriptorType, value, languageIndex)
			field = value
		}

	/**
	 * Creates an Instance.
	 *
	 * @param type     The container type the values (the whole descriptor) is
	 * restricted to.
	 * @param propName Name of the MetadataDescriptor.
	 * @param propType Type of the metadata descriptor. See [.descriptorType]
	 * @param stream   the number of the stream the descriptor refers to.
	 * @param language the index of the language entry in a [LanguageList] this
	 * descriptor refers to.<br></br>
	 * **Consider**: No checks performed if language entry exists.
	 */
	/**
	 * Creates an Instance.<br></br>
	 *
	 * @param type     the container type, this descriptor is resctricted to.
	 * @param propName Name of the MetadataDescriptor.
	 * @param propType Type of the metadata descriptor. See [.descriptorType]
	 */
	init {
		assert(type != null)
		type!!.assertConstraints(propName, ByteArray(0), propType, stream, language)
		containerType = type
		name = propName.toString()
		descriptorType = propType
		streamNumber = stream
		languageIndex = language
	}
	/**
	 * Creates an Instance.<br></br>
	 * Capabilities are set to [ContainerType.METADATA_LIBRARY_OBJECT].<br></br>
	 *
	 * @param propName Name of the MetadataDescriptor.
	 * @param propType Type of the metadata descriptor. See [.descriptorType]
	 */
	/**
	 * Creates an instance.<br></br>
	 * Capabilities are set to [ContainerType.METADATA_LIBRARY_OBJECT].<br></br>
	 *
	 * @param propName name of the metadata descriptor.
	 */
	@JvmOverloads
	constructor(
		propName: String,
		propType: Int = TYPE_STRING
	) : this(ContainerType.METADATA_LIBRARY_OBJECT, propName, propType, 0, 0)

	/**
	 * Converts the descriptors value into a number if possible.<br></br>
	 * A boolean will be converted to &quot;1&quot; if `true`,
	 * otherwise &quot;0&quot;.<br></br>
	 * String will be interpreted as number with radix &quot;10&quot;.<br></br>
	 * Binary data will be interpreted as the default WORD,DWORD or QWORD binary
	 * representation, but only if the data does not exceed 8 bytes. This
	 * precaution is done to prevent creating a number of a multi kilobyte
	 * image.<br></br>
	 * A GUID cannot be converted in any case.
	 *
	 * @return number representation.
	 * @throws NumberFormatException If no conversion is supported.
	 */
	fun asNumber(): BigInteger {
		var result: BigInteger? = null
		when (descriptorType) {
			TYPE_BOOLEAN, TYPE_WORD, TYPE_DWORD, TYPE_QWORD, TYPE_BINARY -> if (content.size > 8) {
				throw NumberFormatException("Binary data would exceed QWORD")
			}
			TYPE_GUID -> throw NumberFormatException("GUID cannot be converted to a number.")
			TYPE_STRING -> result = BigInteger(getString(), 10)
			else -> throw IllegalStateException()
		}
		if (result == null) {
			val copy = ByteArray(content.size)
			for (i in copy.indices) {
				copy[i] = content[content.size - (i + 1)]
			}
			result = BigInteger(1, copy)
		}
		return result
	}

	/**
	 * (overridden)
	 *
	 * @see Object.clone
	 */
	@Throws(CloneNotSupportedException::class)
	public override fun clone(): Any {
		return super.clone()
	}

	/**
	 * {@inheritDoc}
	 */
	override fun compareTo(other: MetadataDescriptor): Int {
		return name.compareTo(other.name)
	}

	/**
	 * This method creates a copy of the current object. <br></br>
	 * All data will be copied, too. <br></br>
	 *
	 * @return A new metadata descriptor containing the same values as the
	 * current one.
	 */
	fun createCopy(): MetadataDescriptor {
		val result = MetadataDescriptor(
			containerType, name, descriptorType, streamNumber, languageIndex
		)
		result.content = rawData
		return result
	}

	/**
	 * (overridden)
	 *
	 * @see Object.equals
	 */
	override fun equals(obj: Any?): Boolean {
		var result = false
		if (obj is MetadataDescriptor) {
			result = if (obj === this) {
				true
			} else {
				val other =
					obj
				other.name == name && other.descriptorType == descriptorType && other.languageIndex == languageIndex && other.streamNumber == streamNumber && Arrays.equals(
					content,
					other.content
				)
			}
		}
		return result
	}

	/**
	 * Returns the value of the MetadataDescriptor as a Boolean. <br></br>
	 * If no Conversion is Possible false is returned. <br></br>
	 * `true` if first byte of [.content]is not zero.
	 *
	 * @return boolean representation of the current value.
	 */
	fun getBoolean(): Boolean {
		return content.size > 0 && content[0].toInt() != 0
	}

	/**
	 * This method will return a byte array, which can directly be written into
	 * an "Extended Content Description"-chunk. <br></br>
	 *
	 * @return byte[] with the data, that occurs in ASF files.
	 */
	@Deprecated("{@link #writeInto(OutputStream, ContainerType)} is used")
	fun getBytes(): ByteArray {
		val result = ByteArrayOutputStream()
		try {
			writeInto(result, containerType)
		} catch (e: IOException) {
			LOGGER.warning(e.message)
		}
		return result.toByteArray()
	}

	/**
	 * Returns the container type this descriptor ist restricted to.
	 *
	 * @return the container type
	 */
	fun getContainerType(): ContainerType? {
		return containerType
	}

	/**
	 * Returns the size (in bytes) this descriptor will take when written to an
	 * ASF file.<br></br>
	 *
	 * @param type the container type for which the size is calculated.
	 * @return size of the descriptor in an ASF file.
	 */
	fun getCurrentAsfSize(type: ContainerType?): Int {
		/*
		 * 2 bytes name length, 2 bytes name zero term, 2 bytes type, 2 bytes
		 * content length
		 */
		var result = 8
		if (type !== ContainerType.EXTENDED_CONTENT) {
			// Stream number and language index (respectively reserved field).
			// And +2 bytes, because data type is 32 bit, not 16
			result += 6
		}
		result += name.length * 2
		if (descriptorType == TYPE_BOOLEAN) {
			result += 2
			if (type === ContainerType.EXTENDED_CONTENT) {
				// Extended content description boolean values are stored with
				// 32-bit
				result += 2
			}
		} else {
			result += content.size
			if (TYPE_STRING == descriptorType) {
				result += 2 // zero term of content string.
			}
		}
		return result
	}

	/**
	 * Returns the GUID value, if content could represent one.
	 *
	 * @return GUID value
	 */
	fun getGuid(): GUID? {
		var result: GUID? = null
		if (descriptorType == TYPE_GUID && content.size == GUID.GUID_LENGTH) {
			result = GUID(
				content
			)
		}
		return result
	}

	/**
	 * This method returns the value of the metadata descriptor as a long. <br></br>
	 * Converts the needed amount of byte out of [.content]to a number. <br></br>
	 * Only possible if [.getType]equals on of the following: <br></br>
	 *
	 * @return integer value.
	 * @see .TYPE_BOOLEAN
	 *
	 * @see .TYPE_DWORD
	 *
	 * @see .TYPE_QWORD
	 *
	 * @see .TYPE_WORD
	 */
	fun getNumber(): Long {
		val bytesNeeded: Int
		bytesNeeded = when (descriptorType) {
			TYPE_BOOLEAN -> 1
			TYPE_DWORD -> 4
			TYPE_QWORD -> 8
			TYPE_WORD -> 2
			else -> throw UnsupportedOperationException("The current type doesn't allow an interpretation as a number. (" + descriptorType + ")")
		}
		check(bytesNeeded <= content.size) { "The stored data cannot represent the type of current object." }
		var result: Long = 0
		for (i in 0 until bytesNeeded) {
			result = result or (content[i].toLong() and 0xFFL shl i) * 8
		}
		return result
	}

	/**
	 * Returns the value of the MetadataDescriptor as a String. <br></br>
	 *
	 * @return String - Representation Value
	 */
	fun getString(): String? {
		var result: String? = null
		when (descriptorType) {
			TYPE_BINARY -> result = "binary data"
			TYPE_BOOLEAN -> result = getBoolean().toString()
			TYPE_GUID -> result = if (getGuid() == null) "Invalid GUID" else getGuid().toString()
			TYPE_QWORD, TYPE_DWORD, TYPE_WORD -> result = getNumber().toString()
			TYPE_STRING -> try {
				result = String(content, Charset.forName("UTF-16LE"))
			} catch (e: UnsupportedEncodingException) {
				LOGGER.warning(e.message)
			}
			else -> throw IllegalStateException("Current type is not known.")
		}
		return result
	}

	/**
	 * {@inheritDoc}
	 */
	override fun hashCode(): Int {
		return name.hashCode()
	}

	/**
	 * This method checks if the binary data is empty. <br></br>
	 * Disregarding the type of the descriptor its content is stored as a byte
	 * array.
	 *
	 * @return `true` if no value is set.
	 */
	fun isEmpty(): Boolean {
		return content.isEmpty()
	}

	/**
	 * Sets the Value of the current metadata descriptor. <br></br>
	 * Using this method will change [.descriptorType]to
	 * [.TYPE_BINARY].<br></br>
	 *
	 * @param data Value to set.
	 * @throws IllegalArgumentException if data is invalid for [                                  container][.getContainerType].
	 */
	@Throws(IllegalArgumentException::class)
	fun setBinaryValue(data: ByteArray?) {
		containerType!!.assertConstraints(name, data, descriptorType, streamNumber, languageIndex)
		content = data!!.clone()
		descriptorType = TYPE_BINARY
	}

	/**
	 * Sets the Value of the current metadata descriptor. <br></br>
	 * Using this method will change [.descriptorType]to
	 * [.TYPE_BOOLEAN].<br></br>
	 *
	 * @param value Value to set.
	 */
	fun setBooleanValue(value: Boolean) {
		content = byteArrayOf(if (value) 1.toByte() else 0)
		descriptorType = TYPE_BOOLEAN
	}

	/**
	 * Sets the Value of the current metadata descriptor. <br></br>
	 * Using this method will change [.descriptorType]to
	 * [.TYPE_DWORD].
	 *
	 * @param value Value to set.
	 */
	fun setDWordValue(value: Long) {
		require(!(value < 0 || value > DWORD_MAXVALUE)) { "value out of range (0-$DWORD_MAXVALUE)" }
		content = Utils.getBytes(value, 4)
		descriptorType = TYPE_DWORD
	}

	/**
	 * Sets the value of the metadata descriptor.<br></br>
	 * Using this method will change [.descriptorType] to
	 * [.TYPE_GUID]
	 *
	 * @param value value to set.
	 */
	fun setGUIDValue(value: GUID) {
		containerType!!.assertConstraints(name, value.bytes, TYPE_GUID, streamNumber, languageIndex)
		content = value.bytes
		descriptorType = TYPE_GUID
	}

	/**
	 * Sets the Value of the current metadata descriptor. <br></br>
	 * Using this method will change [.descriptorType]to
	 * [.TYPE_QWORD]
	 *
	 * @param value Value to set.
	 * @throws NumberFormatException    on `null` values.
	 * @throws IllegalArgumentException on illegal values or values exceeding range.
	 */
	@Throws(IllegalArgumentException::class)
	fun setQWordValue(value: BigInteger?) {
		if (value == null) {
			throw NumberFormatException("null")
		}
		require(BigInteger.ZERO.compareTo(value) <= 0) { "Only unsigned values allowed (no negative)" }
		require(
			QWORD_MAXVALUE.compareTo(
				value
			) >= 0
		) { "Value exceeds QWORD (64 bit unsigned)" }
		content = ByteArray(8)
		val valuesBytes = value.toByteArray()
		if (valuesBytes.size <= 8) {
			for (i in valuesBytes.indices.reversed()) {
				content[valuesBytes.size - (i + 1)] = valuesBytes[i]
			}
		} else {
			/*
			 * In case of 64-Bit set
			 */
			Arrays.fill(content, 0xFF.toByte())
		}
		descriptorType = TYPE_QWORD
	}

	/**
	 * Sets the Value of the current metadata descriptor. <br></br>
	 * Using this method will change [.descriptorType]to
	 * [.TYPE_QWORD]
	 *
	 * @param value Value to set.
	 */
	fun setQWordValue(value: Long) {
		require(value >= 0) { "value out of range (0-$QWORD_MAXVALUE)" }
		content = Utils.getBytes(value, 8)
		descriptorType = TYPE_QWORD
	}

	/**
	 * This method converts the given string value into the current
	 * [data type][.getType].
	 *
	 * @param value value to set.
	 * @throws IllegalArgumentException If conversion was impossible.
	 */
	@Throws(IllegalArgumentException::class)
	fun setString(value: String) {
		try {
			when (descriptorType) {
				TYPE_BINARY -> throw IllegalArgumentException("Cannot interpret binary as string.")
				TYPE_BOOLEAN -> setBooleanValue(java.lang.Boolean.parseBoolean(value))
				TYPE_DWORD -> setDWordValue(value.toLong())
				TYPE_QWORD -> setQWordValue(BigInteger(value, 10))
				TYPE_WORD -> setWordValue(value.toInt())
				TYPE_GUID -> setGUIDValue(parseGUID(value))
				TYPE_STRING -> setStringValue(value)
				else -> throw IllegalStateException()
			}
		} catch (nfe: NumberFormatException) {
			throw IllegalArgumentException(
				"Value cannot be parsed as Number or is out of range (\"$value\")",
				nfe
			)
		}
	}

	/**
	 * Sets the Value of the current metadata descriptor. <br></br>
	 * Using this method will change [.descriptorType]to
	 * [.TYPE_STRING].
	 *
	 * @param value Value to set.
	 * @throws IllegalArgumentException If byte representation would take more than 65535 Bytes.
	 */
	// TODO Test
	@Throws(IllegalArgumentException::class)
	fun setStringValue(value: String?) {
		if (value == null) {
			content = ByteArray(0)
		} else {
			val tmp = Utils.getBytes(value, AsfHeader.ASF_CHARSET)
			if (getContainerType()!!.isWithinValueRange(tmp.size.toLong())) {
				// Everything is fine here, data can be stored.
				content = tmp
			} else {
				// Normally a size violation, check if JAudiotagger my truncate
				// the string
				if (TagOptionSingleton.instance.isTruncateTextWithoutErrors) {
					// truncate the string
					val copyBytes = getContainerType()!!.maximumDataLength.toLong().toInt()
					content = ByteArray(if (copyBytes % 2 == 0) copyBytes else copyBytes - 1)
					System.arraycopy(tmp, 0, content, 0, content.size)
				} else {
					// We may not truncate, so its an error
					throw IllegalArgumentException(
						ErrorMessage.WMA_LENGTH_OF_DATA_IS_TOO_LARGE.getMsg(
							tmp.size,
							getContainerType()!!.maximumDataLength,
							getContainerType()!!.containerGUID.description
						)
					)
				}
			}
		}
		descriptorType = TYPE_STRING
	}

	/**
	 * Sets the Value of the current metadata descriptor. <br></br>
	 * Using this method will change [.descriptorType]to
	 * [.TYPE_WORD]
	 *
	 * @param value Value to set.
	 * @throws IllegalArgumentException on negative values. ASF just supports unsigned values.
	 */
	@Throws(IllegalArgumentException::class)
	fun setWordValue(value: Int) {
		require(!(value < 0 || value > WORD_MAXVALUE)) { "value out of range (0-$WORD_MAXVALUE)" }
		content = Utils.getBytes(value.toLong(), 2)
		descriptorType = TYPE_WORD
	}

	/**
	 * (overridden)
	 *
	 * @see Object.toString
	 */
	override fun toString(): String {
		return name + " : " + arrayOf(
			"String: ",
			"Binary: ",
			"Boolean: ",
			"DWORD: ",
			"QWORD:",
			"WORD:",
			"GUID:"
		)[descriptorType] + getString() + " (language: " + languageIndex + " / stream: " + streamNumber + ")"
	}

	/**
	 * Writes this descriptor into the specified output stream.<br></br>
	 *
	 * @param out      stream to write into.
	 * @param contType the container type this descriptor is written to.
	 * @return amount of bytes written.
	 * @throws IOException on I/O Errors
	 */
	@Throws(IOException::class)
	fun writeInto(out: OutputStream, contType: ContainerType?): Int {
		val size = getCurrentAsfSize(contType)
		/*
		 * Booleans are stored as one byte, if a boolean is written, the data
		 * must be converted according to the container type.
		 */
		val binaryData: ByteArray
		if (descriptorType == TYPE_BOOLEAN) {
			binaryData = ByteArray(if (contType === ContainerType.EXTENDED_CONTENT) 4 else 2)
			binaryData[0] = (if (getBoolean()) 1 else 0).toByte()
		} else {
			binaryData = content
		}
		// for Metadata objects the stream number and language index
		if (contType !== ContainerType.EXTENDED_CONTENT) {
			Utils.writeUINT16(languageIndex, out)
			Utils.writeUINT16(streamNumber, out)
		}
		Utils.writeUINT16(name.length * 2 + 2, out)

		// The name for the metadata objects come later
		if (contType === ContainerType.EXTENDED_CONTENT) {
			out.write(Utils.getBytes(name, AsfHeader.ASF_CHARSET))
			out.write(AsfHeader.ZERO_TERM)
		}

		// type and content len follow up are identical
		val type = descriptorType
		Utils.writeUINT16(type, out)
		var contentLen = binaryData.size
		if (TYPE_STRING == type) {
			contentLen += 2 // Zero Term
		}
		if (contType === ContainerType.EXTENDED_CONTENT) {
			Utils.writeUINT16(contentLen, out)
		} else {
			Utils.writeUINT32(contentLen.toLong(), out)
		}

		// Metadata objects now write their descriptor name
		if (contType !== ContainerType.EXTENDED_CONTENT) {
			out.write(Utils.getBytes(name, AsfHeader.ASF_CHARSET))
			out.write(AsfHeader.ZERO_TERM)
		}

		// The content.
		out.write(binaryData)
		if (TYPE_STRING == type) {
			out.write(AsfHeader.ZERO_TERM)
		}
		return size
	}

	companion object {
		/**
		 * Maximum value for WORD.
		 */
		@JvmField
		val DWORD_MAXVALUE = BigInteger("FFFFFFFF", 16).toLong()

		/**
		 * Logger instance.
		 */
		private val LOGGER = Logger.getLogger("org.jaudiotagger.audio.asf.data")

		/**
		 * The maximum language index allowed. (exclusive)
		 */
		const val MAX_LANG_INDEX = 127

		/**
		 * Maximum stream number. (inclusive)
		 */
		const val MAX_STREAM_NUMBER = 127

		/**
		 * Maximum value for a QWORD value (64 bit unsigned).<br></br>
		 */
		val QWORD_MAXVALUE = BigInteger("FFFFFFFFFFFFFFFF", 16)

		/**
		 * Constant for the metadata descriptor-type for binary data.
		 */
		const val TYPE_BINARY = 1

		/**
		 * Constant for the metadata descriptor-type for booleans.
		 */
		const val TYPE_BOOLEAN = 2

		/**
		 * Constant for the metadata descriptor-type for DWORD (32-bit unsigned). <br></br>
		 */
		const val TYPE_DWORD = 3

		/**
		 * Constant for the metadata descriptor-type for GUIDs (128-bit).<br></br>
		 */
		const val TYPE_GUID = 6

		/**
		 * Constant for the metadata descriptor-type for QWORD (64-bit unsinged). <br></br>
		 */
		const val TYPE_QWORD = 4

		/**
		 * Constant for the metadata descriptor-type for Strings.
		 */
		const val TYPE_STRING = 0

		/**
		 * Constant for the metadata descriptor-type for WORD (16-bit unsigned). <br></br>
		 */
		const val TYPE_WORD = 5

		/**
		 * Maximum value for WORD.
		 */
		const val WORD_MAXVALUE = 65535
	}
}
