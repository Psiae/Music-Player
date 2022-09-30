/**
 * @author : Paul Taylor
 * @author : Eric Farng
 *
 * Version @version:$Id$
 *
 * MusicTag Copyright (C)2003,2004
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 * or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not,
 * you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * Description:
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp3.MP3File
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.truncate
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.images.Artwork
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.GenreTypes
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Represents an ID3v1 tag.
 *
 * @author : Eric Farng
 * @author : Paul Taylor
 */

open class ID3v1Tag : AbstractID3v1Tag, Tag {

	override val release: Byte
		get() = Companion.release

	override val majorVersion: Byte
		get() = Companion.majorVersion

	override val revision: Byte
		get() = Companion.revision

	override fun toString(): String = "ID3v$release.$majorVersion.$release@${hashCode()}"

	/**
	 * Get Album
	 *
	 * @return album
	 */
	/**
	 *
	 */
	var firstAlbum: String? = ""
	/**
	 * Get Artist
	 *
	 * @return artist
	 */
	/**
	 *
	 */
	var firstArtist: String? = ""
	/**
	 * Get Comment
	 *
	 * @return comment
	 */
	/**
	 *
	 */
	open var firstComment: String? = ""
	/**
	 * Get title
	 *
	 * @return Title
	 */
	/**
	 *
	 */
	var firstTitle: String? = ""
	/**
	 * Get year
	 *
	 * @return year
	 */
	/**
	 *
	 */
	var firstYear: String? = ""

	/**
	 *
	 */
	@JvmField
	var genre = ((-1).toByte())

	/**
	 * Creates a new ID3v1 datatype.
	 */
	constructor()
	constructor(copyObject: ID3v1Tag) : super(copyObject) {
		firstAlbum = copyObject.firstAlbum
		firstArtist = copyObject.firstArtist
		firstComment = copyObject.firstComment
		firstTitle = copyObject.firstTitle
		firstYear = copyObject.firstYear
		genre = copyObject.genre
	}

	constructor(mp3tag: AbstractTag?) {
		if (mp3tag != null) {
			val convertedTag: ID3v11Tag
			if (mp3tag is ID3v1Tag) {
				throw UnsupportedOperationException("Copy Constructor not called. Please type cast the argument")
			}
			convertedTag =
				if (mp3tag is ID3v11Tag) {
					mp3tag
				} else {
					ID3v11Tag(mp3tag)
				}
			firstAlbum = convertedTag.firstAlbum
			firstArtist = convertedTag.firstArtist
			firstComment = convertedTag.firstComment
			firstTitle = convertedTag.firstTitle
			firstYear = convertedTag.firstYear
			genre = convertedTag.genre
		}
	}

	/**
	 * Creates a new ID3v1 datatype.
	 *
	 * @param file
	 * @param loggingFilename
	 * @throws TagNotFoundException
	 * @throws IOException
	 */
	constructor(file: RandomAccessFile, loggingFilename: String) {
		this.loggingFilename = loggingFilename
		val fc: FileChannel
		val byteBuffer: ByteBuffer
		fc = file.channel
		if (file.length() < TAG_LENGTH) {
			throw IOException("File not large enough to contain a tag")
		}
		fc.position(file.length() - TAG_LENGTH)
		byteBuffer = ByteBuffer.allocate(TAG_LENGTH)
		fc.read(byteBuffer)
		byteBuffer.flip()
		read(byteBuffer)
	}

	constructor(fc: FileChannel) {
		val byteBuffer: ByteBuffer
		if (fc.size() < TAG_LENGTH) {
			throw IOException("File not large enough to contain a tag")
		}
		fc.position(fc.size() - TAG_LENGTH)
		byteBuffer = ByteBuffer.allocate(TAG_LENGTH)
		fc.read(byteBuffer)
		byteBuffer.flip()
		read(byteBuffer)
	}

	/**
	 * Creates a new ID3v1 datatype.
	 *
	 * @param file
	 * @throws TagNotFoundException
	 * @throws IOException
	 */
	@Deprecated("use {@link #ID3v1Tag(RandomAccessFile,String)} instead")
	constructor(file: RandomAccessFile) : this(file, "")

	override fun addField(field: TagField?) {
		//TODO
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
		val list: MutableList<String?> = ArrayList()
		list.add(getFirst(genericKey!!.name))
		return list
	}

