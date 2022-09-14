package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io

import java.io.IOException
import java.io.OutputStream

/**
 * This output stream wraps around another [OutputStream] and delegates
 * the write calls.<br></br>
 * Additionally all written bytes are counted and available by
 * [.getCount].
 *
 * @author Christian Laireiter
 */
class CountingOutputstream(outputStream: OutputStream?) : OutputStream() {
	/**
	 * @return the count
	 */
	/**
	 * Stores the amount of bytes written.
	 */
	var count: Long = 0
		private set

	/**
	 * The stream to forward the write calls.
	 */
	private val wrapped: OutputStream?

	/**
	 * Creates an instance which will delegate the write calls to the given
	 * output stream.
	 *
	 * @param outputStream stream to wrap.
	 */
	init {
		assert(outputStream != null)
		wrapped = outputStream
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun close() {
		wrapped!!.close()
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun flush() {
		wrapped!!.flush()
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun write(bytes: ByteArray) {
		wrapped!!.write(bytes)
		count += bytes.size.toLong()
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun write(bytes: ByteArray, off: Int, len: Int) {
		wrapped!!.write(bytes, off, len)
		count += len.toLong()
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun write(toWrite: Int) {
		wrapped!!.write(toWrite)
		count++
	}
}
