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
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.AbstractTagCreator
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getSizeBEInt32
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.Mp4AtomIdentifier
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4BoxHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.FieldKey
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.KeyNotFoundException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.field.Mp4TagCoverField
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * Create raw content of mp4 tag data, concerns itself with atoms upto the ilst atom
 *
 *
 * This level was selected because the ilst atom can be recreated without reference to existing mp4 fields
 * but fields above this level are dependent upon other information that is not held in the tag.
 *
 * <pre>
 * |--- ftyp
 * |--- moov
 * |......|
 * |......|----- mvdh
 * |......|----- trak
 * |......|----- udta
 * |..............|
 * |..............|-- meta
 * |....................|
 * |....................|-- hdlr
 * |....................|-- ilst
 * |....................|.. ..|
 * |....................|.....|---- @nam (Optional for each metadatafield)
 * |....................|.....|.......|-- data
 * |....................|.....|....... ecetera
 * |....................|.....|---- ---- (Optional for reverse dns field)
 * |....................|.............|-- mean
 * |....................|.............|-- name
 * |....................|.............|-- data
 * |....................|................ ecetere
 * |....................|-- free
 * |--- free
 * |--- mdat
</pre> *
 */
class Mp4TagCreator : AbstractTagCreator() {
	/**
	 * Convert tagdata to rawdata ready for writing to file
	 *
	 * @param tag
	 * @param isLastBlock TODO padding parameter currently ignored
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	@Throws(UnsupportedEncodingException::class)
	override fun convertMetadata(tag: Tag?, isLastBlock: Boolean): ByteBuffer {
		return try {
			//Add metadata raw content
			val baos = ByteArrayOutputStream()
			val it = tag!!.fields
			var processedArtwork = false
			while (it.hasNext()) {
				val frame = it.next()
				//To ensure order is maintained dont process artwork until iterator hits it.
				if (frame is Mp4TagCoverField) {
					if (processedArtwork) {
						//ignore
					} else {
						processedArtwork = true

						//Because each artwork image is held within the tag as a separate field, but when
						//they are written they are all held under a single covr box we need to do some checks
						//and special processing here if we have any artwork image (this code only necessary
						//if we have more than 1 but do it anyway even if only have 1 image)
						val covrDataBaos = ByteArrayOutputStream()
						try {
							for (artwork in tag.getFields(FieldKey.COVER_ART)!!) {
								covrDataBaos.write((artwork as Mp4TagField?)!!.rawContentDataOnly)
							}
						} catch (knfe: KeyNotFoundException) {
							//This cannot happen
							throw RuntimeException("Unable to find COVERART Key")
						}

						//Now create the parent Data
						val data = covrDataBaos.toByteArray()
						baos.write(getSizeBEInt32(Mp4BoxHeader.HEADER_LENGTH + data.size))
						baos.write(Mp4FieldKey.ARTWORK.fieldName.toByteArray(StandardCharsets.ISO_8859_1))
						baos.write(data)
					}
				} else {
					baos.write(frame!!.rawContent)
				}
			}

			//Wrap into ilst box
			val ilst = ByteArrayOutputStream()
			ilst.write(getSizeBEInt32(Mp4BoxHeader.HEADER_LENGTH + baos.size()))
			ilst.write(Mp4AtomIdentifier.ILST.fieldName.toByteArray(StandardCharsets.ISO_8859_1))
			ilst.write(baos.toByteArray())

			//Put into ByteBuffer
			val buf = ByteBuffer.wrap(ilst.toByteArray())
			buf.rewind()
			buf
		} catch (ioe: IOException) {
			//Should never happen as not writing to file at this point
			throw RuntimeException(ioe)
		}
	}
}