	override fun getFields(id: String?): List<TagField?> {
		if (FieldKey.ARTIST.name == id) {
			return getArtist()
		} else if (FieldKey.ALBUM.name == id) {
			return getAlbum()
		} else if (FieldKey.TITLE.name == id) {
			return getTitle()
		} else if (FieldKey.GENRE.name == id) {
			return getGenre()
		} else if (FieldKey.YEAR.name == id) {
			return getYear()
		} else if (FieldKey.COMMENT.name == id) {
			return getComment()
		}
		return ArrayList()
	}

	override val fieldCount: Int
		get() = 6
	override val fieldCountIncludingSubValues: Int
		get() = fieldCount

	protected fun returnFieldToList(field: ID3v1TagField?): List<TagField?> {
		val fields: MutableList<TagField?> = ArrayList()
		fields.add(field)
		return fields
	}

	/**
	 * Set Album
	 *
	 * @param album
	 */
	fun setAlbum(album: String?) {
		requireNotNull(album) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
		firstAlbum = truncate(album, FIELD_ALBUM_LENGTH)
	}

	/**
	 * @return album within list or empty if does not exist
	 */
	fun getAlbum(): List<TagField?> {
		return if (firstAlbum!!.length > 0) {
			val field = ID3v1TagField(
				ID3v1FieldKey.ALBUM.name,
				firstAlbum
			)
			returnFieldToList(field)
		} else {
			ArrayList()
		}
	}

	/**
	 * Set Artist
	 *
	 * @param artist
	 */
	fun setArtist(artist: String?) {
		requireNotNull(artist) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
		firstArtist = truncate(artist, FIELD_ARTIST_LENGTH)
	}

	/**
	 * @return Artist within list or empty if does not exist
	 */
	fun getArtist(): List<TagField?> {
		return if (firstArtist!!.length > 0) {
			val field = ID3v1TagField(
				ID3v1FieldKey.ARTIST.name,
				firstArtist
			)
			returnFieldToList(field)
		} else {
			ArrayList()
		}
	}

	/**
	 * Set Comment
	 *
	 * @param comment
	 * @throws IllegalArgumentException if comment null
	 */
	open fun setComment(comment: String?) {
		requireNotNull(comment) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
		firstComment = truncate(comment, FIELD_COMMENT_LENGTH)
	}

	/**
	 * @return comment within list or empty if does not exist
	 */
	fun getComment(): List<TagField?> {
		return if (firstComment!!.length > 0) {
			val field = ID3v1TagField(
				ID3v1FieldKey.COMMENT.name,
				firstComment
			)
			returnFieldToList(field)
		} else {
			ArrayList()
		}
	}

	/**
	 * Sets the genreID,
	 *
	 *
	 * ID3v1 only supports genres defined in a predefined list
	 * so if unable to find value in list set 255, which seems to be the value
	 * winamp uses for undefined.
	 *
	 * @param genreVal
	 */
	fun setGenre(genreVal: String?) {
		requireNotNull(genreVal) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
		val genreID = GenreTypes.instanceOf.getIdForValue(genreVal)
		if (genreID != null) {
			genre = genreID.toByte()
		} else {
			genre = GENRE_UNDEFINED.toByte()
		}
	}

	/**
	 * Get Genre
	 *
	 * @return genre or empty string if not valid
	 */
	val firstGenre: String
		get() {
			val genreId = genre.toInt() and BYTE_TO_UNSIGNED
			val genreValue = GenreTypes.instanceOf.getValueForId(genreId)
			return genreValue ?: ""
		}

	/**
	 * Get Genre field
	 *
	 *
	 * Only a single genre is available in ID3v1
	 *
	 * @return
	 */
	fun getGenre(): List<TagField?> {
		return if (getFirst(FieldKey.GENRE)!!.length > 0) {
			val field = ID3v1TagField(
				ID3v1FieldKey.GENRE.name,
				getFirst(FieldKey.GENRE)
			)
			returnFieldToList(field)
		} else {
			ArrayList()
		}
	}

	/**
	 * Set Title
	 *
	 * @param title
	 */
	fun setTitle(title: String) {
		firstTitle = truncate(title, FIELD_TITLE_LENGTH)
	}

