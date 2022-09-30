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
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.lyrics3

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidTagException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagOptionSingleton
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.AbstractID3v2Frame
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.AbstractTagFrame
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.AbstractFrameBodyTextInfo
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.FrameBodyCOMM
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.FrameBodySYLT
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.FrameBodyUSLT
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.util.*

class Lyrics3v2Field : AbstractTagFrame {
	/**
	 * Creates a new Lyrics3v2Field datatype.
	 */
	constructor()
	constructor(copyObject: Lyrics3v2Field?) : super(
		copyObject!!
	)

	/**
	 * Creates a new Lyrics3v2Field datatype.
	 *
	 * @param body
	 */
	constructor(body: AbstractLyrics3v2FieldFrameBody?) {
		frameBody = body
	}

	/**
	 * Creates a new Lyrics3v2Field datatype.
	 *
	 * @param frame
	 * @throws TagException
	 */
	constructor(frame: AbstractID3v2Frame) {
		val textFrame: AbstractFrameBodyTextInfo?
		val text: String
		val frameIdentifier = frame.identifier
		if (frameIdentifier.startsWith("USLT")) {
			frameBody = FieldFrameBodyLYR("")
			(frameBody as FieldFrameBodyLYR).addLyric(frame.body as FrameBodyUSLT?)
		} else if (frameIdentifier.startsWith("SYLT")) {
			frameBody = FieldFrameBodyLYR("")
			(frameBody as FieldFrameBodyLYR).addLyric(frame.body as FrameBodySYLT?)
		} else if (frameIdentifier.startsWith("COMM")) {
			text = (frame.body as FrameBodyCOMM?)?.text ?: ""
			frameBody = FieldFrameBodyINF(text)
		} else if (frameIdentifier == "TCOM") {
			textFrame = frame.body as AbstractFrameBodyTextInfo?
			frameBody = FieldFrameBodyAUT("")
			if (textFrame != null && textFrame.text!!.length > 0) {
				frameBody = FieldFrameBodyAUT(textFrame.text ?: "")
			}
		} else if (frameIdentifier == "TALB") {
			textFrame = frame.body as AbstractFrameBodyTextInfo?
			if (textFrame != null && textFrame.text!!.length > 0) {
				frameBody = FieldFrameBodyEAL(textFrame.text ?: "")
			}
		} else if (frameIdentifier == "TPE1") {
			textFrame = frame.body as AbstractFrameBodyTextInfo?
			if (textFrame != null && textFrame.text!!.length > 0) {
				frameBody = FieldFrameBodyEAR(textFrame.text ?: "")
			}
		} else if (frameIdentifier == "TIT2") {
			textFrame = frame.body as AbstractFrameBodyTextInfo?
			if (textFrame != null && textFrame.text!!.length > 0) {
				frameBody = FieldFrameBodyETT(textFrame.text ?: "")
			}
		} else {
			throw TagException("Cannot createField Lyrics3v2 field from given ID3v2 frame")
		}
	}

	/**
	 * Creates a new Lyrics3v2Field datatype.
	 *
	 * @throws InvalidTagException
	 * @param byteBuffer
	 */
	constructor(byteBuffer: ByteBuffer) {
		read(byteBuffer)
	}

	/**
	 * @return
	 */
	override val identifier: String
		get() = frameBody?.identifier ?: ""

	/**
	 * @return
	 */
	override val size: Int
		get() = frameBody!!.size + 5 + identifier.length

	/**
	 * @param byteBuffer
	 * @throws InvalidTagException
	 * @throws IOException
	 */
	@Throws(InvalidTagException::class)
	override fun read(byteBuffer: ByteBuffer) {
		val buffer = ByteArray(6)
		var b: Byte
		do {
			b = byteBuffer.get()
		} while (b.toInt() == 0)
		byteBuffer.position(byteBuffer.position() - 1)
		// read the 3 character ID
		byteBuffer[buffer, 0, 3]
		val identifier = String(buffer, 0, 3)
		// is this a valid identifier?
		if (!Lyrics3v2Fields.Companion.isLyrics3v2FieldIdentifier(identifier)) {
			throw InvalidTagException("$identifier is not a valid ID3v2.4 frame")
		}
		frameBody = readBody(identifier, byteBuffer)
	}

	/**
	 * @return
	 */
	override fun toString(): String {
		return if (frameBody == null) {
			""
		} else frameBody.toString()
	}

	/**
	 * @param file
	 * @throws IOException
	 */
	@Throws(IOException::class)
	fun write(file: RandomAccessFile?) {
		if (frameBody!!.size > 0 || TagOptionSingleton.instance.isLyrics3SaveEmptyField) {
			val buffer = ByteArray(3)
			val str = identifier
			for (i in 0 until str.length) {
				buffer[i] = str[i].code.toByte()
			}
			file!!.write(buffer, 0, str.length)
			//body.write(file);
		}
	}

	/**
	 * Read a Lyrics3 Field from a file.
	 *
	 * @param identifier
	 * @param byteBuffer
	 * @return
	 * @throws InvalidTagException
	 */
	@Throws(InvalidTagException::class)
	private fun readBody(
		identifier: String,
		byteBuffer: ByteBuffer
	): AbstractLyrics3v2FieldFrameBody {
		val newBody: AbstractLyrics3v2FieldFrameBody = when (identifier) {
			Lyrics3v2Fields.Companion.FIELD_V2_AUTHOR -> {
				FieldFrameBodyAUT(byteBuffer)
			}
			Lyrics3v2Fields.Companion.FIELD_V2_ALBUM -> {
				FieldFrameBodyEAL(byteBuffer)
			}
			Lyrics3v2Fields.Companion.FIELD_V2_ARTIST -> {
				FieldFrameBodyEAR(byteBuffer)
			}
			Lyrics3v2Fields.Companion.FIELD_V2_TRACK -> {
				FieldFrameBodyETT(byteBuffer)
			}
			Lyrics3v2Fields.Companion.FIELD_V2_IMAGE -> {
				FieldFrameBodyIMG(byteBuffer)
			}
			Lyrics3v2Fields.Companion.FIELD_V2_INDICATIONS -> {
				FieldFrameBodyIND(byteBuffer)
			}
			Lyrics3v2Fields.Companion.FIELD_V2_ADDITIONAL_MULTI_LINE_TEXT -> {
				FieldFrameBodyINF(byteBuffer)
			}
			Lyrics3v2Fields.Companion.FIELD_V2_LYRICS_MULTI_LINE_TEXT -> {
				FieldFrameBodyLYR(byteBuffer)
			}
			else -> {
				FieldFrameBodyUnsupported(byteBuffer)
			}
		}
		return newBody
	}

	companion object {
		protected var LYRICS_FRAME_IDS: Set<String>? = null

		init {
			val lyricsFrameIds: MutableSet<String> = HashSet()
			lyricsFrameIds.add("USLT")
			lyricsFrameIds.add("SYLT")
			lyricsFrameIds.add("COMM")
			lyricsFrameIds.add("TCOM")
			lyricsFrameIds.add("TALB")
			lyricsFrameIds.add("TPE1")
			lyricsFrameIds.add("TIT2")
			LYRICS_FRAME_IDS = Collections.unmodifiableSet(lyricsFrameIds)
		}

		fun isLyrics3v2Field(frame: AbstractID3v2Frame): Boolean {
			val frameIdentifier = frame.identifier
			return LYRICS_FRAME_IDS!!.contains(frameIdentifier)
		}
	}
}
