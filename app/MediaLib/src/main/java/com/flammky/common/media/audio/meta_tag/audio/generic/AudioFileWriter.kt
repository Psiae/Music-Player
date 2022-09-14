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
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotWriteException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.ModifyVetoException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp3.MP3File
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagOptionSingleton
import java.io.*
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.util.logging.Level
import java.util.logging.Logger

/**
 * This abstract class is the skeleton for tag writers.
 *
 *
 *
 *
 * It handles the creation/closing of the randomaccessfile objects and then call
 * the subclass method writeTag or deleteTag. These two method have to be
 * implemented in the subclass.
 *
 * @author Raphael Slinckx
 * @version $Id: AudioFileWriter.java,v 1.21 2009/05/05 15:59:14 paultaylor Exp
 * $
 * @since v0.02
 */
abstract class AudioFileWriter {
	/**
	 * If not `null`, this listener is used to notify the listener
	 * about modification events.<br></br>
	 */
	private var modificationListener: AudioFileModificationListener? = null

	/**
	 * Delete the tag (if any) present in the given file
	 *
	 * @param af The file to process
	 * @throws CannotWriteException                                  if anything went wrong
	 * @throws CannotReadException
	 * @throws UnsupportedOperationException
	 */
	@Throws(
		CannotReadException::class,
		CannotWriteException::class,
		UnsupportedOperationException::class
	)
	open fun delete(af: AudioFile) {
		if (!VersionHelper.hasOreo()) throw UnsupportedOperationException()

		val file = af.mFile!!.toPath()
		if (TagOptionSingleton.instance.isCheckIsWritable && !Files.isWritable(file)) {
			logger.severe(Permissions.displayPermissions(file))
			throw CannotWriteException(ErrorMessage.GENERAL_DELETE_FAILED.getMsg(file))
		}
		if (af.mFile!!.length() <= MINIMUM_FILESIZE) {
			throw CannotWriteException(
				ErrorMessage.GENERAL_DELETE_FAILED_BECAUSE_FILE_IS_TOO_SMALL.getMsg(
					file
				)
			)
		}
		var raf: RandomAccessFile? = null
		var rafTemp: RandomAccessFile? = null
		var tempF: File? = null

		// Will be set to true on VetoException, causing the finally block to
		// discard the tempfile.
		var revert = false
		try {
			tempF = File.createTempFile(
				af.mFile!!.name.replace('.', '_'),
				TEMP_FILENAME_SUFFIX,
				af.mFile!!.parentFile
			)
			rafTemp = RandomAccessFile(tempF, WRITE_MODE)
			raf = RandomAccessFile(af.mFile, WRITE_MODE)
			raf.seek(0)
			rafTemp.seek(0)
			try {
				if (modificationListener != null) {
					modificationListener!!.fileWillBeModified(af, true)
				}
				deleteTag(af.tag, raf, rafTemp)
				if (modificationListener != null) {
					modificationListener!!.fileModified(af, tempF)
				}
			} catch (veto: ModifyVetoException) {
				throw CannotWriteException(veto)
			}
		} catch (e: Exception) {
			revert = true
			throw CannotWriteException("\"" + af.mFile!!.absolutePath + "\" :" + e, e)
		} finally {
			// will be set to the remaining file.
			var result = af.mFile
			try {
				raf?.close()
				rafTemp?.close()
				if (tempF!!.length() > 0 && !revert) {
					val deleteResult = af.mFile!!.delete()
					if (!deleteResult) {
						logger.warning(
							ErrorMessage.GENERAL_WRITE_FAILED_TO_DELETE_ORIGINAL_FILE.getMsg(
								af.mFile!!.path,
								tempF.path
							)
						)
						throw CannotWriteException(
							ErrorMessage.GENERAL_WRITE_FAILED_TO_DELETE_ORIGINAL_FILE.getMsg(
								af.mFile!!.path,
								tempF.path
							)
						)
					}
					val renameResult = tempF.renameTo(af.mFile!!)
					if (!renameResult) {
						logger.warning(
							ErrorMessage.GENERAL_WRITE_FAILED_TO_RENAME_TO_ORIGINAL_FILE.getMsg(
								af.mFile!!.path,
								tempF.path
							)
						)
						throw CannotWriteException(
							ErrorMessage.GENERAL_WRITE_FAILED_TO_RENAME_TO_ORIGINAL_FILE.getMsg(
								af.mFile!!.path,
								tempF.path
							)
						)
					}
					result = tempF

					// If still exists we can now delete
					if (tempF.exists()) {
						if (!tempF.delete()) {
							// Non critical failed deletion
							logger.warning(
								ErrorMessage.GENERAL_WRITE_FAILED_TO_DELETE_TEMPORARY_FILE.getMsg(
									tempF.path
								)
							)
						}
					}
				} else {
					// It was created but never used
					if (!tempF.delete()) {
						// Non critical failed deletion
						logger.warning(
							ErrorMessage.GENERAL_WRITE_FAILED_TO_DELETE_TEMPORARY_FILE.getMsg(
								tempF.path
							)
						)
					}
				}
			} catch (ex: Exception) {
				logger.severe(
					"AudioFileWriter exception cleaning up delete:" + af.mFile!!.path +
						" or" + tempF!!.absolutePath + ":" + ex
				)
			}
			// Notify listener
			if (modificationListener != null) {
				modificationListener!!.fileOperationFinished(result)
			}
		}
	}

