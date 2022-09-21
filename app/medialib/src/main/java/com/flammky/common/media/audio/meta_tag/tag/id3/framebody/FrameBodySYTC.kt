/*
 *  MusicTag Copyright (C)2003,2004
 *
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
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidTagException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Frames
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.EventTimingTimestampTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.EventTimingTimestampTypes.Companion.instanceOf
import java.nio.ByteBuffer
import java.util.*

/**
 * Synchronised tempo codes frame.
 *
 *
 * For a more accurate description of the tempo of a musical piece this
 * frame might be used. After the header follows one byte describing
 * which time stamp format should be used. Then follows one or more
 * tempo codes. Each tempo code consists of one tempo part and one time
 * part. The tempo is in BPM described with one or two bytes. If the
 * first byte has the value $FF, one more byte follows, which is added
 * to the first giving a range from 2 - 510 BPM, since $00 and $01 is
 * reserved. $00 is used to describe a beat-free time period, which is
 * not the same as a music-free time period. $01 is used to indicate one
 * single beat-stroke followed by a beat-free period.
 *
 *
 * The tempo descriptor is followed by a time stamp. Every time the
 * tempo in the music changes, a tempo descriptor may indicate this for
 * the player. All tempo descriptors should be sorted in chronological
 * order. The first beat-stroke in a time-period is at the same time as
 * the beat description occurs. There may only be one "SYTC" frame in
 * each tag.
 *
 * <table border=0 width="70%">
 * <tr><td colspan=2> &lt;Header for 'Synchronised tempo codes', ID: "SYTC"&gt;</td></tr>
 * <tr><td>Time stamp format</td><td width="80%">$xx</td></tr>
 * <tr><td>Tempo data </td><td>&lt;binary data&gt;</td></tr>
</table> *
 *
 * Where time stamp format is:
 *
 *
 * $01 Absolute time, 32 bit sized, using MPEG frames as unit<br></br>
 * $02 Absolute time, 32 bit sized, using milliseconds as unit
 *
 *
 * Abolute time means that every stamp contains the time from the
 * beginning of the file.
 *
 *
 *
 * For more details, please refer to the ID3 specifications:
 *
 *  * [ID3 v2.3.0 Spec](http://www.id3.org/id3v2.3.0.txt)
 *
 *
 * @author : Paul Taylor
 * @author : Eric Farng
 * @author : Hendrik Schreiber
 * @version $Id$
 */
class FrameBodySYTC : AbstractID3v2FrameBody, ID3v24FrameBody, ID3v23FrameBody {

	override val identifier: String?
		get() = ID3v24Frames.FRAME_ID_SYNC_TEMPO

	/**
	 * Creates a new FrameBodySYTC datatype.
	 */
	constructor() {
		setObjectValue(DataTypes.OBJ_TIME_STAMP_FORMAT, MILLISECONDS)
	}

	/**
	 * @param timestampFormat
	 * @param tempo
	 */
	constructor(timestampFormat: Int, tempo: ByteArray?) {
		setObjectValue(DataTypes.OBJ_TIME_STAMP_FORMAT, timestampFormat)
		setObjectValue(DataTypes.OBJ_SYNCHRONISED_TEMPO_LIST, tempo)
	}

	/**
	 * Creates a new FrameBody from buffer
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)

	/**
	 * Copy constructor
	 *
	 * @param body
	 */
	constructor(body: FrameBodySYTC) : super(body)
	/**
	 * Timestamp format for all events in this frame.
	 * A value of `1` means absolute time (32 bit) using [MPEG](#MPEG) frames as unit.
	 * A value of `2` means absolute time (32 bit) using milliseconds as unit.
	 *
	 * @return timestamp format
	 * @see .MILLISECONDS
	 *
	 * @see .MPEG_FRAMES
	 */
	/**
	 * Sets the timestamp format.
	 *
	 * @param timestampFormat 1 for MPEG frames or 2 for milliseconds
	 * @see .getTimestampFormat
	 */
	var timestampFormat: Int
		get() = (getObjectValue(DataTypes.OBJ_TIME_STAMP_FORMAT) as Number).toInt()
		set(timestampFormat) {
			requireNotNull(instanceOf.getValueForId(timestampFormat)) { "Timestamp format must be 1 or 2 (ID3v2.4, 4.7): $timestampFormat" }
			setObjectValue(DataTypes.OBJ_TIME_STAMP_FORMAT, timestampFormat)
		}

