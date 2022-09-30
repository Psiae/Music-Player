package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import java.math.BigInteger
import java.util.*

/**
 * Enumerates capabilities, respectively uses, of metadata descriptors.<br></br>
 * <br></br>
 * The [.METADATA_LIBRARY_OBJECT] allows the most variations of data, as
 * well as no size limitation (if it can be stored within a DWORD amount of
 * bytes).<br></br>
 *
 * @author Christian Laireiter
 */
enum class ContainerType(
	/**
	 * Stores the guid that identifies ASF chunks which store metadata of the
	 * current type.
	 */
	val containerGUID: GUID,
	maxDataLenBits: Int,
	guidAllowed: Boolean,
	stream: Boolean,
	language: Boolean,
	multiValue: Boolean
) {
	/**
	 * The descriptor is used in the content branding object (chunk)
	 */
	CONTENT_BRANDING(GUID.GUID_CONTENT_BRANDING, 32, false, false, false, false),

	/**
	 * The descriptor is used in the content description object (chunk), so
	 * [maximum data length][MetadataDescriptor.DWORD_MAXVALUE]
	 * applies, no language index and stream number are allowed, as well as no
	 * multiple values.
	 */
	CONTENT_DESCRIPTION(GUID.GUID_CONTENTDESCRIPTION, 16, false, false, false, false),

	/**
	 * The descriptor is used in an extended content description object, so the
	 * [maximum data size][MetadataDescriptor.DWORD_MAXVALUE] applies,
	 * and no language index and stream number other than &quot;0&quot; is
	 * allowed. Additionally no multiple values are permitted.
	 */
	EXTENDED_CONTENT(GUID.GUID_EXTENDED_CONTENT_DESCRIPTION, 16, false, false, false, false),

	/**
	 * The descriptor is used in a metadata library object. No real size limit
	 * (except DWORD range) applies. Stream numbers and language indexes can be
	 * specified.
	 */
	METADATA_LIBRARY_OBJECT(GUID.GUID_METADATA_LIBRARY, 32, true, true, true, true),

	/**
	 * The descriptor is used in a metadata object. The
	 * [maximum data size][MetadataDescriptor.DWORD_MAXVALUE] applies.
	 * Stream numbers can be specified. But no language index (always
	 * &quot;0&quot;).
	 */
	METADATA_OBJECT(GUID.GUID_METADATA, 16, false, true, false, true);
	/**
	 * @return the containerGUID
	 */
	/**
	 * @return the guidEnabled
	 */
	/**
	 * `true` if the descriptor field can store [GUID] values.
	 */
	val isGuidEnabled: Boolean
	/**
	 * @return the languageEnabled
	 */
	/**
	 * `true` if descriptor field can refer to a language.
	 */
	val isLanguageEnabled: Boolean
	/**
	 * @return the maximumDataLength
	 */
	/**
	 * The maximum amount of bytes the descriptor data may consume.<br></br>
	 */
	val maximumDataLength: BigInteger
	/**
	 * @return the multiValued
	 */
	/**
	 * `true` if the container may store multiple values of the same
	 * metadata descriptor specification (equality on name, language, and
	 * stream).<br></br>
	 * WindowsMedia players advanced tag editor for example stores the
	 * WM/Picture attribute once in the extended content description, and all
	 * others in the metadata library object.
	 */
	val isMultiValued: Boolean

	/**
	 * if `-1` a size value has to be compared against
	 * [.maximumDataLength] because [Long.MAX_VALUE] is exceeded.<br></br>
	 * Otherwise this is the [BigInteger.longValue] representation.
	 */
	private var perfMaxDataLen: Long = 0
	/**
	 * @return the streamEnabled
	 */
	/**
	 * `true` if descriptor field can refer to specific streams.
	 */
	val isStreamNumberEnabled: Boolean

	/**
	 * Creates an instance
	 *
	 * @param guid           see [.containerGUID]
	 * @param maxDataLenBits The amount of bits that is used to represent an unsigned value
	 * for the containers size descriptors. Will create a maximum
	 * value for [.maximumDataLength]. (2 ^ maxDataLenBits -1)
	 * @param guidAllowed    see [.guidEnabled]
	 * @param stream         see [.streamEnabled]
	 * @param language       see [.languageEnabled]
	 * @param multiValue     see [.multiValued]
	 */
	init {
		maximumDataLength = BigInteger.valueOf(2).pow(maxDataLenBits).subtract(BigInteger.ONE)
		if (maximumDataLength.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0) {
			perfMaxDataLen = maximumDataLength.toLong()
		} else {
			perfMaxDataLen = -1
		}
		isGuidEnabled = guidAllowed
		isStreamNumberEnabled = stream
		isLanguageEnabled = language
		isMultiValued = multiValue
	}

	/**
	 * Calls [.checkConstraints] and
	 * actually throws the exception if there is one.
	 *
	 * @param name     name of the descriptor
	 * @param data     content
	 * @param type     data type
	 * @param stream   stream number
	 * @param language language index
	 */
	fun assertConstraints(name: String?, data: ByteArray?, type: Int, stream: Int, language: Int) {
		val result = checkConstraints(name, data, type, stream, language)
		if (result != null) {
			throw result
		}
	}

	/**
	 * Checks if the values for a [content][MetadataDescriptor] match the contraints of the container type, and returns a
	 * [RuntimeException] if the requirements aren't met.
	 *
	 * @param name     name of the descriptor
	 * @param data     content
	 * @param type     data type
	 * @param stream   stream number
	 * @param language language index
	 * @return `null` if everything is fine.
	 */
	fun checkConstraints(
		name: String?,
		data: ByteArray?,
		type: Int,
		stream: Int,
		language: Int
	): RuntimeException? {
		var result: RuntimeException? = null
		// TODO generate tests
		if (name == null || data == null) {
			result = IllegalArgumentException("Arguments must not be null.")
		} else {
			if (!Utils.isStringLengthValidNullSafe(name)) {
				result = IllegalArgumentException(
					ErrorMessage.WMA_LENGTH_OF_STRING_IS_TOO_LARGE.getMsg(name.length)
				)
			}
		}
		if (result == null && !isWithinValueRange(data!!.size.toLong())) {
			result = IllegalArgumentException(
				ErrorMessage.WMA_LENGTH_OF_DATA_IS_TOO_LARGE.getMsg(
					data.size, maximumDataLength, containerGUID.description
				)
			)
		}
		if (result == null && (stream < 0 || stream > MetadataDescriptor.MAX_STREAM_NUMBER || !isStreamNumberEnabled) && stream != 0) {
			val streamAllowed = if (isStreamNumberEnabled) "0 to 127" else "0"
			result = IllegalArgumentException(
				ErrorMessage.WMA_INVALID_STREAM_REFERNCE.getMsg(
					stream,
					streamAllowed,
					containerGUID.description
				)
			)
		}
		if (result == null && type == MetadataDescriptor.TYPE_GUID && !isGuidEnabled) {
			result = IllegalArgumentException(
				ErrorMessage.WMA_INVALID_GUID_USE.getMsg(
					containerGUID.description
				)
			)
		}
		if (result == null && (language != 0 && !isLanguageEnabled || language < 0 || language >= MetadataDescriptor.MAX_LANG_INDEX)) {
			val langAllowed = if (isStreamNumberEnabled) "0 to 126" else "0"
			result = IllegalArgumentException(
				ErrorMessage.WMA_INVALID_LANGUAGE_USE.getMsg(
					language,
					containerGUID.description,
					langAllowed
				)
			)
		}
		if (result == null && this == CONTENT_DESCRIPTION && type != MetadataDescriptor.TYPE_STRING) {
			result = IllegalArgumentException(ErrorMessage.WMA_ONLY_STRING_IN_CD.msg)
		}
		return result
	}

	/**
	 * Tests if the given value is less than or equal to
	 * [.getMaximumDataLength], and greater or equal to zero.<br></br>
	 *
	 * @param value The value to test
	 * @return `true` if size restrictions for binary data are met
	 * with this container type.
	 */
	fun isWithinValueRange(value: Long): Boolean {
		return (perfMaxDataLen == -1L || perfMaxDataLen >= value) && value >= 0
	}

	companion object {
		/**
		 * Determines if low has index as high, in respect to
		 * [.getOrdered]
		 *
		 * @param low
		 * @param high
		 * @return `true` if in correct order.
		 */
		@JvmStatic
		fun areInCorrectOrder(low: ContainerType, high: ContainerType): Boolean {
			val asList = Arrays.asList(*ordered)
			return asList.indexOf(low) <= asList.indexOf(high)
		}

		/**
		 * Returns the elements in an order, that indicates more capabilities
		 * (ascending).<br></br>
		 *
		 * @return capability ordered types
		 */
		@JvmStatic
		val ordered: Array<ContainerType>
			get() = arrayOf(
				CONTENT_DESCRIPTION,
				CONTENT_BRANDING,
				EXTENDED_CONTENT,
				METADATA_OBJECT,
				METADATA_LIBRARY_OBJECT
			)
	}
}
