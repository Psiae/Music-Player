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
 * This class is for a ID3v1.1 Tag
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp3.MP3File
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.findNumber
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.truncate
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.GenreTypes
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.logging.Level

/**
 * Represents an ID3v11 tag.
 *
 * @author : Eric Farng
 * @author : Paul Taylor
 */
class ID3v11Tag : ID3v1Tag {
	/**
	 * Track is held as a single byte in v1.1
	 */
	private var mTrack: Byte = TRACK_UNDEFINED.toByte()

	val track: Byte
		get() = mTrack

	/**
	 * Creates a new ID3v11 datatype.
	 */
	constructor()

	override val fieldCount: Int
		get() = 7

	constructor(copyObject: ID3v11Tag) : super(copyObject) {
		mTrack = copyObject.mTrack
	}

	/**
	 * Creates a new ID3v11 datatype from a non v11 tag
	 *
	 * @param mp3tag
	 * @throws UnsupportedOperationException
	 */
	constructor(mp3tag: AbstractTag?) {
		if (mp3tag != null) {
			if (mp3tag is ID3v1Tag) {
				if (mp3tag is ID3v11Tag) {
					throw UnsupportedOperationException("Copy Constructor not called. Please type cast the argument")
				}
				// id3v1_1 objects are also id3v1 objects
				val id3old = mp3tag
				setTitle(id3old.firstTitle.toString())
				setArtist(id3old.firstArtist.toString())
				setAlbum(id3old.firstAlbum.toString())
				setComment(id3old.firstComment.toString())
				setYear(id3old.firstYear)
				genre = id3old.genre
			} else {
				// first change the tag to ID3v2_4 tag if not one already
				val id3tag: ID3v24Tag =
					if (mp3tag !is ID3v24Tag) {
						ID3v24Tag(mp3tag)
					} else {
						mp3tag
					}
				var frame: ID3v24Frame?
				var text: String?
				if (id3tag.hasFrame(ID3v24Frames.FRAME_ID_TITLE)) {
					val fields = id3tag.getFrame(ID3v24Frames.FRAME_ID_TITLE)
					frame = fields[0] as ID3v24Frame?
					text = (frame!!.body as FrameBodyTIT2?)!!.text
					setTitle(truncate(text, FIELD_TITLE_LENGTH).toString())
				}
				if (id3tag.hasFrame(ID3v24Frames.FRAME_ID_ARTIST)) {
					val fields = id3tag.getFrame(ID3v24Frames.FRAME_ID_ARTIST)
					frame = fields[0] as ID3v24Frame?
					text = (frame!!.body as FrameBodyTPE1?)!!.text
					setArtist(truncate(text, FIELD_ARTIST_LENGTH).toString())
				}
				if (id3tag.hasFrame(ID3v24Frames.FRAME_ID_ALBUM)) {
					val fields = id3tag.getFrame(ID3v24Frames.FRAME_ID_ALBUM)
					frame = fields[0] as ID3v24Frame?
					text = (frame!!.body as FrameBodyTALB?)!!.text
					setArtist(truncate(text, FIELD_ALBUM_LENGTH).toString())
				}
				if (id3tag.hasFrame(ID3v24Frames.FRAME_ID_YEAR)) {
					val fields = id3tag.getFrame(ID3v24Frames.FRAME_ID_YEAR)
					frame = fields[0] as ID3v24Frame?
					text = (frame!!.body as FrameBodyTDRC?)!!.text
					setYear(truncate(text, FIELD_YEAR_LENGTH).toString())
				}
				if (id3tag.hasFrame(ID3v24Frames.FRAME_ID_COMMENT)) {
					val iterator = id3tag.getFrameOfType(ID3v24Frames.FRAME_ID_COMMENT)
					text = ""
					while (iterator.hasNext()) {
						frame = iterator.next() as ID3v24Frame
						text += (frame.body as FrameBodyCOMM?)!!.text + " "
					}
					setComment(truncate(text, FIELD_COMMENT_LENGTH).toString())
				}
				if (id3tag.hasFrame(ID3v24Frames.FRAME_ID_GENRE)) {
					val fields = id3tag.getFrame(ID3v24Frames.FRAME_ID_GENRE)
					frame = fields[0] as ID3v24Frame?
					text = (frame!!.body as FrameBodyTCON?)!!.text
					genre = try {
						findNumber(text).toByte()
					} catch (ex: TagException) {
						val genreId = GenreTypes.instanceOf.getIdForValue(text.toString())
						if (null != genreId) genreId.toByte() else {
							logger.log(
								Level.WARNING,
								"$loggingFilename:Unable to convert TCON frame to format suitable for v11 tag",
								ex
							)
							ID3v1Tag.Companion.GENRE_UNDEFINED.toByte()
						}
					}
				}
				if (id3tag.hasFrame(ID3v24Frames.FRAME_ID_TRACK)) {
					val fields = id3tag.getFrame(ID3v24Frames.FRAME_ID_TRACK)
					frame = fields[0] as ID3v24Frame?
					mTrack = (frame!!.body as FrameBodyTRCK?)!!.trackNo.toInt().toByte()
				}
			}
		}
	}

