package com.flammky.musicplayer.common.media.audio.meta_tag.audio

import com.flammky.android.core.sdk.VersionHelper
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.dsf.Dsf
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.*
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.real.RealTag
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagOptionSingleton
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.aiff.AiffTag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.asf.AsfTag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.flac.FlacTag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v22Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v23Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v2TagBase
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.Mp4Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.ID3V2Version
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.vorbiscomment.VorbisCommentTag.Companion.createNewTag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.wav.WavTag
import java.io.File
import java.io.FileDescriptor
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.util.*
import java.util.logging.Logger
import kotlin.io.path.isReadable
import kotlin.io.path.isWritable

/**
 *
 * This is the main object manipulated by the user representing an audiofile, its properties and its tag.
 *
 * The preferred way to obtain an `AudioFile` is to use the `AudioFileIO.read(File)` method.
 *
 * The `AudioHeader` contains every properties associated with the file itself (no meta-data), like the bitrate, the sampling rate, the encoding audioHeaders, etc.
 *
 * To get the meta-data contained in this file you have to get the `Tag` of this `AudioFile`
 *
 * @author Raphael Slinckx
 * @version $Id$
 * @see AudioFileIO
 *
 * @see Tag
 *
 * @since v0.01
 */
open class AudioFile {
	/**
	 * Retrieve the physical file
	 *
	 * @return
	 */
	/**
	 * Set the file to store the info in
	 *
	 * @param file
	 */
	/**
	 *
	 * The physical file that this instance represents.
	 */
	var mFile: File? = null
	var mFd: FileDescriptor? = null
	var mFc: FileChannel? = null
	/**
	 * Return audio header information
	 * @return
	 */
	/**
	 * The Audio header info
	 */
	var audioHeader: AudioHeader? = null
		protected set
	/**
	 *
	 * Returns the tag contained in this AudioFile, the `Tag` contains any useful meta-data, like
	 * artist, album, title, etc. If the file does not contain any tag the null is returned. Some audio formats do
	 * not allow there to be no tag so in this case the reader would return an empty tag whereas for others such
	 * as mp3 it is purely optional.
	 *
	 * @return Returns the tag contained in this AudioFile, or null if no tag exists.
	 */
	/**
	 * Assign a tag to this audio file
	 *
	 * @param tag   Tag to be assigned
	 */
	/**
	 * The tag
	 */
	open var tag: Tag? = null
	/**
	 * Retrieve the file extension
	 *
	 * @return
	 */
	/**
	 * Set the file extension
	 *
	 * @param ext
	 */
	/**
	 * The tag
	 */
	var ext: String? = null

	constructor()

	/**
	 *
	 * These constructors are used by the different readers, users should not use them, but use the `AudioFileIO.read(File)` method instead !.
	 *
	 * Create the AudioFile representing file f, the encoding audio headers and containing the tag
	 *
	 * @param f           The file of the audio file
	 * @param audioHeader the encoding audioHeaders over this file
	 * @param tag         the tag contained in this file or null if no tag exists
	 */
	constructor(f: File?, audioHeader: AudioHeader?, tag: Tag?) {
		mFile = f
		this.audioHeader = audioHeader
		this.tag = tag
	}

	/**
	 *
	 * These constructors are used by the different readers, users should not use them, but use the `AudioFileIO.read(File)` method instead !.
	 *
	 * Create the AudioFile representing file denoted by pathnames, the encoding audio Headers and containing the tag
	 *
	 * @param s           The pathname of the audio file
	 * @param audioHeader the encoding audioHeaders over this file
	 * @param tag         the tag contained in this file
	 */
	constructor(s: String?, audioHeader: AudioHeader?, tag: Tag?) {
		mFile = File(s)
		this.audioHeader = audioHeader
		this.tag = tag
	}

	constructor(fd: FileDescriptor, audioHeader: AudioHeader?, tag: Tag?) {
		mFd = fd
		this.audioHeader = audioHeader
		this.tag = tag
	}

	constructor(fc: FileChannel, audioHeader: AudioHeader?, tag: Tag?) {
		mFc = fc
		this.audioHeader = audioHeader
		this.tag = tag
	}

