/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Christian Laireiter <liree@web.de>
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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.AudioFile
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.ModifyVetoException
import java.io.File

/**
 *
 *
 * @author Christian Laireiter
 */
class AudioFileModificationAdapter : AudioFileModificationListener {
	/**
	 * (overridden)
	 *
	 * @see AudioFileModificationListener.fileModified
	 */
	@Throws(ModifyVetoException::class)
	override fun fileModified(original: AudioFile?, temporary: File?) {
		// Nothing to do
	}

	/**
	 * (overridden)
	 *
	 * @see AudioFileModificationListener.fileOperationFinished
	 */
	override fun fileOperationFinished(result: File?) {
		// Nothing to do
	}

	/**
	 * (overridden)
	 *
	 * @see AudioFileModificationListener.fileWillBeModified
	 */
	@Throws(ModifyVetoException::class)
	override fun fileWillBeModified(file: AudioFile?, delete: Boolean) {
		// Nothing to do
	}

	/**
	 * (overridden)
	 *
	 * @see AudioFileModificationListener.vetoThrown
	 */
	override fun vetoThrown(
		cause: AudioFileModificationListener?,
		original: AudioFile?,
		veto: ModifyVetoException?
	) {
		// Nothing to do
	}
}
