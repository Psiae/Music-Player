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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.AudioFileReader2
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.GenericAudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagOptionSingleton
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.Path

/**
 * Reads Audio and Metadata information contained in Wav file.
 */
class WavFileReader : AudioFileReader2() {
	@Throws(CannotReadException::class, IOException::class)
	override fun getEncodingInfo(file: Path): GenericAudioHeader {
		return WavInfoReader(file.toString()).read(file)
	}

	override fun getEncodingInfo(fc: FileChannel): GenericAudioHeader {
		return WavInfoReader(fc.toString()).read(fc)
	}

	@Throws(IOException::class, CannotReadException::class)
	override fun getTag(path: Path): Tag {
		val tag = WavTagReader(path.toString()).read(path)
		when (TagOptionSingleton.instance.wavOptions) {
			WavOptions.READ_ID3_ONLY_AND_SYNC,
			WavOptions.READ_ID3_UNLESS_ONLY_INFO_AND_SYNC,
			WavOptions.READ_INFO_ONLY_AND_SYNC,
			WavOptions.READ_INFO_UNLESS_ONLY_ID3_AND_SYNC -> tag.syncTagsAfterRead()
			else -> {}
		}
		return tag
	}

	override fun getTag(fc: FileChannel): Tag {
		val tag = WavTagReader(fc.toString()).read(fc)
		when (TagOptionSingleton.instance.wavOptions) {
			WavOptions.READ_ID3_ONLY_AND_SYNC,
			WavOptions.READ_ID3_UNLESS_ONLY_INFO_AND_SYNC,
			WavOptions.READ_INFO_ONLY_AND_SYNC,
			WavOptions.READ_INFO_UNLESS_ONLY_ID3_AND_SYNC -> tag.syncTagsAfterRead()
			else -> {}
		}
		return tag
	}
}
