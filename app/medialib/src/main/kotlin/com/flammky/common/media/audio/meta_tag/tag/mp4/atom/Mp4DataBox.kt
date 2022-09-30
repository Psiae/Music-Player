package com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.atom

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getIntBE
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getShortBE
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getString
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.AbstractMp4Box
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4BoxHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.field.Mp4FieldType
import java.nio.ByteBuffer

/**
 * This box is used within both normal metadat boxes and ---- boxes to hold the actual data.
 *
 *
 * Format is as follows:
 * :length          (4 bytes)
 * :name 'Data'     (4 bytes)
 * :atom version    (1 byte)
 * :atom type flags (3 bytes)
 * :locale field    (4 bytes) //Currently always zero
 * :data
 */
class Mp4DataBox(header: Mp4BoxHeader, dataBuffer: ByteBuffer) : AbstractMp4Box() {
	val type: Int
	var content: String? = null

	//Holds the numbers decoded
	private var numbers: MutableList<Short> = mutableListOf()

	/**
	 * Return raw byte data only vaid for byte fields
	 *
	 * @return byte data
	 */
	//Holds bytedata for byte fields as is not clear for multibyte fields exactly these should be wrttien

	/** [org.jaudiotagger.tag.mp4.atom.Mp4DataBox] */
	var byteData: ByteArray = byteArrayOf()
		private set


	init {
		this.header = header
		//Double check
		if (header.id != IDENTIFIER) {
			throw RuntimeException("Unable to process data box because identifier is:" + header.id)
		}

		//Make slice so operations here don't effect position of main buffer
		data = dataBuffer.slice()
		val data = data!!

		//Type
		type = getIntBE(
			data, TYPE_POS, TYPE_POS + TYPE_LENGTH - 1
		)
		if (type == Mp4FieldType.TEXT.fileClassId) {
			content =
				getString(data, PRE_DATA_LENGTH, header.dataLength - PRE_DATA_LENGTH, header.encoding)
		} else if (type == Mp4FieldType.IMPLICIT.fileClassId || type == Mp4FieldType.GENRES.fileClassId) {
			numbers = ArrayList()
			for (i in 0 until (header.dataLength - PRE_DATA_LENGTH) / NUMBER_LENGTH) {
				val number = getShortBE(
					data,
					PRE_DATA_LENGTH + i * NUMBER_LENGTH,
					PRE_DATA_LENGTH + i * NUMBER_LENGTH + (NUMBER_LENGTH - 1)
				)
				numbers.add(number)
			}

			//Make String representation  (separate values with slash)
			val sb = StringBuffer()
			val iterator: ListIterator<Short> = numbers.listIterator()
			while (iterator.hasNext()) {
				sb.append(iterator.next())
				if (iterator.hasNext()) {
					sb.append("/")
				}
			}
			content = sb.toString()
		} else if (type == Mp4FieldType.INTEGER.fileClassId) {
			//TODO byte data length seems to be 1 for pgap and cpil but 2 for tmpo ?
			//Create String representation for display
			content = getIntBE(
				data, PRE_DATA_LENGTH, header.dataLength - 1
			).toString() + ""

			//But store data for safer writing back to file
			byteData = ByteArray(header.dataLength - PRE_DATA_LENGTH)
			val pos = dataBuffer.position()
			dataBuffer.position(pos + PRE_DATA_LENGTH)
			dataBuffer[byteData]
			dataBuffer.position(pos)

			//Songbird uses this type for trkn atom (normally implicit type) is used so just added this code so can be used
			//by the Mp4TrackField atom

			numbers = ArrayList()
			for (i in 0 until (header.dataLength - PRE_DATA_LENGTH) / NUMBER_LENGTH) {
				val number = getShortBE(
					data,
					PRE_DATA_LENGTH + i * NUMBER_LENGTH,
					PRE_DATA_LENGTH + i * NUMBER_LENGTH + (NUMBER_LENGTH - 1)
				)
				numbers.add(number)
			}


		} else if (type == Mp4FieldType.COVERART_JPEG.fileClassId) {
			content = getString(
				data, PRE_DATA_LENGTH, header.dataLength - PRE_DATA_LENGTH, header.encoding
			)
		}
	}

	/**
	 * Return numbers, only valid for numeric fields
	 *
	 * @return numbers
	 */
	//TODO this is only applicable for numeric databoxes, should we subclass dont know type until start
	//constructing and we also have Mp4tagTextNumericField class as well
	fun getNumbers(): List<Short> {
		return numbers
	}

	companion object {
		const val IDENTIFIER = "data"
		const val VERSION_LENGTH = 1
		const val TYPE_LENGTH = 3
		const val NULL_LENGTH = 4
		const val PRE_DATA_LENGTH = VERSION_LENGTH + TYPE_LENGTH + NULL_LENGTH
		const val DATA_HEADER_LENGTH = Mp4BoxHeader.HEADER_LENGTH + PRE_DATA_LENGTH
		const val TYPE_POS = VERSION_LENGTH

		//For use externally
		const val TYPE_POS_INCLUDING_HEADER = Mp4BoxHeader.HEADER_LENGTH + TYPE_POS
		const val NUMBER_LENGTH = 2
	}
}
