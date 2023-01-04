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

import com.flammky.common.media.audio.meta_tag.audio.opus.OpusFileReader
import com.flammky.common.media.audio.meta_tag.audio.opus.OpusFileWriter
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.AiffFileReader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.AiffFileWriter
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.AsfFileReader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.AsfFileWriter
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.dff.DffFileReader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.dsf.DsfFileReader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.dsf.DsfFileWriter
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotWriteException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.InvalidAudioFrameException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.ReadOnlyFileException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.flac.FlacFileReader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.flac.FlacFileWriter
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.AudioFileModificationListener
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.AudioFileReader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.AudioFileWriter
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.ModificationHandler
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.copyThrowsOnException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getExtension
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getMagicExtension
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp3.MP3FileReader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp3.MP3FileWriter
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.Mp4FileReader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.Mp4FileWriter
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.ogg.OggFileReader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.ogg.OggFileWriter
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.real.RealFileReader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav.WavFileReader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav.WavFileWriter
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagException
import java.io.File
import java.io.FileDescriptor
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.channels.FileChannel
import java.util.logging.Logger

/**
 *
 * The main entry point for the Tag Reading/Writing operations, this class will
 * select the appropriate reader/writer for the given file.
 *
 *
 * It selects the appropriate reader/writer based on the file extension (case
 * ignored).
 *
 *
 * Here is an simple example of use:
 *
 *
 * `
 * AudioFile audioFile = AudioFileIO.read(new File("audiofile.mp3")); //Reads the given file.
 * int bitrate = audioFile.getBitrate(); //Retreives the bitrate of the file.
 * String artist = audioFile.getTag().getFirst(TagFieldKey.ARTIST); //Retreive the artist name.
 * audioFile.getTag().setGenre("Progressive Rock"); //Sets the genre to Prog. Rock, note the file on disk is still unmodified.
 * AudioFileIO.write(audioFile); //Write the modifications in the file on disk.
` *
 *
 *
 * You can also use the `commit()` method defined for
 * `AudioFile`s to achieve the same goal as
 * `AudioFileIO.write(File)`, like this:
 *
 *
 * `
 * AudioFile audioFile = AudioFileIO.read(new File("audiofile.mp3"));
 * audioFile.getTag().setGenre("Progressive Rock");
 * audioFile.commit(); //Write the modifications in the file on disk.
` *
 *
 *
 * @author Raphael Slinckx
 * @version $Id$
 * @see AudioFile
 *
 * @see Tag
 *
 * @since v0.01
 */
class AudioFileIO {
	/**
	 * This member is used to broadcast modification events to registered
	 */
	private val modificationHandler: ModificationHandler

	// These tables contains all the readers/writers associated with extension
	// as a key
	private val readers: MutableMap<String?, AudioFileReader> = HashMap()
	private val writers: MutableMap<String?, AudioFileWriter> = HashMap()

	/**
	 * Creates an instance.
	 */
	init {
		modificationHandler = ModificationHandler()
		prepareReadersAndWriters()
	}

	/**
	 * Adds an listener for all file formats.
	 *
	 * @param listener listener
	 */
	fun addAudioFileModificationListener(
		listener: AudioFileModificationListener?
	) {
		modificationHandler.addAudioFileModificationListener(listener!!)
	}

	/**
	 *
	 * Delete the tag, if any, contained in the given file.
	 *
	 *
	 * @param f The file where the tag will be deleted
	 * @throws CannotWriteException If the file could not be written/accessed, the extension
	 * wasn't recognized, or other IO error occurred.
	 * @throws CannotReadException
	 */
	@Throws(CannotReadException::class, CannotWriteException::class)
	fun deleteTag(f: AudioFile) {
		val ext = getExtension(f.mFile!!)
		val afw = writers[ext]
			?: throw CannotWriteException(
				ErrorMessage.NO_DELETER_FOR_THIS_FORMAT.getMsg(
					ext
				)
			)
		afw.delete(f)
	}