	/**
	 * Get title field
	 *
	 *
	 * Only a single title is available in ID3v1
	 *
	 * @return
	 */
	fun getTitle(): List<TagField?> {
		return if (getFirst(FieldKey.TITLE)!!.length > 0) {
			val field = ID3v1TagField(
				ID3v1FieldKey.TITLE.name,
				getFirst(FieldKey.TITLE)
			)
			returnFieldToList(field)
		} else {
			ArrayList()
		}
	}

	/**
	 * Set year
	 *
	 * @param year
	 */
	fun setYear(year: String?) {
		firstYear = truncate(year, FIELD_YEAR_LENGTH)
	}

	/**
	 * Get year field
	 *
	 *
	 * Only a single year is available in ID3v1
	 *
	 * @return
	 */
	fun getYear(): List<TagField?> {
		return if (getFirst(FieldKey.YEAR)!!.length > 0) {
			val field = ID3v1TagField(
				ID3v1FieldKey.YEAR.name,
				getFirst(FieldKey.YEAR)
			)
			returnFieldToList(field)
		} else {
			ArrayList()
		}
	}

	open val firstTrack: String
		get() {
			throw UnsupportedOperationException("ID3v10 cannot store track numbers")
		}
	open val trackList: List<TagField?>?
		get() {
			throw UnsupportedOperationException("ID3v10 cannot store track numbers")
		}

	override fun getFirstField(id: String?): TagField? {
		var results: List<TagField?>? = null
		if (FieldKey.ARTIST.name == id) {
			results = getArtist()
		} else if (FieldKey.ALBUM.name == id) {
			results = getAlbum()
		} else if (FieldKey.TITLE.name == id) {
			results = getTitle()
		} else if (FieldKey.GENRE.name == id) {
			results = getGenre()
		} else if (FieldKey.YEAR.name == id) {
			results = getYear()
		} else if (FieldKey.COMMENT.name == id) {
			results = getComment()
		}
		if (results != null) {
			if (results.size > 0) {
				return results[0]
			}
		}
		return null
	}

	override val fields: Iterator<TagField>
		get() {
			throw UnsupportedOperationException("TODO:Not done yet")
		}

	override fun hasCommonFields(): Boolean {
		//TODO
		return true
	}

	override fun hasField(genericKey: FieldKey?): Boolean {
		return getFirst(genericKey)!!.length > 0
	}

	override fun hasField(id: String?): Boolean {
		return try {
			val key = FieldKey.valueOf(
				id!!.uppercase(Locale.getDefault())
			)
			hasField(key)
		} catch (iae: IllegalArgumentException) {
			false
		}
	}

	override val isEmpty: Boolean
		get() = !getFirst(FieldKey.TITLE).isNullOrEmpty() || !firstArtist.isNullOrEmpty() || !firstAlbum.isNullOrEmpty() || !getFirst(
			FieldKey.GENRE
		).isNullOrEmpty() || !getFirst(FieldKey.YEAR).isNullOrEmpty() || !firstComment.isNullOrEmpty()

	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun setField(genericKey: FieldKey?, vararg value: String?) {
		val tagfield = createField(genericKey, *value)
		setField(tagfield)
	}

	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun addField(genericKey: FieldKey?, vararg value: String?) {
		setField(genericKey, *value)
	}

	override fun setField(field: TagField?) {
		val genericKey = FieldKey.valueOf(field!!.id!!)
		when (genericKey) {
			FieldKey.ARTIST -> setArtist(field.toDescriptiveString())
			FieldKey.ALBUM -> setAlbum(field.toDescriptiveString())
			FieldKey.TITLE -> setTitle(field.toDescriptiveString())
			FieldKey.GENRE -> setGenre(field.toDescriptiveString())
			FieldKey.YEAR -> setYear(field.toDescriptiveString())
			FieldKey.COMMENT -> setComment(field.toDescriptiveString())
			else -> {}
		}
	}

	override fun setEncoding(encoding: Charset?): Boolean {
		return true
	}

	/**
	 * Create Tag Field using generic key
	 */
	override fun createField(genericKey: FieldKey?, vararg values: String?): TagField? {
		val value = values[0]
		requireNotNull(genericKey) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
		val idv1FieldKey = tagFieldToID3v1Field[genericKey]
			?: throw KeyNotFoundException(
				ErrorMessage.INVALID_FIELD_FOR_ID3V1TAG.getMsg(
					genericKey.name
				)
			)
		return ID3v1TagField(idv1FieldKey.name, value)
	}

	val encoding: Charset
		get() = StandardCharsets.ISO_8859_1

