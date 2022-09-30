package com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.field

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4BoxHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.FieldDataInvalidException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.Mp4FieldKey
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.atom.Mp4DataBox
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer

/**
 * Represents the Disc No field
 *
 *
 * Contains some reserved fields that we currently ignore
 *
 * Reserved:2 bytes
 * Disc Number:2 bytes
 * Total no of Discs:2 bytes
 *
 */

/** [org.jaudiotagger.tag.mp4.field.Mp4DiscNoField] */
class Mp4DiscNoField : Mp4TagTextNumberField {
	/**
	 * Create new Disc Field parsing the String for the discno/total
	 *
	 * @param discValue
	 * @throws FieldDataInvalidException
	 */
	constructor(discValue: String) : super(Mp4FieldKey.DISCNUMBER.fieldName, discValue) {
		numbers = ArrayList()
		val numbers = numbers as MutableList<Short>
		numbers.add("0".toShort())
		val values = discValue.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		when (values.size) {
			1 -> {
				try {
					numbers.add(values[0].toShort())
				} catch (nfe: NumberFormatException) {
					throw FieldDataInvalidException("Value of:" + values[0] + " is invalid for field:" + id)
				}
				numbers.add("0".toShort())
			}
			2 -> {
				try {
					numbers.add(values[0].toShort())
				} catch (nfe: NumberFormatException) {
					throw FieldDataInvalidException("Value of:" + values[0] + " is invalid for field:" + id)
				}
				try {
					numbers.add(values[1].toShort())
				} catch (nfe: NumberFormatException) {
					throw FieldDataInvalidException("Value of:" + values[1] + " is invalid for field:" + id)
				}
			}
			else -> throw FieldDataInvalidException(
				"Value is invalid for field:$id"
			)
		}
	}

	/**
	 * Create new Disc No field with only discNo
	 *
	 * @param discNo
	 */
	constructor(discNo: Int) : super(Mp4FieldKey.DISCNUMBER.fieldName, discNo.toString()) {
		numbers = ArrayList()
		val numbers = numbers as MutableList<Short>
		numbers.add("0".toShort())
		numbers.add(discNo.toShort())
		numbers.add("0".toShort())
	}

	/**
	 * Create new Disc No Field with Disc No and total number of discs
	 *
	 * @param discNo
	 * @param total
	 */
	constructor(discNo: Int, total: Int) : super(
		Mp4FieldKey.DISCNUMBER.fieldName,
		discNo.toString()
	) {
		numbers = ArrayList()
		val numbers = numbers as MutableList<Short>
		numbers.add("0".toShort())
		numbers.add(discNo.toShort())
		numbers.add(total.toShort())
	}

	constructor(id: String?, data: ByteBuffer) : super(id, data)

	@Throws(UnsupportedEncodingException::class)
	override fun build(data: ByteBuffer) {
		//Data actually contains a 'Data' Box so process data using this
		val header = Mp4BoxHeader(data)
		val databox = Mp4DataBox(header, data)
		dataSize = header.dataLength
		numbers = databox.getNumbers().toMutableList()

		//Disc number always hold four values, we can discard the first one and last one, the second one is the disc no
		//and the third is the total no of discs so only use if not zero
		val sb = StringBuffer()
		if (numbers!!.size > DISC_NO_INDEX && numbers!![DISC_NO_INDEX] > 0) {
			sb.append(numbers!![DISC_NO_INDEX])
		}
		if (numbers!!.size > DISC_TOTAL_INDEX && numbers!![DISC_TOTAL_INDEX] > 0) {
			sb.append("/").append(numbers!![DISC_TOTAL_INDEX])
		}
		content = sb.toString()
	}

	/**
	 * @return
	 */
	val discNo: Short
		get() = numbers!![DISC_NO_INDEX]

	/**
	 * Set Disc No
	 *
	 * @param discNo
	 */
	fun setDiscNo(discNo: Int) {
		numbers!![DISC_NO_INDEX] = discNo.toShort()
	}

	/**
	 * @return
	 */
	val discTotal: Short
		get() = if (numbers!!.size <= DISC_TOTAL_INDEX) {
			0
		} else numbers!![DISC_TOTAL_INDEX]

	/**
	 * Set total number of discs
	 *
	 * @param discTotal
	 */
	fun setDiscTotal(discTotal: Int) {
		numbers!![DISC_TOTAL_INDEX] = discTotal.toShort()
	}

	companion object {
		private const val DISC_NO_INDEX = 1
		private const val DISC_TOTAL_INDEX = 2
	}
}
