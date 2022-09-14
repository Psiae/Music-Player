package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.GUID
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import java.io.*

/**
 * This class creates a modified copy of an ASF file.<br></br>
 *
 * @author Christian Laireiter
 */
class AsfStreamer {
	/**
	 * Simply copies a chunk from `source` to
	 * `destination`.<br></br>
	 * The method assumes, that the GUID has already been read and will write
	 * the provided one to the destination.<br></br>
	 * The chunk length however will be read and used to determine the amount of
	 * bytes to copy.
	 *
	 * @param guid        GUID of the current chunk.
	 * @param source      source of an ASF chunk, which is to be located at the chunk
	 * length field.
	 * @param destination the destination to copy the chunk to.
	 * @throws IOException on I/O errors.
	 */
	@Throws(IOException::class)
	private fun copyChunk(guid: GUID, source: InputStream, destination: OutputStream) {
		val chunkSize = Utils.readUINT64(source)
		destination.write(guid.bytes)
		Utils.writeUINT64(chunkSize, destination)
		Utils.copy(source, destination, chunkSize - 24)
	}

	/**
	 * Reads the `source` and applies the modifications provided by
	 * the given `modifiers`, and puts it to `dest`.<br></br>
	 * Each [modifier][ChunkModifier] is used only once, so if one
	 * should be used multiple times, it should be added multiple times into the
	 * list.<br></br>
	 *
	 * @param source    the source ASF file
	 * @param dest      the destination to write the modified version to.
	 * @param modifiers list of chunk modifiers to apply.
	 * @throws IOException on I/O errors.
	 */
	@Throws(IOException::class)
	fun createModifiedCopy(
		source: InputStream,
		dest: OutputStream,
		modifiers: List<ChunkModifier>?
	) {
		val modders: MutableList<ChunkModifier> = ArrayList()
		if (modifiers != null) {
			modders.addAll(modifiers)
		}
		// Read and check ASF GUID
		val readGUID = Utils.readGUID(source)
		if (GUID.GUID_HEADER.equals(readGUID)) {
			// used to calculate differences
			var totalDiff: Long = 0
			var chunkDiff: Long = 0

			// read header information
			val headerSize = Utils.readUINT64(source)
			val chunkCount = Utils.readUINT32(source)
			val reserved = ByteArray(2)
			reserved[0] = (source.read() and 0xFF).toByte()
			reserved[1] = (source.read() and 0xFF).toByte()

			/*
			 * bos will get all unmodified and modified header chunks. This is
			 * necessary, because the header chunk (and file properties chunk)
			 * need to be adjusted but are written in front of the others.
			 */
			val bos = ByteArrayOutputStream()
			// fileHeader will get the binary representation of the file
			// properties chunk, without GUID
			var fileHeader: ByteArray? = null

			// Iterate through all chunks
			for (i in 0 until chunkCount) {
				// Read GUID
				val curr = Utils.readGUID(source)
				// special case for file properties chunk
				if (GUID.GUID_FILE.equals(curr)) {
					val tmp = ByteArrayOutputStream()
					val size = Utils.readUINT64(source)
					Utils.writeUINT64(size, tmp)
					Utils.copy(source, tmp, size - 24)
					fileHeader = tmp.toByteArray()
				} else {
					/*
					 * Now look for ChunkModifier objects which modify the
					 * current chunk
					 */
					var handled = false
					var j = 0
					while (j < modders.size && !handled) {
						if (modders[j].isApplicable(curr)) {
							// alter current chunk
							val result = modders[j].modify(curr, source, bos)
							// remember size differences.
							chunkDiff += result.chunkCountDifference.toLong()
							totalDiff += result.byteDifference
							// remove current modifier from index.
							modders.removeAt(j)
							handled = true
						}
						j++
					}
					if (!handled) {
						// copy chunks which are not modified.
						copyChunk(curr, source, bos)
					}
				}
			}
			// Now apply the left modifiers.
			for (curr in modders) {
				// chunks, which were not in the source file, will be added to
				// the destination
				val result = curr.modify(null, null, bos)
				chunkDiff += result.chunkCountDifference.toLong()
				totalDiff += result.byteDifference
			}
			/*
			 * Now all header objects have been read or manipulated and stored
			 * in the internal buffer (bos).
			 */
			// write ASF GUID
			dest.write(readGUID.bytes)
			// write altered header object size
			Utils.writeUINT64(headerSize + totalDiff, dest)
			// write altered number of chunks
			Utils.writeUINT32(chunkCount + chunkDiff, dest)
			// write the reserved 2 bytes (0x01,0x02).
			dest.write(reserved)
			// write the new file header
			modifyFileHeader(ByteArrayInputStream(fileHeader), dest, totalDiff)
			// write the header objects (chunks)
			dest.write(bos.toByteArray())
			// copy the rest of the file (data and index)
			Utils.flush(source, dest)
		} else {
			throw IllegalArgumentException("No ASF header object.")
		}
	}

	/**
	 * This is a slight variation of
	 * [.copyChunk], it only handles file
	 * property chunks correctly.<br></br>
	 * The copied chunk will have the file size field modified by the given
	 * `fileSizeDiff` value.
	 *
	 * @param source       source of file properties chunk, located at its chunk length
	 * field.
	 * @param destination  the destination to copy the chunk to.
	 * @param fileSizeDiff the difference which should be applied. (negative values would
	 * subtract the stored file size)
	 * @throws IOException on I/O errors.
	 */
	@Throws(IOException::class)
	private fun modifyFileHeader(
		source: InputStream,
		destination: OutputStream,
		fileSizeDiff: Long
	) {
		destination.write(GUID.GUID_FILE.bytes)
		val chunkSize = Utils.readUINT64(source)
		Utils.writeUINT64(chunkSize, destination)
		destination.write(Utils.readGUID(source).bytes)
		val fileSize = Utils.readUINT64(source)
		Utils.writeUINT64(fileSize + fileSizeDiff, destination)
		Utils.copy(source, destination, chunkSize - 48)
	}
}