	override fun getFirstField(genericKey: FieldKey?): TagField? {
		val l = getFields(genericKey)
		return if (l!!.size != 0) l[0] else null
	}

	/**
	 * Returns a [list][List] of [TagField] objects whose &quot;[id][TagField.getId]&quot;
	 * is the specified one.<br></br>
	 *
	 * @param genericKey The generic field key
	 * @return A list of [TagField] objects with the given &quot;id&quot;.
	 */
	override fun getFields(genericKey: FieldKey?): List<TagField?>? {
		return when (genericKey) {
			FieldKey.ARTIST -> getArtist()
			FieldKey.ALBUM -> getAlbum()
			FieldKey.TITLE -> getTitle()
			FieldKey.GENRE -> getGenre()
			FieldKey.YEAR -> getYear()
			FieldKey.COMMENT -> getComment()
			else -> ArrayList()
		}
	}

	/**
	 * Retrieve the first value that exists for this key id
	 *
	 * @param genericKey
	 * @return
	 */
	override fun getFirst(genericKey: String?): String? {
		val matchingKey = FieldKey.valueOf(
			genericKey!!
		)
		return matchingKey.let { getFirst(it) } ?: ""
	}

	/**
	 * Retrieve the first value that exists for this generic key
	 *
	 * @param genericKey
	 * @return
	 */
	override fun getFirst(genericKey: FieldKey?): String? {
		return when (genericKey) {
			FieldKey.ARTIST -> firstArtist
			FieldKey.ALBUM -> firstAlbum
			FieldKey.TITLE -> firstTitle
			FieldKey.GENRE -> firstGenre
			FieldKey.YEAR -> firstYear
			FieldKey.COMMENT -> firstComment
			else -> ""
		}
	}

	/**
	 * The m parameter is effectively ignored
	 *
	 * @param id
	 * @param n
	 * @param m
	 * @return
	 */
	fun getSubValue(id: FieldKey?, n: Int, m: Int): String? {
		return getValue(id, n)
	}

	override fun getValue(genericKey: FieldKey?, n: Int): String? {
		return getFirst(genericKey)
	}

	/**
	 * Delete any instance of tag fields with this key
	 *
	 * @param genericKey
	 */
	override fun deleteField(genericKey: FieldKey?) {
		when (genericKey) {
			FieldKey.ARTIST -> setArtist("")
			FieldKey.ALBUM -> setAlbum("")
			FieldKey.TITLE -> setTitle("")
			FieldKey.GENRE -> setGenre("")
			FieldKey.YEAR -> setYear("")
			FieldKey.COMMENT -> setComment("")
			else -> {}
		}
	}

	override fun deleteField(id: String?) {
		val key = FieldKey.valueOf(
			id!!
		)
		if (key != null) {
			deleteField(key)
		}
	}

	/**
	 * @param obj
	 * @return true if this and obj are equivalent
	 */
	override fun equals(obj: Any?): Boolean {
		if (obj !is ID3v1Tag) {
			return false
		}
		val `object` = obj
		if (firstAlbum != `object`.firstAlbum) {
			return false
		}
		if (firstArtist != `object`.firstArtist) {
			return false
		}
		if (firstComment != `object`.firstComment) {
			return false
		}
		if (genre != `object`.genre) {
			return false
		}
		return if (firstTitle != `object`.firstTitle) {
			false
		} else firstYear == `object`.firstYear && super.equals(obj)
	}

	/**
	 * @return an iterator to iterate through the fields of the tag
	 */
	override fun iterator(): Iterator<Any?>? {
		return ID3v1Iterator(this)
	}

