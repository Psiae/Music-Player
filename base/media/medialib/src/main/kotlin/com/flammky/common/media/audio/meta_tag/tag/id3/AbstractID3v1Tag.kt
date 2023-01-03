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
 * Abstract superclass of all URL Frames
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3

import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import java.util.logging.Logger
import java.util.regex.Pattern

/**
 * This is the abstract base class for all ID3v1 tags.
 *
 * @author : Eric Farng
 * @author : Paul Taylor
 */
abstract class AbstractID3v1Tag : AbstractID3Tag {
	constructor()
	constructor(copyObject: AbstractID3v1Tag?) : super(copyObject)

	override val size: Int
		/**
		 * Return the size of this tag, the size is fixed for tags of this type
		 *
		 * @return size of this tag in bytes
		 */
		get() = TAG_LENGTH

	/**
	 * Delete tag from file
	 * Looks for tag and if found lops it off the file.
	 *
	 * @param file to delete the tag from
	 * @throws IOException if there was a problem accessing the file
	 */
	@Throws(IOException::class)
	override fun delete(file: RandomAccessFile?) {
		if (file == null) return

		//Read into Byte Buffer
		logger.config("Deleting ID3v1 from file if exists")
		val fc: FileChannel
		val byteBuffer: ByteBuffer
		fc = file.channel
		if (file.length() < TAG_LENGTH) {
			throw IOException("File not large enough to contain a tag")
		}
		fc.position(file.length() - TAG_LENGTH)
		byteBuffer = ByteBuffer.allocate(TAG_LENGTH)
		fc.read(byteBuffer)
		byteBuffer.rewind()
		if (seekForV1OrV11Tag(byteBuffer)) {
			try {
				logger.config("Deleted ID3v1 tag")
				file.setLength(file.length() - TAG_LENGTH)
			} catch (ex: IOException) {
				logger.severe("Unable to delete existing ID3v1 Tag:" + ex.message)
			}
		} else {
			logger.config("Unable to find ID3v1 tag to deleteField")
		}
	}

	companion object {
		//Logger
		@JvmField
		var logger = Logger.getLogger("org.jaudiotagger.tag.id3")

		//If field is less than maximum field length this is how it is terminated
		@JvmStatic
		protected val END_OF_FIELD = 0.toByte()

		//Used to detect end of field in String constructed from Data
		@JvmStatic
		protected var endofStringPattern = Pattern.compile("\\x00")

		//Tag ID as held in file
		const val TAG = "TAG"

		@JvmStatic
		protected val TAG_ID = byteArrayOf('T'.code.toByte(), 'A'.code.toByte(), 'G'.code.toByte())

		//Fields Lengths common to v1 and v1.1 tags

		@JvmStatic
		protected val TAG_LENGTH = 128

		@JvmStatic
		protected val TAG_DATA_LENGTH = 125

		@JvmStatic
		protected val FIELD_TAGID_LENGTH = 3

		@JvmStatic
		protected val FIELD_TITLE_LENGTH = 30

		@JvmStatic
		protected val FIELD_ARTIST_LENGTH = 30

		@JvmStatic
		protected val FIELD_ALBUM_LENGTH = 30

		@JvmStatic
		protected val FIELD_YEAR_LENGTH = 4

		@JvmStatic
		protected val FIELD_GENRE_LENGTH = 1

		//Field Positions, starting from zero so fits in with Java Terminology

		@JvmStatic
		protected val FIELD_TAGID_POS = 0

		@JvmStatic
		protected val FIELD_TITLE_POS = 3

		@JvmStatic
		protected val FIELD_ARTIST_POS = 33

		@JvmStatic
		protected val FIELD_ALBUM_POS = 63

		@JvmStatic
		protected val FIELD_YEAR_POS = 93

		@JvmStatic
		protected val FIELD_GENRE_POS = 127

		//For writing output
		@JvmStatic
		protected val TYPE_TITLE = "title"

		@JvmStatic
		protected val TYPE_ARTIST = "artist"

		@JvmStatic
		protected val TYPE_ALBUM = "album"

		@JvmStatic
		protected val TYPE_YEAR = "year"

		@JvmStatic
		protected val TYPE_GENRE = "genre"

		/**
		 * Does a v1tag or a v11tag exist
		 *
		 * @return whether tag exists within the byteBuffer
		 */
		fun seekForV1OrV11Tag(byteBuffer: ByteBuffer): Boolean {
			val buffer = ByteArray(FIELD_TAGID_LENGTH)
			// read the TAG value
			byteBuffer[buffer, 0, FIELD_TAGID_LENGTH]
			return Arrays.equals(buffer, TAG_ID)
		}
	}
}
