package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp3

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.AudioFile
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotWriteException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.AudioFileWriter
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import java.io.IOException
import java.io.RandomAccessFile

/**
 * Write Mp3 Info (retrofitted to entagged ,done differently to entagged which is why some methods throw RuntimeException)
 * because done elsewhere
 */
class MP3FileWriter : AudioFileWriter() {

	@Throws(CannotWriteException::class)
	fun deleteTag(f: AudioFile) {
		//Because audio file is an instanceof MP3File this directs it to save
		//taking into account if the tag has been sent to null in which case it will be deleted
		f.commit()
	}

	@Throws(CannotWriteException::class)
	fun writeFile(f: AudioFile) {
		//Because audio file is an instanceof MP3File this directs it to save
		f.commit()
	}

	/**
	 * Delete the Id3v1 and ID3v2 tags from file
	 *
	 * @param af
	 * @throws CannotReadException
	 * @throws CannotWriteException
	 */
	@Synchronized
	@Throws(CannotReadException::class, CannotWriteException::class)
	override fun delete(af: AudioFile) {
		(af as MP3File).iD3v1Tag = null
		af.setID3v2TagAs24(null)
		af.commit()
	}

	@Throws(CannotWriteException::class, IOException::class)
	override fun writeTag(
		audioFile: AudioFile?,
		tag: Tag,
		raf: RandomAccessFile,
		rafTemp: RandomAccessFile
	) {
		throw RuntimeException("MP3FileReaderwriteTag should not be called")
	}

	@Throws(CannotWriteException::class, IOException::class)
	override fun deleteTag(tag: Tag?, raf: RandomAccessFile?, tempRaf: RandomAccessFile?) {
		throw RuntimeException("MP3FileReader.getEncodingInfo should be called")
	}
}
