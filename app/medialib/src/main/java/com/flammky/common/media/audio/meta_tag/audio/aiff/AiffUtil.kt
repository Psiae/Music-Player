package com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff

import java.io.IOException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility methods only of use for Aiff datatypes
 */
object AiffUtil {
	private val dateFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

	@Throws(IOException::class)
	fun read80BitDouble(chunkData: ByteBuffer): Double {
		val buf = ByteArray(10)
		chunkData[buf]
		val xd = ExtDouble(buf)
		return xd.toDouble()
	}

	/**
	 * Converts a Macintosh-style timestamp (seconds since
	 * January 1, 1904) into a Java date.  The timestamp is
	 * treated as a time in the default localization.
	 * Depending on that localization,
	 * there may be some variation in the exact hour of the date
	 * returned, e.g., due to daylight savings time.
	 */
	fun timestampToDate(timestamp: Long): Date {
		val cal = Calendar.getInstance()
		cal[1904, 0, 1, 0, 0] = 0

		// If we add the seconds directly, we'll truncate the long
		// value when converting to int.  So convert to hours plus
		// residual seconds.
		val hours = (timestamp / 3600).toInt()
		val seconds = (timestamp - hours.toLong() * 3600L).toInt()
		cal.add(Calendar.HOUR_OF_DAY, hours)
		cal.add(Calendar.SECOND, seconds)
		return cal.time
	}

	/**
	 * Format a date as text
	 */
	fun formatDate(dat: Date?): String {
		return dateFmt.format(dat)
	}
}