	/**
	 * Delete the tag (if any) present in the given randomaccessfile, and do not
	 * close it at the end.
	 *
	 * @param tag
	 * @param raf     The source file, already opened in r-write mode
	 * @param tempRaf The temporary file opened in r-write mode
	 * @throws CannotWriteException                                  if anything went wrong
	 * @throws CannotReadException
	 * @throws IOException
	 */
	@Throws(CannotReadException::class, CannotWriteException::class, IOException::class)
	fun delete(tag: Tag?, raf: RandomAccessFile, tempRaf: RandomAccessFile) {
		raf.seek(0)
		tempRaf.seek(0)
		deleteTag(tag, raf, tempRaf)
	}

	/**
	 * Same as above, but delete tag in the file.
	 *
	 * @param tag
	 * @param raf
	 * @param tempRaf
	 * @throws IOException                                           is thrown when the RandomAccessFile operations throw it (you
	 * should never throw them manually)
	 * @throws CannotWriteException                                  when an error occured during the deletion of the tag
	 * @throws CannotReadException
	 */
	@Throws(CannotReadException::class, CannotWriteException::class, IOException::class)
	protected abstract fun deleteTag(tag: Tag?, raf: RandomAccessFile?, tempRaf: RandomAccessFile?)

	/**
	 * This method sets the [AudioFileModificationListener].<br></br>
	 * There is only one listener allowed, if you want more instances to be
	 * supported, use the [ModificationHandler] to broadcast those events.<br></br>
	 *
	 * @param listener The listener. `null` allowed to deregister.
	 */
	fun setAudioFileModificationListener(listener: AudioFileModificationListener?) {
		modificationListener = listener
	}

	/**
	 * Prechecks before normal write
	 *
	 *
	 *
	 *  * If the tag is actually empty, remove the tag
	 *  * if the file is not writable, throw exception
	 *  *
	 *  * If the file is too small to be a valid file, throw exception
	 *  *
	 *
	 *
	 * @param af
	 * @throws CannotWriteException
	 */
	@Throws(CannotWriteException::class, UnsupportedOperationException::class)
	private fun preCheckWrite(af: AudioFile) {
		if (!VersionHelper.hasOreo()) throw UnsupportedOperationException()

		// Preliminary checks
		try {
			if (af.tag!!.isEmpty) {
				delete(af)
				return
			}
		} catch (re: CannotReadException) {
			throw CannotWriteException(ErrorMessage.GENERAL_WRITE_FAILED.getMsg(af.mFile!!.path))
		}
		val file = af.mFile!!.toPath()
		if (TagOptionSingleton.instance.isCheckIsWritable && !Files.isWritable(file)) {
			logger.severe(Permissions.displayPermissions(file))
			logger.severe(ErrorMessage.GENERAL_WRITE_FAILED.getMsg(af.mFile!!.path))
			throw CannotWriteException(
				ErrorMessage.GENERAL_WRITE_FAILED_TO_OPEN_FILE_FOR_EDITING.getMsg(
					file
				)
			)
		}
		if (af.mFile!!.length() <= MINIMUM_FILESIZE) {
			logger.severe(ErrorMessage.GENERAL_WRITE_FAILED_BECAUSE_FILE_IS_TOO_SMALL.getMsg(file))
			throw CannotWriteException(
				ErrorMessage.GENERAL_WRITE_FAILED_BECAUSE_FILE_IS_TOO_SMALL.getMsg(
					file
				)
			)
		}
	}

