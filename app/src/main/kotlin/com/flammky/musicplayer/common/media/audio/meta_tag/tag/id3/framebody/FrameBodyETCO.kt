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
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.DataTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.EventTimingCode
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.EventTimingCodeList
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.NumberHashMap
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Frames
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.EventTimingTimestampTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.EventTimingTimestampTypes.Companion.instanceOf
import java.nio.ByteBuffer
import java.util.*

/**
 * Event timing codes frame.
 *
 *
 * This frame allows synchronisation with key events in a song or sound.
 * The header is:
 *
 * <table border=0 width="70%">
 * <tr><td colspan=2> &lt;Header for 'Event timing codes', ID: "ETCO"&gt;</td></tr>
 * <tr><td>Time stamp format</td><td width="80%">$xx</td></tr>
</table> *
 *
 * Where time stamp format is:
 *
 *
 * $01 Absolute time, 32 bit sized, using [MPEG](#MPEG) frames as unit<br></br>
 * $02 Absolute time, 32 bit sized, using milliseconds as unit
 *
 *
 * Absolute time means that every stamp contains the time from the
 * beginning of the file.
 *
 *
 * Followed by a list of key events in the following format:
 *
 * <table border=0 width="70%">
 * <tr><td>Type of event</td><td width="80%">$xx</td></tr>
 * <tr><td>Time stamp</td><td>$xx (xx ...)</td></tr>
</table> *
 *
 * The 'Time stamp' is set to zero if directly at the beginning of the
 * sound or after the previous event. All events should be sorted in
 * chronological order. The type of event is as follows:
 *
 * <table border=0 width="70%">
 * <tr><td>$00    </td><td width="80%">padding (has no meaning)</td></tr>
 * <tr><td>$01    </td><td>end of initial silence              </td></tr>
 * <tr><td>$02    </td><td>intro start                         </td></tr>
 * <tr><td>$03    </td><td>mainpart start                      </td></tr>
 * <tr><td>$04    </td><td>outro start                         </td></tr>
 * <tr><td>$05    </td><td>outro end                           </td></tr>
 * <tr><td>$06    </td><td>verse start                         </td></tr>
 * <tr><td>$07    </td><td>refrain start                       </td></tr>
 * <tr><td>$08    </td><td>interlude start                     </td></tr>
 * <tr><td>$09    </td><td>theme start                         </td></tr>
 * <tr><td>$0A    </td><td>variation start                     </td></tr>
 * <tr><td>$0B    </td><td>key change                          </td></tr>
 * <tr><td>$0C    </td><td>time change                         </td></tr>
 * <tr><td>$0D    </td><td>momentary unwanted noise (Snap, Crackle & Pop)</td></tr>
 * <tr><td>$0E    </td><td>sustained noise                     </td></tr>
 * <tr><td>$0F    </td><td>sustained noise end                 </td></tr>
 * <tr><td>$10    </td><td>intro end                           </td></tr>
 * <tr><td>$11    </td><td>mainpart end                        </td></tr>
 * <tr><td>$12    </td><td>verse end                           </td></tr>
 * <tr><td>$13    </td><td>refrain end                         </td></tr>
 * <tr><td>$14    </td><td>theme end                           </td></tr>
 * <tr><td>$15-$DF</td><td>reserved for future use             </td></tr>
 * <tr><td>$E0-$EF</td><td>not predefined sync 0-F             </td></tr>
 * <tr><td>$F0-$FC</td><td>reserved for future use             </td></tr>
 * <tr><td>$FD    </td><td>audio end (start of silence)        </td></tr>
 * <tr><td>$FE    </td><td>audio file ends                     </td></tr>
 * <tr><td>$FF</td><td>one more byte of events follows (all the following bytes with the value $FF have the same function)</td></tr>
</table> *
 *
 *
 * Terminating the start events such as "intro start" is not required.
 * The 'Not predefined sync's ($E0-EF) are for user events. You might
 * want to synchronise your music to something, like setting of an
 * explosion on-stage, turning on your screensaver etc.
 *
 *
 * There may only be one "ETCO" frame in each tag.
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
class FrameBodyETCO : AbstractID3v2FrameBody, ID3v24FrameBody, ID3v23FrameBody {

	override val identifier: String?
		get() = ID3v24Frames.FRAME_ID_EVENT_TIMING_CODES

	/**
	 * Creates a new FrameBodyETCO datatype.
	 */
	constructor() {
		setObjectValue(DataTypes.OBJ_TIME_STAMP_FORMAT, MILLISECONDS)
	}

	constructor(body: FrameBodyETCO) : super(body)

	/**
	 * Creates a new FrameBodyETCO datatype.
	 *
	 * @param byteBuffer buffer to read from
	 * @param frameSize size of the frame
	 * @throws InvalidTagException if unable to create framebody from buffer
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)
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
			requireNotNull(instanceOf.getValueForId(timestampFormat)) { "Timestamp format must be 1 or 2 (ID3v2.4, 4.5): $timestampFormat" }
			setObjectValue(DataTypes.OBJ_TIME_STAMP_FORMAT, timestampFormat)
		}

	/**
	 * Chronological map of timing codes.
	 *
	 * @return map of timing codes
	 */
	val timingCodes: Map<Long, IntArray>
		get() {
			val map: MutableMap<Long, IntArray> = LinkedHashMap()
			val codes = getObjectValue(DataTypes.OBJ_TIMED_EVENT_LIST) as List<EventTimingCode>
			var lastTimestamp: Long = 0
			for (code in codes) {
				val translatedTimestamp =
					if (code.timeStamp == 0L) lastTimestamp else code.timeStamp
				val types = map[translatedTimestamp]
				if (types == null) {
					map[translatedTimestamp] = intArrayOf(code.type)
				} else {
					val newTypes = IntArray(types.size + 1)
					System.arraycopy(types, 0, newTypes, 0, types.size)
					newTypes[newTypes.size - 1] = code.type
					map[translatedTimestamp] = newTypes
				}
				lastTimestamp = translatedTimestamp
			}
			return Collections.unmodifiableMap(map)
		}

	/**
	 * Chronological list of timestamps of a set of given types.
	 *
	 * @param type types
	 * @return list of timestamps
	 */
	fun getTimestamps(vararg type: Int): List<Long> {
		val typeSet = toSet(*type)
		val list: MutableList<Long> = ArrayList()
		val codes = getObjectValue(DataTypes.OBJ_TIMED_EVENT_LIST) as List<EventTimingCode>
		var lastTimestamp: Long = 0
		for (code in codes) {
			val translatedTimestamp = if (code.timeStamp == 0L) lastTimestamp else code.timeStamp
			if (typeSet.contains(code.type)) {
				list.add(translatedTimestamp)
			}
			lastTimestamp = translatedTimestamp
		}
		return Collections.unmodifiableList(list)
	}

	/**
	 * Adds a timing code for each given type.
	 *
	 * @param timestamp timestamp
	 * @param types types
	 */
	fun addTimingCode(timestamp: Long, vararg types: Int) {
		val codes =
			getObjectValue(DataTypes.OBJ_TIMED_EVENT_LIST) as? MutableList<EventTimingCode> ?: return
		var lastTimestamp: Long = 0
		var insertIndex = 0
		if (codes.isNotEmpty() && codes[0].timeStamp <= timestamp) {
			for (code in codes) {
				val translatedTimestamp =
					if (code.timeStamp == 0L) lastTimestamp else code.timeStamp
				if (timestamp < translatedTimestamp) {
					break
				}
				insertIndex++
				lastTimestamp = translatedTimestamp
			}
		}
		for (type in types) {
			codes.add(
				insertIndex,
				EventTimingCode(DataTypes.OBJ_TIMED_EVENT, this, type, timestamp)
			)
			insertIndex++ // preserve order of types
		}
	}

	/**
	 * Removes timestamps at a given time with the given types.
	 *
	 * @param timestamp timestamp
	 * @param types types
	 * @return `true`, if any timestamps were removed
	 */
	fun removeTimingCode(timestamp: Long, vararg types: Int): Boolean {
		// before we can remove anything, we have to resolve relative 0-timestamps
		// otherwise we might remove the anchor a relative timestamp relies on
		resolveRelativeTimestamps()
		val typeSet = toSet(*types)
		val codes = getObjectValue(DataTypes.OBJ_TIMED_EVENT_LIST) as MutableList<EventTimingCode>
		var removed = false
		val iterator = codes.listIterator()
		while (iterator.hasNext()) {
			val code = iterator.next()
			if (timestamp == code.timeStamp && typeSet.contains(code.type)) {
				iterator.remove()
				removed = true
			}
			if (timestamp > code.timeStamp) {
				break
			}
		}
		return removed
	}

	/**
	 * Remove all timing codes.
	 */
	fun clearTimingCodes() {
		(getObjectValue(DataTypes.OBJ_TIMED_EVENT_LIST) as MutableList<EventTimingCode?>).clear()
	}

	/**
	 * Resolve any relative timestamp (zero timestamp after a non-zero timestamp) to absolute timestamp.
	 */
	private fun resolveRelativeTimestamps() {
		val codes = getObjectValue(DataTypes.OBJ_TIMED_EVENT_LIST) as List<EventTimingCode>
		var lastTimestamp: Long = 0
		for (code in codes) {
			val translatedTimestamp = if (code.timeStamp == 0L) lastTimestamp else code.timeStamp
			code.timeStamp = translatedTimestamp
			lastTimestamp = translatedTimestamp
		}
	}

	@Throws(InvalidTagException::class)
	override fun read(byteBuffer: ByteBuffer) {
		super.read(byteBuffer)

		// validate input
		val codes = getObjectValue(DataTypes.OBJ_TIMED_EVENT_LIST) as List<EventTimingCode>
		var lastTimestamp: Long = 0
		for (code in codes) {
			val translatedTimestamp = if (code.timeStamp == 0L) lastTimestamp else code.timeStamp
			if (code.timeStamp < lastTimestamp) {
				logger.warning("Event codes are not in chronological order. " + lastTimestamp + " is followed by " + code.timeStamp + ".")
				// throw exception???
			}
			lastTimestamp = translatedTimestamp
		}
	}

	/**
	 * Setup object list.
	 */
	override fun setupObjectList() {
		objectList.add(
			NumberHashMap(
				DataTypes.OBJ_TIME_STAMP_FORMAT,
				this,
				EventTimingTimestampTypes.TIMESTAMP_KEY_FIELD_SIZE
			)
		)
		objectList.add(EventTimingCodeList(this))
	}

	companion object {
		const val MPEG_FRAMES = 1
		const val MILLISECONDS = 2
		private fun toSet(vararg types: Int): Set<Int> {
			val typeSet: MutableSet<Int> = HashSet()
			for (type in types) {
				typeSet.add(type)
			}
			return typeSet
		}
	}
}
