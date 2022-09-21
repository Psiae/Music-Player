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
 * Classes implementing this interface will be notified on audio file's
 * modifications.<br></br>
 *
 *
 * It will be notified on several occasions:<br></br>
 *
 *  * An audio file is about to be modified
 * [.fileWillBeModified]<br></br>
 * Here one can modify the tag data because of global settings.
 *  * The write process has just finished. But if a copy was created the
 * original has not been replaced yet. ([.fileModified]).
 *  * The operation has been finished. [.fileOperationFinished]
 *
 *
 * @author Christian Laireiter <liree></liree>@web.de>
 */
interface AudioFileModificationListener {
	/**
	 * Notifies that `original` has been processed.<br></br>
	 * Because the audiolibrary allows format implementors to either change the
	 * original file or create a copy, it is possible that the real result is
	 * located in the original and `temporary` is of zero size
	 * **or** the original will be deleted and replaced by temporary.<br></br>
	 *
	 * @param original  The original file on which the operation was started.
	 * @param temporary The modified copy. (It may be of zero size if the original was
	 * modified)
	 * @throws ModifyVetoException If the Results doesn't fit the expectations of the listener,
	 * it can prevent the replacement of the original by temporary.<br></br>
	 * If the original is already modified, this exception results
	 * in nothing.
	 */
	@Throws(ModifyVetoException::class)
	fun fileModified(original: AudioFile?, temporary: File?)

	/**
	 * Informs the listener that the process has been finished.<br></br>
	 * The given file is either the original file or the modified copy.<br></br>
	 *
	 * @param result The remaining file. It's not of [AudioFile] since it may
	 * be possible that a new file was created. In that case the
	 * audiolibs would need to parse the file again, which leads to
	 * long and unnecessary operation time, if the tag data is not
	 * needed any more.
	 */
	fun fileOperationFinished(result: File?)

	/**
	 * Notifies that the `file` is about to be modified.
	 *
	 * @param file   The file that will be modified.
	 * @param delete `true` if the deletion of tag data will be
	 * performed.
	 * @throws ModifyVetoException Thrown if the listener wants to prevent the process.
	 */
	@Throws(ModifyVetoException::class)
	fun fileWillBeModified(file: AudioFile?, delete: Boolean)

	/**
	 * This method notifies about a veto exception that has been thrown by
	 * another listener.<br></br>
	 *
	 * @param cause    The instance which caused the veto.
	 * @param original The original file, that was about to be modified.
	 * @param veto     The thrown exception.
	 */
	fun vetoThrown(
		cause: AudioFileModificationListener?,
		original: AudioFile?,
		veto: ModifyVetoException?
	)
}