	/**
	 * Write the tag (if not empty) present in the AudioFile in the associated
	 * File
	 *
	 * @param af The file we want to process
	 * @throws CannotWriteException if anything went wrong
	 */
	// TODO Creates temp file in same folder as the original file, this is safe
	// but would impose a performance overhead if the original file is on a networked drive
	@Throws(CannotWriteException::class)
	open fun write(af: AudioFile) {
		logger.config("Started writing tag data for file:" + af.mFile!!.name)

		// Prechecks
		preCheckWrite(af)

		//mp3's use a different mechanism to the other formats
		if (af is MP3File) {
			af.commit()
			return
		}
		var raf: RandomAccessFile? = null
		var rafTemp: RandomAccessFile? = null
		val newFile: File
		val result: File

		// Create temporary File
		newFile = try {
			File.createTempFile(
				af.mFile!!.name.replace('.', '_'),
				TEMP_FILENAME_SUFFIX,
				af.mFile!!.parentFile
			)
		} // Unable to create temporary file, can happen in Vista if have Create
		// Files/Write Data set to Deny
		catch (ioe: IOException) {
			if (ioe.message == FILE_NAME_TOO_LONG && af.mFile!!.name.length > FILE_NAME_TOO_LONG_SAFE_LIMIT) {
				try {
					File.createTempFile(
						af.mFile!!.name.substring(0, FILE_NAME_TOO_LONG_SAFE_LIMIT).replace('.', '_'),
						TEMP_FILENAME_SUFFIX,
						af.mFile!!.parentFile
					)
				} catch (ioe2: IOException) {
					logger.log(
						Level.SEVERE,
						ErrorMessage.GENERAL_WRITE_FAILED_TO_CREATE_TEMPORARY_FILE_IN_FOLDER.getMsg(
							af.mFile!!.name,
							af.mFile!!.parentFile.absolutePath
						),
						ioe2
					)
					throw CannotWriteException(
						ErrorMessage.GENERAL_WRITE_FAILED_TO_CREATE_TEMPORARY_FILE_IN_FOLDER.getMsg(
							af.mFile!!.name,
							af.mFile!!.parentFile.absolutePath
						)
					)
				}
			} else {
				logger.log(
					Level.SEVERE,
					ErrorMessage.GENERAL_WRITE_FAILED_TO_CREATE_TEMPORARY_FILE_IN_FOLDER.getMsg(
						af.mFile!!.name,
						af.mFile!!.parentFile.absolutePath
					),
					ioe
				)
				throw CannotWriteException(
					ErrorMessage.GENERAL_WRITE_FAILED_TO_CREATE_TEMPORARY_FILE_IN_FOLDER.getMsg(
						af.mFile!!.name,
						af.mFile!!.parentFile.absolutePath
					)
				)
			}
		}

		// Open temporary file and actual file for editing
		try {
			rafTemp = RandomAccessFile(newFile, WRITE_MODE)
			raf = RandomAccessFile(af.mFile, WRITE_MODE)
		} // Unable to write to writable file, can happen in Vista if have Create
		// Folders/Append Data set to Deny
		catch (ioe: IOException) {
			logger.log(
				Level.SEVERE,
				ErrorMessage.GENERAL_WRITE_FAILED_TO_OPEN_FILE_FOR_EDITING.getMsg(af.mFile!!.absolutePath),
				ioe
			)

			// If we managed to open either file, delete it.
			try {
				raf?.close()
				rafTemp?.close()
			} catch (ioe2: IOException) {
				// Warn but assume has worked okay
				logger.log(
					Level.WARNING,
					ErrorMessage.GENERAL_WRITE_PROBLEM_CLOSING_FILE_HANDLE.getMsg(
						af.mFile,
						ioe.message
					),
					ioe2
				)
			}

			// Delete the temp file ( we cannot delete until closed corresponding
			// rafTemp)
			if (!newFile.delete()) {
				// Non critical failed deletion
				logger.warning(
					ErrorMessage.GENERAL_WRITE_FAILED_TO_DELETE_TEMPORARY_FILE.getMsg(
						newFile.absolutePath
					)
				)
			}
			throw CannotWriteException(
				ErrorMessage.GENERAL_WRITE_FAILED_TO_OPEN_FILE_FOR_EDITING
					.getMsg(af.mFile!!.absolutePath)
			)
		}

		// Write data to File
		try {
			raf.seek(0)
			rafTemp.seek(0)
			try {
				if (modificationListener != null) {
					modificationListener!!.fileWillBeModified(af, false)
				}
				writeTag(af, af.tag!!, raf, rafTemp)
				if (modificationListener != null) {
					modificationListener!!.fileModified(af, newFile)
				}
			} catch (veto: ModifyVetoException) {
				throw CannotWriteException(veto)
			}
		} catch (e: Exception) {
			logger.log(
				Level.SEVERE,
				ErrorMessage.GENERAL_WRITE_FAILED_BECAUSE.getMsg(af.mFile, e.message),
				e
			)
			try {
				if (raf != null) {
					raf.close()
				}
				if (rafTemp != null) {
					rafTemp.close()
				}
			} catch (ioe: IOException) {
				// Warn but assume has worked okay
				logger.log(
					Level.WARNING,
					ErrorMessage.GENERAL_WRITE_PROBLEM_CLOSING_FILE_HANDLE.getMsg(
						af.mFile!!.absolutePath,
						ioe.message
					),
					ioe
				)
			}

			// Delete the temporary file because either it was never used so
			// lets just tidy up or we did start writing to it but
			// the write failed and we havent renamed it back to the original
			// file so we can just delete it.
			if (!newFile.delete()) {
				// Non critical failed deletion
				logger.warning(
					ErrorMessage.GENERAL_WRITE_FAILED_TO_DELETE_TEMPORARY_FILE.getMsg(
						newFile.absolutePath
					)
				)
			}
			throw CannotWriteException(
				ErrorMessage.GENERAL_WRITE_FAILED_BECAUSE.getMsg(
					af.mFile,
					e.message
				)
			)
		} finally {
			try {
				if (raf != null) {
					raf.close()
				}
				if (rafTemp != null) {
					rafTemp.close()
				}
			} catch (ioe: IOException) {
				// Warn but assume has worked okay
				logger.log(
					Level.WARNING,
					ErrorMessage.GENERAL_WRITE_PROBLEM_CLOSING_FILE_HANDLE.getMsg(
						af.mFile!!.absolutePath,
						ioe.message
					),
					ioe
				)
			}
		}

		// Result held in this file
		result = af.mFile!!

		// If the temporary file was used
		if (newFile.length() > 0) {
			transferNewFileToOriginalFile(
				newFile,
				af.mFile!!,
				TagOptionSingleton.instance.isPreserveFileIdentity
			)
		} else {
			// Delete the temporary file that wasn't ever used
			if (!newFile.delete()) {
				// Non critical failed deletion
				logger.warning(
					ErrorMessage.GENERAL_WRITE_FAILED_TO_DELETE_TEMPORARY_FILE.getMsg(
						newFile.path
					)
				)
			}
		}
		if (modificationListener != null) {
			modificationListener!!.fileOperationFinished(result)
		}
	}

