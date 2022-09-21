/**
 * @author : Paul Taylor
 *
 * Version @version:$Id$
 *
 * Jaudiotagger Copyright (C)2004,2005
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 * or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License ainteger with this library; if not,
 * you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * Description:
 * Used by Commercial Frame (COMR)
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.AbstractIntStringValuePair

/**
 * Defines how song was purchased used by the COMR frame
 *
 */
class ReceivedAsTypes private constructor() : AbstractIntStringValuePair() {
	init {
		idToValue[0x00] = "Other"
		idToValue[0x01] = "Standard CD album with other songs"
		idToValue[0x02] = "Compressed audio on CD"
		idToValue[0x03] = "File over the Internet"
		idToValue[0x04] = "Stream over the Internet"
		idToValue[0x05] = "As note sheets"
		idToValue[0x06] = "As note sheets in a book with other sheets"
		idToValue[0x07] = "Music on other media"
		idToValue[0x08] = "Non-musical merchandise"
		createMaps()
	}

	companion object {
		//The number of bytes used to hold the text encoding field size
		const val RECEIVED_AS_FIELD_SIZE = 1
		private var receivedAsTypes: ReceivedAsTypes? = null

		@JvmStatic
		val instanceOf: ReceivedAsTypes
			get() {
				if (receivedAsTypes == null) {
					receivedAsTypes = ReceivedAsTypes()
				}
				return receivedAsTypes!!
			}
	}
}
