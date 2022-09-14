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

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidDataTypeException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.AbstractTagFrameBody
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.EventTimingTypes

/**
 * A single event timing code. Part of a list of timing codes ([EventTimingCodeList]), that are contained in
 * [FrameBodyETCO].
 *
 * @author [Hendrik Schreiber](mailto:hs@tagtraum.com)
 * @version $Id:$
 */
class EventTimingCode : AbstractDataType, Cloneable {
	private val mType: NumberHashMap = NumberHashMap(DataTypes.OBJ_TYPE_OF_EVENT, null, 1)
	private val mTimeStamp: NumberFixedLength = NumberFixedLength(DataTypes.OBJ_DATETIME, null, 4)

	constructor(copy: EventTimingCode) : super(copy) {
		mType.value = copy.mType.value
		mTimeStamp.value = copy.mTimeStamp.value
	}

	@JvmOverloads
	constructor(
		identifier: String?,
		frameBody: AbstractTagFrameBody?,
		type: Int = 0x00,
		timestamp: Long = 0L
	) : super(identifier, frameBody) {
		body = frameBody
		this.mType.value = type
		this.mTimeStamp.value = timestamp
	}

	var timeStamp: Long
		get() = (mTimeStamp.value as Number).toLong()
		set(value) {
			mTimeStamp.value = value
		}

	var type: Int
		get() = (mType.value as Number).toInt()
		set(value) {
			mType.value = value
		}

	override var body: AbstractTagFrameBody?
		get() = super.body
		set(value) {
			super.body = value
			mType.body = value
			mTimeStamp.body = value
		}

	override val size: Int
		get() = SIZE

	@Throws(InvalidDataTypeException::class)
	override fun readByteArray(buffer: ByteArray?, originalOffset: Int) {
		var localOffset = originalOffset
		val size = size
		AbstractDataType.Companion.logger.finest(
			"offset:$localOffset"
		)

		//The read has extended further than the defined frame size (ok to extend upto
		//size because the next datatype may be of length 0.)
		if (originalOffset > buffer!!.size - size) {
			AbstractDataType.Companion.logger.warning("Invalid size for FrameBody")
			throw InvalidDataTypeException("Invalid size for FrameBody")
		}
		mType.readByteArray(buffer, localOffset)
		localOffset += mType.size
		mTimeStamp.readByteArray(buffer, localOffset)
		localOffset += mTimeStamp.size
	}

	override fun writeByteArray(): ByteArray? {
		val typeData = mType.writeByteArray()
		val timeData = mTimeStamp.writeByteArray()
		if (typeData == null || timeData == null) return null
		val objectData = ByteArray(typeData.size + timeData.size)
		System.arraycopy(typeData, 0, objectData, 0, typeData.size)
		System.arraycopy(timeData, 0, objectData, typeData.size, timeData.size)
		return objectData
	}

	override fun equals(o: Any?): Boolean {
		if (this === o) return true
		if (o == null || javaClass != o.javaClass) return false
		if (!super.equals(o)) return false
		val that = o as EventTimingCode
		return !(type != that.type || timeStamp != that.timeStamp)
	}

	override fun hashCode(): Int {
		var result = mType.hashCode()
		result = 31 * result + mTimeStamp.hashCode()
		return result
	}

	override fun toString(): String {
		return "" + type + " (\"" + EventTimingTypes.instanceOf.getValueForId(type) + "\"), " + timeStamp
	}

	@Throws(CloneNotSupportedException::class)
	public override fun clone(): Any {
		return EventTimingCode(this)
	}

	companion object {
		private const val SIZE = 5
	}
}
