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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic

import com.flammky.android.core.sdk.VersionHelper
import com.flammky.common.media.audio.meta_tag.audio.generic.Permissions
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.AudioFile
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.InvalidAudioFrameException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.NoReadPermissionsException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.ReadOnlyFileException
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagException
import java.io.*
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.util.logging.Level
import java.util.logging.Logger

/*
 * This abstract class is the skeleton for tag readers. It handles the creation/closing of
 * the randomAccessFile objects and then call the subclass method getEncodingInfo and getTag.
 * These two method have to be implemented in the subclass.
 *
 *@author	Raphael Slinckx
 *@version	$Id$
 *@since	v0.02
 */
abstract class AudioFileReader {
	/*
	* Returns the encoding info object associated wih the current File.
	* The subclass can assume the RAF pointer is at the first byte of the file.
	* The RandomAccessFile must be kept open after this function, but can point
	* at any offset in the file.
	*
	* @param raf The RandomAccessFile associtaed with the current file
	* @exception IOException is thrown when the RandomAccessFile operations throw it (you should never throw them manually)
	* @exception CannotReadException when an error occured during the parsing of the encoding infos
	*/
	@Throws(CannotReadException::class, IOException::class)
	protected abstract fun getEncodingInfo(raf: RandomAccessFile): GenericAudioHeader

	protected abstract fun getEncodingInfo(fc: FileChannel): GenericAudioHeader

	/*
		* Same as above but returns the Tag contained in the file, or a new one.
		*
		* @param raf The RandomAccessFile associted with the current file
		* @exception IOException is thrown when the RandomAccessFile operations throw it (you should never throw them manually)
		* @exception CannotReadException when an error occured during the parsing of the tag
		*/
	@Throws(CannotReadException::class, IOException::class)
	protected abstract fun getTag(raf: RandomAccessFile): Tag?

	protected abstract fun getTag(fc: FileChannel): Tag?

	/*
		* Reads the given file, and return an AudioFile object containing the Tag
		* and the encoding infos present in the file. If the file has no tag, an
		* empty one is returned. If the encodinginfo is not valid , an exception is thrown.
		*
		* @param f The file to read
		* @exception CannotReadException If anything went bad during the read of this file
		*/
	@Throws(
		CannotReadException::class,
		IOException::class,
		TagException::class,
		ReadOnlyFileException::class,
		InvalidAudioFrameException::class
	)
	open fun read(f: File): AudioFile {
		if (VersionHelper.hasOreo()) {
			if (!Files.isReadable(f.toPath())) {
				if (!Files.exists(f.toPath())) {
					throw FileNotFoundException(ErrorMessage.UNABLE_TO_FIND_FILE.getMsg(f.toPath()))
				} else {
					logger.warning(Permissions.displayPermissions(f.toPath()))
					throw NoReadPermissionsException(
						ErrorMessage.GENERAL_READ_FAILED_DO_NOT_HAVE_PERMISSION_TO_READ_FILE.getMsg(
							f.toPath()
						)
					)
				}
			}
		} else {
			if (!f.canRead()) if (!f.exists()) {
				throw FileNotFoundException(ErrorMessage.UNABLE_TO_FIND_FILE.getMsg(f.toPath()))
			} else {
				logger.warning(Permissions.displayPermissions(f.toPath()))
				throw NoReadPermissionsException(
					ErrorMessage.GENERAL_READ_FAILED_DO_NOT_HAVE_PERMISSION_TO_READ_FILE.getMsg(f.toPath())
				)
			}
		}


		if (f.length() <= MINIMUM_SIZE_FOR_VALID_AUDIO_FILE) {
			throw CannotReadException(ErrorMessage.GENERAL_READ_FAILED_FILE_TOO_SMALL.getMsg(f.absolutePath))
		}
		var raf: RandomAccessFile? = null
		return try {
			raf = RandomAccessFile(f, "r")
			raf.seek(0)
			val info = getEncodingInfo(raf)
			raf.seek(0)
			val tag = getTag(raf)
			AudioFile(f, info, tag)
		} catch (cre: CannotReadException) {
			throw cre
		} catch (e: Exception) {
			logger.log(Level.SEVERE, ErrorMessage.GENERAL_READ.getMsg(f.absolutePath), e)
			throw CannotReadException(f.absolutePath + ":" + e.message, e)
		} finally {
			try {
				raf?.close()
			} catch (ex: Exception) {
				logger.log(
					Level.WARNING,
					ErrorMessage.GENERAL_READ_FAILED_UNABLE_TO_CLOSE_RANDOM_ACCESS_FILE.getMsg(f.absolutePath)
				)
			}
		}
	}

	open fun read(fileDescriptor: FileDescriptor): AudioFile {

		return FileInputStream(fileDescriptor).use { fis ->
			val avail = fis.channel.size()
			if (avail <= MINIMUM_SIZE_FOR_VALID_AUDIO_FILE) {
				throw CannotReadException(ErrorMessage.GENERAL_READ_FAILED_FILE_TOO_SMALL.getMsg(avail.toString()))
			}
			val channel = fis.channel.position(0)
			val info = getEncodingInfo(channel)
			channel.position(0)
			val tag = getTag(channel)
			AudioFile(fileDescriptor, info, tag)
		}
	}

	open fun read(fc: FileChannel): AudioFile {
		fc.position(0)
		val avail = fc.size()
		if (avail <= MINIMUM_SIZE_FOR_VALID_AUDIO_FILE) {
			throw CannotReadException(ErrorMessage.GENERAL_READ_FAILED_FILE_TOO_SMALL.getMsg(avail.toString()))
		}
		fc.position(0)
		val info = getEncodingInfo(fc)
		fc.position(0)
		val tag = getTag(fc)
		return AudioFile(fc, info, tag)
	}

	companion object {
		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.audio.generic")

		@JvmStatic
		protected val MINIMUM_SIZE_FOR_VALID_AUDIO_FILE = 100
	}
}