	/**
	 *
	 * Write the tag contained in this AudioFile in the actual file on the disk, this is the same as calling the `AudioFileIO.write(this)` method.
	 *
	 * @throws NoWritePermissionsException if the file could not be written to due to file permissions
	 * @throws CannotWriteException If the file could not be written/accessed, the extension wasn't recognized, or other IO error occured.
	 * @see AudioFileIO
	 */
	@Throws(CannotWriteException::class)
	open fun commit() {
		AudioFileIO.Companion.write(this)
	}

	/**
	 *
	 * Delete any tags that exist in the fie , this is the same as calling the `AudioFileIO.delete(this)` method.
	 *
	 * @throws CannotWriteException If the file could not be written/accessed, the extension wasn't recognized, or other IO error occured.
	 * @see AudioFileIO
	 */
	@Throws(CannotReadException::class, CannotWriteException::class)
	fun delete() {
		AudioFileIO.Companion.delete(this)
	}

	/**
	 *
	 * Returns a multi-line string with the file path, the encoding audioHeader, and the tag contents.
	 *
	 * @return A multi-line string with the file path, the encoding audioHeader, and the tag contents.
	 * TODO Maybe this can be changed ?
	 */
	override fun toString(): String {
		return """AudioFile ${mFile?.absolutePath}  --------
${audioHeader.toString()}
${if (tag == null) "" else tag.toString()}
-------------------"""
	}

	/**
	 * Check does file exist
	 *
	 * @param file
	 * @throws FileNotFoundException  if file not found
	 */
	@Throws(FileNotFoundException::class)
	fun checkFileExists(file: File) {
		if (!file.exists()) {
			throw FileNotFoundException(ErrorMessage.UNABLE_TO_FIND_FILE.getMsg(file.path))
		}
	}

	/**
	 * Checks the file is accessible with the correct permissions, otherwise exception occurs
	 *
	 * @param file
	 * @param readOnly
	 * @throws ReadOnlyFileException
	 * @throws FileNotFoundException
	 * @return
	 */
	@Throws(ReadOnlyFileException::class, FileNotFoundException::class, CannotReadException::class)
	protected fun checkFilePermissions(file: File, readOnly: Boolean): RandomAccessFile {
		checkFileExists(file)

		val raf: RandomAccessFile =
			if (VersionHelper.hasOreo()) {
				val path: Path = file.toPath()
				if (readOnly) {
					if (!path.isReadable()) throw NoReadPermissionsException()
					RandomAccessFile(file, "r")
				} else {
					if (!path.isWritable()) throw NoWritePermissionsException()
					RandomAccessFile(file, "rw")
				}
			} else {
				if (readOnly) {
					if (!file.canRead()) throw NoReadPermissionsException()
					RandomAccessFile(file, "r")
				} else {
					if (!file.canWrite()) throw NoWritePermissionsException()
					RandomAccessFile(file, "rw")
				}
			}
		return raf
	}

	/**
	 * Optional debugging method. Must override to do anything interesting.
	 *
	 * @return  Empty string.
	 */
	open fun displayStructureAsXML(): String? {
		return ""
	}

	/**
	 * Optional debugging method. Must override to do anything interesting.
	 *
	 * @return
	 */
	open fun displayStructureAsPlainText(): String? {
		return ""
	}

	/** Create Default Tag
	 *
	 * @return
	 */
	open fun createDefaultTag(): Tag? {
		var extension = ext
		if (extension == null) {
			val fileName = mFile?.name ?: return null
			extension = fileName.substring(fileName.lastIndexOf('.') + 1)
			ext = extension
		}
		return if (SupportedFileFormat.FLAC.filesuffix == extension) {
			FlacTag(
				createNewTag(),
				ArrayList()
			)
		} else if (SupportedFileFormat.OGG.filesuffix == extension) {
			createNewTag()
		} else if (SupportedFileFormat.OGA.filesuffix == extension) {
			createNewTag()
		} else if (SupportedFileFormat.MP4.filesuffix == extension) {
			Mp4Tag()
		} else if (SupportedFileFormat.M4A.filesuffix == extension) {
			Mp4Tag()
		} else if (SupportedFileFormat.M4P.filesuffix == extension) {
			Mp4Tag()
		} else if (SupportedFileFormat.WMA.filesuffix == extension) {
			AsfTag()
		} else if (SupportedFileFormat.WAV.filesuffix == extension) {
			WavTag(TagOptionSingleton.instance.wavOptions)
		} else if (SupportedFileFormat.RA.filesuffix == extension) {
			RealTag()
		} else if (SupportedFileFormat.RM.filesuffix == extension) {
			RealTag()
		} else if (SupportedFileFormat.AIF.filesuffix == extension) {
			AiffTag()
		} else if (SupportedFileFormat.AIFC.filesuffix == extension) {
			AiffTag()
		} else if (SupportedFileFormat.AIFF.filesuffix == extension) {
			AiffTag()
		} else if (SupportedFileFormat.DSF.filesuffix == extension) {
			Dsf.createDefaultTag()
		} else {
			throw RuntimeException("Unable to create default tag for this file format")
		}
	}

