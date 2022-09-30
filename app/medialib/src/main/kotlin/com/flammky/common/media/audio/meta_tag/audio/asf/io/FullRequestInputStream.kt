package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/**
 * This implementation repeatedly reads from the wrapped input stream until the
 * requested amount of bytes are read.<br></br>
 *
 * @author Christian Laireiter
 */
class FullRequestInputStream
/**
 * Creates an instance.
 *
 * @param source stream to read from.
 */
	(source: InputStream?) : FilterInputStream(source) {
	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun read(buffer: ByteArray): Int {
		return read(buffer, 0, buffer.size)
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun read(buffer: ByteArray, off: Int, len: Int): Int {
		var totalRead = 0
		var read: Int
		while (totalRead < len) {
			read = super.read(buffer, off + totalRead, len - totalRead)
			if (read >= 0) {
				totalRead += read
			}
			if (read == -1) {
				throw IOException((len - totalRead).toString() + " more bytes expected.")
			}
		}
		return totalRead
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun skip(amount: Long): Long {
		var skipped: Long = 0
		var zeroSkipCnt = 0
		var currSkipped: Long
		while (skipped < amount) {
			currSkipped = super.skip(amount - skipped)
			if (currSkipped == 0L) {
				zeroSkipCnt++
				if (zeroSkipCnt == 2) {
					// If the skip value exceeds streams size, this and the
					// number is extremely large, this can lead to a very long
					// running loop.
					break
				}
			}
			skipped += currSkipped
		}
		return skipped
	}
}