	/**
	 * Creates the readers and writers.
	 */
	private fun prepareReadersAndWriters() {

		// Tag Readers
		readers[SupportedFileFormat.OGG.filesuffix] = OggFileReader()
		readers[SupportedFileFormat.OPUS.filesuffix] = OpusFileReader()
		readers[SupportedFileFormat.OGA.filesuffix] = OggFileReader()
		readers[SupportedFileFormat.FLAC.filesuffix] = FlacFileReader()
		readers[SupportedFileFormat.MP3.filesuffix] = MP3FileReader()
		readers[SupportedFileFormat.MP4.filesuffix] = Mp4FileReader()
		readers[SupportedFileFormat.M4A.filesuffix] = Mp4FileReader()
		readers[SupportedFileFormat.M4P.filesuffix] = Mp4FileReader()
		readers[SupportedFileFormat.M4B.filesuffix] = Mp4FileReader()
		readers[SupportedFileFormat.WAV.filesuffix] = WavFileReader()
		readers[SupportedFileFormat.WMA.filesuffix] = AsfFileReader()
		readers[SupportedFileFormat.AIF.filesuffix] = AiffFileReader()
		readers[SupportedFileFormat.AIFC.filesuffix] = AiffFileReader()
		readers[SupportedFileFormat.AIFF.filesuffix] = AiffFileReader()
		readers[SupportedFileFormat.DSF.filesuffix] = DsfFileReader()
		readers[SupportedFileFormat.DFF.filesuffix] = DffFileReader()
		val realReader = RealFileReader()
		readers[SupportedFileFormat.RA.filesuffix] = realReader
		readers[SupportedFileFormat.RM.filesuffix] = realReader

		// Tag Writers
		writers[SupportedFileFormat.OGG.filesuffix] = OggFileWriter()
		writers[SupportedFileFormat.OPUS.filesuffix] = OpusFileWriter()
		writers[SupportedFileFormat.OGA.filesuffix] = OggFileWriter()
		writers[SupportedFileFormat.FLAC.filesuffix] = FlacFileWriter()
		writers[SupportedFileFormat.MP3.filesuffix] = MP3FileWriter()
		writers[SupportedFileFormat.MP4.filesuffix] = Mp4FileWriter()
		writers[SupportedFileFormat.M4A.filesuffix] = Mp4FileWriter()
		writers[SupportedFileFormat.M4P.filesuffix] = Mp4FileWriter()
		writers[SupportedFileFormat.M4B.filesuffix] = Mp4FileWriter()
		writers[SupportedFileFormat.WAV.filesuffix] = WavFileWriter()
		writers[SupportedFileFormat.WMA.filesuffix] = AsfFileWriter()
		writers[SupportedFileFormat.AIF.filesuffix] = AiffFileWriter()
		writers[SupportedFileFormat.AIFC.filesuffix] = AiffFileWriter()
		writers[SupportedFileFormat.AIFF.filesuffix] = AiffFileWriter()
		writers[SupportedFileFormat.DSF.filesuffix] = DsfFileWriter()
		for (curr in writers.values) {
			curr.setAudioFileModificationListener(modificationHandler)
		}
	}

	/**
	 *
	 * Read the tag contained in the given file.
	 *
	 *
	 * @param f The file to read.
	 * @return The AudioFile with the file tag and the file encoding info.
	 * @throws CannotReadException If the file could not be read, the extension wasn't
	 * recognized, or an IO error occurred during the read.
	 * @throws TagException
	 * @throws ReadOnlyFileException
	 * @throws IOException
	 * @throws InvalidAudioFrameException
	 */
	@Throws(
		CannotReadException::class,
		IOException::class,
		TagException::class,
		ReadOnlyFileException::class,
		InvalidAudioFrameException::class
	)
	fun readFile(f: File?): AudioFile {
		//checkFileExists(f);
		val ext = getExtension(
			f!!
		)
		val afr = readers[ext]
			?: throw CannotReadException(ErrorMessage.NO_READER_FOR_THIS_FORMAT.getMsg(ext))
		val tempFile = afr.read(
			f
		)
		tempFile.ext = ext
		return tempFile
	}

	/**
	 *
	 * Read the tag contained in the given file.
	 *
	 *
	 * @param f The file to read.
	 * @return The AudioFile with the file tag and the file encoding info.
	 * @throws CannotReadException If the file could not be read, the extension wasn't
	 * recognized, or an IO error occurred during the read.
	 * @throws TagException
	 * @throws ReadOnlyFileException
	 * @throws IOException
	 * @throws InvalidAudioFrameException
	 */
	@Throws(
		CannotReadException::class,
		IOException::class,
		TagException::class,
		ReadOnlyFileException::class,
		InvalidAudioFrameException::class
	)
	fun readFileMagic(f: File): AudioFile {
		//checkFileExists(f);
		val ext = getMagicExtension(f)
		val afr = readers[ext]
			?: throw CannotReadException(ErrorMessage.NO_READER_FOR_THIS_FORMAT.getMsg(ext))
		val tempFile = afr.read(f)
		tempFile.ext = ext
		return tempFile
	}

