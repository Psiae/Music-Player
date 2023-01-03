/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Rapha�l Slinckx <raphael@slinckx.net>
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
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.vorbiscomment

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.flac.metadatablock.MetadataBlockDataPicture
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.AbstractTag
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.ogg.util.VorbisHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.images.AndroidArtwork
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.images.Artwork
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.vorbiscomment.util.Base64Coder.decode
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.vorbiscomment.util.Base64Coder.encode
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * This is the logical representation of  Vorbis Comment Data
 */
class VorbisCommentTag
/**
 * Only used within Package, hidden because it doesnt set Vendor
 * which should be done when created by end user
 */
	: AbstractTag() {
	/**
	 * @return the vendor, generically known as the encoder
	 */
	/**
	 * Set the vendor, known as the encoder  generally
	 *
	 * We dont want this to be blank, when written to file this field is written to a different location
	 * to all other fields but user of library can just reat it as another field
	 *
	 * @param vendor
	 */
	var vendor: String?
		get() = getFirst(VorbisCommentFieldKey.VENDOR.fieldName)
		set(vendor) {
			var vendor = vendor
			if (vendor == null) {
				vendor = DEFAULT_VENDOR
			}
			super.setField(
				VorbisCommentTagField(VorbisCommentFieldKey.VENDOR.fieldName, vendor)
			)
		}

	override fun isAllowedEncoding(enc: Charset?): Boolean {
		return enc?.name() == VorbisHeader.CHARSET_UTF_8
	}


	override fun toString(): String {
		return "OGG " + super.toString()
	}

	/**
	 * Create Tag Field using generic key
	 */
	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun createField(genericKey: FieldKey?, vararg values: String?): TagField {
		if (genericKey == null) {
			throw KeyNotFoundException()
		}
		return createField(tagFieldToOggField[genericKey], values[0])
	}

	/**
	 * Create Tag Field using ogg key
	 *
	 * @param vorbisCommentFieldKey
	 * @param value
	 * @return
	 * @throws KeyNotFoundException
	 * @throws FieldDataInvalidException
	 */
	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	fun createField(vorbisCommentFieldKey: VorbisCommentFieldKey?, value: String?): TagField {
		requireNotNull(value) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
		if (vorbisCommentFieldKey == null) {
			throw KeyNotFoundException()
		}
		return VorbisCommentTagField(vorbisCommentFieldKey.fieldName, value)
	}

	/**
	 * Create Tag Field using ogg key
	 *
	 * This method is provided to allow you to create key of any value because VorbisComment allows
	 * arbitary keys.
	 *
	 * @param vorbisCommentFieldKey
	 * @param value
	 * @return
	 */
	fun createField(vorbisCommentFieldKey: String, value: String): TagField {
		requireNotNull(value) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
		return VorbisCommentTagField(vorbisCommentFieldKey, value)
	}

	/**
	 * Maps the generic key to the ogg key and return the list of values for this field
	 *
	 * @param genericKey
	 */
	@Throws(KeyNotFoundException::class)
	override fun getFields(genericKey: FieldKey?): List<TagField?> {
		val vorbisCommentFieldKey = tagFieldToOggField[genericKey]
			?: throw KeyNotFoundException()
		return super.getFields(vorbisCommentFieldKey.fieldName)
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
		val vorbisCommentFieldKey = tagFieldToOggField[genericKey]
			?: throw KeyNotFoundException()
		return super.getAll(vorbisCommentFieldKey.fieldName)
	}

	/**
	 * Retrieve the first value that exists for this vorbis comment key
	 *
	 * @param vorbisCommentKey
	 * @return
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	operator fun get(vorbisCommentKey: VorbisCommentFieldKey?): List<TagField?>? {
		if (vorbisCommentKey == null) {
			throw KeyNotFoundException()
		}
		return super.getFields(vorbisCommentKey.fieldName)
	}

	/**
	 * Retrieve the first value that exists for this vorbis comment key
	 *
	 * @param vorbisCommentKey
	 * @return
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	fun getFirst(vorbisCommentKey: VorbisCommentFieldKey?): String? {
		if (vorbisCommentKey == null) {
			throw KeyNotFoundException()
		}
		return super.getFirst(vorbisCommentKey.fieldName)
	}

	/**
	 *
	 * @param genericKey
	 * @return
	 */
	override fun hasField(genericKey: FieldKey?): Boolean {
		val vorbisFieldKey = tagFieldToOggField[genericKey]
		return getFields(vorbisFieldKey!!.fieldName).size != 0
	}

	/**
	 *
	 * @param vorbisFieldKey
	 * @return
	 */
	fun hasField(vorbisFieldKey: VorbisCommentFieldKey): Boolean {
		return getFields(vorbisFieldKey.fieldName).size != 0
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
		if (genericKey === FieldKey.ALBUM_ARTIST) {
			when (TagOptionSingleton.instance.vorbisAlbumArtistSaveOptions) {
				VorbisAlbumArtistSaveOptions.WRITE_ALBUMARTIST,
				VorbisAlbumArtistSaveOptions.WRITE_ALBUMARTIST_AND_DELETE_JRIVER_ALBUMARTIST -> {
					val vorbisCommentFieldKey = tagFieldToOggField[genericKey]
					deleteField(vorbisCommentFieldKey)
					return
				}
				VorbisAlbumArtistSaveOptions.WRITE_JRIVER_ALBUMARTIST,
				VorbisAlbumArtistSaveOptions.WRITE_JRIVER_ALBUMARTIST_AND_DELETE_ALBUMARTIST -> {
					deleteField(VorbisCommentFieldKey.ALBUMARTIST_JRIVER)
					return
				}
				VorbisAlbumArtistSaveOptions.WRITE_BOTH -> {
					val vorbisCommentFieldKey = tagFieldToOggField[genericKey]
					deleteField(vorbisCommentFieldKey)
					deleteField(VorbisCommentFieldKey.ALBUMARTIST_JRIVER)
					return
				}
			}
		} else {
			val vorbisCommentFieldKey = tagFieldToOggField[genericKey]
			deleteField(vorbisCommentFieldKey)
		}
	}

	/**
	 * Delete fields with this vorbisCommentFieldKey
	 *
	 * @param vorbisCommentFieldKey
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	fun deleteField(vorbisCommentFieldKey: VorbisCommentFieldKey?) {
		if (vorbisCommentFieldKey == null) {
			throw KeyNotFoundException()
		}
		super.deleteField(vorbisCommentFieldKey.fieldName)
	}

	/**
	 * Retrieve artwork raw data when using the deprecated COVERART format
	 *
	 * @return
	 */
	val artworkBinaryData: ByteArray?
		get() {
			val base64data = this.getFirst(VorbisCommentFieldKey.COVERART)
			return base64data?.toCharArray()?.let { decode(it) }
		}

	/**
	 * Retrieve artwork mimeType when using deprecated COVERART format
	 *
	 * @return mimetype
	 */
	val artworkMimeType: String
		get() = this.getFirst(VorbisCommentFieldKey.COVERARTMIME) ?: ""

	/**
	 * Is this tag empty
	 *
	 *
	 * Overridden because check for size of one because there is always a vendor tag unless just
	 * created an empty vorbis tag as part of flac tag in which case size could be zero
	 *
	 * @see Tag.isEmpty
	 */
	override val isEmpty: Boolean
		get() = fieldMap.size <= 1

	/**
	 * Add Field
	 *
	 *
	 * Overidden because there can only be one vendor set
	 *
	 * @param field
	 */
	override fun addField(field: TagField?) {
		if (field!!.id == VorbisCommentFieldKey.VENDOR.fieldName) {
			super.setField(field)
		} else {
			super.addField(field)
		}
	}

	@Throws(KeyNotFoundException::class)
	override fun getFirstField(id: FieldKey?): TagField? {
		if (id == null) {
			throw KeyNotFoundException()
		}
		return getFirstField(
			tagFieldToOggField[id]!!.fieldName
		)
	}//Read Old Format

	//New Format (Supports Multiple Images)
	/**
	 *
	 * @return list of artwork images
	 */
	override val artworkList: List<Artwork>
		get() {
			val artworkList: MutableList<Artwork> = ArrayList(1)

			//Read Old Format
			if ((artworkBinaryData != null) and (artworkBinaryData?.isNotEmpty() == true)) {
				val artwork = AndroidArtwork()
				artwork.mimeType = artworkMimeType
				artwork.binaryData = artworkBinaryData
				artworkList.add(artwork)
			}

			//New Format (Supports Multiple Images)
			val metadataBlockPics = this[VorbisCommentFieldKey.METADATA_BLOCK_PICTURE]
			for (tagField in metadataBlockPics!!) {
				try {
					val imageBinaryData = decode(
						(tagField as TagTextField?)!!.content!!
					)
					val coverArt = MetadataBlockDataPicture(ByteBuffer.wrap(imageBinaryData))
					val artwork = AndroidArtwork.createArtworkFromMetadataBlockDataPicture(coverArt)
					artworkList.add(artwork)
				} catch (ioe: IOException) {
					throw RuntimeException(ioe)
				} catch (ife: InvalidFrameException) {
					throw RuntimeException(ife)
				}
			}
			return artworkList
		}

	/**
	 * Create MetadataBlockPicture field, this is the preferred way of storing artwork in VorbisComment tag now but
	 * has to be base encoded to be stored in VorbisComment
	 *
	 * @return MetadataBlockDataPicture
	 */
	@Throws(FieldDataInvalidException::class)
	private fun createMetadataBlockDataPicture(artwork: Artwork?): MetadataBlockDataPicture {
		return if (artwork!!.isLinked) {
			MetadataBlockDataPicture(
				artwork.imageUrl.toByteArray(StandardCharsets.ISO_8859_1),
				artwork.pictureType,
				MetadataBlockDataPicture.IMAGE_IS_URL,
				"",
				0,
				0,
				0,
				0
			)
		} else {
			if (!artwork.setImageFromData()) {
				throw FieldDataInvalidException("Unable to create MetadataBlockDataPicture from buffered")
			}
			MetadataBlockDataPicture(
				artwork.binaryData,
				artwork.pictureType,
				artwork.mimeType,
				artwork.description ?: "",
				artwork.width ?: 0,
				artwork.height ?: 0,
				0,
				0
			)
		}
	}

	/**
	 * Create Artwork field
	 *
	 * @param artwork
	 * @return
	 * @throws FieldDataInvalidException
	 */
	@Throws(FieldDataInvalidException::class)
	override fun createField(artwork: Artwork?): TagField? {
		return try {
			val testdata: CharArray? =
				createMetadataBlockDataPicture(artwork).rawContent?.let { encode(it) }
			val base64image = String(testdata ?: charArrayOf())
			createField(VorbisCommentFieldKey.METADATA_BLOCK_PICTURE, base64image)
		} catch (uee: UnsupportedEncodingException) {
			throw RuntimeException(uee)
		}
	}

	/**
	 * Create and set artwork field
	 *
	 * @return
	 */
	@Throws(FieldDataInvalidException::class)
	override fun setField(artwork: Artwork?) {
		//Set field
		this.setField(createField(artwork))

		//If worked okay above then that should be first artwork and if we still had old coverart format
		//that should be removed
		if (this.getFirst(VorbisCommentFieldKey.COVERART)!!.length > 0) {
			this.deleteField(VorbisCommentFieldKey.COVERART)
			this.deleteField(VorbisCommentFieldKey.COVERARTMIME)
		}
	}

	/**
	 * Add artwork field
	 *
	 * @param artwork
	 * @throws FieldDataInvalidException
	 */
	@Throws(FieldDataInvalidException::class)
	override fun addField(artwork: Artwork?) {
		this.addField(createField(artwork))
	}

	/**
	 * Create artwork field using the non-standard COVERART tag
	 *
	 *
	 * Actually create two fields , the data field and the mimetype. Its is not recommended that you use this
	 * method anymore.
	 *
	 * @param data     raw image data
	 * @param mimeType mimeType of data
	 *
	 * @return
	 */
	@Deprecated("")
	fun setArtworkField(data: ByteArray?, mimeType: String?) {
		val testdata = encode(
			data!!
		)
		val base64image = String(testdata)
		val dataField = VorbisCommentTagField(VorbisCommentFieldKey.COVERART.fieldName, base64image)
		val mimeField =
			VorbisCommentTagField(VorbisCommentFieldKey.COVERARTMIME.fieldName, mimeType)
		setField(dataField)
		setField(mimeField)
	}

	/**
	 * Create and set field with name of vorbisCommentkey
	 *
	 * @param vorbisCommentKey
	 * @param value
	 * @throws KeyNotFoundException
	 * @throws FieldDataInvalidException
	 */
	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	fun setField(vorbisCommentKey: String, value: String) {
		val tagfield = createField(vorbisCommentKey, value)
		setField(tagfield)
	}

	/**
	 * Create and add field with name of vorbisCommentkey
	 * @param vorbisCommentKey
	 * @param value
	 * @throws KeyNotFoundException
	 * @throws FieldDataInvalidException
	 */
	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	fun addField(vorbisCommentKey: String, value: String) {
		val tagfield = createField(vorbisCommentKey, value)
		addField(tagfield)
	}

	/**
	 * Delete all instance of artwork Field
	 *
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	override fun deleteArtworkField() {
		//New Method
		this.deleteField(VorbisCommentFieldKey.METADATA_BLOCK_PICTURE)

		//Old Method
		this.deleteField(VorbisCommentFieldKey.COVERART)
		this.deleteField(VorbisCommentFieldKey.COVERARTMIME)
	}

	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun createCompilationField(value: Boolean): TagField? {
		return createField(FieldKey.IS_COMPILATION, value.toString())
	}

	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun setField(genericKey: FieldKey?, vararg values: String?) {
		require(!(values == null || values[0] == null)) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
		val value = values[0]
		if (genericKey === FieldKey.ALBUM_ARTIST) {
			when (TagOptionSingleton.instance.vorbisAlbumArtistSaveOptions) {
				VorbisAlbumArtistSaveOptions.WRITE_ALBUMARTIST -> {
					val tagfield = createField(genericKey, value)
					setField(tagfield)
					return
				}
				VorbisAlbumArtistSaveOptions.WRITE_JRIVER_ALBUMARTIST -> {
					val tagfield = createField(VorbisCommentFieldKey.ALBUMARTIST_JRIVER, value)
					setField(tagfield)
					return
				}
				VorbisAlbumArtistSaveOptions.WRITE_ALBUMARTIST_AND_DELETE_JRIVER_ALBUMARTIST -> {
					val tagfield = createField(genericKey, value)
					setField(tagfield)
					deleteField(VorbisCommentFieldKey.ALBUMARTIST_JRIVER.fieldName)
					return
				}
				VorbisAlbumArtistSaveOptions.WRITE_JRIVER_ALBUMARTIST_AND_DELETE_ALBUMARTIST -> {
					val tagfield = createField(VorbisCommentFieldKey.ALBUMARTIST_JRIVER, value)
					setField(tagfield)
					deleteField(VorbisCommentFieldKey.ALBUMARTIST.fieldName)
					return
				}
				VorbisAlbumArtistSaveOptions.WRITE_BOTH -> {
					val tagfield1 = createField(genericKey, value)
					setField(tagfield1)
					val tagfield2 = createField(VorbisCommentFieldKey.ALBUMARTIST_JRIVER, value)
					setField(tagfield2)
					return
				}
			}
		} else {
			val tagfield = createField(genericKey, value)
			setField(tagfield)
		}
	}

	/**
	 * Create new field and add it to the tag
	 *
	 * @param genericKey
	 * @param values
	 * @throws KeyNotFoundException
	 * @throws FieldDataInvalidException
	 */
	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun addField(genericKey: FieldKey?, vararg values: String?) {
		require(!(values == null || values[0] == null)) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
		val value = values[0]
		if (genericKey === FieldKey.ALBUM_ARTIST) {
			when (TagOptionSingleton.instance.vorbisAlbumArtistSaveOptions) {
				VorbisAlbumArtistSaveOptions.WRITE_ALBUMARTIST -> {
					val tagfield = createField(genericKey, value)
					addField(tagfield)
					return
				}
				VorbisAlbumArtistSaveOptions.WRITE_JRIVER_ALBUMARTIST -> {
					val tagfield = createField(VorbisCommentFieldKey.ALBUMARTIST_JRIVER, value)
					addField(tagfield)
					return
				}
				VorbisAlbumArtistSaveOptions.WRITE_ALBUMARTIST_AND_DELETE_JRIVER_ALBUMARTIST -> {
					val tagfield = createField(genericKey, value)
					addField(tagfield)
					deleteField(VorbisCommentFieldKey.ALBUMARTIST_JRIVER.fieldName)
					return
				}
				VorbisAlbumArtistSaveOptions.WRITE_JRIVER_ALBUMARTIST_AND_DELETE_ALBUMARTIST -> {
					val tagfield = createField(VorbisCommentFieldKey.ALBUMARTIST_JRIVER, value)
					addField(tagfield)
					deleteField(VorbisCommentFieldKey.ALBUMARTIST.fieldName)
					return
				}
				VorbisAlbumArtistSaveOptions.WRITE_BOTH -> {
					val tagfield1 = createField(genericKey, value)
					addField(tagfield1)
					val tagfield2 = createField(VorbisCommentFieldKey.ALBUMARTIST_JRIVER, value)
					addField(tagfield2)
					return
				}
			}
		} else {
			val tagfield = createField(genericKey, value)
			addField(tagfield)
		}
	}

	@Throws(KeyNotFoundException::class)
	override fun getValue(genericKey: FieldKey?, n: Int): String? {
		return if (genericKey === FieldKey.ALBUM_ARTIST) {
			when (TagOptionSingleton.instance.vorbisAlbumArtisReadOptions) {
				VorbisAlbumArtistReadOptions.READ_ALBUMARTIST -> {
					val vorbisCommentFieldKey =
						VorbisCommentFieldKey.ALBUMARTIST
					super.getItem(vorbisCommentFieldKey.fieldName, n)
				}
				VorbisAlbumArtistReadOptions.READ_JRIVER_ALBUMARTIST -> {
					val vorbisCommentFieldKey =
						VorbisCommentFieldKey.ALBUMARTIST_JRIVER
					super.getItem(vorbisCommentFieldKey.fieldName, n)
				}
				VorbisAlbumArtistReadOptions.READ_ALBUMARTIST_THEN_JRIVER -> {
					var vorbisCommentFieldKey =
						VorbisCommentFieldKey.ALBUMARTIST
					val value = super.getItem(vorbisCommentFieldKey.fieldName, n)
					if (value.isEmpty()) {
						vorbisCommentFieldKey =
							VorbisCommentFieldKey.ALBUMARTIST_JRIVER
						super.getItem(vorbisCommentFieldKey.fieldName, n)
					} else {
						value
					}
				}
				VorbisAlbumArtistReadOptions.READ_JRIVER_THEN_ALBUMARTIST -> {
					var vorbisCommentFieldKey =
						VorbisCommentFieldKey.ALBUMARTIST_JRIVER
					val value = super.getItem(vorbisCommentFieldKey.fieldName, n)
					if (value.isEmpty()) {
						vorbisCommentFieldKey =
							VorbisCommentFieldKey.ALBUMARTIST
						super.getItem(vorbisCommentFieldKey.fieldName, n)
					} else {
						value
					}
				}
				else -> {
					val vorbisCommentFieldKey =
						tagFieldToOggField[genericKey]
							?: throw KeyNotFoundException()
					super.getItem(vorbisCommentFieldKey.fieldName, n)
				}
			}
		} else {
			val vorbisCommentFieldKey =
				tagFieldToOggField[genericKey]
					?: throw KeyNotFoundException()
			super.getItem(vorbisCommentFieldKey.fieldName, n)
		}
	}

	companion object {
		private val tagFieldToOggField = EnumMap<FieldKey, VorbisCommentFieldKey>(
			FieldKey::class.java
		)

		init {
			tagFieldToOggField[FieldKey.ACOUSTID_FINGERPRINT] =
				VorbisCommentFieldKey.ACOUSTID_FINGERPRINT
			tagFieldToOggField[FieldKey.ACOUSTID_ID] =
				VorbisCommentFieldKey.ACOUSTID_ID
			tagFieldToOggField[FieldKey.ALBUM] =
				VorbisCommentFieldKey.ALBUM
			tagFieldToOggField[FieldKey.ALBUM_ARTIST] =
				VorbisCommentFieldKey.ALBUMARTIST
			tagFieldToOggField[FieldKey.ALBUM_YEAR] =
				VorbisCommentFieldKey.ALBUM_YEAR
			tagFieldToOggField[FieldKey.ALBUM_ARTISTS] =
				VorbisCommentFieldKey.ALBUMARTISTS
			tagFieldToOggField[FieldKey.ALBUM_ARTISTS_SORT] =
				VorbisCommentFieldKey.ALBUMARTISTSSORT
			tagFieldToOggField[FieldKey.ALBUM_ARTIST_SORT] =
				VorbisCommentFieldKey.ALBUMARTISTSORT
			tagFieldToOggField[FieldKey.ALBUM_SORT] =
				VorbisCommentFieldKey.ALBUMSORT
			tagFieldToOggField[FieldKey.AMAZON_ID] =
				VorbisCommentFieldKey.ASIN
			tagFieldToOggField[FieldKey.ARRANGER] =
				VorbisCommentFieldKey.ARRANGER
			tagFieldToOggField[FieldKey.ARRANGER_SORT] =
				VorbisCommentFieldKey.ARRANGER_SORT
			tagFieldToOggField[FieldKey.ARTIST] =
				VorbisCommentFieldKey.ARTIST
			tagFieldToOggField[FieldKey.ARTISTS] =
				VorbisCommentFieldKey.ARTISTS
			tagFieldToOggField[FieldKey.ARTISTS_SORT] =
				VorbisCommentFieldKey.ARTISTS_SORT
			tagFieldToOggField[FieldKey.ARTIST_SORT] =
				VorbisCommentFieldKey.ARTISTSORT
			tagFieldToOggField[FieldKey.BARCODE] =
				VorbisCommentFieldKey.BARCODE
			tagFieldToOggField[FieldKey.BPM] =
				VorbisCommentFieldKey.BPM
			tagFieldToOggField[FieldKey.CATALOG_NO] =
				VorbisCommentFieldKey.CATALOGNUMBER
			tagFieldToOggField[FieldKey.CHOIR] =
				VorbisCommentFieldKey.CHOIR
			tagFieldToOggField[FieldKey.CHOIR_SORT] =
				VorbisCommentFieldKey.CHOIR_SORT
			tagFieldToOggField[FieldKey.CLASSICAL_CATALOG] =
				VorbisCommentFieldKey.CLASSICAL_CATALOG
			tagFieldToOggField[FieldKey.CLASSICAL_NICKNAME] =
				VorbisCommentFieldKey.CLASSICAL_NICKNAME
			tagFieldToOggField[FieldKey.COMMENT] =
				VorbisCommentFieldKey.COMMENT
			tagFieldToOggField[FieldKey.COMPOSER] =
				VorbisCommentFieldKey.COMPOSER
			tagFieldToOggField[FieldKey.COMPOSER_SORT] =
				VorbisCommentFieldKey.COMPOSERSORT
			tagFieldToOggField[FieldKey.COPYRIGHT] =
				VorbisCommentFieldKey.COPYRIGHT
			tagFieldToOggField[FieldKey.CONDUCTOR] =
				VorbisCommentFieldKey.CONDUCTOR
			tagFieldToOggField[FieldKey.CONDUCTOR_SORT] =
				VorbisCommentFieldKey.CONDUCTOR_SORT
			tagFieldToOggField[FieldKey.COUNTRY] =
				VorbisCommentFieldKey.COUNTRY
			tagFieldToOggField[FieldKey.COVER_ART] =
				VorbisCommentFieldKey.METADATA_BLOCK_PICTURE
			tagFieldToOggField[FieldKey.CUSTOM1] =
				VorbisCommentFieldKey.CUSTOM1
			tagFieldToOggField[FieldKey.CUSTOM2] =
				VorbisCommentFieldKey.CUSTOM2
			tagFieldToOggField[FieldKey.CUSTOM3] =
				VorbisCommentFieldKey.CUSTOM3
			tagFieldToOggField[FieldKey.CUSTOM4] =
				VorbisCommentFieldKey.CUSTOM4
			tagFieldToOggField[FieldKey.CUSTOM5] =
				VorbisCommentFieldKey.CUSTOM5
			tagFieldToOggField[FieldKey.DISC_NO] =
				VorbisCommentFieldKey.DISCNUMBER
			tagFieldToOggField[FieldKey.DISC_SUBTITLE] =
				VorbisCommentFieldKey.DISCSUBTITLE
			tagFieldToOggField[FieldKey.DISC_TOTAL] =
				VorbisCommentFieldKey.DISCTOTAL
			tagFieldToOggField[FieldKey.DJMIXER] =
				VorbisCommentFieldKey.DJMIXER
			tagFieldToOggField[FieldKey.DJMIXER_SORT] =
				VorbisCommentFieldKey.DJMIXER_SORT
			tagFieldToOggField[FieldKey.ENCODER] =
				VorbisCommentFieldKey.VENDOR //Known as vendor in VorbisComment
			tagFieldToOggField[FieldKey.ENGINEER] =
				VorbisCommentFieldKey.ENGINEER
			tagFieldToOggField[FieldKey.ENGINEER_SORT] =
				VorbisCommentFieldKey.ENGINEER_SORT
			tagFieldToOggField[FieldKey.ENSEMBLE] =
				VorbisCommentFieldKey.ENSEMBLE
			tagFieldToOggField[FieldKey.ENSEMBLE_SORT] =
				VorbisCommentFieldKey.ENSEMBLE_SORT
			tagFieldToOggField[FieldKey.FBPM] =
				VorbisCommentFieldKey.FBPM
			tagFieldToOggField[FieldKey.GENRE] =
				VorbisCommentFieldKey.GENRE
			tagFieldToOggField[FieldKey.GROUP] =
				VorbisCommentFieldKey.GROUP
			tagFieldToOggField[FieldKey.GROUPING] =
				VorbisCommentFieldKey.GROUPING
			tagFieldToOggField[FieldKey.INSTRUMENT] =
				VorbisCommentFieldKey.INSTRUMENT
			tagFieldToOggField[FieldKey.INVOLVEDPEOPLE] =
				VorbisCommentFieldKey.INVOLVEDPEOPLE
			tagFieldToOggField[FieldKey.IPI] =
				VorbisCommentFieldKey.IPI
			tagFieldToOggField[FieldKey.ISRC] =
				VorbisCommentFieldKey.ISRC
			tagFieldToOggField[FieldKey.ISWC] =
				VorbisCommentFieldKey.ISWC
			tagFieldToOggField[FieldKey.IS_CLASSICAL] =
				VorbisCommentFieldKey.IS_CLASSICAL
			tagFieldToOggField[FieldKey.IS_COMPILATION] =
				VorbisCommentFieldKey.COMPILATION
			tagFieldToOggField[FieldKey.IS_GREATEST_HITS] =
				VorbisCommentFieldKey.IS_GREATEST_HITS
			tagFieldToOggField[FieldKey.IS_HD] =
				VorbisCommentFieldKey.IS_HD
			tagFieldToOggField[FieldKey.IS_LIVE] =
				VorbisCommentFieldKey.IS_LIVE
			tagFieldToOggField[FieldKey.IS_SOUNDTRACK] =
				VorbisCommentFieldKey.IS_SOUNDTRACK
			tagFieldToOggField[FieldKey.JAIKOZ_ID] =
				VorbisCommentFieldKey.JAIKOZ_ID
			tagFieldToOggField[FieldKey.KEY] =
				VorbisCommentFieldKey.KEY
			tagFieldToOggField[FieldKey.LANGUAGE] =
				VorbisCommentFieldKey.LANGUAGE
			tagFieldToOggField[FieldKey.LYRICIST] =
				VorbisCommentFieldKey.LYRICIST
			tagFieldToOggField[FieldKey.LYRICIST_SORT] =
				VorbisCommentFieldKey.LYRICIST_SORT
			tagFieldToOggField[FieldKey.LYRICS] =
				VorbisCommentFieldKey.LYRICS
			tagFieldToOggField[FieldKey.MEDIA] =
				VorbisCommentFieldKey.MEDIA
			tagFieldToOggField[FieldKey.MIXER] =
				VorbisCommentFieldKey.MIXER
			tagFieldToOggField[FieldKey.MIXER_SORT] =
				VorbisCommentFieldKey.MIXER_SORT
			tagFieldToOggField[FieldKey.MOOD] =
				VorbisCommentFieldKey.MOOD
			tagFieldToOggField[FieldKey.MOOD_ACOUSTIC] =
				VorbisCommentFieldKey.MOOD_ACOUSTIC
			tagFieldToOggField[FieldKey.MOOD_AGGRESSIVE] =
				VorbisCommentFieldKey.MOOD_AGGRESSIVE
			tagFieldToOggField[FieldKey.MOOD_AROUSAL] =
				VorbisCommentFieldKey.MOOD_AROUSAL
			tagFieldToOggField[FieldKey.MOOD_DANCEABILITY] =
				VorbisCommentFieldKey.MOOD_DANCEABILITY
			tagFieldToOggField[FieldKey.MOOD_ELECTRONIC] =
				VorbisCommentFieldKey.MOOD_ELECTRONIC
			tagFieldToOggField[FieldKey.MOOD_HAPPY] =
				VorbisCommentFieldKey.MOOD_HAPPY
			tagFieldToOggField[FieldKey.MOOD_INSTRUMENTAL] =
				VorbisCommentFieldKey.MOOD_INSTRUMENTAL
			tagFieldToOggField[FieldKey.MOOD_PARTY] =
				VorbisCommentFieldKey.MOOD_PARTY
			tagFieldToOggField[FieldKey.MOOD_RELAXED] =
				VorbisCommentFieldKey.MOOD_RELAXED
			tagFieldToOggField[FieldKey.MOOD_SAD] =
				VorbisCommentFieldKey.MOOD_SAD
			tagFieldToOggField[FieldKey.MOOD_VALENCE] =
				VorbisCommentFieldKey.MOOD_VALENCE
			tagFieldToOggField[FieldKey.MOVEMENT] =
				VorbisCommentFieldKey.MOVEMENT
			tagFieldToOggField[FieldKey.MOVEMENT_NO] =
				VorbisCommentFieldKey.MOVEMENT_NO
			tagFieldToOggField[FieldKey.MOVEMENT_TOTAL] =
				VorbisCommentFieldKey.MOVEMENT_TOTAL
			tagFieldToOggField[FieldKey.MUSICBRAINZ_ARTISTID] =
				VorbisCommentFieldKey.MUSICBRAINZ_ARTISTID
			tagFieldToOggField[FieldKey.MUSICBRAINZ_DISC_ID] =
				VorbisCommentFieldKey.MUSICBRAINZ_DISCID
			tagFieldToOggField[FieldKey.MUSICBRAINZ_ORIGINAL_RELEASE_ID] =
				VorbisCommentFieldKey.MUSICBRAINZ_ORIGINAL_ALBUMID
			tagFieldToOggField[FieldKey.MUSICBRAINZ_RELEASEARTISTID] =
				VorbisCommentFieldKey.MUSICBRAINZ_ALBUMARTISTID
			tagFieldToOggField[FieldKey.MUSICBRAINZ_RELEASEID] =
				VorbisCommentFieldKey.MUSICBRAINZ_ALBUMID
			tagFieldToOggField[FieldKey.MUSICBRAINZ_RELEASE_COUNTRY] =
				VorbisCommentFieldKey.RELEASECOUNTRY
			tagFieldToOggField[FieldKey.MUSICBRAINZ_RELEASE_GROUP_ID] =
				VorbisCommentFieldKey.MUSICBRAINZ_RELEASEGROUPID
			tagFieldToOggField[FieldKey.MUSICBRAINZ_RELEASE_STATUS] =
				VorbisCommentFieldKey.MUSICBRAINZ_ALBUMSTATUS
			tagFieldToOggField[FieldKey.MUSICBRAINZ_RELEASE_TRACK_ID] =
				VorbisCommentFieldKey.MUSICBRAINZ_RELEASETRACKID
			tagFieldToOggField[FieldKey.MUSICBRAINZ_RELEASE_TYPE] =
				VorbisCommentFieldKey.MUSICBRAINZ_ALBUMTYPE
			tagFieldToOggField[FieldKey.MUSICBRAINZ_TRACK_ID] =
				VorbisCommentFieldKey.MUSICBRAINZ_TRACKID
			tagFieldToOggField[FieldKey.MUSICBRAINZ_WORK] =
				VorbisCommentFieldKey.MUSICBRAINZ_WORK
			tagFieldToOggField[FieldKey.MUSICBRAINZ_RECORDING_WORK] =
				VorbisCommentFieldKey.MUSICBRAINZ_RECORDING_WORK
			tagFieldToOggField[FieldKey.MUSICBRAINZ_RECORDING_WORK_ID] =
				VorbisCommentFieldKey.MUSICBRAINZ_RECORDING_WORK_ID
			tagFieldToOggField[FieldKey.MUSICBRAINZ_WORK_ID] =
				VorbisCommentFieldKey.MUSICBRAINZ_WORKID
			tagFieldToOggField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL1] =
				VorbisCommentFieldKey.MUSICBRAINZ_WORK_PART_LEVEL1
			tagFieldToOggField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL1_ID] =
				VorbisCommentFieldKey.MUSICBRAINZ_WORK_PART_LEVEL1_ID
			tagFieldToOggField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL1_TYPE] =
				VorbisCommentFieldKey.MUSICBRAINZ_WORK_PART_LEVEL1_TYPE
			tagFieldToOggField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL2] =
				VorbisCommentFieldKey.MUSICBRAINZ_WORK_PART_LEVEL2
			tagFieldToOggField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL2_ID] =
				VorbisCommentFieldKey.MUSICBRAINZ_WORK_PART_LEVEL2_ID
			tagFieldToOggField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL2_TYPE] =
				VorbisCommentFieldKey.MUSICBRAINZ_WORK_PART_LEVEL2_TYPE
			tagFieldToOggField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL3] =
				VorbisCommentFieldKey.MUSICBRAINZ_WORK_PART_LEVEL3
			tagFieldToOggField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL3_ID] =
				VorbisCommentFieldKey.MUSICBRAINZ_WORK_PART_LEVEL3_ID
			tagFieldToOggField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL3_TYPE] =
				VorbisCommentFieldKey.MUSICBRAINZ_WORK_PART_LEVEL3_TYPE
			tagFieldToOggField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL4] =
				VorbisCommentFieldKey.MUSICBRAINZ_WORK_PART_LEVEL4
			tagFieldToOggField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL4_ID] =
				VorbisCommentFieldKey.MUSICBRAINZ_WORK_PART_LEVEL4_ID
			tagFieldToOggField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL4_TYPE] =
				VorbisCommentFieldKey.MUSICBRAINZ_WORK_PART_LEVEL4_TYPE
			tagFieldToOggField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL5] =
				VorbisCommentFieldKey.MUSICBRAINZ_WORK_PART_LEVEL5
			tagFieldToOggField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL5_ID] =
				VorbisCommentFieldKey.MUSICBRAINZ_WORK_PART_LEVEL5_ID
			tagFieldToOggField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL5_TYPE] =
				VorbisCommentFieldKey.MUSICBRAINZ_WORK_PART_LEVEL5_TYPE
			tagFieldToOggField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL6] =
				VorbisCommentFieldKey.MUSICBRAINZ_WORK_PART_LEVEL6
			tagFieldToOggField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL6_ID] =
				VorbisCommentFieldKey.MUSICBRAINZ_WORK_PART_LEVEL6_ID
			tagFieldToOggField[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL6_TYPE] =
				VorbisCommentFieldKey.MUSICBRAINZ_WORK_PART_LEVEL6_TYPE
			tagFieldToOggField[FieldKey.MUSICIP_ID] =
				VorbisCommentFieldKey.MUSICIP_PUID
			tagFieldToOggField[FieldKey.OCCASION] =
				VorbisCommentFieldKey.OCCASION
			tagFieldToOggField[FieldKey.OPUS] =
				VorbisCommentFieldKey.OPUS
			tagFieldToOggField[FieldKey.ORCHESTRA] =
				VorbisCommentFieldKey.ORCHESTRA
			tagFieldToOggField[FieldKey.ORCHESTRA_SORT] =
				VorbisCommentFieldKey.ORCHESTRA_SORT
			tagFieldToOggField[FieldKey.ORIGINAL_ALBUM] =
				VorbisCommentFieldKey.ORIGINAL_ALBUM
			tagFieldToOggField[FieldKey.ORIGINALRELEASEDATE] =
				VorbisCommentFieldKey.ORIGINALRELEASEDATE
			tagFieldToOggField[FieldKey.ORIGINAL_ARTIST] =
				VorbisCommentFieldKey.ORIGINAL_ARTIST
			tagFieldToOggField[FieldKey.ORIGINAL_LYRICIST] =
				VorbisCommentFieldKey.ORIGINAL_LYRICIST
			tagFieldToOggField[FieldKey.ORIGINAL_YEAR] =
				VorbisCommentFieldKey.ORIGINAL_YEAR
			tagFieldToOggField[FieldKey.OVERALL_WORK] =
				VorbisCommentFieldKey.OVERALL_WORK
			tagFieldToOggField[FieldKey.PART] =
				VorbisCommentFieldKey.PART
			tagFieldToOggField[FieldKey.PART_NUMBER] =
				VorbisCommentFieldKey.PART_NUMBER
			tagFieldToOggField[FieldKey.PART_TYPE] =
				VorbisCommentFieldKey.PART_TYPE
			tagFieldToOggField[FieldKey.PERFORMER] =
				VorbisCommentFieldKey.PERFORMER
			tagFieldToOggField[FieldKey.PERFORMER_NAME] =
				VorbisCommentFieldKey.PERFORMER_NAME
			tagFieldToOggField[FieldKey.PERFORMER_NAME_SORT] =
				VorbisCommentFieldKey.PERFORMER_NAME_SORT
			tagFieldToOggField[FieldKey.PERIOD] =
				VorbisCommentFieldKey.PERIOD
			tagFieldToOggField[FieldKey.PRODUCER] =
				VorbisCommentFieldKey.PRODUCER
			tagFieldToOggField[FieldKey.PRODUCER_SORT] =
				VorbisCommentFieldKey.PRODUCER_SORT
			tagFieldToOggField[FieldKey.QUALITY] =
				VorbisCommentFieldKey.QUALITY
			tagFieldToOggField[FieldKey.RANKING] =
				VorbisCommentFieldKey.RANKING
			tagFieldToOggField[FieldKey.RATING] =
				VorbisCommentFieldKey.RATING
			tagFieldToOggField[FieldKey.RECORD_LABEL] =
				VorbisCommentFieldKey.LABEL
			tagFieldToOggField[FieldKey.RECORDINGLOCATION] =
				VorbisCommentFieldKey.RECORDINGLOCATION
			tagFieldToOggField[FieldKey.RECORDINGDATE] =
				VorbisCommentFieldKey.RECORDINGDATE
			tagFieldToOggField[FieldKey.RECORDINGSTARTDATE] =
				VorbisCommentFieldKey.RECORDINGSTARTDATE
			tagFieldToOggField[FieldKey.RECORDINGENDDATE] =
				VorbisCommentFieldKey.RECORDINGENDDATE
			tagFieldToOggField[FieldKey.REMIXER] =
				VorbisCommentFieldKey.REMIXER
			tagFieldToOggField[FieldKey.ROONALBUMTAG] =
				VorbisCommentFieldKey.ROONALBUMTAG
			tagFieldToOggField[FieldKey.ROONTRACKTAG] =
				VorbisCommentFieldKey.ROONTRACKTAG
			tagFieldToOggField[FieldKey.SCRIPT] =
				VorbisCommentFieldKey.SCRIPT
			tagFieldToOggField[FieldKey.SECTION] =
				VorbisCommentFieldKey.SECTION
			tagFieldToOggField[FieldKey.SINGLE_DISC_TRACK_NO] =
				VorbisCommentFieldKey.SINGLE_DISC_TRACK_NO
			tagFieldToOggField[FieldKey.SONGKONG_ID] =
				VorbisCommentFieldKey.SONGKONG_ID
			tagFieldToOggField[FieldKey.SUBTITLE] =
				VorbisCommentFieldKey.SUBTITLE
			tagFieldToOggField[FieldKey.TAGS] =
				VorbisCommentFieldKey.TAGS
			tagFieldToOggField[FieldKey.TEMPO] =
				VorbisCommentFieldKey.TEMPO
			tagFieldToOggField[FieldKey.TIMBRE] =
				VorbisCommentFieldKey.TIMBRE
			tagFieldToOggField[FieldKey.TITLE] =
				VorbisCommentFieldKey.TITLE
			tagFieldToOggField[FieldKey.TITLE_MOVEMENT] =
				VorbisCommentFieldKey.TITLE_MOVEMENT
			tagFieldToOggField[FieldKey.TITLE_SORT] =
				VorbisCommentFieldKey.TITLESORT
			tagFieldToOggField[FieldKey.TONALITY] =
				VorbisCommentFieldKey.TONALITY
			tagFieldToOggField[FieldKey.TRACK] =
				VorbisCommentFieldKey.TRACKNUMBER
			tagFieldToOggField[FieldKey.TRACK_TOTAL] =
				VorbisCommentFieldKey.TRACKTOTAL
			tagFieldToOggField[FieldKey.URL_DISCOGS_ARTIST_SITE] =
				VorbisCommentFieldKey.URL_DISCOGS_ARTIST_SITE
			tagFieldToOggField[FieldKey.URL_DISCOGS_RELEASE_SITE] =
				VorbisCommentFieldKey.URL_DISCOGS_RELEASE_SITE
			tagFieldToOggField[FieldKey.URL_LYRICS_SITE] =
				VorbisCommentFieldKey.URL_LYRICS_SITE
			tagFieldToOggField[FieldKey.URL_OFFICIAL_ARTIST_SITE] =
				VorbisCommentFieldKey.URL_OFFICIAL_ARTIST_SITE
			tagFieldToOggField[FieldKey.URL_OFFICIAL_RELEASE_SITE] =
				VorbisCommentFieldKey.URL_OFFICIAL_RELEASE_SITE
			tagFieldToOggField[FieldKey.URL_WIKIPEDIA_ARTIST_SITE] =
				VorbisCommentFieldKey.URL_WIKIPEDIA_ARTIST_SITE
			tagFieldToOggField[FieldKey.URL_WIKIPEDIA_RELEASE_SITE] =
				VorbisCommentFieldKey.URL_WIKIPEDIA_RELEASE_SITE
			tagFieldToOggField[FieldKey.VERSION] =
				VorbisCommentFieldKey.VERSION
			tagFieldToOggField[FieldKey.WORK] =
				VorbisCommentFieldKey.WORK
			tagFieldToOggField[FieldKey.WORK_TYPE] =
				VorbisCommentFieldKey.WORK_TYPE
			tagFieldToOggField[FieldKey.YEAR] =
				VorbisCommentFieldKey.DATE
		}

		//This is the vendor string that will be written if no other is supplied. Should be the name of the software
		//that actually encoded the file in the first place.
		const val DEFAULT_VENDOR = "jaudiotagger"

		/**
		 * Use to construct a new tag properly initialized
		 *
		 * @return
		 */
		@JvmStatic
		fun createNewTag(): VorbisCommentTag {
			val tag = VorbisCommentTag()
			tag.vendor = DEFAULT_VENDOR
			return tag
		}
	}
}
