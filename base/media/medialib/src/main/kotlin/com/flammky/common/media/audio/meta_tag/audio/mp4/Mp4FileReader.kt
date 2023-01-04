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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.AudioFileReader2
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.GenericAudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.Path

/**
 * Mp4 File Reader
 *
 *
 * This can read files containing either the .mp4 or .m4a suffixes
 */
class Mp4FileReader : AudioFileReader2() {
	private val ir = Mp4InfoReader()
	private val tr = Mp4TagReader()

	@Throws(CannotReadException::class, IOException::class)
	override fun getEncodingInfo(file: Path): GenericAudioHeader {
		return ir.read(file)
	}

	@Throws(IOException::class, CannotReadException::class)
	override fun getTag(path: Path): Tag {
		return tr.read(path)
	}

	override fun getEncodingInfo(fc: FileChannel): GenericAudioHeader {
		return ir.read(fc)
	}

	override fun getTag(fc: FileChannel): Tag? {
		return tr.read(fc)
	}
}
