/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Raphaël Slinckx <raphael@slinckx.net>
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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.ogg

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.AudioFile
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotWriteException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.AudioFileWriter
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import java.io.IOException
import java.io.RandomAccessFile
import java.util.logging.Logger

/**
 * Write tag data to Ogg File
 *
 * Only works for Ogg files containing a vorbis stream
 */
class OggFileWriter : AudioFileWriter() {
	private val vtw = OggVorbisTagWriter()

	@Throws(CannotReadException::class, CannotWriteException::class, IOException::class)
	override fun writeTag(
		audioFile: AudioFile?,
		tag: Tag,
		raf: RandomAccessFile,
		rafTemp: RandomAccessFile
	) {
		vtw.write(tag, raf, rafTemp)
	}

	@Throws(CannotReadException::class, CannotWriteException::class, IOException::class)
	override fun deleteTag(tag: Tag?, raf: RandomAccessFile?, tempRaf: RandomAccessFile?) {
		vtw.delete(raf, tempRaf)
	}

	companion object {
		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.audio.ogg")
	}
}
