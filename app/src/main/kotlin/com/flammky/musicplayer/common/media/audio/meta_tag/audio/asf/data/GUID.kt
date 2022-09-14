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

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import java.util.*
import java.util.regex.Pattern

/**
 * This class is used for representation of GUIDs and as a reference list of all
 * Known GUIDs. <br></br>
 *
 * @author Christian Laireiter
 */
class GUID {
	/**
	 * @return Returns the description.
	 */
	/**
	 * Stores an optionally description of the GUID.
	 */
	var description = ""
		private set

	/**
	 * An instance of this class stores the value of the wrapped GUID in this
	 * field. <br></br>
	 */
	private var guidData: IntArray? = null

	/**
	 * Stores the hash code of the object.<br></br>
	 * `"-1"` if not determined yet.
	 */
	private var hash = 0

	/**
	 * Creates an instance and assigns given `value`.<br></br>
	 *
	 * @param value GUID, which should be assigned. (will be converted to int[])
	 */
	constructor(value: ByteArray?) {
		assert(value != null)
		val tmp = IntArray(value!!.size)
		for (i in value.indices) {
			tmp[i] = 0xFF and value[i].toInt()
		}
		gUID = tmp
	}

	/**
	 * Creates an instance and assigns given `value`.<br></br>
	 *
	 * @param value GUID, which should be assigned.
	 */
	constructor(value: IntArray) {
		gUID = value
	}

	/**
	 * Creates an instance like [.GUID]and sets the optional
	 * description. <br></br>
	 *
	 * @param value GUID, which should be assigned.
	 * @param desc  Description for the GUID.
	 */
	constructor(value: IntArray, desc: String?) : this(value) {
		requireNotNull(desc) { "Argument must not be null." }
		description = desc
	}

	/**
	 * Creates an instance like [.GUID] and sets the optional
	 * description. (the int[] is obtained by [GUID.parseGUID]) <br></br>
	 *
	 * @param guidString GUID, which should be assigned.
	 * @param desc       Description for the GUID.
	 */
	constructor(guidString: String?, desc: String?) : this(parseGUID(guidString).gUID) {
		requireNotNull(desc) { "Argument must not be null." }
		description = desc
	}

	/**
	 * This method compares two objects. If the given Object is a [GUID],
	 * the stored GUID values are compared. <br></br>
	 *
	 * @see Object.equals
	 */
	override fun equals(obj: Any?): Boolean {
		var result = false
		if (obj is GUID) {
			result = Arrays.equals(gUID, obj.gUID)
		}
		return result
	}

	/**
	 * This method returns the GUID as an array of bytes. <br></br>
	 *
	 * @return The GUID as a byte array.
	 * @see .getGUID
	 */
	val bytes: ByteArray
		get() {
			val result = ByteArray(guidData!!.size)
			for (i in result.indices) {
				result[i] = (guidData!![i] and 0xFF).toByte()
			}
			return result
		}
	/**
	 * This method returns the GUID of this object. <br></br>
	 *
	 * @return stored GUID.
	 */
	/**
	 * This method saves a copy of the given `value` as the
	 * represented value of this object. <br></br>
	 * The given value is checked with [.assertGUID].<br></br>
	 *
	 * @param value GUID to assign.
	 */
	var gUID: IntArray
		get() {
			val copy = IntArray(guidData!!.size)
			System.arraycopy(guidData, 0, copy, 0, guidData!!.size)
			return copy
		}
		private set(value) {
			if (assertGUID(value)) {
				guidData = IntArray(GUID_LENGTH)
				System.arraycopy(value, 0, guidData, 0, GUID_LENGTH)
			} else {
				throw IllegalArgumentException("The given guidData doesn't match the GUID specification.")
			}
		}

	/**
	 * Convenience method to get 2digit hex values of each byte.
	 *
	 * @param bytes bytes to convert.
	 * @return each byte as 2 digit hex.
	 */
	private fun getHex(bytes: ByteArray): Array<String?> {
		val result = arrayOfNulls<String>(bytes.size)
		val tmp = StringBuilder()
		for (i in bytes.indices) {
			tmp.delete(0, tmp.length)
			tmp.append(Integer.toHexString(0xFF and bytes[i].toInt()))
			if (tmp.length == 1) {
				tmp.insert(0, "0")
			}
			result[i] = tmp.toString()
		}
		return result
	}

