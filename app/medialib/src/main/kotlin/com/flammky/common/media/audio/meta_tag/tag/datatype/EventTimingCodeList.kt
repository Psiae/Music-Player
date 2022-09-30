/*
 *  This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 *  General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 *  or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with this library; if not,
 *  you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.FrameBodyETCO

/**
 * List of [EventTimingCode]s.
 *
 * @author [Hendrik Schreiber](mailto:hs@tagtraum.com)
 * @version $Id:$
 */
class EventTimingCodeList : AbstractDataTypeList<EventTimingCode> {
	/**
	 * Mandatory, concretely-typed copy constructor, as required by
	 * [AbstractDataTypeList.AbstractDataTypeList].
	 *
	 * @param copy instance to copy
	 */
	constructor(copy: EventTimingCodeList?) : super(copy)
	constructor(body: FrameBodyETCO) : super(DataTypes.OBJ_TIMED_EVENT_LIST, body)

	override fun createListElement(): EventTimingCode {
		return EventTimingCode(DataTypes.OBJ_TIMED_EVENT, body)
	}
}
