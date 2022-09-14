/**
 * @author : Paul Taylor
 * @author : Eric Farng
 *
 * Version @version:$Id$
 *
 * MusicTag Copyright (C)2003,2004
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 * or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not,
 * you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * Description:
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.DataTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v23Frames
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Frames
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import java.nio.ByteBuffer
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Level

class FrameBodyTDRC : AbstractFrameBodyTextInfo, ID3v24FrameBody {

	override val identifier: String?
		get() = ID3v24Frames.FRAME_ID_YEAR

	/**
	 * Retrieve the original identifier
	 * @return
	 */
	/**
	 * Used when converting from v3 tags , these fields should ALWAYS hold the v23 value
	 */
	var originalID: String? = null
		private set
	var year: String? = ""
		private set
	var time: String? = ""
		private set
	var date: String? = ""
		private set
	var isMonthOnly = false
	var isHoursOnly = false

	/**
	 * Creates a new FrameBodyTDRC datatype.
	 */
	constructor() : super()
	constructor(body: FrameBodyTDRC) : super(body)

	val formattedText: String?
		get() {
			val sb = StringBuffer()
			return if (originalID == null) {
				text
			} else {
				if (year != null && !year!!.trim { it <= ' ' }.isEmpty()) {
					sb.append(formatAndParse(formatYearOut, formatYearIn, year))
				}
				if (date != "") {
					if (isMonthOnly) {
						sb.append(formatAndParse(formatMonthOut, formatDateIn, date))
					} else {
						sb.append(formatAndParse(formatDateOut, formatDateIn, date))
					}
				}
				if (time != "") {
					if (isHoursOnly) {
						sb.append(formatAndParse(formatHoursOut, formatTimeIn, time))
					} else {
						sb.append(formatAndParse(formatTimeOut, formatTimeIn, time))
					}
				}
				sb.toString()
			}
		}

	fun setYear(year: String?) {
		logger.finest(
			"Setting year to$year"
		)
		this.year = year
	}

	fun setTime(time: String?) {
		logger.finest(
			"Setting time to:$time"
		)
		this.time = time
	}

	fun setDate(date: String?) {
		logger.finest(
			"Setting date to:$date"
		)
		this.date = date
	}

	/**
	 * When converting v3 TYER to v4 TDRC frame
	 * @param body
	 */
	constructor(body: FrameBodyTYER) {
		originalID = ID3v23Frames.FRAME_ID_V3_TYER
		year = body.text
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, TextEncoding.ISO_8859_1)
		setObjectValue(DataTypes.OBJ_TEXT, formattedText)
	}

	/**
	 * When converting v3 TIME to v4 TDRC frame
	 * @param body
	 */
	constructor(body: FrameBodyTIME) {
		originalID = ID3v23Frames.FRAME_ID_V3_TIME
		time = body.text
		isHoursOnly = body.isHoursOnly
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, TextEncoding.ISO_8859_1)
		setObjectValue(DataTypes.OBJ_TEXT, formattedText)
	}

	/**
	 * When converting v3 TDAT to v4 TDRC frame
	 * @param body
	 */
	constructor(body: FrameBodyTDAT) {
		originalID = ID3v23Frames.FRAME_ID_V3_TDAT
		date = body.text
		isMonthOnly = body.isMonthOnly
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, TextEncoding.ISO_8859_1)
		setObjectValue(DataTypes.OBJ_TEXT, formattedText)
	}

	/**
	 * When converting v3 TDRA to v4 TDRC frame
	 * @param body
	 */
	constructor(body: FrameBodyTRDA) {
		originalID = ID3v23Frames.FRAME_ID_V3_TRDA
		date = body.text
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, TextEncoding.ISO_8859_1)
		setObjectValue(DataTypes.OBJ_TEXT, formattedText)
	}

	/**
	 * Creates a new FrameBodyTDRC dataType.
	 *
	 * Tries to decode the text to find the v24 date mask being used, and store the v3 components of the mask
	 *
	 * @param textEncoding
	 * @param text
	 */
	constructor(textEncoding: Byte, text: String?) : super(textEncoding, text) {
		findMatchingMaskAndExtractV3Values()
	}

	/**
	 * Creates a new FrameBodyTDRC datatype from File
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize) {
		findMatchingMaskAndExtractV3Values()
	}

	fun findMatchingMaskAndExtractV3Values() {
		//Find the date format of the text
		for (i in formatters.indices) {
			try {
				var d: Date?
				synchronized(formatters[i]) { d = formatters[i].parse(text) }
				//If able to parse a date from the text
				if (d != null) {
					extractID3v23Formats(d!!, i)
					break
				}
			} //Dont display will occur for each failed format
			catch (e: ParseException) {
				//Do nothing;
			} catch (nfe: NumberFormatException) {
				//Do nothing except log warning because not really expecting this to happen
				logger.log(
					Level.WARNING,
					"Date Formatter:" + formatters[i].toPattern() + "failed to parse:" + text + "with " + nfe.message,
					nfe
				)
			}
		}
	}

	/**
	 * Extract the components ans store the v23 version of the various values
	 *
	 * @param dateRecord
	 * @param precision
	 */
	//TODO currently if user has entered Year and Month, we only store in v23, should we store month with
	//first day
	private fun extractID3v23Formats(dateRecord: Date, precision: Int) {
		logger.fine("Precision is:" + precision + "for date:" + dateRecord.toString())

		//Precision Year
		if (precision == PRECISION_YEAR) {
			setYear(
				formatDateAsYear(
					dateRecord
				)
			)
		} else if (precision == PRECISION_MONTH) {
			setYear(
				formatDateAsYear(
					dateRecord
				)
			)
			setDate(
				formatDateAsDate(
					dateRecord
				)
			)
			isMonthOnly = true
		} else if (precision == PRECISION_DAY) {
			setYear(
				formatDateAsYear(
					dateRecord
				)
			)
			setDate(
				formatDateAsDate(
					dateRecord
				)
			)
		} else if (precision == PRECISION_HOUR) {
			setYear(
				formatDateAsYear(
					dateRecord
				)
			)
			setDate(
				formatDateAsDate(
					dateRecord
				)
			)
			setTime(
				formatDateAsTime(
					dateRecord
				)
			)
			isHoursOnly = true
		} else if (precision == PRECISION_MINUTE) {
			setYear(
				formatDateAsYear(
					dateRecord
				)
			)
			setDate(
				formatDateAsDate(
					dateRecord
				)
			)
			setTime(
				formatDateAsTime(
					dateRecord
				)
			)
		} else if (precision == PRECISION_SECOND) {
			setYear(
				formatDateAsYear(
					dateRecord
				)
			)
			setDate(
				formatDateAsDate(
					dateRecord
				)
			)
			setTime(
				formatDateAsTime(
					dateRecord
				)
			)
		}
	}

	companion object {
		private var formatYearIn: SimpleDateFormat? = null
		private var formatYearOut: SimpleDateFormat? = null
		private var formatDateIn: SimpleDateFormat? = null
		private var formatDateOut: SimpleDateFormat? = null
		private var formatMonthOut: SimpleDateFormat? = null
		private var formatTimeIn: SimpleDateFormat? = null
		private var formatTimeOut: SimpleDateFormat? = null
		private var formatHoursOut: SimpleDateFormat? = null
		private val formatters: MutableList<SimpleDateFormat> = ArrayList()
		private const val PRECISION_SECOND = 0
		private const val PRECISION_MINUTE = 1
		private const val PRECISION_HOUR = 2
		private const val PRECISION_DAY = 3
		private const val PRECISION_MONTH = 4
		private const val PRECISION_YEAR = 5

		init {
			//This is allowable v24 format , we use UK Locale not because we are restricting to UK
			//but because these formats are fixed in ID3 spec, and could possibly get unexpected results if library
			//used with a default locale that has Date Format Symbols that interfere with the pattern
			formatters.add(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.UK))
			formatters.add(SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.UK))
			formatters.add(SimpleDateFormat("yyyy-MM-dd'T'HH", Locale.UK))
			formatters.add(SimpleDateFormat("yyyy-MM-dd", Locale.UK))
			formatters.add(SimpleDateFormat("yyyy-MM", Locale.UK))
			formatters.add(SimpleDateFormat("yyyy", Locale.UK))

			//These are formats used by v23 Frames
			formatYearIn = SimpleDateFormat("yyyy", Locale.UK)
			formatDateIn = SimpleDateFormat("ddMM", Locale.UK)
			formatTimeIn = SimpleDateFormat("HHmm", Locale.UK)

			//These are the separate components of the v24 format that the v23 formats map to
			formatYearOut = SimpleDateFormat("yyyy", Locale.UK)
			formatDateOut = SimpleDateFormat("-MM-dd", Locale.UK)
			formatMonthOut = SimpleDateFormat("-MM", Locale.UK)
			formatTimeOut = SimpleDateFormat("'T'HH:mm", Locale.UK)
			formatHoursOut = SimpleDateFormat("'T'HH", Locale.UK)
		}
		/**
		 * When this has been generated as an amalgamation of v3 frames assumes
		 * the v3 frames match the the format in specification and convert them
		 * to their equivalent v4 format and return the generated String.
		 * i.e if the v3 frames contain a valid value this will return a valid
		 * v4 value, if not this won't.
		 */
		/**
		 * Synchronized because SimpleDatFormat aren't thread safe
		 *
		 * @param formatDate
		 * @param parseDate
		 * @param text
		 * @return
		 */
		@Synchronized
		private fun formatAndParse(
			formatDate: SimpleDateFormat?,
			parseDate: SimpleDateFormat?,
			text: String?
		): String {
			try {
				val date = parseDate!!.parse(text)
				return formatDate!!.format(date)
			} catch (e: ParseException) {
				logger.warning(
					"Unable to parse:$text"
				)
			}
			return ""
		}

		/**
		 * Format Date
		 *
		 * Synchronized because SimpleDateFormat is invalid
		 *
		 * @param d
		 * @return
		 */
		@Synchronized
		private fun formatDateAsYear(d: Date): String {
			return formatYearIn!!.format(d)
		}

		/**
		 * Format Date
		 *
		 * Synchronized because SimpleDateFormat is invalid
		 *
		 * @param d
		 * @return
		 */
		@Synchronized
		private fun formatDateAsDate(d: Date): String {
			return formatDateIn!!.format(d)
		}

		/**
		 * Format Date
		 *
		 * Synchronized because SimpleDateFormat is invalid
		 *
		 * @param d
		 * @return
		 */
		@Synchronized
		private fun formatDateAsTime(d: Date): String {
			return formatTimeIn!!.format(d)
		}
	}
}
