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
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.vorbiscomment

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.AbstractTagCreator
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * Create the raw packet data for a Vorbis Comment Tag
 */
class VorbisCommentCreator : AbstractTagCreator() {
	/**
	 * Convert tagdata to rawdata ready for writing to file
	 *
	 * @param tag
	 * @param isLastBlock
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	//TODO padding parameter currently ignored
	@Throws(UnsupportedEncodingException::class)
	override fun convertMetadata(tag: Tag?, isLastBlock: Boolean): ByteBuffer {
		return try {
			val baos = ByteArrayOutputStream()

			//Vendor
			val vendorString = (tag as VorbisCommentTag).vendor!!
			val vendorLength = vendorString.toByteArray(StandardCharsets.UTF_8).size
			baos.write(Utils.getSizeLEInt32(vendorLength))
			baos.write(vendorString.toByteArray(StandardCharsets.UTF_8))

			//User Comment List
			var listLength = tag.fieldCount
			if (tag.hasField(VorbisCommentFieldKey.VENDOR)) {
				listLength = listLength - 1
			}
			baos.write(Utils.getSizeLEInt32(listLength))

			//Add metadata raw content
			val it = tag.fields
			while (it.hasNext()) {
				val frame = it.next()
				if (frame.id == VorbisCommentFieldKey.VENDOR.fieldName) {
					//this is always stored above so ignore
				} else {
					baos.write(frame.rawContent)
				}
			}

			//Put into ByteBuffer
			val buf = ByteBuffer.wrap(baos.toByteArray())
			buf.rewind()
			buf
		} catch (ioe: IOException) {
			//Should never happen as not writing to file at this point
			throw RuntimeException(ioe)
		}
	}
}