	/**
	 * Chronological map of tempi.
	 *
	 * @return map of tempi
	 */
	val tempi: Map<Long, Int>
		get() {
			val map: MutableMap<Long, Int> = LinkedHashMap()
			val codes =
				getObjectValue(DataTypes.OBJ_SYNCHRONISED_TEMPO_LIST) as List<SynchronisedTempoCode>
			for (code in codes) {
				map[code.getTimestamp()] = code.getTempo()
			}
			return Collections.unmodifiableMap(map)
		}

	/**
	 * Chronological list of timestamps.
	 *
	 * @return list of timestamps
	 */
	val timestamps: List<Long>
		get() {
			val list: MutableList<Long> = ArrayList()
			val codes =
				getObjectValue(DataTypes.OBJ_SYNCHRONISED_TEMPO_LIST) as List<SynchronisedTempoCode>
			for (code in codes) {
				list.add(code.getTimestamp())
			}
			return Collections.unmodifiableList(list)
		}

	/**
	 * Adds a tempo.
	 *
	 * @param timestamp timestamp
	 * @param tempo tempo
	 */
	fun addTempo(timestamp: Long, tempo: Int) {
		// make sure we don't have two tempi at the same time
		removeTempo(timestamp)
		val codes =
			getObjectValue(DataTypes.OBJ_SYNCHRONISED_TEMPO_LIST) as MutableList<SynchronisedTempoCode>
		var insertIndex = 0
		if (codes.isNotEmpty() && codes[0].getTimestamp() <= timestamp) {
			for (code in codes) {
				val translatedTimestamp = code.getTimestamp()
				if (timestamp < translatedTimestamp) {
					break
				}
				insertIndex++
			}
		}
		codes.add(
			insertIndex,
			SynchronisedTempoCode(DataTypes.OBJ_SYNCHRONISED_TEMPO, this, tempo, timestamp)
		)
	}

	/**
	 * Removes a tempo at a given timestamp.
	 *
	 * @param timestamp timestamp
	 * @return `true`, if any timestamps were removed
	 */
	fun removeTempo(timestamp: Long): Boolean {
		val codes =
			getObjectValue(DataTypes.OBJ_SYNCHRONISED_TEMPO_LIST) as MutableList<SynchronisedTempoCode>
		var removed = false
		val iterator = codes.listIterator()
		while (iterator.hasNext()) {
			val code = iterator.next()
			if (timestamp == code.getTimestamp()) {
				iterator.remove()
				removed = true
			}
			if (timestamp > code.getTimestamp()) {
				break
			}
		}
		return removed
	}

	/**
	 * Remove all timing codes.
	 */
	fun clearTempi() {
		(getObjectValue(DataTypes.OBJ_SYNCHRONISED_TEMPO_LIST) as MutableList<EventTimingCode?>).clear()
	}

	@Throws(InvalidTagException::class)
	override fun read(byteBuffer: ByteBuffer) {
		super.read(byteBuffer)

		// validate input
		val codes =
			getObjectValue(DataTypes.OBJ_SYNCHRONISED_TEMPO_LIST) as List<SynchronisedTempoCode>
		var lastTimestamp: Long = 0
		for (code in codes) {
			if (code.getTimestamp() < lastTimestamp) {
				logger.warning("Synchronised tempo codes are not in chronological order. " + lastTimestamp + " is followed by " + code.getTimestamp() + ".")
				// throw exception???
			}
			lastTimestamp = code.getTimestamp()
		}
	}

	override fun setupObjectList() {
		objectList.add(
			NumberHashMap(
				DataTypes.OBJ_TIME_STAMP_FORMAT,
				this,
				EventTimingTimestampTypes.TIMESTAMP_KEY_FIELD_SIZE
			)
		)
		objectList.add(SynchronisedTempoCodeList(this))
	}

	companion object {
		const val MPEG_FRAMES = 1
		const val MILLISECONDS = 2
	}
}
