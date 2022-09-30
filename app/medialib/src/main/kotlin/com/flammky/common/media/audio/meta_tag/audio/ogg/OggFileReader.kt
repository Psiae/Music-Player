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

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.AudioFileReader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.GenericAudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.ogg.util.OggInfoReader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.ogg.util.OggPageHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.ogg.util.OggPageHeader.Companion.read
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.logging.Logger

/**
 * Read Ogg File Tag and Encoding information
 *
 * Only implemented for ogg files containing a vorbis stream with vorbis comments
 */
class OggFileReader : AudioFileReader() {
	private val ir: OggInfoReader
	private val vtr: OggVorbisTagReader

	init {
		ir = OggInfoReader()
		vtr = OggVorbisTagReader()
	}

	@Throws(CannotReadException::class, IOException::class)
	override fun getEncodingInfo(raf: RandomAccessFile): GenericAudioHeader {
		return ir.read(raf)
	}

	@Throws(CannotReadException::class, IOException::class)
	override fun getTag(raf: RandomAccessFile): Tag {
		return vtr.read(raf)
	}

	/**
	 * Return count Ogg Page header, count starts from zero
	 *
	 * count=0; should return PageHeader that contains Vorbis Identification Header
	 * count=1; should return Pageheader that contains VorbisComment and possibly SetupHeader
	 * count>=2; should return PageHeader containing remaining VorbisComment,SetupHeader and/or Audio
	 *
	 * @param raf
	 * @param count
	 * @return
	 * @throws CannotReadException
	 * @throws IOException
	 */
	@Throws(CannotReadException::class, IOException::class)
	fun readOggPageHeader(raf: RandomAccessFile, count: Int): OggPageHeader {
		var count = count
		var pageHeader = read(raf)
		while (count > 0) {
			raf.seek(raf.filePointer + pageHeader.getPageLength())
			pageHeader = read(raf)
			count--
		}
		return pageHeader
	}

	/**
	 * Summarize all the ogg headers in a file
	 *
	 * A useful utility function
	 *
	 * @param oggFile
	 * @throws CannotReadException
	 * @throws IOException
	 */
	@Throws(CannotReadException::class, IOException::class)
	fun summarizeOggPageHeaders(oggFile: File?) {
		val raf = RandomAccessFile(oggFile, "r")
		while (raf.filePointer < raf.length()) {
			println("pageHeader starts at absolute file position:" + raf.filePointer)
			val pageHeader = read(raf)
			println("pageHeader finishes at absolute file position:" + raf.filePointer)
			println(
				"""
    $pageHeader

    """.trimIndent()
			)
			raf.seek(raf.filePointer + pageHeader.getPageLength())
		}
		println("Raf File Pointer at:" + raf.filePointer + "File Size is:" + raf.length())
		raf.close()
	}

	/**
	 * Summarizes the first five pages, normally all we are interested in
	 *
	 * @param oggFile
	 * @throws CannotReadException
	 * @throws IOException
	 */
	@Throws(CannotReadException::class, IOException::class)
	fun shortSummarizeOggPageHeaders(oggFile: File?) {
		val raf = RandomAccessFile(oggFile, "r")
		var i = 0
		while (raf.filePointer < raf.length()) {
			println("pageHeader starts at absolute file position:" + raf.filePointer)
			val pageHeader = read(raf)
			println("pageHeader finishes at absolute file position:" + raf.filePointer)
			println(
				"""
    $pageHeader

    """.trimIndent()
			)
			raf.seek(raf.filePointer + pageHeader.getPageLength())
			i++
			if (i >= 5) {
				break
			}
		}
		println("Raf File Pointer at:" + raf.filePointer + "File Size is:" + raf.length())
		raf.close()
	}

	companion object {
		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.audio.ogg")
	}
}