	fun readFileMagic(fd: FileDescriptor): AudioFile {
		val ext = getMagicExtension(fd)
		val afr = readers[ext]
			?: throw CannotReadException(ErrorMessage.NO_READER_FOR_THIS_FORMAT.getMsg(ext))
		val tempFile = afr.read(fd)
		tempFile.ext = ext
		return afr.read(fd).apply af@ {
			this@af.ext = ext
		}
	}

	fun readFileMagic(fc: FileChannel): AudioFile {
		val ext = getMagicExtension(fc)
		val afr = readers[ext]
			?: throw CannotReadException(ErrorMessage.NO_READER_FOR_THIS_FORMAT.getMsg(ext))
		val tempFile = afr.read(fc.position(0))
		tempFile.ext = ext
		return afr.read(fc).apply af@ {
			this@af.ext = ext
		}
	}

	/**
	 *
	 * Read the tag contained in the given file.
	 *
	 *
	 * @param f The file to read.
	 * @param ext The extension to be used.
	 * @return The AudioFile with the file tag and the file encoding info.
	 * @throws CannotReadException If the file could not be read, the extension wasn't
	 * recognized, or an IO error occurred during the read.
	 * @throws TagException
	 * @throws ReadOnlyFileException
	 * @throws IOException
	 * @throws InvalidAudioFrameException
	 */
	@Throws(
		CannotReadException::class,
		IOException::class,
		TagException::class,
		ReadOnlyFileException::class,
		InvalidAudioFrameException::class
	)
	fun readFileAs(f: File?, ext: String?): AudioFile {
		//checkFileExists(f);
		val afr = readers[ext]
			?: throw CannotReadException(ErrorMessage.NO_READER_FOR_THIS_FORMAT.getMsg(ext))
		val tempFile = afr.read(
			f!!
		)
		tempFile.ext = ext
		return tempFile
	}

	/**
	 * Check does file exist
	 *
	 * @param file
	 * @throws FileNotFoundException
	 */
	@Throws(FileNotFoundException::class)
	fun checkFileExists(file: File) {
		logger.config("Reading file:" + "path" + file.path + ":abs:" + file.absolutePath)
		if (!file.exists()) {
			logger.severe("Unable to find:" + file.path)
			throw FileNotFoundException(ErrorMessage.UNABLE_TO_FIND_FILE.getMsg(file.path))
		}
	}

	/**
	 * Removes a listener for all file formats.
	 *
	 * @param listener listener
	 */
	fun removeAudioFileModificationListener(
		listener: AudioFileModificationListener?
	) {
		modificationHandler.removeAudioFileModificationListener(listener!!)
	}

	/**
	 *
	 * Write the tag contained in the audioFile in the actual file on the disk.
	 *
	 *
	 * @param f The AudioFile to be written
	 * @param targetPath a file path, without an extension, which provides a "save as". If null, then normal "save" function
	 * @throws NoWritePermissionsException if the file could not be written to due to file permissions
	 * @throws CannotWriteException If the file could not be written/accessed, the extension
	 * wasn't recognized, or other IO error occurred.
	 */
	@Throws(CannotWriteException::class)
	fun writeFile(f: AudioFile, targetPath: String?) {
		val ext = f.ext
		if (targetPath != null && !targetPath.isEmpty()) {
			val destination = File("$targetPath.$ext")
			try {
				copyThrowsOnException(f.mFile, destination)
				f.mFile = destination
			} catch (e: IOException) {
				throw CannotWriteException("Error While Copying" + e.message)
			}
		}
		val afw = writers[ext]
			?: throw CannotWriteException(ErrorMessage.NO_WRITER_FOR_THIS_FORMAT.getMsg(ext))
		afw.write(f)
	}