	/**
	 * Creates a new ID3v11 datatype.
	 *
	 * @param file
	 * @param loggingFilename
	 * @throws TagNotFoundException
	 * @throws IOException
	 */
	constructor(file: RandomAccessFile, loggingFilename: String) {
		this.loggingFilename = loggingFilename
		val fc: FileChannel
		val byteBuffer = ByteBuffer.allocate(TAG_LENGTH)
		if (file.length() < TAG_LENGTH) {
			throw IOException("File not large enough to contain a tag")
		}
		fc = file.channel
		fc.position(file.length() - TAG_LENGTH)
		fc.read(byteBuffer)
		byteBuffer.flip()
		read(byteBuffer)
	}

	constructor(fc: FileChannel) {
		val byteBuffer = ByteBuffer.allocate(TAG_LENGTH)
		if (fc.size() < TAG_LENGTH) {
			throw IOException("File not large enough to contain a tag")
		}
		fc.position(fc.position() - TAG_LENGTH)
		fc.read(byteBuffer)
		byteBuffer.flip()
		read(byteBuffer)
	}

	/**
	 * Creates a new ID3v11 datatype.
	 *
	 * @param file
	 * @throws TagNotFoundException
	 * @throws IOException
	 */
	@Deprecated("use {@link #ID3v11Tag(RandomAccessFile,String)} instead")
	constructor(file: RandomAccessFile) : this(file, "")

	/**
	 * Set Comment
	 *
	 * @param comment
	 */
	override fun setComment(comment: String?) {
		requireNotNull(comment) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
		firstComment = truncate(comment, FIELD_COMMENT_LENGTH)
	}

	/**
	 * Get Comment
	 *
	 * @return comment
	 */
	override var firstComment: String? = super.firstComment

	/**
	 * Set the track, v11 stores track numbers in a single byte value so can only
	 * handle a simple number in the range 0-255.
	 *
	 * @param trackValue
	 */
	fun setTrack(trackValue: String) {
		val trackAsInt: Int
		//Try and convert String representation of track into an integer
		trackAsInt = try {
			trackValue.toInt()
		} catch (e: NumberFormatException) {
			0
		}

		//This value cannot be held in v1_1
		if (trackAsInt > TRACK_MAX_VALUE || trackAsInt < TRACK_MIN_VALUE) {
			mTrack = TRACK_UNDEFINED.toByte()
		} else {
			mTrack = trackValue.toInt().toByte()
		}
	}

	/**
	 * Return the track number as a String.
	 *
	 * @return track
	 */
	override val firstTrack: String
		get() = (mTrack.toInt() and ID3v1Tag.Companion.BYTE_TO_UNSIGNED).toString()

	fun addTrack(track: String) {
		setTrack(track)
	}

	override val trackList: List<TagField?>
		get() {
			return if (!getFirst(FieldKey.TRACK).isNullOrEmpty()) {
				val field = ID3v1TagField(
					ID3v1FieldKey.TRACK.name,
					getFirst(FieldKey.TRACK)
				)
				returnFieldToList(field)
			} else {
				ArrayList()
			}
		}

	override fun setField(field: TagField?) {
		val genericKey = FieldKey.valueOf(
			field!!.id!!
		)
		if (genericKey === FieldKey.TRACK) {
			setTrack(field.toDescriptiveString())
		} else {
			super.setField(field)
		}
	}

	override fun getFields(genericKey: FieldKey?): List<TagField?>? {
		return if (genericKey === FieldKey.TRACK) {
			trackList
		} else {
			super.getFields(genericKey)
		}
	}

	override fun getFirst(genericKey: FieldKey?): String? {
		return when (genericKey) {
			FieldKey.ARTIST -> firstArtist
			FieldKey.ALBUM -> firstAlbum
			FieldKey.TITLE -> firstTitle
			FieldKey.GENRE -> firstGenre
			FieldKey.YEAR -> firstYear
			FieldKey.TRACK -> firstTrack
			FieldKey.COMMENT -> firstComment
			else -> ""
		}
	}

