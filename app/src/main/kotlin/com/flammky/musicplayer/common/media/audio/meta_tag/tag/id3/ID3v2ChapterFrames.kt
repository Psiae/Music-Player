/*
 * Horizon Wimba Copyright (C)2006
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
 * you can getFields a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3

import java.util.*

/**
 * Defines ID3 Chapter frames and collections that categorise frames.
 *
 *
 * For more details, please refer to the ID3 Chapter Frame specifications:
 *
 *  * [ID3 v2 Chapter Frame Spec](http://www.id3.org/id3v2-chapters-1.0.txt)
 *
 *
 * @author Marc Gimpel, Horizon Wimba S.A.
 * @version $Id$
 */
class ID3v2ChapterFrames private constructor() : ID3Frames() {
	init {
		idToValue[FRAME_ID_CHAPTER] = "Chapter"
		idToValue[FRAME_ID_TABLE_OF_CONTENT] = "Table of content"
		createMaps()
		multipleFrames = TreeSet()
		discardIfFileAlteredFrames = TreeSet()
	}

	override fun setITunes12_6WorkGroupingMode(id3v2ITunes12_6Mode: Boolean) {
		throw UnsupportedOperationException()
	}

	companion object {
		const val FRAME_ID_CHAPTER = "CHAP"
		const val FRAME_ID_TABLE_OF_CONTENT = "CTOC"
		private var id3v2ChapterFrames: ID3v2ChapterFrames? = null
		val instanceOf: ID3v2ChapterFrames
			get() {
				if (id3v2ChapterFrames == null) {
					id3v2ChapterFrames = ID3v2ChapterFrames()
				}
				return id3v2ChapterFrames!!
			}
	}
}