	companion object {
		//Logger
		var logger = Logger.getLogger("org.jaudiotagger.audio")
		// !! Do not forget to also add new supported extensions to AudioFileFilter
		// !!
		/**
		 * This field contains the default instance for static use.
		 */
		private var defaultInstance: AudioFileIO? = null

		/**
		 *
		 * Delete the tag, if any, contained in the given file.
		 *
		 *
		 * @param f The file where the tag will be deleted
		 * @throws CannotWriteException If the file could not be written/accessed, the extension
		 * wasn't recognized, or other IO error occurred.
		 * @throws CannotReadException
		 */
		@Throws(CannotReadException::class, CannotWriteException::class)
		fun delete(f: AudioFile) {
			defaultAudioFileIO!!.deleteTag(f)
		}

		/**
		 * This method returns the default instance for static use.<br></br>
		 *
		 * @return The default instance.
		 */
		val defaultAudioFileIO: AudioFileIO?
			get() {
				if (defaultInstance == null) {
					defaultInstance = AudioFileIO()
				}
				return defaultInstance
			}

		/**
		 *
		 * Read the tag contained in the given file.
		 *
		 *
		 * @param f The file to read.
		 * @param ext The extension to be used.
		 * @return The AudioFile with the file tag and the file encoding info.
		 * @throws CannotReadException If the file could not be read, the extension wasn't
		 * recognized, or an IO error occurred during the read.
		 * @throws TagException
		 * @throws ReadOnlyFileException
		 * @throws IOException
		 * @throws InvalidAudioFrameException
		 */
		@Throws(
			CannotReadException::class,
			IOException::class,
			TagException::class,
			ReadOnlyFileException::class,
			InvalidAudioFrameException::class
		)
		fun readAs(f: File?, ext: String?): AudioFile {
			return defaultAudioFileIO!!.readFileAs(f, ext)
		}

		/**
		 *
		 * Read the tag contained in the given file.
		 *
		 *
		 * @param f The file to read.
		 * @return The AudioFile with the file tag and the file encoding info.
		 * @throws CannotReadException If the file could not be read, the extension wasn't
		 * recognized, or an IO error occurred during the read.
		 * @throws TagException
		 * @throws ReadOnlyFileException
		 * @throws IOException
		 * @throws InvalidAudioFrameException
		 */
		@Throws(
			CannotReadException::class,
			IOException::class,
			TagException::class,
			ReadOnlyFileException::class,
			InvalidAudioFrameException::class
		)

		fun readMagic(f: File): AudioFile {
			return defaultAudioFileIO!!.readFileMagic(f)
		}

		fun readMagic(fd: FileDescriptor): AudioFile {
			return defaultAudioFileIO!!.readFileMagic(fd)
		}

		fun readMagic(fc: FileChannel): AudioFile {
			return defaultAudioFileIO!!.readFileMagic(fc)
		}

		/**
		 *
		 * Read the tag contained in the given file.
		 *
		 *
		 * @param f The file to read.
		 * @return The AudioFile with the file tag and the file encoding info.
		 * @throws CannotReadException If the file could not be read, the extension wasn't
		 * recognized, or an IO error occurred during the read.
		 * @throws TagException
		 * @throws ReadOnlyFileException
		 * @throws IOException
		 * @throws InvalidAudioFrameException
		 */
		@Throws(
			CannotReadException::class,
			IOException::class,
			TagException::class,
			ReadOnlyFileException::class,
			InvalidAudioFrameException::class
		)
		fun read(f: File?): AudioFile {
			return defaultAudioFileIO!!.readFile(f)
		}

		/**
		 *
		 * Write the tag contained in the audioFile in the actual file on the disk.
		 *
		 *
		 * @param f The AudioFile to be written
		 * @throws NoWritePermissionsException if the file could not be written to due to file permissions
		 * @throws CannotWriteException If the file could not be written/accessed, the extension
		 * wasn't recognized, or other IO error occurred.
		 */
		@Throws(CannotWriteException::class)
		fun write(f: AudioFile) {
			defaultAudioFileIO!!.writeFile(f, null)
		}

		/**
		 *
		 * Write the tag contained in the audioFile in the actual file on the disk.
		 *
		 *
		 * @param f The AudioFile to be written
		 * @param targetPath The AudioFile path to which to be written without the extension. Cannot be null
		 * @throws NoWritePermissionsException if the file could not be written to due to file permissions
		 * @throws CannotWriteException If the file could not be written/accessed, the extension
		 * wasn't recognized, or other IO error occurred.
		 */
		@Throws(CannotWriteException::class)
		fun writeAs(f: AudioFile, targetPath: String?) {
			if (targetPath == null || targetPath.isEmpty()) {
				throw CannotWriteException(
					"Not a valid target path: $targetPath"
				)
			}
			defaultAudioFileIO!!.writeFile(f, targetPath)
		}
	}
}