	/**
	 * {@inheritDoc}
	 */
	override fun hashCode(): Int {
		if (hash == -1) {
			var tmp = 0
			for (curr in gUID) {
				tmp = tmp * 31 + curr
			}
			hash = tmp
		}
		return hash
	}

	/**
	 * This method checks if the currently stored GUID ([.guidData]) is
	 * correctly filled. <br></br>
	 *
	 * @return `true` if it is.
	 */
	val isValid: Boolean
		get() = assertGUID(
			gUID
		)

	/**
	 * This method gives a hex formatted representation of [.getGUID]
	 *
	 * @return hex formatted representation.
	 */
	fun prettyPrint(): String {
		val result = StringBuilder()
		var descr: String? = description
		if (Utils.isBlank(descr)) {
			descr = getGuidDescription(this)
		}
		if (!Utils.isBlank(descr)) {
			result.append("Description: ").append(descr).append(Utils.LINE_SEPARATOR).append("   ")
		}
		result.append(this.toString())
		return result.toString()
	}

	/**
	 * {@inheritDoc}
	 */
	override fun toString(): String {
		// C5F8CBEA-5BAF-4877-8467-AA8C44FA4CCA
		// 0xea, 0xcb,0xf8, 0xc5, 0xaf, 0x5b, 0x77, 0x48, 0x84, 0x67, 0xaa,
		// 0x8c, 0x44,0xfa, 0x4c, 0xca
		val result = StringBuilder()
		val bytes = getHex(bytes)
		result.append(bytes[3])
		result.append(bytes[2])
		result.append(bytes[1])
		result.append(bytes[0])
		result.append('-')
		result.append(bytes[5])
		result.append(bytes[4])
		result.append('-')
		result.append(bytes[7])
		result.append(bytes[6])
		result.append('-')
		result.append(bytes[8])
		result.append(bytes[9])
		result.append('-')
		result.append(bytes[10])
		result.append(bytes[11])
		result.append(bytes[12])
		result.append(bytes[13])
		result.append(bytes[14])
		result.append(bytes[15])
		return result.toString()
	}

