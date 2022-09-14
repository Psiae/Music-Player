package com.flammky.musicplayer.common.media.audio.meta_tag.tag.flac

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.flac.metadatablock.MetadataBlockDataPicture
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.images.AndroidArtwork
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.images.Artwork
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.PictureTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.vorbiscomment.VorbisAlbumArtistSaveOptions
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.vorbiscomment.VorbisCommentFieldKey
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.vorbiscomment.VorbisCommentTag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.vorbiscomment.VorbisCommentTag.Companion.createNewTag
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Flac uses Vorbis Comment for most of its metadata and a Flac Picture Block for images
 *
 *
 * This class enscapulates the items into a single tag
 */
class FlacTag @JvmOverloads constructor(
	tag: VorbisCommentTag? = createNewTag(),
	private val images: MutableList<MetadataBlockDataPicture> = ArrayList()
) : Tag {
	/**
	 * @return the vorbis tag (this is what handles text metadata)
	 */
	var vorbisCommentTag: VorbisCommentTag? = tag

	init {
		vorbisCommentTag = tag
	}

	/**
	 * @return images
	 */
	fun getImages(): List<MetadataBlockDataPicture> {
		return images
	}

	@Throws(FieldDataInvalidException::class)
	override fun addField(field: TagField?) {
		if (field is MetadataBlockDataPicture) {
			images.add(field)
		} else {
			vorbisCommentTag!!.addField(field)
		}
	}

	override fun getFields(id: String?): List<TagField?> {
		return if (id == FieldKey.COVER_ART.name) {
			val castImages: MutableList<TagField?> = ArrayList()
			for (image in images) {
				castImages.add(image)
			}
			castImages
		} else {
			vorbisCommentTag?.getFields(id) ?: emptyList()
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
		return if (genericKey === FieldKey.COVER_ART) {
			throw UnsupportedOperationException(ErrorMessage.ARTWORK_CANNOT_BE_CREATED_WITH_THIS_METHOD.msg)
		} else {
			vorbisCommentTag!!.getAll(genericKey)
		}
	}

	override fun hasCommonFields(): Boolean {
		return vorbisCommentTag!!.hasCommonFields()
	}

	/**
	 * Determines whether the tag has no fields specified.<br></br>
	 *
	 *
	 * If there are no images we return empty if either there is no VorbisTag or if there is a
	 * VorbisTag but it is empty
	 *
	 * @return `true` if tag contains no field.
	 */
	override val isEmpty: Boolean
		get() = vorbisCommentTag?.isEmpty != false && images.size == 0

	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun setField(genericKey: FieldKey?, vararg values: String?) {
		require(values[0] != null) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
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
		require(values[0] != null) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
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
	 * @param field
	 * @throws FieldDataInvalidException
	 */
	@Throws(FieldDataInvalidException::class)
	override fun setField(field: TagField?) {
		if (field is MetadataBlockDataPicture) {
			if (images.size == 0) {
				images.add(0, field)
			} else {
				images[0] = field
			}
		} else {
			vorbisCommentTag!!.setField(field)
		}
	}

	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun createField(genericKey: FieldKey?, vararg value: String?): TagField? {
		return if (genericKey == FieldKey.COVER_ART) {
			throw UnsupportedOperationException(ErrorMessage.ARTWORK_CANNOT_BE_CREATED_WITH_THIS_METHOD.msg)
		} else {
			vorbisCommentTag?.createField(genericKey, *value)
		}
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
	fun createField(vorbisCommentFieldKey: VorbisCommentFieldKey, value: String?): TagField {
		if (vorbisCommentFieldKey == VorbisCommentFieldKey.COVERART) {
			throw UnsupportedOperationException(ErrorMessage.ARTWORK_CANNOT_BE_CREATED_WITH_THIS_METHOD.msg)
		}
		return vorbisCommentTag!!.createField(vorbisCommentFieldKey, value)
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
		if (vorbisCommentFieldKey == VorbisCommentFieldKey.COVERART.fieldName) {
			throw UnsupportedOperationException(ErrorMessage.ARTWORK_CANNOT_BE_CREATED_WITH_THIS_METHOD.msg)
		}
		return vorbisCommentTag!!.createField(vorbisCommentFieldKey, value)
	}

	override fun getFirst(id: String?): String? {
		return if (id == FieldKey.COVER_ART.name) {
			throw UnsupportedOperationException(ErrorMessage.ARTWORK_CANNOT_BE_CREATED_WITH_THIS_METHOD.msg)
		} else {
			vorbisCommentTag!!.getFirst(id)
		}
	}

	@Throws(KeyNotFoundException::class)
	override fun getValue(id: FieldKey?, n: Int): String? {
		return if (id == FieldKey.COVER_ART) {
			throw UnsupportedOperationException(ErrorMessage.ARTWORK_CANNOT_BE_RETRIEVED_WITH_THIS_METHOD.msg)
		} else {
			vorbisCommentTag!!.getValue(id, n)
		}
	}

	@Throws(KeyNotFoundException::class)
	override fun getFirst(id: FieldKey?): String? {
		return getValue(id, 0)
	}

	override fun getFirstField(id: String?): TagField? {
		return if (id == FieldKey.COVER_ART.name) {
			if (images.size > 0) {
				images[0]
			} else {
				null
			}
		} else {
			vorbisCommentTag!!.getFirstField(id)
		}
	}

	@Throws(KeyNotFoundException::class)
	override fun getFirstField(genericKey: FieldKey?): TagField? {
		if (genericKey == null) {
			throw KeyNotFoundException()
		}
		return if (genericKey === FieldKey.COVER_ART) {
			getFirstField(FieldKey.COVER_ART.name)
		} else {
			vorbisCommentTag!!.getFirstField(genericKey)
		}
	}

	/**
	 * Delete any instance of tag fields with this key
	 *
	 * @param fieldKey
	 */
	@Throws(KeyNotFoundException::class)
	override fun deleteField(fieldKey: FieldKey?) {
		if (fieldKey == FieldKey.COVER_ART) {
			images.clear()
		} else {
			vorbisCommentTag!!.deleteField(fieldKey)
		}
	}

	@Throws(KeyNotFoundException::class)
	override fun deleteField(id: String?) {
		if (id == FieldKey.COVER_ART.name) {
			images.clear()
		} else {
			vorbisCommentTag!!.deleteField(id)
		}
	}

	//TODO addField images to iterator
	override val fields: Iterator<TagField>
		get() = vorbisCommentTag!!.fields
	override val fieldCount: Int
		get() = vorbisCommentTag!!.fieldCount + images.size
	override val fieldCountIncludingSubValues: Int
		get() = fieldCount

	@Throws(FieldDataInvalidException::class)
	override fun setEncoding(enc: Charset?): Boolean {
		return vorbisCommentTag!!.setEncoding(enc)
	}

	@Throws(KeyNotFoundException::class)
	override fun getFields(id: FieldKey?): List<TagField?>? {
		return if (id == FieldKey.COVER_ART) {
			val castImages: MutableList<TagField?> = ArrayList()
			for (image in images) {
				castImages.add(image)
			}
			castImages
		} else {
			vorbisCommentTag!!.getFields(id)
		}
	}

	@Throws(FieldDataInvalidException::class)
	fun createArtworkField(
		imageData: ByteArray?,
		pictureType: Int,
		mimeType: String?,
		description: String?,
		width: Int,
		height: Int,
		colourDepth: Int,
		indexedColouredCount: Int
	): TagField {
		if (imageData == null) {
			throw FieldDataInvalidException("ImageData cannot be null")
		}
		return MetadataBlockDataPicture(
			imageData,
			pictureType,
			mimeType,
			description!!,
			width,
			height,
			colourDepth,
			indexedColouredCount
		)
	}

	/**
	 * Create Link to Image File, not recommended because if either flac or image file is moved link
	 * will be broken.
	 * @param url
	 * @return
	 */
	fun createLinkedArtworkField(url: String): TagField {
		//Add to image list
		return MetadataBlockDataPicture(
			url.toByteArray(StandardCharsets.ISO_8859_1),
			PictureTypes.DEFAULT_ID,
			MetadataBlockDataPicture.IMAGE_IS_URL,
			"",
			0,
			0,
			0,
			0
		)
	}

	/**
	 * Create artwork field
	 *
	 * @return
	 */
	@Throws(FieldDataInvalidException::class)
	override fun createField(artwork: Artwork?): TagField? {
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
				throw FieldDataInvalidException(
					"Unable to createField buffered image from the image"
				)
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

	@Throws(FieldDataInvalidException::class)
	override fun setField(artwork: Artwork?) {
		this.setField(createField(artwork))
	}

	@Throws(FieldDataInvalidException::class)
	override fun addField(artwork: Artwork?) {
		this.addField(createField(artwork))
	}

	override val artworkList: List<Artwork>
		get() {
			val artworkList: MutableList<Artwork> = ArrayList(images.size)
			for (coverArt in images) {
				val artwork = AndroidArtwork.createArtworkFromMetadataBlockDataPicture(coverArt)
				artworkList.add(artwork)
			}
			return artworkList
		}
	override val firstArtwork: Artwork?
		get() {
			val artwork = artworkList
			return if (artwork.size > 0) {
				artwork[0]
			} else null
		}

	/**
	 * Delete all instance of artwork Field
	 *
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	override fun deleteArtworkField() {
		this.deleteField(FieldKey.COVER_ART)
	}

	/**
	 *
	 * @param genericKey
	 * @return
	 */
	override fun hasField(genericKey: FieldKey?): Boolean {
		return if (genericKey === FieldKey.COVER_ART) {
			images.size > 0
		} else {
			vorbisCommentTag!!.hasField(genericKey)
		}
	}

	/**
	 *
	 * @param vorbisFieldKey
	 * @return
	 */
	fun hasField(vorbisFieldKey: VorbisCommentFieldKey?): Boolean {
		return vorbisCommentTag!!.hasField(vorbisFieldKey!!)
	}

	override fun hasField(id: String?): Boolean {
		return if (id == FieldKey.COVER_ART.name) {
			images.size > 0
		} else {
			vorbisCommentTag!!.hasField(id)
		}
	}

	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun createCompilationField(value: Boolean): TagField? {
		return vorbisCommentTag!!.createCompilationField(value)
	}

	override fun toString(): String {
		val sb = StringBuilder("FLAC " + vorbisCommentTag)
		if (images.size > 0) {
			sb.append("\n\tImages\n")
			for (next in images) {
				sb.append(next)
			}
		}
		return sb.toString()
	}
}
