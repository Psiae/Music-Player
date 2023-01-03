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
import java.util.*

/**
 * This class multicasts the events to multiple listener instances.<br></br>
 * Additionally the Vetos are handled. (other listeners are notified).
 *
 * @author Christian Laireiter
 */
class ModificationHandler : AudioFileModificationListener {
	/**
	 * The listeners to wich events are broadcasted are stored here.
	 */
	private val listeners = Vector<AudioFileModificationListener>()

	/**
	 * This method adds an [AudioFileModificationListener]
	 *
	 * @param l Listener to add.
	 */
	fun addAudioFileModificationListener(l: AudioFileModificationListener) {
		if (!listeners.contains(l)) {
			listeners.add(l)
		}
	}

	/**
	 * (overridden)
	 *
	 * @see AudioFileModificationListener.fileModified
	 */
	@Throws(ModifyVetoException::class)
	override fun fileModified(original: AudioFile?, temporary: File?) {
		for (listener in listeners) {
			try {
				listener.fileModified(original, temporary)
			} catch (e: ModifyVetoException) {
				vetoThrown(listener, original, e)
				throw e
			}
		}
	}

	/**
	 * (overridden)
	 *
	 * @see AudioFileModificationListener.fileOperationFinished
	 */
	override fun fileOperationFinished(result: File?) {
		for (listener in listeners) {
			listener.fileOperationFinished(result)
		}
	}

	/**
	 * (overridden)
	 *
	 * @see AudioFileModificationListener.fileWillBeModified
	 */
	@Throws(ModifyVetoException::class)
	override fun fileWillBeModified(file: AudioFile?, delete: Boolean) {
		for (listener in listeners) {
			try {
				listener.fileWillBeModified(file, delete)
			} catch (e: ModifyVetoException) {
				vetoThrown(listener, file, e)
				throw e
			}
		}
	}

	/**
	 * This method removes an [AudioFileModificationListener]
	 *
	 * @param l Listener to remove.
	 */
	fun removeAudioFileModificationListener(l: AudioFileModificationListener) {
		if (listeners.contains(l)) {
			listeners.remove(l)
		}
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
		for (listener in listeners) {
			listener.vetoThrown(cause, original, veto)
		}
	}
}
