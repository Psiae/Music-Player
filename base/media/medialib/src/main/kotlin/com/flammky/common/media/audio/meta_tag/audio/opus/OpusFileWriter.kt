package com.flammky.common.media.audio.meta_tag.audio.opus


import com.flammky.musicplayer.common.media.audio.meta_tag.audio.AudioFile
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotWriteException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.AudioFileWriter
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import java.io.IOException
import java.io.RandomAccessFile
import java.util.logging.Logger


/**
 * Write tag data to Opus File
 *
 *
 * Only works for Opus files containing a vorbis stream
 */
class OpusFileWriter : AudioFileWriter() {
	private val vtw = OpusVorbisTagWriter()

	@Throws(CannotReadException::class, CannotWriteException::class, IOException::class)
	override fun writeTag(
		audioFile: AudioFile?,
		tag: Tag,
		raf: RandomAccessFile,
		rafTemp: RandomAccessFile
	) {
		vtw.write(tag, raf, rafTemp)
	}

	@Throws(CannotReadException::class, CannotWriteException::class, IOException::class)
	protected override fun deleteTag(tag: Tag?, raf: RandomAccessFile?, tempRaf: RandomAccessFile?) {
		vtw.delete(raf!!, tempRaf!!)
	}

	companion object {
		var logger = Logger.getLogger("org.jaudiotagger.audio.opus")
	}
}
