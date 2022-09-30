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
 * A single synchronized tempo code. Part of a list of temnpo codes ([SynchronisedTempoCodeList]), that are contained in
 * [FrameBodySYTC]
 *
 * @author [Hendrik Schreiber](mailto:hs@tagtraum.com)
 * @version $Id:$
 */
class SynchronisedTempoCode : AbstractDataType, Cloneable {
	private val tempo: TempoCode = TempoCode(DataTypes.OBJ_SYNCHRONISED_TEMPO_DATA, null, 1)
	private val timestamp: NumberFixedLength = NumberFixedLength(DataTypes.OBJ_DATETIME, null, 4)

	constructor(copy: SynchronisedTempoCode) : super(copy) {
		tempo.value = copy.tempo.value
		timestamp.value = copy.timestamp.value
	}

	@JvmOverloads
	constructor(
		identifier: String?,
		frameBody: AbstractTagFrameBody?,
		tempo: Int = 0x00,
		timestamp: Long = 0L
	) : super(identifier, frameBody) {
		body = frameBody
		this.tempo.value = tempo
		this.timestamp.value = timestamp
	}

	override var body: AbstractTagFrameBody?
		get() = super.body
		set(value) {
			super.body = value
			tempo.body = value
			timestamp.body = value
		}

	fun getTimestamp(): Long {
		return (timestamp.value as Number).toLong()
	}

	fun setTimestamp(timestamp: Long) {
		this.timestamp.value = timestamp
	}

	fun getTempo(): Int {
		return (tempo.value as Number).toInt()
	}

	fun setTempo(tempo: Int) {
		require(!(tempo < 0 || tempo > 510)) { "Tempo must be a positive value less than 511: $tempo" }
		this.tempo.value = tempo
	}

	override val size: Int
		get() = tempo.size + timestamp.size

	@Throws(InvalidDataTypeException::class)
	override fun readByteArray(buffer: ByteArray?, originalOffset: Int) {
		var localOffset = originalOffset
		val size = this.size
		logger.finest(
			"offset:$localOffset"
		)

		//The read has extended further than the defined frame size (ok to extend upto
		//size because the next datatype may be of length 0.)
		if (originalOffset > buffer!!.size - size) {
			logger.warning("Invalid size for FrameBody")
			throw InvalidDataTypeException("Invalid size for FrameBody")
		}
		tempo.readByteArray(buffer, localOffset)
		localOffset += tempo.size
		timestamp.readByteArray(buffer, localOffset)
		localOffset += timestamp.size
	}

	override fun writeByteArray(): ByteArray? {
		val typeData = tempo.writeByteArray()
		val timeData = timestamp.writeByteArray()
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
		val that = o as SynchronisedTempoCode
		return !(getTempo() != that.getTempo() || getTimestamp() != that.getTimestamp())
	}

	override fun hashCode(): Int {
		var result = tempo.hashCode()
		result = 31 * result + timestamp.hashCode()
		return result
	}

	override fun toString(): String {
		return "" + getTempo() + " (\"" + EventTimingTypes.instanceOf.getValueForId(getTempo()) + "\"), " + getTimestamp()
	}

	@Throws(CloneNotSupportedException::class)
	public override fun clone(): Any {
		return SynchronisedTempoCode(this)
	}
}