	companion object {
		/**
		 * This constant defines the GUID for stream chunks describing audio
		 * streams, indicating the the audio stream has no error concealment. <br></br>
		 */
		val GUID_AUDIO_ERROR_CONCEALEMENT_ABSENT = GUID(
			intArrayOf(
				0x40,
				0xA4,
				0xF1,
				0x49,
				0xCE,
				0x4E,
				0xD0,
				0x11,
				0xA3,
				0xAC,
				0x00,
				0xA0,
				0xC9,
				0x03,
				0x48,
				0xF6
			), "Audio error concealment absent."
		)

		/**
		 * This constant defines the GUID for stream chunks describing audio
		 * streams, indicating the the audio stream has interleaved error
		 * concealment. <br></br>
		 */
		val GUID_AUDIO_ERROR_CONCEALEMENT_INTERLEAVED = GUID(
			intArrayOf(
				0x40,
				0xA4,
				0xF1,
				0x49,
				0xCE,
				0x4E,
				0xD0,
				0x11,
				0xA3,
				0xAC,
				0x00,
				0xA0,
				0xC9,
				0x03,
				0x48,
				0xF6
			), "Interleaved audio error concealment."
		)

		/**
		 * This constant stores the GUID indicating that stream type is audio.
		 */
		@JvmField
		val GUID_AUDIOSTREAM = GUID(
			intArrayOf(
				0x40,
				0x9E,
				0x69,
				0xF8,
				0x4D,
				0x5B,
				0xCF,
				0x11,
				0xA8,
				0xFD,
				0x00,
				0x80,
				0x5F,
				0x5C,
				0x44,
				0x2B
			), " Audio stream"
		)

		/**
		 * This constant stores the GUID indicating a content branding object.
		 */
		@JvmField
		val GUID_CONTENT_BRANDING = GUID(
			intArrayOf(
				0xFA,
				0xB3,
				0x11,
				0x22,
				0x23,
				0xBD,
				0xD2,
				0x11,
				0xB4,
				0xB7,
				0x00,
				0xA0,
				0xC9,
				0x55,
				0xFC,
				0x6E
			), "Content Branding"
		)

		/**
		 * This is for the Content Encryption Object
		 * 2211B3FB-BD23-11D2-B4B7-00A0C955FC6E, needs to be little-endian.
		 */
		@JvmField
		val GUID_CONTENT_ENCRYPTION = GUID(
			intArrayOf(
				0xfb,
				0xb3,
				0x11,
				0x22,
				0x23,
				0xbd,
				0xd2,
				0x11,
				0xb4,
				0xb7,
				0x00,
				0xa0,
				0xc9,
				0x55,
				0xfc,
				0x6e
			), "Content Encryption Object"
		)

		/**
		 * This constant represents the guidData for a chunk which contains Title,
		 * author, copyright, description and rating.
		 */
		@JvmField
		val GUID_CONTENTDESCRIPTION = GUID(
			intArrayOf(
				0x33,
				0x26,
				0xB2,
				0x75,
				0x8E,
				0x66,
				0xCF,
				0x11,
				0xA6,
				0xD9,
				0x00,
				0xAA,
				0x00,
				0x62,
				0xCE,
				0x6C
			), "Content Description"
		)

		/**
		 * This constant stores the GUID for Encoding-Info chunks.
		 */
		@JvmField
		val GUID_ENCODING = GUID(
			intArrayOf(
				0x40,
				0x52,
				0xD1,
				0x86,
				0x1D,
				0x31,
				0xD0,
				0x11,
				0xA3,
				0xA4,
				0x00,
				0xA0,
				0xC9,
				0x03,
				0x48,
				0xF6
			), "Encoding description"
		)

		/**
		 * This constant defines the GUID for a WMA "Extended Content Description"
		 * chunk. <br></br>
		 */
		val GUID_EXTENDED_CONTENT_DESCRIPTION = GUID(
			intArrayOf(
				0x40,
				0xA4,
				0xD0,
				0xD2,
				0x07,
				0xE3,
				0xD2,
				0x11,
				0x97,
				0xF0,
				0x00,
				0xA0,
				0xC9,
				0x5E,
				0xA8,
				0x50
			), "Extended Content Description"
		)

		/**
		 * GUID of ASF file header.
		 */
		@JvmField
		val GUID_FILE = GUID(
			intArrayOf(
				0xA1,
				0xDC,
				0xAB,
				0x8C,
				0x47,
				0xA9,
				0xCF,
				0x11,
				0x8E,
				0xE4,
				0x00,
				0xC0,
				0x0C,
				0x20,
				0x53,
				0x65
			), "File header"
		)

		/**
		 * This constant defines the GUID of a asf header chunk.
		 */
		@JvmField
		val GUID_HEADER = GUID(
			intArrayOf(
				0x30,
				0x26,
				0xb2,
				0x75,
				0x8e,
				0x66,
				0xcf,
				0x11,
				0xa6,
				0xd9,
				0x00,
				0xaa,
				0x00,
				0x62,
				0xce,
				0x6c
			), "Asf header"
		)

		/**
		 * This constant stores a GUID whose functionality is unknown.
		 */
		@JvmField
		val GUID_HEADER_EXTENSION = GUID(
			intArrayOf(
				0xB5,
				0x03,
				0xBF,
				0x5F,
				0x2E,
				0xA9,
				0xCF,
				0x11,
				0x8E,
				0xE3,
				0x00,
				0xC0,
				0x0C,
				0x20,
				0x53,
				0x65
			), "Header Extension"
		)

		/**
		 * This constant stores the GUID indicating the asf language list object.<br></br>
		 */
		@JvmField
		val GUID_LANGUAGE_LIST = GUID(
			intArrayOf(
				0xa9,
				0x46,
				0x43,
				0x7c,
				0xe0,
				0xef,
				0xfc,
				0x4b,
				0xb2,
				0x29,
				0x39,
				0x3e,
				0xde,
				0x41,
				0x5c,
				0x85
			), "Language List"
		)

		/**
		 * This constant stores the length of GUIDs used with ASF streams. <br></br>
		 */
		const val GUID_LENGTH = 16

		/**
		 * This constant stores the GUID indicating the asf metadata object.<br></br>
		 */
		val GUID_METADATA = GUID(
			intArrayOf(
				0xea,
				0xcb,
				0xf8,
				0xc5,
				0xaf,
				0x5b,
				0x77,
				0x48,
				0x84,
				0x67,
				0xaa,
				0x8c,
				0x44,
				0xfa,
				0x4c,
				0xca
			), "Metadata"
		)

		/**
		 * This constant stores the GUID indicating the asf metadata library object.<br></br>
		 */
		val GUID_METADATA_LIBRARY = GUID(
			intArrayOf(
				0x94,
				0x1c,
				0x23,
				0x44,
				0x98,
				0x94,
				0xd1,
				0x49,
				0xa1,
				0x41,
				0x1d,
				0x13,
				0x4e,
				0x45,
				0x70,
				0x54
			), "Metadata Library"
		)

		/**
		 * The GUID String values format.<br></br>
		 */
		private val GUID_PATTERN = Pattern.compile(
			"[a-f0-9]{8}\\-[a-f0-9]{4}\\-[a-f0-9]{4}\\-[a-f0-9]{4}\\-[a-f0-9]{12}",
			Pattern.CASE_INSENSITIVE
		)

		/**
		 * This constant stores the GUID indicating a stream object.
		 */
		@JvmField
		val GUID_STREAM = GUID(
			intArrayOf(
				0x91,
				0x07,
				0xDC,
				0xB7,
				0xB7,
				0xA9,
				0xCF,
				0x11,
				0x8E,
				0xE6,
				0x00,
				0xC0,
				0x0C,
				0x20,
				0x53,
				0x65
			), "Stream"
		)

		/**
		 * This constant stores a GUID indicating a "stream bitrate properties"
		 * chunk.
		 */
		@JvmField
		val GUID_STREAM_BITRATE_PROPERTIES = GUID(
			intArrayOf(
				0xCE,
				0x75,
				0xF8,
				0x7B,
				0x8D,
				0x46,
				0xD1,
				0x11,
				0x8D,
				0x82,
				0x00,
				0x60,
				0x97,
				0xC9,
				0xA2,
				0xB2
			), "Stream bitrate properties"
		)

		/**
		 * This map is used, to get the description of a GUID instance, which has
		 * been created by reading.<br></br>
		 * The map comparison is done against the [GUID.guidData] field. But
		 * only the [.KNOWN_GUIDS] have a description set.
		 */
		private val GUID_TO_CONFIGURED: MutableMap<GUID, GUID>

		/**
		 * This constant represents a GUID implementation which can be used for
		 * generic implementations, which have to provide a GUID, but do not really
		 * require a specific GUID to work.
		 */
		@JvmField
		val GUID_UNSPECIFIED = GUID(
			intArrayOf(
				0x00,
				0x00,
				0x00,
				0x00,
				0x00,
				0x00,
				0x00,
				0x00,
				0x00,
				0x00,
				0x00,
				0x00,
				0x00,
				0x00,
				0x00,
				0x00
			), "Unspecified"
		)

		/**
		 * This constant stores the GUID indicating that stream type is video.
		 */
		@JvmField
		val GUID_VIDEOSTREAM = GUID(
			intArrayOf(
				0xC0,
				0xEF,
				0x19,
				0xBC,
				0x4D,
				0x5B,
				0xCF,
				0x11,
				0xA8,
				0xFD,
				0x00,
				0x80,
				0x5F,
				0x5C,
				0x44,
				0x2B
			), "Video stream"
		)

		/**
		 * This field stores all known GUIDs.
		 */
		val KNOWN_GUIDS: Array<GUID>

		/**
		 * This constant stores the GUID for a &quot;script command object&quot;.<br></br>
		 */
		val SCRIPT_COMMAND_OBJECT = GUID(
			intArrayOf(
				0x30,
				0x1a,
				0xfb,
				0x1e,
				0x62,
				0x0b,
				0xd0,
				0x11,
				0xa3,
				0x9b,
				0x00,
				0xa0,
				0xc9,
				0x03,
				0x48,
				0xf6
			), "Script Command Object"
		)

		init {
			KNOWN_GUIDS = arrayOf(
				GUID_AUDIO_ERROR_CONCEALEMENT_ABSENT,
				GUID_CONTENTDESCRIPTION,
				GUID_AUDIOSTREAM,
				GUID_ENCODING,
				GUID_FILE,
				GUID_HEADER,
				GUID_STREAM,
				GUID_EXTENDED_CONTENT_DESCRIPTION,
				GUID_VIDEOSTREAM,
				GUID_HEADER_EXTENSION,
				GUID_STREAM_BITRATE_PROPERTIES,
				SCRIPT_COMMAND_OBJECT,
				GUID_CONTENT_ENCRYPTION,
				GUID_CONTENT_BRANDING,
				GUID_UNSPECIFIED,
				GUID_METADATA_LIBRARY,
				GUID_METADATA,
				GUID_LANGUAGE_LIST
			)
			GUID_TO_CONFIGURED = HashMap(KNOWN_GUIDS.size)
			for (curr in KNOWN_GUIDS) {
				assert(!GUID_TO_CONFIGURED.containsKey(curr)) {
					"Double definition: \"" +
						GUID_TO_CONFIGURED[curr]?.description + "\" <-> \"" + curr.description + "\""
				}
				GUID_TO_CONFIGURED[curr] = curr
			}
		}

		/**
		 * This method checks if the given `value` is matching the GUID
		 * specification of ASF streams. <br></br>
		 *
		 * @param value possible GUID.
		 * @return `true` if `value` matches the specification
		 * of a GUID.
		 */
		fun assertGUID(value: IntArray?): Boolean {
			return value != null && value.size == GUID_LENGTH
		}

		/**
		 * This method looks up a GUID instance from [.KNOWN_GUIDS] which
		 * matches the value of the given GUID.
		 *
		 * @param orig GUID to look up.
		 * @return a GUID instance from [.KNOWN_GUIDS] if available.
		 * `null` else.
		 */
		fun getConfigured(orig: GUID): GUID? {
			// safe against null
			return GUID_TO_CONFIGURED[orig]
		}

		/**
		 * This method searches a GUID in [.KNOWN_GUIDS]which is equal to the
		 * given `guidData` and returns its description. <br></br>
		 * This method is useful if a GUID was read out of a file and no
		 * identification has been done yet.
		 *
		 * @param guid GUID, which description is needed.
		 * @return description of the GUID if found. Else `null`
		 */
		fun getGuidDescription(guid: GUID?): String? {
			var result: String? = null
			requireNotNull(guid) { "Argument must not be null." }
			if (getConfigured(guid) != null) {
				result = getConfigured(
					guid
				)!!.description
			}
			return result
		}

		/**
		 * This method parses a String as GUID.<br></br>
		 * The format is like the one in the ASF specification.<br></br>
		 * An Example: `C5F8CBEA-5BAF-4877-8467-AA8C44FA4CCA`<br></br>
		 *
		 * @param guid the string to parse.
		 * @return the GUID.
		 * @throws GUIDFormatException If the GUID has an invalid format.
		 */
		@JvmStatic
		@Throws(GUIDFormatException::class)
		fun parseGUID(guid: String?): GUID {
			if (guid == null) {
				throw GUIDFormatException("null")
			}
			if (!GUID_PATTERN.matcher(guid).matches()) {
				throw GUIDFormatException("Invalid guidData format.")
			}
			val bytes = IntArray(GUID_LENGTH)
			/*
	 * Don't laugh, but did not really come up with a nicer solution today
	 */
			val arrayIndices = intArrayOf(3, 2, 1, 0, 5, 4, 7, 6, 8, 9, 10, 11, 12, 13, 14, 15)
			var arrayPointer = 0
			var i = 0
			while (i < guid.length) {
				if (guid[i] == '-') {
					i++
					continue
				}
				bytes[arrayIndices[arrayPointer++]] = guid.substring(i, i + 2).toInt(16)
				i++
				i++
			}
			return GUID(bytes)
		}
	}
}
