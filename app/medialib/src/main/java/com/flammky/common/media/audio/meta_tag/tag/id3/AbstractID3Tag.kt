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
 * Base class for all ID3 tags
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3

import java.util.logging.Logger

/**
 * This is the abstract base class for all ID3 tags.
 *
 * @author : Eric Farng
 * @author : Paul Taylor
 */
abstract class AbstractID3Tag : AbstractTag {
	constructor()
	/**
	 * Retrieve the logging filename to be used in debugging
	 *
	 * @return logging filename to be used in debugging
	 */
	/**
	 * Set logging filename when construct tag for read from file
	 *
	 * @param loggingFilename
	 */
	//The purpose of this is to provide the filename that should be used when writing debug messages
	//when problems occur reading or writing to file, otherwise it is difficult to track down the error
	//when processing many files
	var loggingFilename = ""

	override val identifier: String?
		get() = TAG_RELEASE + release + "." + majorVersion + "." + revision

	/**
	 * Retrieve the Release
	 *
	 * @return
	 */
	abstract val release: Byte

	/**
	 * Retrieve the Major Version
	 *
	 * @return
	 */
	abstract val majorVersion: Byte

	/**
	 * Retrieve the Revision
	 *
	 * @return
	 */
	abstract val revision: Byte

	constructor(copyObject: AbstractID3Tag?) : super(copyObject)

	companion object {
		//Logger
		@JvmField
		var logger = Logger.getLogger("org.jaudiotagger.tag.id3")
		protected const val TAG_RELEASE = "ID3v"
	}
}
