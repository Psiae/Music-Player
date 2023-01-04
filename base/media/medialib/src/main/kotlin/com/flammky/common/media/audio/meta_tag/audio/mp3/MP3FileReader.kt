package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp3

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.AudioFile
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.InvalidAudioFrameException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.ReadOnlyFileException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.AudioFileReader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.GenericAudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagException
import java.io.File
import java.io.FileDescriptor
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel

/**
 * Read Mp3 Info (retrofitted to entagged ,done differently to entagged which is why some methods throw RuntimeException)
 * because done elsewhere
 */
class MP3FileReader : AudioFileReader() {

	@Throws(CannotReadException::class, IOException::class)
	override fun getEncodingInfo(raf: RandomAccessFile): GenericAudioHeader {
		throw RuntimeException("MP3FileReader.getEncodingInfo should be called")
	}

	override fun getEncodingInfo(fc: FileChannel): GenericAudioHeader {
		throw RuntimeException("MP3FileReader.getEncodingInfo should be called")
	}

	@Throws(CannotReadException::class, IOException::class)
	override fun getTag(raf: RandomAccessFile): Tag {
		throw RuntimeException("MP3FileReader.getEncodingInfo should be called")
	}

	override fun getTag(fc: FileChannel): Tag {
		throw RuntimeException("MP3FileReader.getEncodingInfo should be called")
	}

	/**
	 * @param f
	 * @return
	 */
	//Override because we read mp3s differently to the entagged code
	@Throws(
		IOException::class,
		TagException::class,
		ReadOnlyFileException::class,
		CannotReadException::class,
		InvalidAudioFrameException::class
	)
	override fun read(f: File): AudioFile {
		return MP3File(f, MP3File.Companion.LOAD_IDV1TAG or MP3File.Companion.LOAD_IDV2TAG, true)
	}

	override fun read(fileDescriptor: FileDescriptor): AudioFile {
		return MP3File(fileDescriptor, MP3File.Companion.LOAD_IDV1TAG or MP3File.Companion.LOAD_IDV2TAG, true)
	}



	/**
	 * Read
	 *
	 * @param f
	 * @return
	 * @throws ReadOnlyFileException thrown if the file is not writable
	 * @throws TagException
	 * @throws IOException
	 * @throws InvalidAudioFrameException
	 */
	@Throws(
		IOException::class,
		TagException::class,
		ReadOnlyFileException::class,
		CannotReadException::class,
		InvalidAudioFrameException::class
	)
	fun readMustBeWritable(f: File): AudioFile {
		return MP3File(f, MP3File.Companion.LOAD_IDV1TAG or MP3File.Companion.LOAD_IDV2TAG, false)
	}
}