	override fun getFirstField(id: String?): TagField? {
		val results: List<TagField?>?
		return if (FieldKey.TRACK.name == id) {
			results = trackList
			if (results != null) {
				if (results.isNotEmpty()) {
					return results[0]
				}
			}
			null
		} else {
			super.getFirstField(id)
		}
	}

	override val isEmpty: Boolean
		get() = mTrack <= 0 && super.isEmpty

	/**
	 * Delete any instance of tag fields with this key
	 *
	 * @param genericKey
	 */
	override fun deleteField(genericKey: FieldKey?) {
		if (genericKey === FieldKey.TRACK) {
			mTrack = 0
		} else {
			super.deleteField(genericKey)
		}
	}

	/**
	 * Compares Object with this only returns true if both v1_1 tags with all
	 * fields set to same value
	 *
	 * @param obj Comparing Object
	 * @return
	 */
	override fun equals(obj: Any?): Boolean {
		if (obj !is ID3v11Tag) {
			return false
		}
		return mTrack == obj.mTrack && super.equals(obj)
	}

	/**
	 * Find identifier within byteBuffer to indicate that a v11 tag exists within the buffer
	 *
	 * @param byteBuffer
	 * @return true if find header for v11 tag within buffer
	 */
	override fun seek(byteBuffer: ByteBuffer?): Boolean {
		val buffer = ByteArray(FIELD_TAGID_LENGTH)
		// read the TAG value
		byteBuffer!![buffer, 0, FIELD_TAGID_LENGTH]
		if (!Arrays.equals(buffer, TAG_ID)) {
			return false
		}

		// Check for the empty byte before the TRACK
		byteBuffer.position(FIELD_TRACK_INDICATOR_POS)
		return if (byteBuffer.get() != END_OF_FIELD) {
			false
		} else byteBuffer.get() != END_OF_FIELD
		//Now check for TRACK if the next byte is also null byte then not v1.1
		//tag, however this means cannot have v1_1 tag with track setField to zero/undefined
		//because on next read will be v1 tag.
	}

	/**
	 * Read in a tag from the ByteBuffer
	 *
	 * @param byteBuffer from where to read in a tag
	 * @throws TagNotFoundException if unable to read a tag in the byteBuffer
	 */
	@Throws(TagNotFoundException::class)
	override fun read(byteBuffer: ByteBuffer) {
		if (!seek(byteBuffer)) {
			throw TagNotFoundException("ID3v1 tag not found")
		}
		logger.finer("Reading v1.1 tag")

		//Do single file read of data to cut down on file reads
		val dataBuffer = ByteArray(TAG_LENGTH)
		byteBuffer.position(0)
		byteBuffer[dataBuffer, 0, TAG_LENGTH]
		setTitle(String(
			dataBuffer,
			FIELD_TITLE_POS,
			FIELD_TITLE_LENGTH,
			StandardCharsets.ISO_8859_1
		).trim { it <= ' ' })

		var m = endofStringPattern.matcher(firstTitle.toString())
		if (m.find()) {
			setTitle(firstTitle!!.substring(0, m.start()))
		}
		setArtist(String(
			dataBuffer,
			FIELD_ARTIST_POS,
			FIELD_ARTIST_LENGTH,
			StandardCharsets.ISO_8859_1
		).trim { it <= ' ' })

		m = endofStringPattern.matcher(firstArtist.toString())
		if (m.find()) {
			setArtist(firstArtist!!.substring(0, m.start()))
		}

		setAlbum(String(
			dataBuffer,
			FIELD_ALBUM_POS,
			FIELD_ALBUM_LENGTH,
			StandardCharsets.ISO_8859_1
		).trim { it <= ' ' })

		m = endofStringPattern.matcher(firstAlbum.toString())
		if (m.find()) {
			setAlbum(firstAlbum!!.substring(0, m.start()))
		}

		setYear(String(
			dataBuffer,
			FIELD_YEAR_POS,
			FIELD_YEAR_LENGTH,
			StandardCharsets.ISO_8859_1
		).trim { it <= ' ' })

		m = endofStringPattern.matcher(firstYear.toString())
		if (m.find()) {
			setYear(firstYear!!.substring(0, m.start()))
		}

		setComment(String(
			dataBuffer,
			FIELD_COMMENT_POS,
			FIELD_COMMENT_LENGTH,
			StandardCharsets.ISO_8859_1
		).trim { it <= ' ' })

		m = endofStringPattern.matcher(firstComment.toString())
		if (m.find()) {
			setComment(firstComment!!.substring(0, m.start()))
		}
		mTrack = dataBuffer[FIELD_TRACK_POS]
		genre = dataBuffer[FIELD_GENRE_POS]
	}

