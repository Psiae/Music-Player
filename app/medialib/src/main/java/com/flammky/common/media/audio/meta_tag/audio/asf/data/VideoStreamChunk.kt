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

/**
 * @author Christian Laireiter
 */
class VideoStreamChunk
/**
 * Creates an instance.
 *
 * @param chunkLen Length of the entire chunk (including guid and size)
 */
	(chunkLen: BigInteger?) : StreamChunk(GUID.GUID_VIDEOSTREAM, chunkLen) {
	/**
	 * Stores the codecs id. Normally the Four-CC (4-Bytes).
	 */
	private var codecId: ByteArray? = ByteArray(0)
	/**
	 * @return Returns the pictureHeight.
	 */
	/**
	 * @param picHeight
	 */
	/**
	 * This field stores the height of the video stream.
	 */
	var pictureHeight: Long = 0
	/**
	 * @return Returns the pictureWidth.
	 */
	/**
	 * @param picWidth
	 */
	/**
	 * This field stores the width of the video stream.
	 */
	var pictureWidth: Long = 0

	/**
	 * @return Returns the codecId.
	 */
	fun getCodecId(): ByteArray {
		return codecId!!.clone()
	}

	/**
	 * Returns the [.getCodecId], as a String, where each byte has been
	 * converted to a `char`.
	 *
	 * @return Codec Id as String.
	 */
	val codecIdAsString: String
		get() {
			val result: String
			result = if (codecId == null) {
				"Unknown"
			} else {
				String(getCodecId())
			}
			return result
		}

	/**
	 * (overridden)
	 *
	 * @see StreamChunk.prettyPrint
	 */
	override fun prettyPrint(prefix: String): String {
		val result = StringBuilder(super.prettyPrint(prefix))
		result.insert(0, Utils.LINE_SEPARATOR + prefix + "|->VideoStream")
		result.append(prefix).append("Video info:").append(Utils.LINE_SEPARATOR)
		result.append(prefix).append("      |->Width  : ").append(pictureWidth)
			.append(Utils.LINE_SEPARATOR)
		result.append(prefix).append("      |->Heigth : ").append(pictureHeight)
			.append(Utils.LINE_SEPARATOR)
		result.append(prefix).append("      |->Codec  : ").append(codecIdAsString)
			.append(Utils.LINE_SEPARATOR)
		return result.toString()
	}

	/**
	 * @param codecIdentifier The codecId to set.
	 */
	fun setCodecId(codecIdentifier: ByteArray) {
		codecId = codecIdentifier.clone()
	}
}
