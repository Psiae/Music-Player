package com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.readFourBytesAsChars
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets

/**
 * Each [Chunk] starts with a chunk header consisting of a 4 byte id and then a 4 byte size field, the size field
 * stores the size of the chunk itself excluding the size of the header.
 */
class ChunkHeader(private val byteOrder: ByteOrder) {
	/**
	 * Returns the chunk size (excluding the first 8 bytes).
	 *
	 * @see .setSize
	 */
	/**
	 * Set chunk size.
	 *
	 * @param size chunk size without header
	 * @see .getSize
	 */
	var size // This does not include the 8 bytes of header itself
		: Long = 0
	/**
	 * Returns the chunk type, which is a 4-character code.
	 *
	 * @return id
	 */
	/**
	 * Sets the chunk type, which is a 4-character code, directly.
	 *
	 * @param id 4-char id
	 */
	var id // Four character Id of the chunk
		: String? = null

	/** The start of this chunk(header) in the file  */
	var startLocationInFile: Long = 0
		private set

	/**
	 * Reads the header of a chunk.
	 *
	 * @return `true`, if we were able to read a chunk header and believe we found a valid chunk id.
	 */
	@Throws(IOException::class)
	fun readHeader(fc: FileChannel): Boolean {
		val header = ByteBuffer.allocate(CHUNK_HEADER_SIZE)
		startLocationInFile = fc.position()
		fc.read(header)
		header.order(byteOrder)
		header.position(0)
		id = readFourBytesAsChars(header)
		size = Integer.toUnsignedLong(header.int)
		return true
	}

	/**
	 * Reads the header of a chunk.
	 *
	 * @return `true`, if we were able to read a chunk header and believe we found a valid chunk id.
	 */
	@Throws(IOException::class)
	fun readHeader(raf: RandomAccessFile): Boolean {
		val header = ByteBuffer.allocate(CHUNK_HEADER_SIZE)
		startLocationInFile = raf.filePointer
		raf.channel.read(header)
		header.order(byteOrder)
		header.position(0)
		id = readFourBytesAsChars(header)
		size = header.int.toLong()
		return true
	}

	/**
	 * Writes this chunk header to a [ByteBuffer].
	 *
	 * @return the byte buffer containing the
	 */
	fun writeHeader(): ByteBuffer {
		val bb = ByteBuffer.allocate(CHUNK_HEADER_SIZE)
		bb.order(byteOrder)
		bb.put(id!!.toByteArray(StandardCharsets.US_ASCII))
		bb.putInt(size.toInt())
		bb.flip()
		return bb
	}

	override fun toString(): String {
		return id + ":Size:" + size + "startLocation:" + startLocationInFile
	}

	companion object {
		const val CHUNK_HEADER_SIZE = 8
	}
}
