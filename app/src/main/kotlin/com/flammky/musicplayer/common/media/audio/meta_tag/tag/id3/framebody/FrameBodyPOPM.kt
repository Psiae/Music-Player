/*
 *  MusicTag Copyright (C)2003,2004
 *
 *  This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 *  General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 *  or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with this library; if not,
 *  you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.DataTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.NumberFixedLength
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.NumberVariableLength
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.StringNullTerminated
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Frames
import java.nio.ByteBuffer

/**
 * Popularimeter frame.
 *
 *
 * The purpose of this frame is to specify how good an audio file is.
 * Many interesting applications could be found to this frame such as a
 * playlist that features better audiofiles more often than others or it
 * could be used to profile a person's taste and find other 'good' files
 * by comparing people's profiles. The frame is very simple. It contains
 * the email address to the user, one rating byte and a four byte play
 * counter, intended to be increased with one for every time the file is
 * played. The email is a terminated string. The rating is 1-255 where
 * 1 is worst and 255 is best. 0 is unknown. If no personal counter is
 * wanted it may be omitted. When the counter reaches all one's, one
 * byte is inserted in front of the counter thus making the counter
 * eight bits bigger in the same away as the play counter ("PCNT").
 * There may be more than one "POPM" frame in each tag, but only one
 * with the same email address.
 *
 *
 * For more details, please refer to the ID3 specifications:
 *
 *  * [ID3 v2.3.0 Spec](http://www.id3.org/id3v2.3.0.txt)
 *
 *
 * @author : Paul Taylor
 * @author : Eric Farng
 * @version $Id$
 * TODO : Counter should be optional, whereas we always expect it although allow a size of zero
 * needs testing.
 */
class FrameBodyPOPM : AbstractID3v2FrameBody, ID3v24FrameBody, ID3v23FrameBody {

	override val identifier: String?
		get() = ID3v24Frames.FRAME_ID_POPULARIMETER

	/**
	 * Creates a new FrameBodyPOPM datatype.
	 */
	constructor() {
		setObjectValue(DataTypes.OBJ_EMAIL, "")
		setObjectValue(DataTypes.OBJ_RATING, 0L)
		setObjectValue(DataTypes.OBJ_COUNTER, 0L)
	}

	constructor(body: FrameBodyPOPM) : super(body)

	/**
	 * Creates a new FrameBodyPOPM datatype.
	 *
	 * @param emailToUser
	 * @param rating
	 * @param counter
	 */
	constructor(emailToUser: String?, rating: Long, counter: Long) {
		setObjectValue(DataTypes.OBJ_EMAIL, emailToUser)
		setObjectValue(DataTypes.OBJ_RATING, rating)
		setObjectValue(DataTypes.OBJ_COUNTER, counter)
	}

	/**
	 * Creates a new FrameBodyPOPM datatype.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException if unable to create framebody from buffer
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)
	/**
	 * @return the memail of the user who rated this
	 */
	/**
	 * @param description
	 */
	var emailToUser: String?
		get() = getObjectValue(DataTypes.OBJ_EMAIL) as String
		set(description) {
			setObjectValue(DataTypes.OBJ_EMAIL, description)
		}
	/**
	 * @return the rating given to this file
	 */
	/**
	 * Set the rating given to this file
	 *
	 * @param rating
	 */
	var rating: Long
		get() = (getObjectValue(DataTypes.OBJ_RATING) as Number).toLong()
		set(rating) {
			setObjectValue(DataTypes.OBJ_RATING, rating)
		}
	/**
	 * @return the play count of this file
	 */
	/**
	 * Set the play counter of this file
	 *
	 * @param counter
	 */
	var counter: Long
		get() = (getObjectValue(DataTypes.OBJ_COUNTER) as Number).toLong()
		set(counter) {
			setObjectValue(DataTypes.OBJ_COUNTER, counter)
		}
	override val userFriendlyValue: String
		get() = emailToUser + ":" + rating + ":" + counter

	fun parseString(data: String) {
		try {
			val value = data.toInt()
			rating = value.toLong()
			emailToUser = MEDIA_MONKEY_NO_EMAIL
		} catch (nfe: NumberFormatException) {
		}
	}

	/**
	 *
	 */
	override fun setupObjectList() {
		objectList.add(StringNullTerminated(DataTypes.OBJ_EMAIL, this))
		objectList.add(NumberFixedLength(DataTypes.OBJ_RATING, this, RATING_FIELD_SIZE))
		objectList.add(
			NumberVariableLength(
				DataTypes.OBJ_COUNTER,
				this,
				COUNTER_MINIMUM_FIELD_SIZE
			)
		)
	}

	companion object {
		private const val RATING_FIELD_SIZE = 1
		private const val COUNTER_MINIMUM_FIELD_SIZE = 0
		const val MEDIA_MONKEY_NO_EMAIL = "no@email"
	}
}
