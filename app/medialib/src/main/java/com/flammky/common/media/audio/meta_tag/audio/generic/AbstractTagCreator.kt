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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer

/**
 * Abstract class for creating the raw content that represents the tag so it can be written
 * to file.
 */
abstract class AbstractTagCreator {
	/**
	 * Convert tagdata to rawdata ready for writing to file with no additional padding
	 *
	 * @param tag
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	@Throws(UnsupportedEncodingException::class)
	fun convertMetadata(tag: Tag?): ByteBuffer {
		return convertMetadata(tag, false)
	}

	/**
	 * Convert tagdata to rawdata ready for writing to file
	 *
	 * @param tag
	 * @param isLastBlock
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	@Throws(UnsupportedEncodingException::class)
	abstract fun convertMetadata(tag: Tag?, isLastBlock: Boolean): ByteBuffer
}