	/**
	 * Get the tag or if the file doesn't have one at all, create a default tag  and return
	 *
	 * @return
	 */
	open val tagOrCreateDefault: Tag?
		get() = tag ?: createDefaultTag()

	/**
	 * Get the tag or if the file doesn't have one at all, create a default tag and set it
	 * as the tag of this file
	 *
	 * @return
	 */
	val tagOrCreateAndSetDefault: Tag?
		get() {
			val tag = tagOrCreateDefault
			this.tag = tag
			return tag
		}/* TODO Currently only works for Dsf We need additional check here for Wav and Aif because they wrap the ID3 tag so never return
         * null for getTag() and the wrapper stores the location of the existing tag, would that be broken if tag set to something else
         */

	/**
	 * Get the tag and convert to the default tag version or if the file doesn't have one at all, create a default tag
	 *
	 * Conversions are currently only necessary/available for some formats that support ID3- Dsf, Mp3
	 *
	 * @return
	 */
	open val tagAndConvertOrCreateDefault: Tag?
		get() {
			val tag = tagOrCreateDefault

			/* TODO Currently only works for Dsf We need additional check here for Wav and Aif because they wrap the ID3 tag so never return
	 * null for getTag() and the wrapper stores the location of the existing tag, would that be broken if tag set to something else
	 */return if (tag is ID3v2TagBase) {
				val convertedTag: Tag? =
					convertID3Tag(tag, TagOptionSingleton.instance.iD3V2Version)
				convertedTag ?: tag
			} else {
				tag
			}
		}

	/**
	 * Get the tag and convert to the default tag version or if the file doesn't have one at all, create a default tag
	 * set as tag for this file
	 *
	 * Conversions are currently only necessary/available for some formats that support ID3- Dsf, Mp3
	 *
	 * @return
	 */
	open val tagAndConvertOrCreateAndSetDefault: Tag?
		get() {
			val tag = tagAndConvertOrCreateDefault
			this.tag = tag
			return this.tag
		}

	/**
	 * If using ID3 format convert tag from current version to another as specified by id3V2Version,
	 *
	 * @return null if no conversion necessary
	 */
	fun convertID3Tag(tag: ID3v2TagBase, id3V2Version: ID3V2Version): ID3v2TagBase? {

		when (tag) {
			is ID3v24Tag -> {
				when (id3V2Version) {
					ID3V2Version.ID3_V22 -> return ID3v22Tag(tag)
					ID3V2Version.ID3_V23 -> return ID3v23Tag(tag)
					ID3V2Version.ID3_V24 -> return null
				}
			}
			is ID3v23Tag -> {
				when (id3V2Version) {
					ID3V2Version.ID3_V22 -> return ID3v22Tag(tag)
					ID3V2Version.ID3_V23 -> return null
					ID3V2Version.ID3_V24 -> return ID3v24Tag(tag)
				}
			}
			is ID3v22Tag -> {
				when (id3V2Version) {
					ID3V2Version.ID3_V22 -> return null
					ID3V2Version.ID3_V23 -> return ID3v23Tag(tag)
					ID3V2Version.ID3_V24 -> return ID3v24Tag(tag)
				}
			}
		}

		return null
	}

	companion object {
		//Logger
		var logger = Logger.getLogger("org.jaudiotagger.audio")

		/**
		 *
		 * @param file
		 * @return filename with audioFormat separator stripped off.
		 */
		fun getBaseFilename(file: File): String {
			val index = file.name.lowercase(Locale.getDefault()).lastIndexOf(".")
			return if (index > 0) {
				file.name.substring(0, index)
			} else file.name
		}
	}
}