	/**
	 * @param byteBuffer
	 * @throws TagNotFoundException
	 */
	@Throws(TagNotFoundException::class)
	override fun read(byteBuffer: ByteBuffer) {
		if (!seek(byteBuffer)) {
			throw TagNotFoundException("$loggingFilename:ID3v1 tag not found")
		}
		logger.finer(
			"$loggingFilename:Reading v1 tag"
		)
		//Do single file read of data to cut down on file reads
		val dataBuffer = ByteArray(TAG_LENGTH)
		byteBuffer.position(0)
		byteBuffer[dataBuffer, 0, TAG_LENGTH]
		firstTitle = String(
			dataBuffer,
			FIELD_TITLE_POS,
			FIELD_TITLE_LENGTH,
			StandardCharsets.ISO_8859_1
		).trim { it <= ' ' }
		var m = endofStringPattern.matcher(
			firstTitle
		)
		if (m.find()) {
			firstTitle = firstTitle!!.substring(0, m.start())
		}
		firstArtist = String(
			dataBuffer,
			FIELD_ARTIST_POS,
			FIELD_ARTIST_LENGTH,
			StandardCharsets.ISO_8859_1
		).trim { it <= ' ' }
		m = endofStringPattern.matcher(
			firstArtist
		)
		if (m.find()) {
			firstArtist = firstArtist!!.substring(0, m.start())
		}
		firstAlbum = String(
			dataBuffer,
			FIELD_ALBUM_POS,
			FIELD_ALBUM_LENGTH,
			StandardCharsets.ISO_8859_1
		).trim { it <= ' ' }
		m = endofStringPattern.matcher(
			firstAlbum
		)
		logger.finest(loggingFilename + ":" + "Orig Album is:" + firstComment + ":")
		if (m.find()) {
			firstAlbum = firstAlbum!!.substring(0, m.start())
			logger.finest(loggingFilename + ":" + "Album is:" + firstAlbum + ":")
		}
		firstYear = String(
			dataBuffer,
			FIELD_YEAR_POS,
			FIELD_YEAR_LENGTH,
			StandardCharsets.ISO_8859_1
		).trim { it <= ' ' }
		m = endofStringPattern.matcher(
			firstYear
		)
		if (m.find()) {
			firstYear = firstYear!!.substring(0, m.start())
		}
		firstComment = String(
			dataBuffer,
			FIELD_COMMENT_POS,
			FIELD_COMMENT_LENGTH,
			StandardCharsets.ISO_8859_1
		).trim { it <= ' ' }
		m = endofStringPattern.matcher(
			firstComment
		)
		logger.finest(loggingFilename + ":" + "Orig Comment is:" + firstComment + ":")
		if (m.find()) {
			firstComment = firstComment!!.substring(0, m.start())
			logger.finest(loggingFilename + ":" + "Comment is:" + firstComment + ":")
		}
		genre = dataBuffer[FIELD_GENRE_POS]
	}

	/**
	 * Does a tag of this version exist within the byteBuffer
	 *
	 * @return whether tag exists within the byteBuffer
	 */
	override fun seek(byteBuffer: ByteBuffer?): Boolean {
		val buffer = ByteArray(FIELD_TAGID_LENGTH)
		// read the TAG value
		byteBuffer!![buffer, 0, FIELD_TAGID_LENGTH]
		return Arrays.equals(buffer, TAG_ID)
	}

	/**
	 * Write this tag to the file, replacing any tag previously existing
	 *
	 * @param file
	 * @throws IOException
	 */
	@Throws(IOException::class)
	override fun write(file: RandomAccessFile?) {
		logger.config("Saving ID3v1 tag to file")
		val buffer = ByteArray(TAG_LENGTH)
		var i: Int
		var str: String?
		delete(file)
		file!!.seek(file.length())
		//Copy the TAGID into new buffer
		System.arraycopy(TAG_ID, FIELD_TAGID_POS, buffer, FIELD_TAGID_POS, TAG_ID.size)
		var offset = FIELD_TITLE_POS
		if (TagOptionSingleton.instance.isId3v1SaveTitle) {
			str = truncate(
				firstTitle, FIELD_TITLE_LENGTH
			)
			i = 0
			while (i < str!!.length) {
				buffer[i + offset] = str[i].code.toByte()
				i++
			}
		}
		offset = FIELD_ARTIST_POS
		if (TagOptionSingleton.instance.isId3v1SaveArtist) {
			str = truncate(
				firstArtist, FIELD_ARTIST_LENGTH
			)
			i = 0
			while (i < str!!.length) {
				buffer[i + offset] = str[i].code.toByte()
				i++
			}
		}
		offset = FIELD_ALBUM_POS
		if (TagOptionSingleton.instance.isId3v1SaveAlbum) {
			str = truncate(
				firstAlbum, FIELD_ALBUM_LENGTH
			)
			i = 0
			while (i < str!!.length) {
				buffer[i + offset] = str[i].code.toByte()
				i++
			}
		}
		offset = FIELD_YEAR_POS
		if (TagOptionSingleton.instance.isId3v1SaveYear) {
			str = truncate(
				firstYear, FIELD_YEAR_LENGTH
			)
			i = 0
			while (i < str!!.length) {
				buffer[i + offset] = str[i].code.toByte()
				i++
			}
		}
		offset = FIELD_COMMENT_POS
		if (TagOptionSingleton.instance.isId3v1SaveComment) {
			str = truncate(
				firstComment, FIELD_COMMENT_LENGTH
			)
			i = 0
			while (i < str!!.length) {
				buffer[i + offset] = str[i].code.toByte()
				i++
			}
		}
		offset = FIELD_GENRE_POS
		if (TagOptionSingleton.instance.isId3v1SaveGenre) {
			buffer[offset] = genre
		}
		file.write(buffer)
		logger.config("Saved ID3v1 tag to file")
	}

