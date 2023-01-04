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
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path

/**
 * Replacement for AudioFileReader class
 */
abstract class AudioFileReader2 : AudioFileReader() {
	/*
 * Reads the given file, and return an AudioFile object containing the Tag
 * and the encoding infos present in the file. If the file has no tag, an
 * empty one is returned. If the encodinginfo is not valid , an exception is thrown.
 *
 * @param f The file to read
 * @exception NoReadPermissionsException if permissions prevent reading of file
 * @exception CannotReadException If anything went bad during the read of this file
 */
	@Throws(
		CannotReadException::class,
		IOException::class,
		TagException::class,
		ReadOnlyFileException::class,
		InvalidAudioFrameException::class
	)
	override fun read(f: File): AudioFile {
		return if (VersionHelper.hasOreo()) {
			val path = f.toPath()
			if (!Files.isReadable(path)) {
				if (!Files.exists(path)) {
					throw FileNotFoundException(ErrorMessage.UNABLE_TO_FIND_FILE.getMsg(path))
				} else {
					logger.warning(Permissions.displayPermissions(path))
					throw NoReadPermissionsException(
						ErrorMessage.GENERAL_READ_FAILED_DO_NOT_HAVE_PERMISSION_TO_READ_FILE.getMsg(
							path
						)
					)
				}
			}
			if (f.length() <= MINIMUM_SIZE_FOR_VALID_AUDIO_FILE) {
				throw CannotReadException(ErrorMessage.GENERAL_READ_FAILED_FILE_TOO_SMALL.getMsg(path))
			}
			val info = getEncodingInfo(path)
			val tag = getTag(path)
			AudioFile(f, info, tag)
		} else {
			if (!f.canRead()) {
				if (!f.exists()) {
					throw FileNotFoundException(ErrorMessage.UNABLE_TO_FIND_FILE.getMsg(f))
				} else {
					throw NoReadPermissionsException(
						ErrorMessage.GENERAL_READ_FAILED_DO_NOT_HAVE_PERMISSION_TO_READ_FILE.getMsg(
							f
						)
					)
				}
			}
			if (f.length() <= MINIMUM_SIZE_FOR_VALID_AUDIO_FILE) {
				throw CannotReadException(ErrorMessage.GENERAL_READ_FAILED_FILE_TOO_SMALL.getMsg(f))
			}
			val info = getEncodingInfo(RandomAccessFile(f, "r").channel)
			val tag = getTag(RandomAccessFile(f, "r").channel)
			AudioFile(f, info, tag)
		}
	}

	/**
	 *
	 * Read Encoding Information
	 *
	 * @param file
	 * @return
	 * @throws CannotReadException
	 * @throws IOException
	 */
	@Throws(CannotReadException::class, IOException::class)
	protected abstract fun getEncodingInfo(file: Path): GenericAudioHeader

	@Throws(CannotReadException::class, IOException::class)
	override fun getEncodingInfo(raf: RandomAccessFile): GenericAudioHeader {
		throw UnsupportedOperationException("Old method not used in version 2")
	}

	/**
	 * Read tag Information
	 *
	 * @param path
	 * @return
	 * @throws CannotReadException
	 * @throws IOException
	 */
	@Throws(CannotReadException::class, IOException::class)
	protected abstract fun getTag(path: Path): Tag?

	@Throws(CannotReadException::class, IOException::class)
	override fun getTag(file: RandomAccessFile): Tag {
		throw UnsupportedOperationException("Old method not used in version 2")
	}
}
