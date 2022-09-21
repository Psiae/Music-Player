package com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.field

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4BoxHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.Mp4FieldKey
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.atom.Mp4DataBox
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.GenreTypes
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer

/**
 * Represents the Genre field , when user has selected from the set list of genres
 *
 *
 * This class allows you to retrieve either the internal genreid, or the display value
 */
class Mp4GenreField : Mp4TagTextNumberField {
	constructor(id: String?, data: ByteBuffer) : super(id, data)

	/**
	 * Construct genre, if cant find match just default to first genre
	 *
	 * @param genreId key into ID3v1 list (offset by one) or String value in ID3list
	 */
	constructor(genreId: String) : super(Mp4FieldKey.GENRE.fieldName, genreId) {

		//Is it an id
		try {
			var genreVal = genreId.toShort()
			if (genreVal <= GenreTypes.maxStandardGenreId) {
				numbers = ArrayList()
				numbers!!.add(++genreVal)
				return
			}
			//Default
			numbers = ArrayList()
			numbers!!.add(1.toShort())
			return
		} catch (nfe: NumberFormatException) {
			//Do Nothing test as String instead
		}

		//Is it the String value ?
		val id3GenreId = GenreTypes.instanceOf.getIdForValue(genreId)
		if (id3GenreId != null) {
			if (id3GenreId <= GenreTypes.maxStandardGenreId) {
				numbers = ArrayList()
				numbers!!.add((id3GenreId + 1).toShort())
				return
			}
		}
		numbers = ArrayList()
		numbers!!.add(1.toShort())
	}

	@Throws(UnsupportedEncodingException::class)
	override fun build(data: ByteBuffer) {
		//Data actually contains a 'Data' Box so process data using this
		val header = Mp4BoxHeader(data)
		val databox = Mp4DataBox(header, data)
		dataSize = header.dataLength
		numbers = databox.getNumbers().toMutableList()
		if (numbers != null && numbers!!.size > 0) {
			val genreId = numbers!![0].toInt()
			//Get value, we have to adjust index by one because iTunes labels from one instead of zero
			content = GenreTypes.instanceOf.getValueForId(genreId - 1)
			//Some apps set genre to invalid value, we dont disguise this by setting content to empty string we leave
			//as null so apps can handle if they wish, but we do display a warning to make them aware.
			if (content == null) {
				logger.warning(ErrorMessage.MP4_GENRE_OUT_OF_RANGE.getMsg(genreId))
			}
		} else {
			logger.warning(ErrorMessage.MP4_NO_GENREID_FOR_GENRE.getMsg(header.dataLength))
		}
	}

	companion object {
		/**
		 * Precheck to see if the value is a valid genre or whether you should use a custom genre.
		 *
		 * @param genreId
		 * @return
		 */
		@JvmStatic
		fun isValidGenre(genreId: String): Boolean {
			//Is it an id (within old id3 range)
			try {
				val genreVal = genreId.toShort()
				if (genreVal - 1 <= GenreTypes.maxStandardGenreId) {
					return true
				}
			} catch (nfe: NumberFormatException) {
				//Do Nothing test as String instead
			}

			//Is it the String value ?
			val id3GenreId = GenreTypes.instanceOf.getIdForValue(genreId)
			if (id3GenreId != null) {
				if (id3GenreId <= GenreTypes.maxStandardGenreId) {
					return true
				}
			}
			return false
		}
	}
}
