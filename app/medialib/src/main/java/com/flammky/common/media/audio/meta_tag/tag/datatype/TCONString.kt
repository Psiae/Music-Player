package com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.AbstractTagFrameBody
import java.util.*

/**
 * Overrides in order to properly support the ID3v23 implemenation of TCON
 */
class TCONString : TextEncodedStringSizeTerminated {
	/**
	 * If this field is used with ID3v24 then it is usual to null separate values. Within ID3v23 not many
	 * frames officially support multiple values, so in absense of better solution we use the v24 method, however
	 * some frames such as TCON have there own method and should not null separate values. This can be controlled
	 * by this field.
	 */
	var isNullSeperateMultipleValues = true

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
	constructor(`object`: TCONString?) : super(`object`)

	override fun equals(other: Any?): Boolean {
		return if (this === other) {
			true
		} else other is TCONString && super.equals(
			other
		)
	}

	/**
	 * Add an additional String to the current String value
	 *
	 * @param value
	 */
	override fun addValue(value: String) {
		//For ID3v24 we separate each value by a null
		if (isNullSeperateMultipleValues) {
			this.value = this.value.toString() + "\u0000" + value
		} else {
			//For ID3v23 if they pass a numeric value in brackets this indicates a mapping to an ID3v2 genre and
			//can be seen as a refinement and therefore do not need the non-standard (for ID3v23) null seperator
			if (value.startsWith("(")) {
				this.value = this.value.toString() + value
			} else {
				this.value = this.value.toString() + "\u0000" + value
			}
		}
	}


	override val numberOfValues: Int
		/**
		 * How many values are held, each value is separated by a null terminator
		 *
		 * @return number of values held, usually this will be one.
		 */
		get() = values.size

	/**
	 * Get the nth value
	 *
	 * @param index
	 * @return the nth value
	 * @throws IndexOutOfBoundsException if value does not exist
	 */
	override fun getValueAtIndex(index: Int): String {
		//Split String into separate components
		val values = values
		return values[index]
	}


	override val values: List<String>
		get() =
			if (isNullSeperateMultipleValues) {
				TextEncodedStringSizeTerminated.Companion.splitByNullSeperator(
					value as String
				)
			} else {
				splitV23(value as String)
			}

	override val valueWithoutTrailingNull: String
		/**
		 * Get value(s) whilst removing any trailing nulls
		 *
		 * @return
		 */
		get() {
			val values = values
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
		fun splitV23(value: String): List<String> {
			val valuesarray =
				value.replace("(\\(\\d+\\)|\\(RX\\)|\\(CR\\)\\w*)".toRegex(), "$1\u0000")
					.split("\u0000".toRegex()).dropLastWhile { it.isEmpty() }
					.toTypedArray()
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