	/**
	 *
	 *
	 * Transfers the content from `newFile` to a file named `originalFile`.
	 * With regards to file identity (inode/[fileIndex](https://msdn.microsoft.com/en-us/library/aa363788(v=vs.85).aspx)),
	 * after execution, `originalFile` may be a completely new file or the same file as before execution, depending
	 * on `reuseExistingOriginalFile`.
	 *
	 *
	 *
	 * Reusing the existing file may be slower, if both the temp file and the original file are located
	 * in the same filesystem, because an actual copy is created instead of just a file rename.
	 * If both files are on different filesystems, a copy is always needed — regardless of which method is used.
	 *
	 *
	 * @param newFile                   new file
	 * @param originalFile              original file
	 * @param reuseExistingOriginalFile `true` or `false`
	 * @throws CannotWriteException If the file cannot be written
	 */
	@Throws(CannotWriteException::class)
	private fun transferNewFileToOriginalFile(
		newFile: File,
		originalFile: File,
		reuseExistingOriginalFile: Boolean
	) {
		if (reuseExistingOriginalFile) {
			transferNewFileContentToOriginalFile(newFile, originalFile)
		} else {
			transferNewFileToNewOriginalFile(newFile, originalFile)
		}
	}

	/**
	 *
	 *
	 * Writes the contents of the given `newFile` to the given `originalFile`,
	 * overwriting the already existing content in `originalFile`.
	 * This ensures that the file denoted by the abstract pathname `originalFile`
	 * keeps the same Unix inode or Windows
	 * [fileIndex](https://msdn.microsoft.com/en-us/library/aa363788(v=vs.85).aspx).
	 *
	 *
	 *
	 * If no errors occur, the method follows this approach:
	 *
	 *
	 *  1. Rename `originalFile` to `originalFile.old`
	 *  1. Rename `newFile` to `originalFile` (this implies a file identity change for `originalFile`)
	 *  1. Delete `originalFile.old`
	 *  1. Delete `newFile`
	 *
	 *
	 * @param newFile      File containing the data we want in the `originalFile`
	 * @param originalFile Before execution this denotes the original, unmodified file.
	 * After execution it denotes the name of the file with the modified content and new inode/fileIndex.
	 * @throws CannotWriteException if the file cannot be written
	 */
	@Throws(CannotWriteException::class)
	private fun transferNewFileContentToOriginalFile(newFile: File, originalFile: File) {
		// try to obtain exclusive lock on the file
		try {
			RandomAccessFile(originalFile, "rw").use { raf ->
				val outChannel = raf.channel
				try {
					outChannel.tryLock().use { lock ->
						if (lock != null) {
							transferNewFileContentToOriginalFile(
								newFile,
								originalFile,
								raf,
								outChannel
							)
						} else {
							// we didn't get a lock
							logger.warning(
								ErrorMessage.GENERAL_WRITE_FAILED_FILE_LOCKED.getMsg(
									originalFile.path
								)
							)
							throw CannotWriteException(
								ErrorMessage.GENERAL_WRITE_FAILED_FILE_LOCKED.getMsg(
									originalFile.path
								)
							)
						}
					}
				} catch (e: IOException) {
					logger.warning(ErrorMessage.GENERAL_WRITE_FAILED_FILE_LOCKED.getMsg(originalFile.path))
					// we didn't get a lock, this may be, because locking is not supported by the OS/JRE
					// this can happen on OS X with network shares (samba, afp)
					// for details see https://stackoverflow.com/questions/33220148/samba-share-gradle-java-io-exception
					// coarse check that works on OS X:
					if ("Operation not supported" == e.message) {
						// transfer without lock
						transferNewFileContentToOriginalFile(newFile, originalFile, raf, outChannel)
					} else {
						throw CannotWriteException(
							ErrorMessage.GENERAL_WRITE_FAILED_FILE_LOCKED.getMsg(
								originalFile.path
							), e
						)
					}
				} catch (e: Exception) {
					// tryLock failed for some reason other than an IOException — we're definitely doomed
					logger.warning(ErrorMessage.GENERAL_WRITE_FAILED_FILE_LOCKED.getMsg(originalFile.path))
					throw CannotWriteException(
						ErrorMessage.GENERAL_WRITE_FAILED_FILE_LOCKED.getMsg(
							originalFile.path
						), e
					)
				}
			}
		} catch (e: FileNotFoundException) {
			logger.warning(
				ErrorMessage.GENERAL_WRITE_FAILED_BECAUSE_FILE_NOT_FOUND.getMsg(
					originalFile.absolutePath
				)
			)
			throw CannotWriteException(
				ErrorMessage.GENERAL_WRITE_FAILED_BECAUSE_FILE_NOT_FOUND.getMsg(
					originalFile.path
				), e
			)
		} catch (e: Exception) {
			logger.warning(ErrorMessage.GENERAL_WRITE_FAILED.getMsg(originalFile.absolutePath))
			throw CannotWriteException(
				ErrorMessage.GENERAL_WRITE_FAILED.getMsg(originalFile.path),
				e
			)
		}
	}

