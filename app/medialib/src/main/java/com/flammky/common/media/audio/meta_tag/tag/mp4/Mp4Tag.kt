/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Raphael Slinckx <raphael@slinckx.net>
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
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.AbstractTag
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.images.AndroidArtwork
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.images.Artwork
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.field.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.field.Mp4GenreField.Companion.isValidGenre
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.field.Mp4TagCoverField.Companion.getMimeTypeForImageType
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * A Logical representation of Mp4Tag, i.e the meta information stored in an Mp4 file underneath the
 * moov.udt.meta.ilst atom.
 */
class Mp4Tag : AbstractTag() {
	/**
	 * Create genre field
	 *
	 *
	 * If the content can be parsed to one of the known values use the genre field otherwise
	 * use the custom field.
	 *
	 * @param content
	 * @return
	 */
	fun createGenreField(content: String?): TagField {
		requireNotNull(content) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }

		//Always write as text
		if (TagOptionSingleton.instance.isWriteMp4GenresAsText) {
			return Mp4TagTextField(
				Mp4FieldKey.GENRE_CUSTOM.fieldName,
				content
			)
		}
		return if (isValidGenre(
				content
			)
		) {
			Mp4GenreField(content)
		} else {
			Mp4TagTextField(
				Mp4FieldKey.GENRE_CUSTOM.fieldName,
				content
			)
		}
	}

	override fun isAllowedEncoding(enc: Charset?): Boolean {
		return StandardCharsets.UTF_8 == enc
	}

	override fun toString(): String {
		return "Mpeg4 " + super.toString()
	}

	/**
	 *
	 * @param genericKey
	 * @return
	 */
	override fun hasField(genericKey: FieldKey?): Boolean {
		return getFields(genericKey).isNotEmpty()
	}

	/**
	 *
	 * @param mp4FieldKey
	 * @return
	 */
	fun hasField(mp4FieldKey: Mp4FieldKey): Boolean {
		return getFields(mp4FieldKey.fieldName).isNotEmpty()
	}

	/**
	 * Maps the generic key to the mp4 key and return the list of values for this field
	 *
	 * @param genericKey
	 */
	@Throws(KeyNotFoundException::class)
	override fun getFields(genericKey: FieldKey?): List<TagField?> {
		if (genericKey == null) {
			throw KeyNotFoundException()
		}
		val mp4FieldKey = mapping[genericKey]
			?: throw KeyNotFoundException()
		var list = getFields(mp4FieldKey.fieldName)
		val filteredList: MutableList<TagField?> = ArrayList()
		return if (genericKey === FieldKey.KEY) {
			if (list.isEmpty()) {
				list = getFields(Mp4FieldKey.KEY_OLD.fieldName)
			}
			list
		} else if (genericKey === FieldKey.GENRE) {
			if (list.isEmpty()) {
				list = getFields(Mp4FieldKey.GENRE_CUSTOM.fieldName)
			}
			list
		} else if (genericKey === FieldKey.TRACK) {
			for (next in list) {
				val trackField = next as Mp4TrackField?
				if (trackField!!.trackNo > 0) {
					filteredList.add(next)
				}
			}
			filteredList
		} else if (genericKey === FieldKey.TRACK_TOTAL) {
			for (next in list) {
				val trackField = next as Mp4TrackField?
				if (trackField!!.trackTotal > 0) {
					filteredList.add(next)
				}
			}
			filteredList
		} else if (genericKey === FieldKey.DISC_NO) {
			for (next in list) {
				val discNoField = next as Mp4DiscNoField?
				if (discNoField!!.discNo > 0) {
					filteredList.add(next)
				}
			}
			filteredList
		} else if (genericKey === FieldKey.DISC_TOTAL) {
			for (next in list) {
				val discNoField = next as Mp4DiscNoField?
				if (discNoField!!.discTotal > 0) {
					filteredList.add(next)
				}
			}
			filteredList
		} else {
			list
		}
	}

	/**
	 * Maps the generic key to the specific key and return the list of values for this field as strings
	 *
	 * @param genericKey
	 * @return
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	override fun getAll(genericKey: FieldKey?): List<String?>? {
		val values: MutableList<String?> = ArrayList()
		val fields = getFields(genericKey)
		for (tagfield in fields) {
			if (genericKey === FieldKey.TRACK) {
				values.add((tagfield as Mp4TrackField?)!!.trackNo.toString())
			} else if (genericKey === FieldKey.TRACK_TOTAL) {
				values.add((tagfield as Mp4TrackField?)!!.trackTotal.toString())
			} else if (genericKey === FieldKey.DISC_NO) {
				values.add((tagfield as Mp4DiscNoField?)!!.discNo.toString())
			} else if (genericKey === FieldKey.DISC_TOTAL) {
				values.add((tagfield as Mp4DiscNoField?)!!.discTotal.toString())
			} else {
				values.add(tagfield.toString())
			}
		}
		return values
	}

	/**
	 * Retrieve the  values that exists for this mp4keyId (this is the internalid actually used)
	 *
	 *
	 * @param mp4FieldKey
	 * @throws KeyNotFoundException
	 * @return
	 */
	@Throws(KeyNotFoundException::class)
	operator fun get(mp4FieldKey: Mp4FieldKey?): List<TagField?>? {
		if (mp4FieldKey == null) {
			throw KeyNotFoundException()
		}
		return super.getFields(mp4FieldKey.fieldName)
	}

	/**
	 * Retrieve the indexed value that exists for this generic key
	 *
	 * @param genericKey
	 * @return
	 */
	@Throws(KeyNotFoundException::class)
	override fun getValue(genericKey: FieldKey?, n: Int): String? {
		val fields = getFields(genericKey)
		if (fields.size > n) {
			val field = fields[n]
			return if (genericKey === FieldKey.TRACK) {
				(field as Mp4TrackField?)!!.trackNo.toString()
			} else if (genericKey === FieldKey.DISC_NO) {
				(field as Mp4DiscNoField?)!!.discNo.toString()
			} else if (genericKey === FieldKey.TRACK_TOTAL) {
				(field as Mp4TrackField?)!!.trackTotal.toString()
			} else if (genericKey === FieldKey.DISC_TOTAL) {
				(field as Mp4DiscNoField?)!!.discTotal.toString()
			} else {
				field.toString()
			}
		}
		return ""
	}

	/**
	 * Retrieve the first value that exists for this mp4key
	 *
	 * @param mp4Key
	 * @return
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	fun getFirst(mp4Key: Mp4FieldKey?): String? {
		if (mp4Key == null) {
			throw KeyNotFoundException()
		}
		return super.getFirst(mp4Key.fieldName)
	}

	@Throws(KeyNotFoundException::class)
	override fun getFirstField(id: FieldKey?): Mp4TagField? {
		val fields = getFields(id)
		return if (fields.size == 0) {
			null
		} else fields[0] as Mp4TagField?
	}

	@Throws(KeyNotFoundException::class)
	fun getFirstField(mp4Key: Mp4FieldKey?): Mp4TagField? {
		if (mp4Key == null) {
			throw KeyNotFoundException()
		}
		return super.getFirstField(mp4Key.fieldName) as Mp4TagField?
	}

	/**
	 * Delete fields with this generic key
	 *
	 * @param genericKey
	 */
	@Throws(KeyNotFoundException::class)
	override fun deleteField(genericKey: FieldKey?) {
		if (genericKey == null) {
			throw KeyNotFoundException()
		}
		val mp4FieldName = mapping[genericKey]!!.fieldName
		if (genericKey === FieldKey.KEY) {
			deleteField(Mp4FieldKey.KEY_OLD)
			deleteField(mp4FieldName)
		} else if (genericKey === FieldKey.TRACK) {
			val trackTotal = this.getFirst(FieldKey.TRACK_TOTAL)
			if (trackTotal!!.length == 0) {
				super.deleteField(mp4FieldName)
				return
			} else {
				val field = this.getFirstField(FieldKey.TRACK_TOTAL) as Mp4TrackField?
				field!!.setTrackNo(0)
				return
			}
		} else if (genericKey === FieldKey.TRACK_TOTAL) {
			val track = this.getFirst(FieldKey.TRACK)
			if (track!!.length == 0) {
				super.deleteField(mp4FieldName)
				return
			} else {
				val field = this.getFirstField(FieldKey.TRACK) as Mp4TrackField?
				field!!.setTrackTotal(0)
				return
			}
		} else if (genericKey === FieldKey.DISC_NO) {
			val discTotal = this.getFirst(FieldKey.DISC_TOTAL)
			if (discTotal!!.length == 0) {
				super.deleteField(mp4FieldName)
				return
			} else {
				val field = this.getFirstField(FieldKey.DISC_TOTAL) as Mp4DiscNoField?
				field!!.setDiscNo(0)
				return
			}
		} else if (genericKey === FieldKey.DISC_TOTAL) {
			val discno = this.getFirst(FieldKey.DISC_NO)
			if (discno!!.length == 0) {
				super.deleteField(mp4FieldName)
				return
			} else {
				val field = this.getFirstField(FieldKey.DISC_NO) as Mp4DiscNoField?
				field!!.setDiscTotal(0)
				return
			}
		} else if (genericKey === FieldKey.GENRE) {
			super.deleteField(Mp4FieldKey.GENRE.fieldName)
			super.deleteField(Mp4FieldKey.GENRE_CUSTOM.fieldName)
		} else {
			super.deleteField(mp4FieldName)
		}
	}

	/**
	 * Delete fields with this mp4key
	 *
	 * @param mp4Key
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	fun deleteField(mp4Key: Mp4FieldKey?) {
		if (mp4Key == null) {
			throw KeyNotFoundException()
		}
		super.deleteField(mp4Key.fieldName)
	}

	/**
	 * Create artwork field
	 *
	 * @param data raw image data
	 * @return
	 * @throws FieldDataInvalidException
	 */
	fun createArtworkField(data: ByteArray?): TagField {
		return Mp4TagCoverField(
			data!!
		)
	}

	/**
	 * Create artwork field
	 *
	 * @return
	 */
	@Throws(FieldDataInvalidException::class)
	override fun createField(artwork: Artwork?): TagField? {
		return artwork?.binaryData?.let { Mp4TagCoverField(it) }
	}

	/**
	 * Create new field and add it to the tag, with special handling for trackNo, discNo fields as we dont want multiple
	 * fields to be created for these fields
	 *
	 * @param genericKey
	 * @param value
	 * @throws KeyNotFoundException
	 * @throws FieldDataInvalidException
	 */
	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun addField(genericKey: FieldKey?, vararg value: String?) {
		if (genericKey === FieldKey.TRACK || genericKey === FieldKey.TRACK_TOTAL || genericKey === FieldKey.DISC_NO || genericKey === FieldKey.DISC_TOTAL) {
			setField(genericKey, *value)
		} else {
			val tagfield = createField(genericKey, *value)
			addField(tagfield)
		}
	}

	/**
	 * Create Tag Field using generic key
	 *
	 * This should use the correct subclass for the key
	 *
	 * @param genericKey
	 * @param values
	 * @return
	 * @throws KeyNotFoundException
	 * @throws FieldDataInvalidException
	 */
	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun createField(genericKey: FieldKey?, vararg values: String?): TagField? {
		if (genericKey == null) {
			throw KeyNotFoundException()
		}
		val value = values[0] ?: return null

		//Special handling for these number fields because multiple generic keys map to a single mp4 field
		if (genericKey === FieldKey.TRACK || genericKey === FieldKey.TRACK_TOTAL || genericKey === FieldKey.DISC_NO || genericKey === FieldKey.DISC_TOTAL) {
			try {
				val number = value.toInt()
				if (genericKey === FieldKey.TRACK) {
					return Mp4TrackField(number)
				} else if (genericKey === FieldKey.TRACK_TOTAL) {
					return Mp4TrackField(0, number)
				} else if (genericKey === FieldKey.DISC_NO) {
					return Mp4DiscNoField(number)
				} else if (genericKey === FieldKey.DISC_TOTAL) {
					return Mp4DiscNoField(0, number)
				}
			} catch (nfe: NumberFormatException) {
				//If not number we want to convertMetadata to an expected exception (which is not a RuntimeException)
				//so can be handled properly by calling program
				throw FieldDataInvalidException(
					"Value $value is not a number as required", nfe
				)
			}
		} else if (genericKey === FieldKey.GENRE) {
			//Always write as text
			if (TagOptionSingleton.instance.isWriteMp4GenresAsText) {
				return Mp4TagTextField(
					Mp4FieldKey.GENRE_CUSTOM.fieldName,
					value
				)
			}
			return if (isValidGenre(value)) {
				Mp4GenreField(value)
			} else {
				Mp4TagTextField(Mp4FieldKey.GENRE_CUSTOM.fieldName, value)
			}
		}
		//Default for all other fields
		return createField(mapping[genericKey], value)
	}

	/**
	 * Overidden to ensure cannot have both a genre field and a custom genre field
	 *
	 * @param genericKey
	 * @param value
	 * @throws KeyNotFoundException
	 * @throws FieldDataInvalidException
	 */
	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun setField(genericKey: FieldKey?, vararg value: String?) {
		val tagfield = createField(genericKey, *value)
		if (genericKey === FieldKey.GENRE) {
			if (tagfield!!.id == Mp4FieldKey.GENRE.fieldName) {
				this.deleteField(Mp4FieldKey.GENRE_CUSTOM)
			} else if (tagfield.id == Mp4FieldKey.GENRE_CUSTOM.fieldName) {
				this.deleteField(Mp4FieldKey.GENRE)
			}
		}
		setField(tagfield)
	}

	/**
	 * Set mp4 field
	 * @param fieldKey
	 * @param value
	 * @throws KeyNotFoundException
	 * @throws FieldDataInvalidException
	 */
	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	fun setField(fieldKey: Mp4FieldKey?, value: String?) {
		val tagfield = createField(fieldKey, value)
		setField(tagfield)
	}

	/**
	 * Set field, special handling for track and disc because they hold two fields
	 *
	 * @param field
	 */
	override fun setField(field: TagField?) {
		if (field == null) {
			return
		}
		if (field.id == Mp4FieldKey.TRACK.fieldName) {
			val list: List<TagField?> = getFields(field.id)
			if (list == null || list.isEmpty()) {
				super.setField(field)
			} else {
				val existingTrackField = list[0] as Mp4TrackField
				val newTrackField = field as Mp4TrackField
				var trackNo = existingTrackField.trackNo
				var trackTotal = existingTrackField.trackTotal
				if (newTrackField.trackNo > 0) {
					trackNo = newTrackField.trackNo
				}
				if (newTrackField.trackTotal > 0) {
					trackTotal = newTrackField.trackTotal
				}
				val mergedTrackField = Mp4TrackField(trackNo.toInt(), trackTotal.toInt())
				super.setField(mergedTrackField)
			}
		} else if (field.id == Mp4FieldKey.DISCNUMBER.fieldName) {
			val list: List<TagField?> = getFields(field.id)
			if (list == null || list.isEmpty()) {
				super.setField(field)
			} else {
				val existingDiscNoField = list[0] as Mp4DiscNoField
				val newDiscNoField = field as Mp4DiscNoField
				var discNo = existingDiscNoField.discNo
				var discTotal = existingDiscNoField.discTotal
				if (newDiscNoField.discNo > 0) {
					discNo = newDiscNoField.discNo
				}
				if (newDiscNoField.discTotal > 0) {
					discTotal = newDiscNoField.discTotal
				}
				val mergedDiscNoField = Mp4DiscNoField(discNo.toInt(), discTotal.toInt())
				super.setField(mergedDiscNoField)
			}
		} else {
			super.setField(field)
		}
	}

	/**
	 * Create Tag Field using mp4 key
	 *
	 * Uses the correct subclass for the key
	 *
	 * @param mp4FieldKey
	 * @param value
	 * @return
	 * @throws KeyNotFoundException
	 * @throws FieldDataInvalidException
	 */
	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	fun createField(mp4FieldKey: Mp4FieldKey?, value: String?): TagField? {
		requireNotNull(value) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
		if (mp4FieldKey == null) {
			throw KeyNotFoundException()
		}

		//This is boolean stored as 1, but calling program might setField as 'true' so we handle this
		//case internally , any other values it is set to we treat as false
		return if (mp4FieldKey === Mp4FieldKey.COMPILATION) {
			if (value.equals("true", ignoreCase = true) || value == "1") {
				createCompilationField(true)
			} else {
				createCompilationField(false)
			}
		} else if (mp4FieldKey === Mp4FieldKey.GENRE) {
			if (isValidGenre(
					value
				)
			) {
				Mp4GenreField(value)
			} else {
				throw IllegalArgumentException(ErrorMessage.`NOT_STANDARD_MP$_GENRE`.msg)
			}
		} else if (mp4FieldKey === Mp4FieldKey.GENRE_CUSTOM) {
			Mp4TagTextField(
				Mp4FieldKey.GENRE_CUSTOM.fieldName,
				value
			)
		} else if (mp4FieldKey.subClassFieldType === Mp4TagFieldSubType.DISC_NO) {
			Mp4DiscNoField(value)
		} else if (mp4FieldKey.subClassFieldType === Mp4TagFieldSubType.TRACK_NO) {
			Mp4TrackField(value)
		} else if (mp4FieldKey.subClassFieldType === Mp4TagFieldSubType.BYTE) {
			Mp4TagByteField(
				mp4FieldKey,
				value,
				mp4FieldKey.fieldLength
			)
		} else if (mp4FieldKey.subClassFieldType === Mp4TagFieldSubType.NUMBER) {
			Mp4TagTextNumberField(
				mp4FieldKey.fieldName,
				value
			)
		} else if (mp4FieldKey.subClassFieldType === Mp4TagFieldSubType.REVERSE_DNS) {
			Mp4TagReverseDnsField(
				mp4FieldKey,
				value
			)
		} else if (mp4FieldKey.subClassFieldType === Mp4TagFieldSubType.ARTWORK) {
			throw UnsupportedOperationException(ErrorMessage.ARTWORK_CANNOT_BE_CREATED_WITH_THIS_METHOD.msg)
		} else if (mp4FieldKey.subClassFieldType === Mp4TagFieldSubType.TEXT) {
			Mp4TagTextField(
				mp4FieldKey.fieldName,
				value
			)
		} else if (mp4FieldKey.subClassFieldType === Mp4TagFieldSubType.UNKNOWN) {
			throw UnsupportedOperationException(
				ErrorMessage.DO_NOT_KNOW_HOW_TO_CREATE_THIS_ATOM_TYPE.getMsg(
					mp4FieldKey.fieldName
				)
			)
		} else {
			throw UnsupportedOperationException(
				ErrorMessage.DO_NOT_KNOW_HOW_TO_CREATE_THIS_ATOM_TYPE.getMsg(
					mp4FieldKey.fieldName
				)
			)
		}
	}

	override val artworkList: List<Artwork>
		get() {
			val coverartList = get(Mp4FieldKey.ARTWORK) ?: return emptyList()
			val artworkList: MutableList<Artwork> = ArrayList(coverartList.size)
			for (next in coverartList) {
				val mp4CoverArt = next as? Mp4TagCoverField ?: continue
				val artwork = AndroidArtwork()
				artwork.binaryData = mp4CoverArt.data
				artwork.mimeType = getMimeTypeForImageType(mp4CoverArt.fieldType) ?: ""
				artworkList.add(artwork)
			}
			return artworkList
		}

	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun createCompilationField(origValue: Boolean): TagField {
		var value = ""
		return if (origValue) {
			value =
				Mp4TagByteField.TRUE_VALUE
			Mp4TagByteField(
				Mp4FieldKey.COMPILATION,
				value,
				Mp4FieldKey.COMPILATION.fieldLength
			)
		} else {
			value =
				Mp4TagByteField.FALSE_VALUE
			Mp4TagByteField(
				Mp4FieldKey.COMPILATION,
				value,
				Mp4FieldKey.COMPILATION.fieldLength
			)
		}
	}

	companion object {
		val mapping = EnumMap<FieldKey, Mp4FieldKey>(
			FieldKey::class.java
		)

		//Mapping from generic key to mp4 key
		init {
			mapping[FieldKey.ACOUSTID_FINGERPRINT] =
				Mp4FieldKey.ACOUSTID_FINGERPRINT
			mapping[FieldKey.ACOUSTID_ID] =
				Mp4FieldKey.ACOUSTID_ID
			mapping[FieldKey.ALBUM] =
				Mp4FieldKey.ALBUM
			mapping[FieldKey.ALBUM_ARTIST] =
				Mp4FieldKey.ALBUM_ARTIST
			mapping[FieldKey.ALBUM_ARTIST_SORT] =
				Mp4FieldKey.ALBUM_ARTIST_SORT
			mapping[FieldKey.ALBUM_ARTISTS] =
				Mp4FieldKey.ALBUM_ARTISTS
			mapping[FieldKey.ALBUM_ARTISTS_SORT] =
				Mp4FieldKey.ALBUM_ARTISTS_SORT
			mapping[FieldKey.ALBUM_SORT] =
				Mp4FieldKey.ALBUM_SORT
			mapping[FieldKey.ALBUM_YEAR] =
				Mp4FieldKey.ALBUM_YEAR
			mapping[FieldKey.AMAZON_ID] =
				Mp4FieldKey.ASIN
			mapping[FieldKey.ARRANGER] = Mp4FieldKey.ARRANGER
			mapping[FieldKey.ARRANGER_SORT] = Mp4FieldKey.ARRANGER_SORT
			mapping[FieldKey.ARTIST] = Mp4FieldKey.ARTIST
			mapping[FieldKey.ARTISTS] = Mp4FieldKey.ARTISTS
			mapping[FieldKey.ARTIST_SORT] = Mp4FieldKey.ARTIST_SORT
			mapping[FieldKey.ARTISTS_SORT] =
				Mp4FieldKey.ARTISTS_SORT
			mapping[FieldKey.BARCODE] =
				Mp4FieldKey.BARCODE
			mapping[FieldKey.BPM] =
				Mp4FieldKey.BPM
			mapping[FieldKey.CATALOG_NO] =
				Mp4FieldKey.CATALOGNO
			mapping[FieldKey.CHOIR] = Mp4FieldKey.CHOIR
			mapping[FieldKey.CHOIR_SORT] =
				Mp4FieldKey.CHOIR_SORT
			mapping[FieldKey.CLASSICAL_CATALOG] =
				Mp4FieldKey.CLASSICAL_CATALOG
			mapping[FieldKey.CLASSICAL_NICKNAME] =
				Mp4FieldKey.CLASSICAL_NICKNAME
			mapping[FieldKey.COMMENT] =
				Mp4FieldKey.COMMENT
			mapping[FieldKey.COMPOSER] =
				Mp4FieldKey.COMPOSER
			mapping[FieldKey.COMPOSER_SORT] =
				Mp4FieldKey.COMPOSER_SORT
			mapping[FieldKey.CONDUCTOR] =
				Mp4FieldKey.CONDUCTOR
			mapping[FieldKey.COUNTRY] =
				Mp4FieldKey.COUNTRY
			mapping[FieldKey.CONDUCTOR_SORT] =
				Mp4FieldKey.CONDUCTOR_SORT
			mapping[FieldKey.COPYRIGHT] =
				Mp4FieldKey.COPYRIGHT
			mapping[FieldKey.COVER_ART] =
				Mp4FieldKey.ARTWORK
			mapping[FieldKey.CUSTOM1] =
				Mp4FieldKey.MM_CUSTOM_1
			mapping[FieldKey.CUSTOM2] =
				Mp4FieldKey.MM_CUSTOM_2
			mapping[FieldKey.CUSTOM3] =
				Mp4FieldKey.MM_CUSTOM_3
			mapping[FieldKey.CUSTOM4] =
				Mp4FieldKey.MM_CUSTOM_4
			mapping[FieldKey.CUSTOM5] = Mp4FieldKey.MM_CUSTOM_5
			mapping[FieldKey.DISC_NO] =
				Mp4FieldKey.DISCNUMBER
			mapping[FieldKey.DISC_SUBTITLE] =
				Mp4FieldKey.DISC_SUBTITLE
			mapping[FieldKey.DISC_TOTAL] =
				Mp4FieldKey.DISCNUMBER
			mapping[FieldKey.DJMIXER] =
				Mp4FieldKey.DJMIXER
			mapping[FieldKey.DJMIXER_SORT] = Mp4FieldKey.DJMIXER_SORT
			mapping[FieldKey.MOOD_ELECTRONIC] =
				Mp4FieldKey.MOOD_ELECTRONIC
			mapping[FieldKey.ENCODER] = Mp4FieldKey.ENCODER
			mapping[FieldKey.ENGINEER] =
				Mp4FieldKey.ENGINEER
			mapping[FieldKey.ENGINEER_SORT] =
				Mp4FieldKey.ENGINEER_SORT
			mapping[FieldKey.ENSEMBLE] = Mp4FieldKey.ENSEMBLE
			mapping[FieldKey.ENSEMBLE_SORT] =
				Mp4FieldKey.ENSEMBLE_SORT
			mapping[FieldKey.FBPM] =
				Mp4FieldKey.FBPM
			mapping[FieldKey.GENRE] = Mp4FieldKey.GENRE
			mapping[FieldKey.GROUP] =
				Mp4FieldKey.GROUP
			mapping[FieldKey.GROUPING] =
				Mp4FieldKey.GROUPING
			mapping[FieldKey.INSTRUMENT] =
				Mp4FieldKey.INSTRUMENT
			mapping[FieldKey.INVOLVEDPEOPLE] =
				Mp4FieldKey.INVOLVEDPEOPLE
			mapping[FieldKey.IPI] =
				Mp4FieldKey.IPI
			mapping[FieldKey.ISRC] =
				Mp4FieldKey.ISRC
			mapping[FieldKey.ISWC] = Mp4FieldKey.ISWC
			mapping[FieldKey.IS_COMPILATION] = Mp4FieldKey.COMPILATION
			mapping[FieldKey.IS_CLASSICAL] =
				Mp4FieldKey.IS_CLASSICAL
			mapping[FieldKey.IS_GREATEST_HITS] =
				Mp4FieldKey.IS_GREATEST_HITS
			mapping[FieldKey.IS_HD] =
				Mp4FieldKey.IS_HD
			mapping[FieldKey.IS_LIVE] =
				Mp4FieldKey.IS_LIVE
			mapping[FieldKey.IS_SOUNDTRACK] = Mp4FieldKey.IS_SOUNDTRACK
			mapping[FieldKey.JAIKOZ_ID] =
				Mp4FieldKey.JAIKOZ_ID
			mapping[FieldKey.KEY] = Mp4FieldKey.KEY
			mapping[FieldKey.LANGUAGE] =
				Mp4FieldKey.LANGUAGE
			mapping[FieldKey.LYRICIST] =
				Mp4FieldKey.LYRICIST
			mapping[FieldKey.LYRICIST_SORT] = Mp4FieldKey.LYRICIST_SORT
			mapping[FieldKey.LYRICS] =
				Mp4FieldKey.LYRICS
			mapping[FieldKey.MEDIA] =
				Mp4FieldKey.MEDIA
			mapping[FieldKey.MIXER] =
				Mp4FieldKey.MIXER
			mapping[FieldKey.MIXER_SORT] =
				Mp4FieldKey.MIXER_SORT
			mapping[FieldKey.MOOD] =
				Mp4FieldKey.MOOD
			mapping[FieldKey.MOOD_ACOUSTIC] = Mp4FieldKey.MOOD_ACOUSTIC
			mapping[FieldKey.MOOD_AGGRESSIVE] =
				Mp4FieldKey.MOOD_AGGRESSIVE
			mapping[FieldKey.MOOD_AROUSAL] = Mp4FieldKey.MOOD_AROUSAL
			mapping[FieldKey.MOOD_DANCEABILITY] =
				Mp4FieldKey.MOOD_DANCEABILITY
			mapping[FieldKey.MOOD_HAPPY] = Mp4FieldKey.MOOD_HAPPY
			mapping[FieldKey.MOOD_INSTRUMENTAL] =
				Mp4FieldKey.MOOD_INSTRUMENTAL
			mapping[FieldKey.MOOD_PARTY] =
				Mp4FieldKey.MOOD_PARTY
			mapping[FieldKey.MOOD_RELAXED] =
				Mp4FieldKey.MOOD_RELAXED
			mapping[FieldKey.MOOD_SAD] =
				Mp4FieldKey.MOOD_SAD
			mapping[FieldKey.MOOD_VALENCE] = Mp4FieldKey.MOOD_VALENCE
			mapping[FieldKey.MOVEMENT] =
				Mp4FieldKey.MOVEMENT
			mapping[FieldKey.MOVEMENT_NO] = Mp4FieldKey.MOVEMENT_NO
			mapping[FieldKey.MOVEMENT_TOTAL] =
				Mp4FieldKey.MOVEMENT_TOTAL
			mapping[FieldKey.MUSICBRAINZ_WORK] =
				Mp4FieldKey.MUSICBRAINZ_WORK
			mapping[FieldKey.MUSICBRAINZ_ARTISTID] =
				Mp4FieldKey.MUSICBRAINZ_ARTISTID
			mapping[FieldKey.MUSICBRAINZ_DISC_ID] =
				Mp4FieldKey.MUSICBRAINZ_DISCID
			mapping[FieldKey.MUSICBRAINZ_ORIGINAL_RELEASE_ID] =
				Mp4FieldKey.MUSICBRAINZ_ORIGINALALBUMID
			mapping[FieldKey.MUSICBRAINZ_RELEASEARTISTID] =
				Mp4FieldKey.MUSICBRAINZ_ALBUMARTISTID
			mapping[FieldKey.MUSICBRAINZ_RELEASEID] =
				Mp4FieldKey.MUSICBRAINZ_ALBUMID
			mapping[FieldKey.MUSICBRAINZ_RELEASE_COUNTRY] = Mp4FieldKey.RELEASECOUNTRY
			mapping[FieldKey.MUSICBRAINZ_RELEASE_GROUP_ID] =
				Mp4FieldKey.MUSICBRAINZ_RELEASE_GROUPID
			mapping[FieldKey.MUSICBRAINZ_RELEASE_STATUS] =
				Mp4FieldKey.MUSICBRAINZ_ALBUM_STATUS
			mapping[FieldKey.MUSICBRAINZ_RELEASE_TRACK_ID] =
				Mp4FieldKey.MUSICBRAINZ_RELEASE_TRACKID
			mapping[FieldKey.MUSICBRAINZ_RELEASE_TYPE] =
				Mp4FieldKey.MUSICBRAINZ_ALBUM_TYPE
			mapping[FieldKey.MUSICBRAINZ_TRACK_ID] =
				Mp4FieldKey.MUSICBRAINZ_TRACKID
			mapping[FieldKey.MUSICBRAINZ_WORK_ID] =
				Mp4FieldKey.MUSICBRAINZ_WORKID
			mapping[FieldKey.MUSICBRAINZ_RECORDING_WORK_ID] =
				Mp4FieldKey.MUSICBRAINZ_RECORDING_WORK_ID
			mapping[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL1_ID] =
				Mp4FieldKey.MUSICBRAINZ_WORK_PART_LEVEL1_ID
			mapping[FieldKey.MUSICBRAINZ_RECORDING_WORK] =
				Mp4FieldKey.MUSICBRAINZ_RECORDING_WORK
			mapping[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL1] =
				Mp4FieldKey.MUSICBRAINZ_WORK_PART_LEVEL1
			mapping[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL1_TYPE] =
				Mp4FieldKey.MUSICBRAINZ_WORK_PART_LEVEL1_TYPE
			mapping[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL2_ID] =
				Mp4FieldKey.MUSICBRAINZ_WORK_PART_LEVEL2_ID
			mapping[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL2] =
				Mp4FieldKey.MUSICBRAINZ_WORK_PART_LEVEL2
			mapping[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL2_TYPE] =
				Mp4FieldKey.MUSICBRAINZ_WORK_PART_LEVEL2_TYPE
			mapping[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL3_ID] =
				Mp4FieldKey.MUSICBRAINZ_WORK_PART_LEVEL3_ID
			mapping[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL3] =
				Mp4FieldKey.MUSICBRAINZ_WORK_PART_LEVEL3
			mapping[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL3_TYPE] =
				Mp4FieldKey.MUSICBRAINZ_WORK_PART_LEVEL3_TYPE
			mapping[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL4_ID] =
				Mp4FieldKey.MUSICBRAINZ_WORK_PART_LEVEL4_ID
			mapping[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL4] =
				Mp4FieldKey.MUSICBRAINZ_WORK_PART_LEVEL4
			mapping[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL4_TYPE] =
				Mp4FieldKey.MUSICBRAINZ_WORK_PART_LEVEL4_TYPE
			mapping[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL5_ID] =
				Mp4FieldKey.MUSICBRAINZ_WORK_PART_LEVEL5_ID
			mapping[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL5] =
				Mp4FieldKey.MUSICBRAINZ_WORK_PART_LEVEL5
			mapping[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL5_TYPE] =
				Mp4FieldKey.MUSICBRAINZ_WORK_PART_LEVEL5_TYPE
			mapping[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL6_ID] =
				Mp4FieldKey.MUSICBRAINZ_WORK_PART_LEVEL6_ID
			mapping[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL6] =
				Mp4FieldKey.MUSICBRAINZ_WORK_PART_LEVEL6
			mapping[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL6_TYPE] =
				Mp4FieldKey.MUSICBRAINZ_WORK_PART_LEVEL6_TYPE
			mapping[FieldKey.MUSICIP_ID] =
				Mp4FieldKey.MUSICIP_PUID
			mapping[FieldKey.OCCASION] =
				Mp4FieldKey.MM_OCCASION
			mapping[FieldKey.OPUS] =
				Mp4FieldKey.OPUS
			mapping[FieldKey.ORCHESTRA] =
				Mp4FieldKey.ORCHESTRA
			mapping[FieldKey.ORCHESTRA_SORT] = Mp4FieldKey.ORCHESTRA_SORT
			mapping[FieldKey.ORIGINAL_ALBUM] =
				Mp4FieldKey.MM_ORIGINAL_ALBUM_TITLE
			mapping[FieldKey.ORIGINALRELEASEDATE] =
				Mp4FieldKey.ORIGINALRELEASEDATE
			mapping[FieldKey.ORIGINAL_ARTIST] =
				Mp4FieldKey.MM_ORIGINAL_ARTIST
			mapping[FieldKey.ORIGINAL_LYRICIST] =
				Mp4FieldKey.MM_ORIGINAL_LYRICIST
			mapping[FieldKey.ORIGINAL_YEAR] =
				Mp4FieldKey.MM_ORIGINAL_YEAR
			mapping[FieldKey.OVERALL_WORK] = Mp4FieldKey.OVERALL_WORK
			mapping[FieldKey.PART] = Mp4FieldKey.PART
			mapping[FieldKey.PART_NUMBER] =
				Mp4FieldKey.PART_NUMBER
			mapping[FieldKey.PART_TYPE] =
				Mp4FieldKey.PART_TYPE
			mapping[FieldKey.PERFORMER] =
				Mp4FieldKey.PERFORMER
			mapping[FieldKey.PERFORMER_NAME] =
				Mp4FieldKey.PERFORMER_NAME
			mapping[FieldKey.PERFORMER_NAME_SORT] =
				Mp4FieldKey.PERFORMER_NAME_SORT
			mapping[FieldKey.PERIOD] =
				Mp4FieldKey.PERIOD
			mapping[FieldKey.PRODUCER] = Mp4FieldKey.PRODUCER
			mapping[FieldKey.PRODUCER_SORT] =
				Mp4FieldKey.PRODUCER_SORT
			mapping[FieldKey.QUALITY] =
				Mp4FieldKey.MM_QUALITY
			mapping[FieldKey.RANKING] =
				Mp4FieldKey.RANKING
			mapping[FieldKey.RATING] =
				Mp4FieldKey.SCORE
			mapping[FieldKey.RECORD_LABEL] = Mp4FieldKey.LABEL
			mapping[FieldKey.RECORDINGDATE] =
				Mp4FieldKey.RECORDINGDATE
			mapping[FieldKey.RECORDINGSTARTDATE] =
				Mp4FieldKey.RECORDINGSTARTDATE
			mapping[FieldKey.RECORDINGENDDATE] =
				Mp4FieldKey.RECORDINGENDDATE
			mapping[FieldKey.RECORDINGLOCATION] =
				Mp4FieldKey.RECORDINGLOCATION
			mapping[FieldKey.REMIXER] =
				Mp4FieldKey.REMIXER
			mapping[FieldKey.ROONALBUMTAG] = Mp4FieldKey.ROONALBUMTAG
			mapping[FieldKey.ROONTRACKTAG] = Mp4FieldKey.ROONTRACKTAG
			mapping[FieldKey.SCRIPT] =
				Mp4FieldKey.SCRIPT
			mapping[FieldKey.SECTION] =
				Mp4FieldKey.SECTION
			mapping[FieldKey.SINGLE_DISC_TRACK_NO] =
				Mp4FieldKey.SINGLE_DISC_TRACK_NO
			mapping[FieldKey.SONGKONG_ID] = Mp4FieldKey.SONGKONG_ID
			mapping[FieldKey.SUBTITLE] = Mp4FieldKey.SUBTITLE
			mapping[FieldKey.TAGS] =
				Mp4FieldKey.TAGS
			mapping[FieldKey.TEMPO] = Mp4FieldKey.TEMPO
			mapping[FieldKey.TIMBRE] =
				Mp4FieldKey.TIMBRE
			mapping[FieldKey.TITLE] =
				Mp4FieldKey.TITLE
			mapping[FieldKey.TITLE_MOVEMENT] =
				Mp4FieldKey.TITLE_MOVEMENT
			mapping[FieldKey.TITLE_SORT] = Mp4FieldKey.TITLE_SORT
			mapping[FieldKey.TONALITY] =
				Mp4FieldKey.TONALITY
			mapping[FieldKey.TRACK] =
				Mp4FieldKey.TRACK
			mapping[FieldKey.TRACK_TOTAL] =
				Mp4FieldKey.TRACK
			mapping[FieldKey.URL_DISCOGS_ARTIST_SITE] =
				Mp4FieldKey.URL_DISCOGS_ARTIST_SITE
			mapping[FieldKey.URL_DISCOGS_RELEASE_SITE] =
				Mp4FieldKey.URL_DISCOGS_RELEASE_SITE
			mapping[FieldKey.URL_LYRICS_SITE] =
				Mp4FieldKey.URL_LYRICS_SITE
			mapping[FieldKey.URL_OFFICIAL_ARTIST_SITE] =
				Mp4FieldKey.URL_OFFICIAL_ARTIST_SITE
			mapping[FieldKey.URL_OFFICIAL_RELEASE_SITE] =
				Mp4FieldKey.URL_OFFICIAL_RELEASE_SITE
			mapping[FieldKey.URL_WIKIPEDIA_ARTIST_SITE] =
				Mp4FieldKey.URL_WIKIPEDIA_ARTIST_SITE
			mapping[FieldKey.URL_WIKIPEDIA_RELEASE_SITE] =
				Mp4FieldKey.URL_WIKIPEDIA_RELEASE_SITE
			mapping[FieldKey.VERSION] = Mp4FieldKey.VERSION
			mapping[FieldKey.WORK] =
				Mp4FieldKey.WORK
			mapping[FieldKey.YEAR] =
				Mp4FieldKey.DAY
			mapping[FieldKey.WORK_TYPE] = Mp4FieldKey.WORK_TYPE
		}
	}
}