	/**
	 * Write this representation of tag to the file indicated
	 *
	 * @param file that this tag should be written to
	 * @throws IOException thrown if there were problems writing to the file
	 */
	@Throws(IOException::class)
	override fun write(file: RandomAccessFile?) {
		logger.config("Saving ID3v11 tag to file")
		val buffer = ByteArray(TAG_LENGTH)
		var i: Int
		var str: String?
		delete(file)
		file!!.seek(file.length())
		System.arraycopy(TAG_ID, FIELD_TAGID_POS, buffer, FIELD_TAGID_POS, TAG_ID.size)
		var offset = FIELD_TITLE_POS
		if (TagOptionSingleton.instance.isId3v1SaveTitle) {
			str = truncate(firstTitle, FIELD_TITLE_LENGTH)
			i = 0
			while (i < str!!.length) {
				buffer[i + offset] = str[i].code.toByte()
				i++
			}
		}
		offset = FIELD_ARTIST_POS
		if (TagOptionSingleton.instance.isId3v1SaveArtist) {
			str = truncate(firstArtist, FIELD_ARTIST_LENGTH)
			i = 0
			while (i < str!!.length) {
				buffer[i + offset] = str[i].code.toByte()
				i++
			}
		}
		offset = FIELD_ALBUM_POS
		if (TagOptionSingleton.instance.isId3v1SaveAlbum) {
			str = truncate(firstAlbum, FIELD_ALBUM_LENGTH)
			i = 0
			while (i < str!!.length) {
				buffer[i + offset] = str[i].code.toByte()
				i++
			}
		}
		offset = FIELD_YEAR_POS
		if (TagOptionSingleton.instance.isId3v1SaveYear) {
			str = truncate(firstYear, FIELD_YEAR_LENGTH)
			i = 0
			while (i < str!!.length) {
				buffer[i + offset] = str[i].code.toByte()
				i++
			}
		}
		offset = FIELD_COMMENT_POS
		if (TagOptionSingleton.instance.isId3v1SaveComment) {
			str = truncate(firstComment, FIELD_COMMENT_LENGTH)
			i = 0
			while (i < str!!.length) {
				buffer[i + offset] = str[i].code.toByte()
				i++
			}
		}
		offset = FIELD_TRACK_POS
		buffer[offset] = mTrack // skip one byte extra blank for 1.1 definition
		offset = FIELD_GENRE_POS
		if (TagOptionSingleton.instance.isId3v1SaveGenre) {
			buffer[offset] = genre
		}
		file.write(buffer)
		logger.config("Saved ID3v11 tag to file")
	}

	override fun createStructure() {
		MP3File.structureFormatter?.openHeadingElement(TYPE_TAG, identifier.toString())
		//Header
		MP3File.structureFormatter?.addElement(TYPE_TITLE, firstArtist)
		MP3File.structureFormatter?.addElement(TYPE_ARTIST, firstArtist)
		MP3File.structureFormatter?.addElement(TYPE_ALBUM, firstAlbum)
		MP3File.structureFormatter?.addElement(TYPE_YEAR, firstYear)
		MP3File.structureFormatter?.addElement(TYPE_COMMENT, firstComment)
		MP3File.structureFormatter?.addElement(TYPE_TRACK, mTrack.toInt())
		MP3File.structureFormatter?.addElement(TYPE_GENRE, genre.toInt())
		MP3File.structureFormatter?.closeHeadingElement(TYPE_TAG)
	}

	companion object {
		//For writing output
		protected const val TYPE_TRACK = "track"
		protected const val TRACK_UNDEFINED = 0
		protected const val TRACK_MAX_VALUE = 255
		protected const val TRACK_MIN_VALUE = 1
		protected const val FIELD_COMMENT_LENGTH = 28
		protected const val FIELD_COMMENT_POS = 97
		protected const val FIELD_TRACK_INDICATOR_LENGTH = 1
		protected const val FIELD_TRACK_INDICATOR_POS = 125
		protected const val FIELD_TRACK_LENGTH = 1
		protected const val FIELD_TRACK_POS = 126

		/**
		 * Retrieve the Release
		 */
		val release: Byte = 1

		/**
		 * Retrieve the Major Version
		 */
		val majorVersion: Byte = 1

		/**
		 * Retrieve the Revision
		 */
		val revision: Byte = 0
	}
}