	@Throws(CannotWriteException::class)
	private fun transferNewFileContentToOriginalFile(
		newFile: File,
		originalFile: File,
		raf: RandomAccessFile,
		outChannel: FileChannel
	) {
		try {
			FileInputStream(newFile).use { fileInputStream ->
				val inChannel = fileInputStream.channel
				// copy contents of newFile to originalFile,
				// overwriting the old content in that file
				val size = inChannel.size()
				var position: Long = 0
				while (position < size) {
					position += inChannel.transferTo(position, 1024L * 1024L, outChannel)
				}
				// truncate raf, in case it used to be longer
				raf.setLength(size)
			}
		} catch (e: FileNotFoundException) {
			logger.warning(ErrorMessage.GENERAL_WRITE_FAILED_NEW_FILE_DOESNT_EXIST.getMsg(newFile.absolutePath))
			throw CannotWriteException(
				ErrorMessage.GENERAL_WRITE_FAILED_NEW_FILE_DOESNT_EXIST.getMsg(
					newFile.name
				), e
			)
		} catch (e: IOException) {
			logger.warning(
				ErrorMessage.GENERAL_WRITE_FAILED_TO_RENAME_TO_ORIGINAL_FILE.getMsg(
					originalFile.absolutePath,
					newFile.name
				)
			)
			throw CannotWriteException(
				ErrorMessage.GENERAL_WRITE_FAILED_TO_RENAME_TO_ORIGINAL_FILE.getMsg(
					originalFile.absolutePath,
					newFile.name
				), e
			)
		}
		// file is written, all is good, let's delete newFile, as it's not needed anymore
		if (newFile.exists() && !newFile.delete()) {
			// non-critical failed deletion
			logger.warning(ErrorMessage.GENERAL_WRITE_FAILED_TO_DELETE_TEMPORARY_FILE.getMsg(newFile.path))
		}
	}

