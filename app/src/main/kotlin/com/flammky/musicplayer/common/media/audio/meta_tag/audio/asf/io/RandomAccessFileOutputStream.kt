package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io

import java.io.IOException
import java.io.OutputStream
import java.io.RandomAccessFile

/**
 * Wraps a [RandomAccessFile] into an [OutputStream].<br></br>
 *
 * @author Christian Laireiter
 */
class RandomAccessFileOutputStream
/**
 * Creates an instance.<br></br>
 *
 * @param target file to write to.
 */(
	/**
	 * the file to write to.
	 */
	private val targetFile: RandomAccessFile
) : OutputStream() {
	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun write(bytes: ByteArray, off: Int, len: Int) {
		targetFile.write(bytes, off, len)
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun write(toWrite: Int) {
		targetFile.write(toWrite)
	}
}
