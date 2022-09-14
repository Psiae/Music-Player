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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.flac

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotWriteException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.AudioFileWriter2
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import java.nio.file.Path

/**
 * Write/delete tag info for Flac file (opensource lossless encoding)
 */
class FlacFileWriter : AudioFileWriter2() {
	private val tw = FlacTagWriter()

	@Throws(CannotWriteException::class)
	override fun writeTag(tag: Tag, file: Path) {
		tw.write(tag, file)
	}

	@Throws(CannotWriteException::class)
	override fun deleteTag(tag: Tag, file: Path) {
		tw.delete(tag, file)
	}
}