	/**
	 *
	 *
	 * Replaces the original file with the new file in a way that changes the file identity.
	 * In other words, the Unix inode or the Windows
	 * [fileIndex](https://msdn.microsoft.com/en-us/library/aa363788(v=vs.85).aspx)
	 * of the resulting file with the name `originalFile` is not identical to the inode/fileIndex
	 * of the file named `originalFile` before this method was called.
	 *
	 *
	 *
	 * If no errors occur, the method follows this approach:
	 *
	 *
	 *  1. Rename `originalFile` to `originalFile.old`
	 *  1. Rename `newFile` to `originalFile` (this implies a file identity change for `originalFile`)
	 *  1. Delete `originalFile.old`
	 *  1. Delete `newFile`
	 *
	 *
	 * @param newFile      File containing the data we want in the `originalFile`
	 * @param originalFile Before execution this denotes the original, unmodified file.
	 * After execution it denotes the name of the file with the modified content and new inode/fileIndex.
	 * @throws CannotWriteException if the file cannot be written
	 */
	@Throws(CannotWriteException::class)
	private fun transferNewFileToNewOriginalFile(newFile: File?, originalFile: File) {
		// get original creation date
		val creationTime = getCreationTime(originalFile)

		// Rename Original File
		// Can fail on Vista if have Special Permission 'Delete' set Deny
		var originalFileBackup = File(
			originalFile.absoluteFile.parentFile.path,
			AudioFile.getBaseFilename(originalFile) + ".old"
		)

		//If already exists modify the suffix
		var count = 1
		while (originalFileBackup.exists()) {
			originalFileBackup = File(
				originalFile.absoluteFile.parentFile.path,
				AudioFile.getBaseFilename(originalFile) + ".old" + count
			)
			count++
		}
		var renameResult = Utils.rename(originalFile, originalFileBackup)
		if (!renameResult) {
			logger.log(
				Level.SEVERE,
				ErrorMessage.GENERAL_WRITE_FAILED_TO_RENAME_ORIGINAL_FILE_TO_BACKUP.getMsg(
					originalFile.absolutePath,
					originalFileBackup.name
				)
			)
			//Delete the temp file because write has failed
			// TODO: Simplify: newFile is always != null, otherwise we would not have entered this block (-> if (newFile.length() > 0) {})
			newFile?.delete()
			throw CannotWriteException(
				ErrorMessage.GENERAL_WRITE_FAILED_TO_RENAME_ORIGINAL_FILE_TO_BACKUP.getMsg(
					originalFile.path,
					originalFileBackup.name
				)
			)
		}

		// Rename Temp File to Original File
		renameResult = Utils.rename(newFile, originalFile)
		if (!renameResult) {
			// Renamed failed so lets do some checks rename the backup back to the original file
			// New File doesnt exist
			if (!newFile!!.exists()) {
				logger.warning(
					ErrorMessage.GENERAL_WRITE_FAILED_NEW_FILE_DOESNT_EXIST.getMsg(
						newFile.absolutePath
					)
				)
			}

			// Rename the backup back to the original
			if (!originalFileBackup.renameTo(originalFile)) {
				// TODO now if this happens we are left with testfile.old
				// instead of testfile.mp4
				logger.warning(
					ErrorMessage.GENERAL_WRITE_FAILED_TO_RENAME_ORIGINAL_BACKUP_TO_ORIGINAL.getMsg(
						originalFileBackup.absolutePath,
						originalFile.name
					)
				)
			}
			logger.warning(
				ErrorMessage.GENERAL_WRITE_FAILED_TO_RENAME_TO_ORIGINAL_FILE.getMsg(
					originalFile.absolutePath,
					newFile.name
				)
			)
			throw CannotWriteException(
				ErrorMessage.GENERAL_WRITE_FAILED_TO_RENAME_TO_ORIGINAL_FILE.getMsg(
					originalFile.absolutePath,
					newFile.name
				)
			)
		} else {
			// Rename was okay so we can now delete the backup of the
			// original
			val deleteResult = originalFileBackup.delete()
			if (!deleteResult) {
				// Not a disaster but can't delete the backup so make a
				// warning
				logger.warning(
					ErrorMessage.GENERAL_WRITE_WARNING_UNABLE_TO_DELETE_BACKUP_FILE.getMsg(
						originalFileBackup.absolutePath
					)
				)
			}

			// now also set the creation date to the creation date of the original file
			creationTime?.let { setCreationTime(originalFile, it) }
		}

		// Delete the temporary file if still exists
		if (newFile!!.exists()) {
			if (!newFile.delete()) {
				// Non critical failed deletion
				logger.warning(
					ErrorMessage.GENERAL_WRITE_FAILED_TO_DELETE_TEMPORARY_FILE.getMsg(
						newFile.path
					)
				)
			}
		}
	}

