package com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.AudioFile
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotWriteException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import java.io.IOException
import java.io.RandomAccessFile

/**
 * Created by Paul on 15/09/2015.
 */
interface TagWriter {
	@Throws(IOException::class, CannotWriteException::class)
	fun delete(tag: Tag?, raf: RandomAccessFile?, tempRaf: RandomAccessFile?)

	/**
	 * Write tag to file
	 *
	 * @param tag
	 * @param raf
	 * @param rafTemp
	 * @throws CannotWriteException
	 * @throws IOException
	 */
	@Throws(CannotWriteException::class, IOException::class)
	fun write(af: AudioFile?, tag: Tag?, raf: RandomAccessFile?, rafTemp: RandomAccessFile?)
}
