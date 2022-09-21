package com.flammky.musicplayer.common.media.audio.meta_tag.tag.asf

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.AsfHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.AbstractTag
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.images.AndroidArtwork
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.images.Artwork
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.PictureTypes
import java.nio.charset.Charset
import java.util.*

/**
 * Tag implementation for ASF.<br></br>
 *
 * @author Christian Laireiter
 */
class AsfTag @JvmOverloads constructor(val isCopyingFields: Boolean = false) : AbstractTag() {
	/**
	 * This iterator is used to iterator an [Iterator] with
	 * [TagField] objects and returns them by casting to
	 * [AsfTagField].<br></br>
	 *
	 * @author Christian Laireiter
	 */
	private class AsfFieldIterator(iterator: MutableIterator<TagField>) :
		MutableIterator<AsfTagField> {
		/**
		 * source iterator.
		 */
		private val fieldIterator: MutableIterator<TagField>

		/**
		 * Creates an isntance.
		 *
		 * @param iterator iterator to read from.
		 */
		init {
			fieldIterator = iterator
		}

		/**
		 * {@inheritDoc}
		 */
		override fun hasNext(): Boolean {
			return fieldIterator.hasNext()
		}

		/**
		 * {@inheritDoc}
		 */
		override fun next(): AsfTagField {
			return fieldIterator.next() as AsfTagField
		}

		/**
		 * {@inheritDoc}
		 */
		override fun remove() {
			fieldIterator.remove()
		}
	}
	/**
	 * If `true`, the [.copyFrom] method creates a
	 * new [AsfTagField] instance and copies the content from the source.<br></br>
	 * This method is utilized by [.addField] and
	 * [.setField].<br></br>
	 * So if `true` it is ensured that the [AsfTag] instance
	 * has its own copies of fields, which cannot be modified after assignment
	 * (which could pass some checks), and it just stores [AsfTagField]
	 * objects.<br></br>
	 * Only then [.getAsfFields] can work. otherwise
	 * [IllegalStateException] is thrown.
	 *
	 * @return state of field conversion.
	 */
	/**
	 * Creates an instance and sets the field conversion property.<br></br>
	 *
	 * @param isCopyingFields look at [.isCopyingFields].
	 */
	/**
	 * Creates an instance and copies the fields of the source into the own
	 * structure.<br></br>
	 *
	 * @param source source to read tag fields from.
	 * @param copy   look at [.isCopyingFields].
	 * @throws UnsupportedEncodingException [TagField.getRawContent] which may be called
	 */
	constructor(source: Tag, copy: Boolean) : this(copy) {
		copyFrom(source)
	}

	/**
	 * {@inheritDoc}
	 */
	// TODO introduce copy idea to all formats
	override fun addField(field: TagField?) {
		if (isValidField(field)) {
			if (AsfFieldKey.Companion.isMultiValued(
					field!!.id
				)
			) {
				super.addField(copyFrom(field))
			} else {
				super.setField(copyFrom(field))
			}
		}
	}

	/**
	 * Creates a field for copyright and adds it.<br></br>
	 *
	 * @param copyRight copyright content
	 */
	fun addCopyright(copyRight: String) {
		addField(createCopyrightField(copyRight))
	}

	/**
	 * Creates a field for rating and adds it.<br></br>
	 *
	 * @param rating rating.
	 */
	fun addRating(rating: String) {
		addField(createRatingField(rating))
	}

	/**
	 * This method copies tag fields from the source.<br></br>
	 *
	 * @param source source to read tag fields from.
	 */
	private fun copyFrom(source: Tag) {
		val fieldIterator = source.fields
		// iterate over all fields
		while (fieldIterator.hasNext()) {
			val copy = copyFrom(
				fieldIterator.next()
			)
			if (copy != null) {
				super.addField(copy)
			}
		}
	}

	/**
	 * If [.isCopyingFields] is `true`, Creates a copy of
	 * `source`, if its not empty-<br></br>
	 * However, plain [TagField] objects can only be transformed into
	 * binary fields using their [TagField.getRawContent] method.<br></br>
	 *
	 * @param source source field to copy.
	 * @return A copy, which is as close to the source as possible, or
	 * `null` if the field is empty (empty byte[] or blank
	 * string}.
	 */
	private fun copyFrom(source: TagField?): TagField? {
		val result: TagField?
		result = if (isCopyingFields) {
			if (source is AsfTagField) {
				try {
					source.clone() as TagField
				} catch (e: CloneNotSupportedException) {
					AsfTagField(source.descriptor)
				}
			} else if (source is TagTextField) {
				val content = source.content
				AsfTagTextField(source.id, content ?: "")
			} else {
				throw RuntimeException(
					"Unknown Asf Tag Field class:" // NOPMD
						// by
						// Christian
						// Laireiter
						// on
						// 5/9/09
						// 5:44
						// PM
						+ source!!.javaClass
				)
			}
		} else {
			source
		}
		return result
	}

