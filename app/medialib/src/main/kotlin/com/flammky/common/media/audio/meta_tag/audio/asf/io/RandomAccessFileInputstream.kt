package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io

import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile

/**
 * Wraps a [RandomAccessFile] into an [InputStream].<br></br>
 *
 * @author Christian Laireiter
 */
class RandomAccessFileInputstream(file: RandomAccessFile?) : InputStream() {
	/**
	 * The file access to read from.<br></br>
	 */
	private val source: RandomAccessFile

	/**
	 * Creates an instance that will provide [InputStream] functionality
	 * on the given [RandomAccessFile] by delegating calls.<br></br>
	 *
	 * @param file The file to read.
	 */
	init {
		requireNotNull(file) { "null" }
		source = file
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun read(): Int {
		return source.read()
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun read(buffer: ByteArray, off: Int, len: Int): Int {
		return source.read(buffer, off, len)
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun skip(amount: Long): Long {
		require(amount >= 0) { "invalid negative value" }
		var left = amount
		while (left > Int.MAX_VALUE) {
			source.skipBytes(Int.MAX_VALUE)
			left -= Int.MAX_VALUE
		}
		return source.skipBytes(left.toInt()).toLong()
	}
}