	/**
	 * Sets the creation time for a given file.
	 * Fails silently with a log message.
	 *
	 * @param file         file
	 * @param creationTime creation time
	 */
	@kotlin.jvm.Throws(UnsupportedOperationException::class)
	private fun setCreationTime(file: File, creationTime: FileTime) {
		if (!VersionHelper.hasOreo()) throw UnsupportedOperationException()
		try {
			Files.setAttribute(file.toPath(), "creationTime", creationTime)
		} catch (e: Exception) {
			logger.log(
				Level.WARNING,
				ErrorMessage.GENERAL_SET_CREATION_TIME_FAILED.getMsg(file.absolutePath, e.message),
				e
			)
		}
	}

	/**
	 * Get file creation time.
	 *
	 * @param file file
	 * @return time object or `null`, if we could not read it for some reason.
	 */
	private fun getCreationTime(file: File): FileTime? {
		if (!VersionHelper.hasOreo()) return null

		return try {
			val attributes = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
			attributes.creationTime()
		} catch (e: Exception) {
			logger.log(
				Level.WARNING,
				ErrorMessage.GENERAL_GET_CREATION_TIME_FAILED.getMsg(file.absolutePath, e.message),
				e
			)
			null
		}
	}

	/**
	 * This is called when a tag has to be written in a file. Three parameters
	 * are provided, the tag to write (not empty) Two randomaccessfiles, the
	 * first points to the file where we want to write the given tag, and the
	 * second is an empty temporary file that can be used if e.g. the file has
	 * to be bigger than the original.
	 *
	 *
	 * If something has been written in the temporary file, when this method
	 * returns, the original file is deleted, and the temporary file is renamed
	 * the the original name
	 *
	 *
	 * If nothing has been written to it, it is simply deleted.
	 *
	 *
	 * This method can assume the raf, rafTemp are pointing to the first byte of
	 * the file. The subclass must not close these two files when the method
	 * returns.
	 *
	 * @param audioFile
	 * @param tag
	 * @param raf
	 * @param rafTemp
	 * @throws IOException                                           is thrown when the RandomAccessFile operations throw it (you
	 * should never throw them manually)
	 * @throws CannotWriteException                                  when an error occured during the generation of the tag
	 * @throws CannotReadException
	 */
	@Throws(CannotReadException::class, CannotWriteException::class, IOException::class)
	protected abstract fun writeTag(
		audioFile: AudioFile?,
		tag: Tag,
		raf: RandomAccessFile,
		rafTemp: RandomAccessFile
	)

	companion object {
		private const val TEMP_FILENAME_SUFFIX = ".tmp"
		private const val WRITE_MODE = "rw"

		const val MINIMUM_FILESIZE = 100

		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.audio.generic")

		//If filename too long try recreating it with length no longer than 50 that should be safe on all operating
		//systems
		private const val FILE_NAME_TOO_LONG = "File name too long"
		private const val FILE_NAME_TOO_LONG_SAFE_LIMIT = 50
	}
}
