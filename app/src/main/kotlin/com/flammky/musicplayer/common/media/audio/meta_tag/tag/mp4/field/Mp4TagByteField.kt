package com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.field

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getSizeBEInt16
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getSizeBEInt32
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4BoxHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.FieldDataInvalidException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.Mp4FieldKey
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.atom.Mp4DataBox
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer

/**
 * Represents a single byte as a number
 *
 *
 * Usually single byte fields are used as a boolean field, but not always so we dont do this conversion
 */
class Mp4TagByteField : Mp4TagTextField {
	//Holds the actual size of the data content as held in the databoxitem, this is required when creating new
	//items because we cant accurately work out the size by looking at the content because sometimes field must be longer
	//than is actually required to hold the value
	//e.g byte data length seems to be 1 for pgap and cpil but 2 for tmpo, so we stored the dataSize
	//when we loaded the value so if greater than 1 we pad the value.
	private var realDataLength = 0

	//Preserved from data from file
	private var bytedata: ByteArray? = null
	/**
	 * Create new field with known length
	 *
	 * @param id
	 * @param value is a String representation of a number
	 * @param realDataLength
	 * @throws FieldDataInvalidException
	 */
	/**
	 * Create new field
	 *
	 * Assume length of 1 which is correct for most but not all byte fields
	 *
	 * @param id
	 * @param value is a String representation of a number
	 * @throws FieldDataInvalidException
	 */
	@JvmOverloads
	constructor(id: Mp4FieldKey, value: String, realDataLength: Int = 1) : super(
		id.fieldName,
		value
	) {
		this.realDataLength = realDataLength
		//Check that can actually be stored numercially, otherwise will have big problems
		//when try and save the field
		try {
			value.toLong()
		} catch (nfe: NumberFormatException) {
			throw FieldDataInvalidException(
				"Value of:$value is invalid for field:$id"
			)
		}
	}

	/**
	 * Construct from rawdata from audio file
	 *
	 * @param id
	 * @param raw
	 * @throws UnsupportedEncodingException
	 */
	constructor(id: String?, raw: ByteBuffer) : super(id, raw)

	override val fieldType: Mp4FieldType
		get() = Mp4FieldType.INTEGER

	override val dataBytes: ByteArray
		get() {
			return bytedata ?: kotlin.run {
				when (realDataLength) {
					2 -> {
						//Save as two bytes
						val shortValue: Short = content!!.toShort()
						getSizeBEInt16(shortValue)
					}
					1 -> {

						//Save as 1 bytes
						val shortValue: Short = content!!.toShort()
						val rawData = ByteArray(1)
						rawData[0] = shortValue.toByte()
						rawData
					}
					4 -> {

						//Assume could be int
						val intValue: Int = content!!.toInt()
						getSizeBEInt32(intValue)
					}
					else -> {
						throw RuntimeException("$id:$realDataLength:Dont know how to write byte fields of this length")
					}
				}
			}
		}

	@Throws(UnsupportedEncodingException::class)
	override fun build(data: ByteBuffer) {
		//Data actually contains a 'Data' Box so process data using this
		val header = Mp4BoxHeader(data)
		val databox = Mp4DataBox(header, data)
		dataSize = header.dataLength
		//Needed for subsequent write
		realDataLength = dataSize - Mp4DataBox.PRE_DATA_LENGTH
		bytedata = databox.byteData
		content = databox.content
	}

	companion object {
		@JvmField
		var TRUE_VALUE = "1" //when using this field to hold a boolean

		@JvmField
		var FALSE_VALUE = "0"
	}
}