	/**
	 * Create structured representation of this item.
	 */
	open fun createStructure() {
		MP3File.structureFormatter?.openHeadingElement(TYPE_TAG, identifier.toString())
		//Header
		MP3File.structureFormatter?.addElement(TYPE_TITLE, firstTitle)
		MP3File.structureFormatter?.addElement(TYPE_ARTIST, firstArtist)
		MP3File.structureFormatter?.addElement(TYPE_ALBUM, firstAlbum)
		MP3File.structureFormatter?.addElement(TYPE_YEAR, firstYear)
		MP3File.structureFormatter?.addElement(TYPE_COMMENT, firstComment)
		MP3File.structureFormatter?.addElement(TYPE_GENRE, genre.toInt())
		MP3File.structureFormatter?.closeHeadingElement(TYPE_TAG)
	}

	override val artworkList: List<Artwork>
		get() = emptyList()
	override val firstArtwork: Artwork?
		get() = null

	@Throws(FieldDataInvalidException::class)
	override fun createField(artwork: Artwork?): TagField? {
		throw UnsupportedOperationException(ErrorMessage.GENERIC_NOT_SUPPORTED.msg)
	}

	@Throws(FieldDataInvalidException::class)
	override fun setField(artwork: Artwork?) {
		throw UnsupportedOperationException(ErrorMessage.GENERIC_NOT_SUPPORTED.msg)
	}

	@Throws(FieldDataInvalidException::class)
	override fun addField(artwork: Artwork?) {
		throw UnsupportedOperationException(ErrorMessage.GENERIC_NOT_SUPPORTED.msg)
	}

	/**
	 * Delete all instance of artwork Field
	 *
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	override fun deleteArtworkField() {
		throw UnsupportedOperationException(ErrorMessage.GENERIC_NOT_SUPPORTED.msg)
	}

	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun createCompilationField(value: Boolean): TagField? {
		throw UnsupportedOperationException(ErrorMessage.GENERIC_NOT_SUPPORTED.msg)
	}

	companion object {
		var tagFieldToID3v1Field = EnumMap<FieldKey, ID3v1FieldKey>(
			FieldKey::class.java
		)

		init {
			tagFieldToID3v1Field[FieldKey.ARTIST] =
				ID3v1FieldKey.ARTIST
			tagFieldToID3v1Field[FieldKey.ALBUM] =
				ID3v1FieldKey.ALBUM
			tagFieldToID3v1Field[FieldKey.TITLE] = ID3v1FieldKey.TITLE
			tagFieldToID3v1Field[FieldKey.TRACK] =
				ID3v1FieldKey.TRACK
			tagFieldToID3v1Field[FieldKey.YEAR] =
				ID3v1FieldKey.YEAR
			tagFieldToID3v1Field[FieldKey.GENRE] =
				ID3v1FieldKey.GENRE
			tagFieldToID3v1Field[FieldKey.COMMENT] =
				ID3v1FieldKey.COMMENT
		}

		//For writing output
		const val BYTE_TO_UNSIGNED = 0xff

		@JvmStatic
		protected val TYPE_COMMENT = "comment"

		@JvmStatic
		protected val FIELD_COMMENT_LENGTH = 30

		@JvmStatic
		protected val FIELD_COMMENT_POS = 97

		@JvmStatic
		protected val GENRE_UNDEFINED = 0xff

		/**
		 * Retrieve the Release
		 */
		val release: Byte = 1

		/**
		 * Retrieve the Major Version
		 */
		val majorVersion: Byte = 0

		/**
		 * Retrieve the Revision
		 */
		val revision: Byte = 0
	}

}
