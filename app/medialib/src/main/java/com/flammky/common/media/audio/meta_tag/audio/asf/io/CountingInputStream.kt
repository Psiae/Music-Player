package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/**
 * This implementation of [FilterInputStream] counts each read byte.<br></br>
 * So at each time, with [.getReadCount] one can determine how many
 * bytes have been read, by this classes read and skip methods (mark and reset
 * are also taken into account).<br></br>
 *
 * @author Christian Laireiter
 */
internal class CountingInputStream
/**
 * Creates an instance, which delegates the commands to the given stream.
 *
 * @param stream stream to actually work with.
 */(stream: InputStream?) : FilterInputStream(stream) {
	/**
	 * If [.mark] has been called, the current value of
	 * [.readCount] is stored, in order to reset it upon [.reset].
	 */
	private var markPos: Long = 0
	/**
	 * @return the readCount
	 */
	/**
	 * The amount of read or skipped bytes.
	 */
	@get:Synchronized
	var readCount: Long = 0
		private set

	/**
	 * Counts the given amount of bytes.
	 *
	 * @param amountRead number of bytes to increase.
	 */
	@Synchronized
	private fun bytesRead(amountRead: Long) {
		if (amountRead >= 0) {
			readCount += amountRead
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Synchronized
	override fun mark(readlimit: Int) {
		super.mark(readlimit)
		markPos = readCount
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun read(): Int {
		val result = super.read()
		bytesRead(1)
		return result
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun read(destination: ByteArray, off: Int, len: Int): Int {
		val result = super.read(destination, off, len)
		bytesRead(result.toLong())
		return result
	}

	/**
	 * {@inheritDoc}
	 */
	@Synchronized
	@Throws(IOException::class)
	override fun reset() {
		super.reset()
		synchronized(this) { readCount = markPos }
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun skip(amount: Long): Long {
		val skipped = super.skip(amount)
		bytesRead(skipped)
		return skipped
	}
}