	/**
	 * Creates an [AsfTagCoverField] from given artwork
	 *
	 * @param artwork artwork to create a ASF field from.
	 * @return ASF field capable of storing artwork.
	 */
	override fun createField(artwork: Artwork?): AsfTagCoverField? {
		if (artwork?.binaryData == null) return null
		return AsfTagCoverField(
			artwork.binaryData!!,
			artwork.pictureType,
			artwork.description,
			artwork.mimeType
		)
	}

	/**
	 * Create artwork field
	 *
	 * @param data raw image data
	 * @return creates a default ASF picture field with default
	 * [picture type][PictureTypes.DEFAULT_ID].
	 */
	fun createArtworkField(data: ByteArray): AsfTagCoverField {
		return AsfTagCoverField(data, PictureTypes.DEFAULT_ID, null, null)
	}

	/**
	 * Creates a field for storing the copyright.<br></br>
	 *
	 * @param content Copyright value.
	 * @return [AsfTagTextField]
	 */
	fun createCopyrightField(content: String): AsfTagTextField {
		return AsfTagTextField(AsfFieldKey.COPYRIGHT, content)
	}

	/**
	 * Creates a field for storing the copyright.<br></br>
	 *
	 * @param content Rating value.
	 * @return [AsfTagTextField]
	 */
	fun createRatingField(content: String): AsfTagTextField {
		return AsfTagTextField(AsfFieldKey.RATING, content)
	}

