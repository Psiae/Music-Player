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
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.readFileDataIntoBufferLE
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.readFourBytesAsChars
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.IffHeaderChunk
import java.io.IOException
import java.nio.channels.FileChannel

/**
 * Processes the Wav Header
 *
 * This is simply the first 12 bytes of the file http://www-mmsp.ece.mcgill.ca/Documents/AudioFormats/WAVE/WAVE.html
 */
object WavRIFFHeader {
	const val RIFF_SIGNATURE = "RIFF"
	const val WAVE_SIGNATURE = "WAVE"

	@Throws(IOException::class, CannotReadException::class)
	fun isValidHeader(loggingName: String, fc: FileChannel): Boolean {
		if (fc.size() - fc.position() < IffHeaderChunk.FORM_HEADER_LENGTH) {
			throw CannotReadException(
				"$loggingName:This is not a WAV File (<12 bytes)"
			)
		}
		val headerBuffer = readFileDataIntoBufferLE(fc, IffHeaderChunk.FORM_HEADER_LENGTH)
		if (readFourBytesAsChars(headerBuffer) == RIFF_SIGNATURE) {
			IffHeaderChunk.logger.finer(loggingName + ":Header:File:Size:" + headerBuffer.int) //Size
			if (readFourBytesAsChars(headerBuffer) == WAVE_SIGNATURE) {
				return true
			}
		}
		return false
	}
}
