/*
 * Entagged Audio Tag library
 * Copyright (c) 2004-2005 Christian Laireiter <liree@web.de>
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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import java.math.BigInteger
import java.util.*

/**
 * This class stores the information about the file, which is contained within a
 * special chunk of ASF files.<br></br>
 *
 * @author Christian Laireiter
 */
class FileHeader(
	chunckLen: BigInteger?,
	/**
	 * Size of the file or stream.
	 */
	val fileSize: BigInteger, fileTime: BigInteger,
	/**
	 * Number of stream packages within the File.
	 */
	val packageCount: BigInteger,
	/**
	 * Duration of the media content in 100ns steps.
	 */
	val duration: BigInteger,
	/**
	 * Like [.timeEndPos]no Idea.
	 */
	val timeStartPos: BigInteger,
	/**
	 * No Idea of the Meaning, but stored anyway. <br></br>
	 * Source documentation says it is: "Timestamp of end position"
	 */
	val timeEndPos: BigInteger,
	/**
	 * Usually contains value of 2.
	 */
	val flags: Long,
	/**
	 * Minimun size of stream packages. <br></br>
	 * **Warning: ** must be same size as [.maxPackageSize]. Its not
	 * known how to handle deviating values.
	 */
	val minPackageSize: Long,
	/**
	 * Maximum size of stream packages. <br></br>
	 * **Warning: ** must be same size as [.minPackageSize]. Its not
	 * known how to handle deviating values.
	 */
	val maxPackageSize: Long,
	/**
	 * Size of an uncompressed video frame.
	 */
	val uncompressedFrameSize: Long
) : Chunk(GUID.GUID_FILE, chunckLen) {
	/**
	 * @return Returns the duration.
	 */

	/**
	 * The time the file was created.
	 */
	private val fileCreationTime: Date

	/**
	 * Creates an instance.
	 *
	 * @param chunckLen           Length of the file header (chunk)
	 * @param size                Size of file or stream
	 * @param fileTime            Time file or stream was created. Time is calculated since 1st
	 * january of 1601 in 100ns steps.
	 * @param pkgCount            Number of stream packages.
	 * @param dur                 Duration of media clip in 100ns steps
	 * @param timestampStart      Timestamp of start [.timeStartPos]
	 * @param timestampEnd        Timestamp of end [.timeEndPos]
	 * @param headerFlags         some stream related flags.
	 * @param minPkgSize          minimum size of packages
	 * @param maxPkgSize          maximum size of packages
	 * @param uncmpVideoFrameSize Size of an uncompressed Video Frame.
	 */
	init {
		fileCreationTime = Utils.getDateOf(fileTime).time
	}

	/**
	 * This method converts [.getDuration]from 100ns steps to normal
	 * seconds.
	 *
	 * @return Duration of the media in seconds.
	 */
	val durationInSeconds: Int
		get() = duration.divide(BigInteger("10000000")).toInt()

	/**
	 * @return Returns the fileCreationTime.
	 */
	fun getFileCreationTime(): Date {
		return Date(fileCreationTime.time)
	}

	/**
	 * This method converts [.getDuration] from 100ns steps to normal
	 * seconds with a fractional part taking milliseconds.<br></br>
	 *
	 * @return The duration of the media in seconds (with a precision of
	 * milliseconds)
	 */
	val preciseDuration: Float
		get() = (duration.toDouble() / 10000000.0).toFloat()

	/**
	 * (overridden)
	 *
	 * @see Chunk.prettyPrint
	 */
	override fun prettyPrint(prefix: String): String {
		val result = StringBuilder(super.prettyPrint(prefix))
		result.append(prefix).append("  |-> Filesize      = ").append(fileSize.toString())
			.append(" Bytes").append(
				Utils.LINE_SEPARATOR
			)
		result.append(prefix).append("  |-> Media duration= ")
			.append(duration.divide(BigInteger("10000")).toString()).append(" ms").append(
				Utils.LINE_SEPARATOR
			)
		result.append(prefix).append("  |-> Created at    = ").append(getFileCreationTime()).append(
			Utils.LINE_SEPARATOR
		)
		return result.toString()
	}
}
