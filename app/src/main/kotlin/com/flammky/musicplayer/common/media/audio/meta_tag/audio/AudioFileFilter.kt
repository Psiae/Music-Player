/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Raphaël Slinckx <raphael@slinckx.net>
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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getExtension
import java.io.File
import java.io.FileFilter
import java.util.*

/**
 *
 * This is a simple FileFilter that will only allow the file supported by this library.
 *
 * It will also accept directories. An additional condition is that file must be readable (read permission) and
 * are not hidden (dot files, or hidden files)
 *
 * @author Raphael Slinckx
 * @version $Id$
 * @since v0.01
 */
class AudioFileFilter @JvmOverloads constructor(
	/**
	 * allows Directories
	 */
	private val allowDirectories: Boolean = true
) : FileFilter {
	/**
	 *
	 * Check whether the given file meet the required conditions (supported by the library OR directory).
	 * The File must also be readable and not hidden.
	 *
	 * @param    f    The file to test
	 * @return a boolean indicating if the file is accepted or not
	 */
	override fun accept(f: File): Boolean {
		if (f.isHidden || !f.canRead()) {
			return false
		}
		if (f.isDirectory) {
			return allowDirectories
		}
		val ext = getExtension(f)
		try {
			if (SupportedFileFormat.valueOf(ext.uppercase(Locale.getDefault())) != null) {
				return true
			}
		} catch (iae: IllegalArgumentException) {
			//Not known enum value
			return false
		}
		return false
	}
}
