package com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic

import com.flammky.android.core.sdk.VersionHelper
import com.flammky.common.media.audio.meta_tag.audio.generic.Permissions
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.AudioFile
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotWriteException
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagOptionSingleton
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path

/**
 * Created by Paul on 28/01/2016.
 */
abstract class AudioFileWriter2 : AudioFileWriter() {
	/**
	 * Delete the tag (if any) present in the given file
	 *
	 * @param af The file to process
	 *
	 * @throws CannotWriteException if anything went wrong
	 * @throws CannotReadException
	 */
	@Throws(
		CannotReadException::class,
		CannotWriteException::class,
		UnsupportedOperationException::class
	)
	override fun delete(af: AudioFile) {
		if (!VersionHelper.hasOreo()) throw UnsupportedOperationException()
		val path = af.mFile!!.toPath()
		if (TagOptionSingleton.instance.isCheckIsWritable && !Files.isWritable(path)) {
			logger.severe(Permissions.displayPermissions(path))
			throw CannotWriteException(ErrorMessage.GENERAL_DELETE_FAILED.getMsg(path))
		}

		if (af.mFile!!.length() <= MINIMUM_FILESIZE) {
			throw CannotWriteException(
				ErrorMessage.GENERAL_DELETE_FAILED_BECAUSE_FILE_IS_TOO_SMALL.getMsg(path)
			)
		}
		deleteTag(af.tag!!, path)
	}

	/**
	 * Replace with new tag
	 *
	 * @param af The file we want to process
	 * @throws CannotWriteException
	 */
	@Throws(CannotWriteException::class, UnsupportedOperationException::class)
	override fun write(af: AudioFile) {
		if (!VersionHelper.hasOreo()) throw UnsupportedOperationException()

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
			throw CannotWriteException(
				ErrorMessage.GENERAL_WRITE_FAILED_BECAUSE_FILE_IS_TOO_SMALL
					.getMsg(file)
			)
		}
		writeTag(af.tag!!, file)
	}

	/**
	 * Must be implemented by each audio format
	 *
	 * @param tag
	 * @param file
	 * @throws CannotReadException
	 * @throws CannotWriteException
	 */
	@Throws(CannotReadException::class, CannotWriteException::class)
	protected abstract fun deleteTag(tag: Tag, file: Path)

	@Throws(CannotReadException::class, CannotWriteException::class, IOException::class)
	public override fun deleteTag(tag: Tag?, raf: RandomAccessFile?, tempRaf: RandomAccessFile?) {
		throw UnsupportedOperationException("Old method not used in version 2")
	}

	/**
	 * Must be implemented by each audio format
	 *
	 * @param tag
	 * @param file
	 * @throws CannotWriteException
	 */
	@Throws(CannotWriteException::class)
	protected abstract fun writeTag(tag: Tag, file: Path)

	@Throws(CannotReadException::class, CannotWriteException::class, IOException::class)
	override fun writeTag(
		audioFile: AudioFile?,
		tag: Tag,
		raf: RandomAccessFile,
		rafTemp: RandomAccessFile
	) {
		throw UnsupportedOperationException("Old method not used in version 2")
	}
}
