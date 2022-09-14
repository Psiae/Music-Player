package com.flammky.musicplayer.common.media.audio.meta_tag.utils

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagOptionSingleton
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel

/** Shift Data to allow metadata to be fitted inside existing file
 */
object ShiftData {
	/**
	 * Shift the remainder of data from current position to position + offset
	 * Reads/writes starting from end of file in chunks so works on large files on low memory systems
	 *
	 * @param  fc
	 * @param  offset (if negative writes the data earlier (i,e smaller file)
	 * @throws IOException
	 * @throws CannotWriteException
	 */
	@JvmStatic
	@Throws(IOException::class)
	fun shiftDataByOffsetToMakeSpace(fc: SeekableByteChannel, offset: Int) {
		val origFileSize = fc.size()
		val startPos = fc.position()
		val amountToBeWritten = fc.size() - startPos
		val chunkSize = TagOptionSingleton.instance.writeChunkSize.toInt()
		val count = amountToBeWritten / chunkSize
		val mod = amountToBeWritten % chunkSize

		//Buffer to hold a chunk
		var chunkBuffer = ByteBuffer.allocate(chunkSize)

		//Start from end of file
		var readPos = fc.size() - chunkSize
		var writePos = fc.size() - chunkSize + offset
		for (i in 0 until count) {
			//Read Data Into Buffer starting from end of file
			fc.position(readPos)
			fc.read(chunkBuffer)

			//Now write to new location
			chunkBuffer.flip()
			fc.position(writePos)
			fc.write(chunkBuffer)

			//Rewind so can use in next iteration of loop
			chunkBuffer.rewind()
			readPos -= chunkSize.toLong()
			writePos -= chunkSize.toLong()
		}
		if (mod > 0) {
			chunkBuffer = ByteBuffer.allocate(mod.toInt())
			fc.position(startPos)
			fc.read(chunkBuffer)

			//Now write to new location
			chunkBuffer.flip()
			fc.position(startPos + offset)
			fc.write(chunkBuffer)
		}
		@Suppress("USELESS_IS_CHECK")
		if (fc is SeekableByteChannel) {
			if (offset < 0) {
				fc.truncate(origFileSize + offset)
			}
		}
	}

	/**
	 * Used by ID3 to shrink space by shrinkBy bytes before current position
	 * @param fc
	 * @param shrinkBy
	 * @throws IOException
	 */
	@Throws(IOException::class)
	fun shiftDataByOffsetToShrinkSpace(fc: SeekableByteChannel, shrinkBy: Int) {
		val startPos = fc.position()
		val amountToBeWritten = fc.size() - startPos
		val chunkSize = TagOptionSingleton.instance.writeChunkSize.toInt()
		val count = amountToBeWritten / chunkSize
		val mod = amountToBeWritten % chunkSize

		//Buffer to hold a chunk
		var chunkBuffer = ByteBuffer.allocate(chunkSize)

		//Start from start of data that needs to be shifted
		var readPos = startPos
		var writePos = startPos - shrinkBy
		for (i in 0 until count) {
			//Read Data Into Buffer starting from start of data that has to be copied
			fc.position(readPos)
			fc.read(chunkBuffer)

			//Now write to new location
			chunkBuffer.flip()
			fc.position(writePos)
			fc.write(chunkBuffer)

			//Rewind so can use in next iteration of loop
			chunkBuffer.rewind()
			readPos += chunkSize.toLong()
			writePos += chunkSize.toLong()
		}
		if (mod > 0) {
			chunkBuffer = ByteBuffer.allocate(mod.toInt())
			fc.position(readPos)
			fc.read(chunkBuffer)

			//Now write to new location
			chunkBuffer.flip()
			fc.position(writePos)
			fc.write(chunkBuffer)
		}
		@Suppress("USELESS_IS_CHECK")
		if (fc is SeekableByteChannel) {
			fc.truncate(fc.position())
		}
	}
}