	/**
	 * Create tag text field using ASF key
	 *
	 * Uses the correct subclass for the key.<br></br>
	 *
	 * @param asfFieldKey field key to create field for.
	 * @param value       string value for the created field.
	 * @return text field with given content.
	 */
	fun createField(asfFieldKey: AsfFieldKey?, value: String?): AsfTagTextField {
		requireNotNull(value) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
		requireNotNull(asfFieldKey) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
		return when (asfFieldKey) {
			AsfFieldKey.COVER_ART -> throw UnsupportedOperationException(
				"Cover Art cannot be created using this method"
			)
			AsfFieldKey.BANNER_IMAGE -> throw UnsupportedOperationException(
				"Banner Image cannot be created using this method"
			)
			else -> AsfTagTextField(
				asfFieldKey.fieldName,
				value
			)
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun createField(genericKey: FieldKey?, vararg values: String?): AsfTagTextField {
		require(!(values == null || values[0] == null)) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
		requireNotNull(genericKey) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
		val asfFieldKey = tagFieldToAsfField[genericKey]
			?: throw KeyNotFoundException(genericKey.toString())
		return createField(asfFieldKey, values[0])
	}

	/**
	 * Removes all fields which are stored to the provided field key.
	 *
	 * @param fieldKey fields to remove.
	 */
	fun deleteField(fieldKey: AsfFieldKey) {
		super.deleteField(fieldKey.fieldName)
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(KeyNotFoundException::class)
	override fun deleteField(fieldKey: FieldKey?) {
		if (fieldKey == null) {
			throw KeyNotFoundException()
		}
		super.deleteField(tagFieldToAsfField[fieldKey]!!.fieldName)
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(KeyNotFoundException::class)
	override fun getFields(fieldKey: FieldKey?): List<TagField?>? {
		if (fieldKey == null) {
			throw KeyNotFoundException()
		}
		val asfKey = tagFieldToAsfField[fieldKey]
			?: throw KeyNotFoundException()
		return super.getFields(asfKey.fieldName)
	}

	/**
	 * Maps the generic key to the ogg key and return the list of values for this field as strings
	 *
	 * @param genericKey
	 * @return
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	override fun getAll(genericKey: FieldKey?): List<String?>? {
		val asfFieldKey = tagFieldToAsfField[genericKey]
			?: throw KeyNotFoundException()
		return super.getAll(asfFieldKey.fieldName)
	}

	/**
	 * @return
	 */
	override val artworkList: List<Artwork>
		get() {
			val coverartList = getFields(FieldKey.COVER_ART)
			val artworkList: MutableList<Artwork> = ArrayList(
				coverartList!!.size
			)
			for (next in coverartList) {
				val coverArt = next as AsfTagCoverField
				val artwork = AndroidArtwork()
				artwork.binaryData = coverArt.rawImageData
				artwork.mimeType = coverArt.mimeType ?: ""
				artwork.description = coverArt.description ?: ""
				artwork.pictureType = coverArt.pictureType
				artworkList.add(artwork)
			}
			return artworkList
		}

	/**
	 * This method iterates through all stored fields.<br></br>
	 * This method can only be used if this class has been created with field
	 * conversion turned on.
	 *
	 * @return Iterator for iterating through ASF fields.
	 */
	val asfFields: MutableIterator<AsfTagField>
		get() {
			check(isCopyingFields) { "Since the field conversion is not enabled, this method cannot be executed" }
			return AsfFieldIterator(fields)
		}

	/**
	 * Returns a list of stored copyrights.
	 *
	 * @return list of stored copyrights.
	 */
	val copyright: List<TagField?>?
		get() = getFields(AsfFieldKey.COPYRIGHT.fieldName)

	/**
	 * {@inheritDoc}
	 */
	@Throws(KeyNotFoundException::class)
	override fun getFirst(genericKey: FieldKey?): String? {
		return getValue(genericKey, 0)
	}

	/**
	 * Retrieve the first value that exists for this asfkey
	 *
	 * @param asfKey
	 * @return
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	fun getFirst(asfKey: AsfFieldKey?): String? {
		if (asfKey == null) {
			throw KeyNotFoundException()
		}
		return super.getFirst(asfKey.fieldName)
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(KeyNotFoundException::class)
	override fun getValue(id: FieldKey?, n: Int): String? {
		if (id == null) {
			throw KeyNotFoundException()
		}
		return super.getItem(tagFieldToAsfField[id]?.fieldName, n)
	}

	/**
	 * Returns the Copyright.
	 *
	 * @return the Copyright.
	 */
	val firstCopyright: String?
		get() = getFirst(AsfFieldKey.COPYRIGHT.fieldName)

	/**
	 * {@inheritDoc}
	 */
	@Throws(KeyNotFoundException::class)
	override fun getFirstField(id: FieldKey?): AsfTagField? {
		if (id == null) {
			throw KeyNotFoundException()
		}
		return super.getFirstField(tagFieldToAsfField[id]?.fieldName) as AsfTagField?
	}

	/**
	 * Returns the Rating.
	 *
	 * @return the Rating.
	 */
	val firstRating: String?
		get() = getFirst(AsfFieldKey.RATING.fieldName)

	/**
	 * Returns a list of stored ratings.
	 *
	 * @return list of stored ratings.
	 */
	val rating: List<TagField?>?
		get() = getFields(AsfFieldKey.RATING.fieldName)

	/**
	 * {@inheritDoc}
	 * @param enc
	 */
	override fun isAllowedEncoding(enc: Charset?): Boolean {
		return AsfHeader.ASF_CHARSET == enc
	}

	/**
	 * Check field is valid and can be added to this tag
	 *
	 * @param field field to add
	 * @return `true` if field may be added.
	 */
	// TODO introduce this concept to all formats
	private fun isValidField(field: TagField?): Boolean {
		if (field == null) {
			return false
		}
		return if (field !is AsfTagField) {
			false
		} else !field.isEmpty
	}

	/**
	 * {@inheritDoc}
	 */
	// TODO introduce copy idea to all formats
	override fun setField(field: TagField?) {
		if (isValidField(field)) {
			// Copy only occurs if flag setField
			super.setField(copyFrom(field))
		}
	}

	/**
	 * Sets the copyright.<br></br>
	 *
	 * @param Copyright the copyright to set.
	 */
	fun setCopyright(Copyright: String) {
		setField(createCopyrightField(Copyright))
	}

	/**
	 * Sets the Rating.<br></br>
	 *
	 * @param rating the rating to set.
	 */
	fun setRating(rating: String) {
		setField(createRatingField(rating))
	}

	/**
	 *
	 * @param genericKey
	 * @return
	 */
	override fun hasField(genericKey: FieldKey?): Boolean {
		val mp4FieldKey = tagFieldToAsfField[genericKey]
		return getFields(mp4FieldKey?.fieldName).isNotEmpty()
	}

	/**
	 *
	 * @param asfFieldKey
	 * @return
	 */
	fun hasField(asfFieldKey: AsfFieldKey): Boolean {
		return getFields(asfFieldKey.fieldName).size != 0
	}

	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun createCompilationField(value: Boolean): TagField? {
		return createField(FieldKey.IS_COMPILATION, value.toString())
	}

	companion object {
		/**
		 * Stores a list of field keys, which identify common fields.<br></br>
		 */
		val COMMON_FIELDS: MutableSet<AsfFieldKey>

		/**
		 * This map contains the mapping from [FieldKey] to
		 * [AsfFieldKey].
		 */
		private val tagFieldToAsfField: EnumMap<FieldKey, AsfFieldKey> = EnumMap(FieldKey::class.java)

		// Mapping from generic key to asf key
		init {
			tagFieldToAsfField[FieldKey.ACOUSTID_FINGERPRINT] = AsfFieldKey.ACOUSTID_FINGERPRINT
			tagFieldToAsfField[FieldKey.ACOUSTID_ID] = AsfFieldKey.ACOUSTID_ID
			tagFieldToAsfField[FieldKey.ALBUM] = AsfFieldKey.ALBUM
			tagFieldToAsfField[FieldKey.ALBUM_ARTIST] = AsfFieldKey.ALBUM_ARTIST
			tagFieldToAsfField[FieldKey.ALBUM_ARTIST_SORT] = AsfFieldKey.ALBUM_ARTIST_SORT
			tagFieldToAsfField[FieldKey.ALBUM_ARTISTS] = AsfFieldKey.ALBUM_ARTISTS
			tagFieldToAsfField[FieldKey.ALBUM_ARTISTS_SORT] = AsfFieldKey.ALBUM_ARTISTS_SORT
			tagFieldToAsfField[FieldKey.ALBUM_SORT] = AsfFieldKey.ALBUM_SORT
			tagFieldToAsfField[FieldKey.ALBUM_YEAR] = AsfFieldKey.ALBUM_YEAR
			tagFieldToAsfField[FieldKey.AMAZON_ID] = AsfFieldKey.AMAZON_ID
			tagFieldToAsfField[FieldKey.ARRANGER] = AsfFieldKey.ARRANGER
			tagFieldToAsfField[FieldKey.ARRANGER_SORT] = AsfFieldKey.ARRANGER_SORT
			tagFieldToAsfField[FieldKey.ARTIST] = AsfFieldKey.AUTHOR
			tagFieldToAsfField[FieldKey.ARTISTS] = AsfFieldKey.ARTISTS
			tagFieldToAsfField[FieldKey.ARTISTS_SORT] = AsfFieldKey.ARTISTS_SORT
			tagFieldToAsfField[FieldKey.ARTIST_SORT] = AsfFieldKey.ARTIST_SORT
			tagFieldToAsfField[FieldKey.BARCODE] = AsfFieldKey.BARCODE
			tagFieldToAsfField[FieldKey.BPM] = AsfFieldKey.BPM
			tagFieldToAsfField[FieldKey.CATALOG_NO] = AsfFieldKey.CATALOG_NO
			tagFieldToAsfField[FieldKey.CHOIR] = AsfFieldKey.CHOIR
			tagFieldToAsfField[FieldKey.CHOIR_SORT] = AsfFieldKey.CHOIR_SORT
			tagFieldToAsfField[FieldKey.CLASSICAL_CATALOG] = AsfFieldKey.CLASSICAL_CATALOG
			tagFieldToAsfField[FieldKey.CLASSICAL_NICKNAME] = AsfFieldKey.CLASSICAL_NICKNAME
			tagFieldToAsfField[FieldKey.COMMENT] = AsfFieldKey.DESCRIPTION
			tagFieldToAsfField[FieldKey.COMPOSER] = AsfFieldKey.COMPOSER
			tagFieldToAsfField[FieldKey.COMPOSER_SORT] = AsfFieldKey.COMPOSER_SORT
			tagFieldToAsfField[FieldKey.CONDUCTOR] = AsfFieldKey.CONDUCTOR
			tagFieldToAsfField[FieldKey.CONDUCTOR_SORT] = AsfFieldKey.CONDUCTOR_SORT
			tagFieldToAsfField[FieldKey.COPYRIGHT] = AsfFieldKey.COPYRIGHT
			tagFieldToAsfField[FieldKey.COUNTRY] = AsfFieldKey.COUNTRY
			tagFieldToAsfField[FieldKey.COVER_ART] = AsfFieldKey.COVER_ART
			tagFieldToAsfField[FieldKey.CUSTOM1] = AsfFieldKey.CUSTOM1
			tagFieldToAsfField[FieldKey.CUSTOM2] = AsfFieldKey.CUSTOM2
			tagFieldToAsfField[FieldKey.CUSTOM3] = AsfFieldKey.CUSTOM3
			tagFieldToAsfField[FieldKey.CUSTOM4] = AsfFieldKey.CUSTOM4
			tagFieldToAsfField[FieldKey.CUSTOM5] = AsfFieldKey.CUSTOM5
			tagFieldToAsfField[FieldKey.DISC_NO] = AsfFieldKey.DISC_NO
			tagFieldToAsfField[FieldKey.DISC_SUBTITLE] = AsfFieldKey.DISC_SUBTITLE
			tagFieldToAsfField[FieldKey.DISC_TOTAL] = AsfFieldKey.DISC_TOTAL
			tagFieldToAsfField[FieldKey.DJMIXER] = AsfFieldKey.DJMIXER
			tagFieldToAsfField[FieldKey.DJMIXER_SORT] = AsfFieldKey.DJMIXER_SORT
			tagFieldToAsfField[FieldKey.MOOD_ELECTRONIC] = AsfFieldKey.MOOD_ELECTRONIC
			tagFieldToAsfField[FieldKey.ENCODER] = AsfFieldKey.ENCODER
			tagFieldToAsfField[FieldKey.ENGINEER] = AsfFieldKey.ENGINEER
			tagFieldToAsfField[FieldKey.ENGINEER_SORT] = AsfFieldKey.ENGINEER_SORT
			tagFieldToAsfField[FieldKey.ENSEMBLE] = AsfFieldKey.ENSEMBLE
			tagFieldToAsfField[FieldKey.ENSEMBLE_SORT] = AsfFieldKey.ENSEMBLE_SORT
			tagFieldToAsfField[FieldKey.FBPM] = AsfFieldKey.FBPM
			tagFieldToAsfField[FieldKey.GENRE] = AsfFieldKey.GENRE
			tagFieldToAsfField[FieldKey.GROUP] = AsfFieldKey.GROUP
			tagFieldToAsfField[FieldKey.GROUPING] = AsfFieldKey.GROUPING
			tagFieldToAsfField[FieldKey.INSTRUMENT] = AsfFieldKey.INSTRUMENT
			tagFieldToAsfField[FieldKey.INVOLVEDPEOPLE] = AsfFieldKey.INVOLVED_PERSON
			tagFieldToAsfField[FieldKey.IPI] = AsfFieldKey.IPI
			tagFieldToAsfField[FieldKey.ISRC] = AsfFieldKey.ISRC
			tagFieldToAsfField[FieldKey.ISWC] = AsfFieldKey.ISWC
			tagFieldToAsfField[FieldKey.IS_CLASSICAL] = AsfFieldKey.IS_CLASSICAL
			tagFieldToAsfField[FieldKey.IS_COMPILATION] = AsfFieldKey.IS_COMPILATION
			tagFieldToAsfField[FieldKey.IS_GREATEST_HITS] = AsfFieldKey.IS_GREATEST_HITS
			tagFieldToAsfField[FieldKey.IS_HD] = AsfFieldKey.IS_HD
			tagFieldToAsfField[FieldKey.IS_LIVE] = AsfFieldKey.IS_LIVE
			tagFieldToAsfField[FieldKey.IS_SOUNDTRACK] = AsfFieldKey.IS_SOUNDTRACK
			tagFieldToAsfField[FieldKey.JAIKOZ_ID] = AsfFieldKey.JAIKOZ_ID
			tagFieldToAsfField[FieldKey.KEY] = AsfFieldKey.INITIAL_KEY
			tagFieldToAsfField[FieldKey.LANGUAGE] = AsfFieldKey.LANGUAGE
			tagFieldToAsfField[FieldKey.LYRICIST] = AsfFieldKey.LYRICIST
			tagFieldToAsfField[FieldKey.LYRICIST_SORT] = AsfFieldKey.LYRICIST_SORT
			tagFieldToAsfField[FieldKey.LYRICS] = AsfFieldKey.LYRICS
			tagFieldToAsfField[FieldKey.MEDIA] = AsfFieldKey.MEDIA
			tagFieldToAsfField[FieldKey.MIXER] = AsfFieldKey.MIXER
			tagFieldToAsfField[FieldKey.MIXER_SORT] = AsfFieldKey.MIXER_SORT
			tagFieldToAsfField[FieldKey.MOOD] = AsfFieldKey.MOOD
			tagFieldToAsfField[FieldKey.MOOD_ACOUSTIC] = AsfFieldKey.MOOD_ACOUSTIC
			tagFieldToAsfField[FieldKey.MOOD_AGGRESSIVE] = AsfFieldKey.MOOD_AGGRESSIVE
			tagFieldToAsfField[FieldKey.MOOD_AROUSAL] = AsfFieldKey.MOOD_AROUSAL
			tagFieldToAsfField[FieldKey.MOOD_DANCEABILITY] = AsfFieldKey.MOOD_DANCEABILITY
			tagFieldToAsfField[FieldKey.MOOD_HAPPY] = AsfFieldKey.MOOD_HAPPY
			tagFieldToAsfField[FieldKey.MOOD_INSTRUMENTAL] = AsfFieldKey.MOOD_INSTRUMENTAL
			tagFieldToAsfField[FieldKey.MOOD_PARTY] = AsfFieldKey.MOOD_PARTY
			tagFieldToAsfField[FieldKey.MOOD_RELAXED] = AsfFieldKey.MOOD_RELAXED
			tagFieldToAsfField[FieldKey.MOOD_SAD] = AsfFieldKey.MOOD_SAD
			tagFieldToAsfField[FieldKey.MOOD_VALENCE] = AsfFieldKey.MOOD_VALENCE
			tagFieldToAsfField[FieldKey.MOVEMENT] = AsfFieldKey.MOVEMENT
			tagFieldToAsfField[FieldKey.MOVEMENT_NO] = AsfFieldKey.MOVEMENT_NO
			tagFieldToAsfField[FieldKey.MOVEMENT_TOTAL] = AsfFieldKey.MOVEMENT_TOTAL
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_ARTISTID] = AsfFieldKey.MUSICBRAINZ_ARTISTID
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_DISC_ID] = AsfFieldKey.MUSICBRAINZ_DISC_ID
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_ORIGINAL_RELEASE_ID] =
				AsfFieldKey.MUSICBRAINZ_ORIGINAL_RELEASEID
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_RELEASEARTISTID] =
				AsfFieldKey.MUSICBRAINZ_RELEASEARTISTID
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_RELEASEID] = AsfFieldKey.MUSICBRAINZ_RELEASEID
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_RELEASE_COUNTRY] =
				AsfFieldKey.MUSICBRAINZ_RELEASE_COUNTRY
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_RELEASE_GROUP_ID] =
				AsfFieldKey.MUSICBRAINZ_RELEASEGROUPID
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_RELEASE_STATUS] =
				AsfFieldKey.MUSICBRAINZ_RELEASE_STATUS
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_RELEASE_TRACK_ID] =
				AsfFieldKey.MUSICBRAINZ_RELEASETRACKID
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_RELEASE_TYPE] =
				AsfFieldKey.MUSICBRAINZ_RELEASE_TYPE
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_TRACK_ID] = AsfFieldKey.MUSICBRAINZ_TRACK_ID
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_WORK] = AsfFieldKey.MUSICBRAINZ_WORK
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_WORK_ID] = AsfFieldKey.MUSICBRAINZ_WORKID
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_RECORDING_WORK] =
				AsfFieldKey.MUSICBRAINZ_RECORDING_WORK
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_RECORDING_WORK_ID] =
				AsfFieldKey.MUSICBRAINZ_RECORDING_WORK_ID
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL1] =
				AsfFieldKey.MUSICBRAINZ_WORK_PART_LEVEL1
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL1_ID] =
				AsfFieldKey.MUSICBRAINZ_WORK_PART_LEVEL1_ID
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL1_TYPE] =
				AsfFieldKey.MUSICBRAINZ_WORK_PART_LEVEL1_TYPE
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL2] =
				AsfFieldKey.MUSICBRAINZ_WORK_PART_LEVEL2
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL2_ID] =
				AsfFieldKey.MUSICBRAINZ_WORK_PART_LEVEL2_ID
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL2_TYPE] =
				AsfFieldKey.MUSICBRAINZ_WORK_PART_LEVEL2_TYPE
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL3] =
				AsfFieldKey.MUSICBRAINZ_WORK_PART_LEVEL3
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL3_ID] =
				AsfFieldKey.MUSICBRAINZ_WORK_PART_LEVEL3_ID
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL3_TYPE] =
				AsfFieldKey.MUSICBRAINZ_WORK_PART_LEVEL3_TYPE
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL4] =
				AsfFieldKey.MUSICBRAINZ_WORK_PART_LEVEL4
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL4_ID] =
				AsfFieldKey.MUSICBRAINZ_WORK_PART_LEVEL4_ID
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL4_TYPE] =
				AsfFieldKey.MUSICBRAINZ_WORK_PART_LEVEL4_TYPE
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL5] =
				AsfFieldKey.MUSICBRAINZ_WORK_PART_LEVEL5
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL5_ID] =
				AsfFieldKey.MUSICBRAINZ_WORK_PART_LEVEL5_ID
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL5_TYPE] =
				AsfFieldKey.MUSICBRAINZ_WORK_PART_LEVEL5_TYPE
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL6] =
				AsfFieldKey.MUSICBRAINZ_WORK_PART_LEVEL6
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL6_ID] =
				AsfFieldKey.MUSICBRAINZ_WORK_PART_LEVEL6_ID
			tagFieldToAsfField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL6_TYPE] =
				AsfFieldKey.MUSICBRAINZ_WORK_PART_LEVEL6_TYPE
			tagFieldToAsfField[FieldKey.MUSICIP_ID] = AsfFieldKey.MUSICIP_ID
			tagFieldToAsfField[FieldKey.OCCASION] = AsfFieldKey.OCCASION
			tagFieldToAsfField[FieldKey.OPUS] = AsfFieldKey.OPUS
			tagFieldToAsfField[FieldKey.ORCHESTRA] = AsfFieldKey.ORCHESTRA
			tagFieldToAsfField[FieldKey.ORCHESTRA_SORT] = AsfFieldKey.ORCHESTRA_SORT
			tagFieldToAsfField[FieldKey.ORIGINAL_ALBUM] = AsfFieldKey.ORIGINAL_ALBUM
			tagFieldToAsfField[FieldKey.ORIGINAL_ARTIST] = AsfFieldKey.ORIGINAL_ARTIST
			tagFieldToAsfField[FieldKey.ORIGINAL_LYRICIST] = AsfFieldKey.ORIGINAL_LYRICIST
			tagFieldToAsfField[FieldKey.ORIGINAL_YEAR] = AsfFieldKey.ORIGINAL_YEAR
			tagFieldToAsfField[FieldKey.ORIGINALRELEASEDATE] = AsfFieldKey.ORIGINALRELEASEDATE
			tagFieldToAsfField[FieldKey.OVERALL_WORK] = AsfFieldKey.OVERALL_WORK
			tagFieldToAsfField[FieldKey.PART] = AsfFieldKey.PART
			tagFieldToAsfField[FieldKey.PART_NUMBER] = AsfFieldKey.PART_NUMBER
			tagFieldToAsfField[FieldKey.PART_TYPE] = AsfFieldKey.PART_TYPE
			tagFieldToAsfField[FieldKey.PERFORMER] = AsfFieldKey.PERFORMER
			tagFieldToAsfField[FieldKey.PERFORMER_NAME] = AsfFieldKey.PERFORMER_NAME
			tagFieldToAsfField[FieldKey.PERFORMER_NAME_SORT] = AsfFieldKey.PERFORMER_NAME_SORT
			tagFieldToAsfField[FieldKey.PERIOD] = AsfFieldKey.PERIOD
			tagFieldToAsfField[FieldKey.PRODUCER] = AsfFieldKey.PRODUCER
			tagFieldToAsfField[FieldKey.PRODUCER_SORT] = AsfFieldKey.PRODUCER_SORT
			tagFieldToAsfField[FieldKey.QUALITY] = AsfFieldKey.QUALITY
			tagFieldToAsfField[FieldKey.RANKING] = AsfFieldKey.RANKING
			tagFieldToAsfField[FieldKey.RATING] = AsfFieldKey.USER_RATING
			tagFieldToAsfField[FieldKey.RECORD_LABEL] = AsfFieldKey.RECORD_LABEL
			tagFieldToAsfField[FieldKey.RECORDINGDATE] = AsfFieldKey.RECORDINGDATE
			tagFieldToAsfField[FieldKey.RECORDINGSTARTDATE] = AsfFieldKey.RECORDINGSTARTDATE
			tagFieldToAsfField[FieldKey.RECORDINGENDDATE] = AsfFieldKey.RECORDINGENDDATE
			tagFieldToAsfField[FieldKey.RECORDINGLOCATION] = AsfFieldKey.RECORDINGLOCATION
			tagFieldToAsfField[FieldKey.REMIXER] = AsfFieldKey.REMIXER
			tagFieldToAsfField[FieldKey.ROONALBUMTAG] = AsfFieldKey.ROONALBUMTAG
			tagFieldToAsfField[FieldKey.ROONTRACKTAG] = AsfFieldKey.ROONTRACKTAG
			tagFieldToAsfField[FieldKey.SCRIPT] = AsfFieldKey.SCRIPT
			tagFieldToAsfField[FieldKey.SECTION] = AsfFieldKey.SECTION
			tagFieldToAsfField[FieldKey.SINGLE_DISC_TRACK_NO] = AsfFieldKey.SINGLE_DISC_TRACK_NO
			tagFieldToAsfField[FieldKey.SONGKONG_ID] = AsfFieldKey.SONGKONG_ID
			tagFieldToAsfField[FieldKey.SUBTITLE] = AsfFieldKey.SUBTITLE
			tagFieldToAsfField[FieldKey.TAGS] = AsfFieldKey.TAGS
			tagFieldToAsfField[FieldKey.TEMPO] = AsfFieldKey.TEMPO
			tagFieldToAsfField[FieldKey.TIMBRE] = AsfFieldKey.TIMBRE
			tagFieldToAsfField[FieldKey.TITLE] = AsfFieldKey.TITLE
			tagFieldToAsfField[FieldKey.TITLE_MOVEMENT] = AsfFieldKey.TITLE_MOVEMENT
			tagFieldToAsfField[FieldKey.TITLE_SORT] = AsfFieldKey.TITLE_SORT
			tagFieldToAsfField[FieldKey.TONALITY] = AsfFieldKey.TONALITY
			tagFieldToAsfField[FieldKey.TRACK] = AsfFieldKey.TRACK
			tagFieldToAsfField[FieldKey.TRACK_TOTAL] = AsfFieldKey.TRACK_TOTAL
			tagFieldToAsfField[FieldKey.URL_DISCOGS_ARTIST_SITE] =
				AsfFieldKey.URL_DISCOGS_ARTIST_SITE
			tagFieldToAsfField[FieldKey.URL_DISCOGS_RELEASE_SITE] =
				AsfFieldKey.URL_DISCOGS_RELEASE_SITE
			tagFieldToAsfField[FieldKey.URL_LYRICS_SITE] = AsfFieldKey.URL_LYRICS_SITE
			tagFieldToAsfField[FieldKey.URL_OFFICIAL_ARTIST_SITE] =
				AsfFieldKey.URL_OFFICIAL_ARTIST_SITE
			tagFieldToAsfField[FieldKey.URL_OFFICIAL_RELEASE_SITE] =
				AsfFieldKey.URL_OFFICIAL_RELEASE_SITE
			tagFieldToAsfField[FieldKey.URL_WIKIPEDIA_ARTIST_SITE] =
				AsfFieldKey.URL_WIKIPEDIA_ARTIST_SITE
			tagFieldToAsfField[FieldKey.URL_WIKIPEDIA_RELEASE_SITE] =
				AsfFieldKey.URL_WIKIPEDIA_RELEASE_SITE
			tagFieldToAsfField[FieldKey.VERSION] = AsfFieldKey.VERSION
			tagFieldToAsfField[FieldKey.WORK] = AsfFieldKey.WORK
			tagFieldToAsfField[FieldKey.WORK_TYPE] = AsfFieldKey.WORK_TYPE
			tagFieldToAsfField[FieldKey.YEAR] = AsfFieldKey.YEAR

			COMMON_FIELDS = HashSet()
			COMMON_FIELDS.add(AsfFieldKey.ALBUM)
			COMMON_FIELDS.add(AsfFieldKey.AUTHOR)
			COMMON_FIELDS.add(AsfFieldKey.DESCRIPTION)
			COMMON_FIELDS.add(AsfFieldKey.GENRE)
			COMMON_FIELDS.add(AsfFieldKey.TITLE)
			COMMON_FIELDS.add(AsfFieldKey.TRACK)
			COMMON_FIELDS.add(AsfFieldKey.YEAR)
		}
	}
}
