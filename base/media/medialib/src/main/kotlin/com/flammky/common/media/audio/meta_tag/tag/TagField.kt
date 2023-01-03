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
package com.flammky.musicplayer.common.media.audio.meta_tag.tag

import java.io.UnsupportedEncodingException

/**
 * Implementing classes represent a tag field for the entagged audio library.<br></br>
 * Very basic functionality is defined for use with
 * [Tag].
 *
 * @author Rapha�l Slinckx
 */
interface TagField {
	/**
	 * This method copies the data of the given field to the current data.<br></br>
	 *
	 * @param field The field containing the data to be taken.
	 */
	fun copyContent(field: TagField?)

	/**
	 * Returns the Id of the represented tag field.<br></br>
	 * This value should uniquely identify a kind of tag data, like title.
	 * [AbstractTag] will use the &quot;id&quot; to summarize multiple
	 * fields.
	 *
	 * @return Unique identifier for the fields type. (title, artist...)
	 */
	val id: String?

	/**
	 * This method delivers the binary representation of the fields data in
	 * order to be directly written to the file.<br></br>
	 *
	 * @return Binary data representing the current tag field.<br></br>
	 * @throws UnsupportedEncodingException Most tag data represents text. In some cases the underlying
	 * implementation will need to convert the text data in java to
	 * a specific charset encoding. In these cases an
	 * [UnsupportedEncodingException] may occur.
	 */
	@get:Throws(UnsupportedEncodingException::class)
	val rawContent: ByteArray?

	/**
	 * Determines whether the represented field contains (is made up of) binary
	 * data, instead of text data.<br></br>
	 * Software can identify fields to be displayed because they are human
	 * readable if this method returns `false`.
	 *
	 * @return `true` if field represents binary data (not human
	 * readable).
	 */
	val isBinary: Boolean

	/**
	 * This method will set the field to represent binary data.<br></br>
	 *
	 * Some implementations may support conversions.<br></br>
	 * As of now (Octobre 2005) there is no implementation really using this
	 * method to perform useful operations.
	 *
	 * @param b `true`, if the field contains binary data.
	 * //@deprecated As for now is of no use. Implementations should use another
	 * //            way of setting this property.
	 */
	fun isBinary(b: Boolean)

	/**
	 * Identifies a field to be of common use.<br></br>
	 *
	 * Some software may differ between common and not common fields. A common
	 * one is for sure the title field. A web link may not be of common use for
	 * tagging. However some file formats, or future development of users
	 * expectations will make more fields common than now can be known.
	 *
	 * @return `true` if the field is of common use.
	 */
	val isCommon: Boolean

	/**
	 * Determines whether the content of the field is empty.<br></br>
	 *
	 * @return `true` if no data is stored (or empty String).
	 */
	val isEmpty: Boolean

	/**
	 * This method returns a human readable description of the fields contents.<br></br>
	 * For text fields it should be the text itself. Other fields containing
	 * images may return a formatted string with image properties like width,
	 * height and so on.
	 *
	 * @return Description of the fields content.
	 */
	fun toDescriptiveString(): String
}
