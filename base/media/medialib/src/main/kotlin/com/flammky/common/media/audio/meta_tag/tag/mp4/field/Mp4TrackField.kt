package com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.field

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4BoxHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.FieldDataInvalidException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.Mp4FieldKey
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.atom.Mp4DataBox
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer

/**
 * Represents the Track No field
 *
 *
 * There are a number of reserved fields making matters more complicated
 * Reserved:2 bytes
 * Track Number:2 bytes
 * No of Tracks:2 bytes (or zero if not known)
 * PlayListTitleReserved: 1 byte
 * playtitlenameReserved:0 bytes
 *
 */
class Mp4TrackField : Mp4TagTextNumberField {
	/**
	 * Create new Track Field parsing the String for the trackno/total
	 *
	 * @param trackValue
	 * @throws FieldDataInvalidException
	 */
	constructor(trackValue: String) : super(Mp4FieldKey.TRACK.fieldName, trackValue) {
		numbers = ArrayList()
		val numbers = numbers!!

		numbers.add("0".toShort())
		val values = trackValue.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		when (values.size) {
			1 -> {
				try {
					numbers.add(values[0].toShort())
				} catch (nfe: NumberFormatException) {
					throw FieldDataInvalidException("Value of:" + values[0] + " is invalid for field:" + id)
				}
				numbers.add("0".toShort())
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
				numbers.add("0".toShort())
			}
			else -> throw FieldDataInvalidException(
				"Value is invalid for field:$id"
			)
		}
	}

	/**
	 * Create new Track Field with only track No
	 *
	 * @param trackNo
	 */
	constructor(trackNo: Int) : super(Mp4FieldKey.TRACK.fieldName, trackNo.toString()) {
		numbers = ArrayList()
		val numbers = numbers!!
		numbers.add("0".toShort())
		numbers.add(trackNo.toShort())
		numbers.add("0".toShort())
		numbers.add("0".toShort())
	}

	/**
	 * Create new Track Field with track No and total tracks
	 *
	 * @param trackNo
	 * @param total
	 */
	constructor(trackNo: Int, total: Int) : super(Mp4FieldKey.TRACK.fieldName, trackNo.toString()) {
		numbers = ArrayList()
		val numbers = numbers!!
		numbers.add("0".toShort())
		numbers.add(trackNo.toShort())
		numbers.add(total.toShort())
		numbers.add("0".toShort())
	}

	/**
	 * Construct from filedata
	 *
	 * @param id
	 * @param data
	 * @throws UnsupportedEncodingException
	 */
	constructor(id: String?, data: ByteBuffer) : super(id, data)

	@Throws(UnsupportedEncodingException::class)
	override fun build(data: ByteBuffer) {
		//Data actually contains a 'Data' Box so process data using this
		val header = Mp4BoxHeader(data)
		val databox = Mp4DataBox(header, data)
		dataSize = header.dataLength
		numbers = databox.getNumbers().toMutableList()
		//Track number always hold three values, we can discard the first one, the second one is the track no
		//and the third is the total no of tracks so only use if not zero
		val sb = StringBuffer()
		if (numbers != null) {
			if (numbers!!.size > TRACK_NO_INDEX && numbers!![TRACK_NO_INDEX] > 0) {
				sb.append(numbers!![TRACK_NO_INDEX])
			}
			if (numbers!!.size > TRACK_TOTAL_INDEX && numbers!![TRACK_TOTAL_INDEX] > 0) {
				sb.append("/").append(numbers!![TRACK_TOTAL_INDEX])
			}
		}
		content = sb.toString()
	}

	/**
	 * @return
	 */
	val trackNo: Short
		get() = if (numbers?.get(TRACK_NO_INDEX) != null) {
			numbers!![TRACK_NO_INDEX]
		} else 0

	/**
	 * @return
	 */
	val trackTotal: Short
		get() = if (numbers?.get(TRACK_NO_INDEX) != null) {
			numbers!![TRACK_TOTAL_INDEX]
		} else 0

	/**
	 * Set Track No
	 *
	 * @param trackNo
	 */
	fun setTrackNo(trackNo: Int) {
		numbers!![TRACK_NO_INDEX] = trackNo.toShort()
	}

	/**
	 * Set total number of tracks
	 *
	 * @param trackTotal
	 */
	fun setTrackTotal(trackTotal: Int) {
		numbers!![TRACK_TOTAL_INDEX] = trackTotal.toShort()
	}

	companion object {
		private const val TRACK_NO_INDEX = 1
		private const val TRACK_TOTAL_INDEX = 2
	}
}
