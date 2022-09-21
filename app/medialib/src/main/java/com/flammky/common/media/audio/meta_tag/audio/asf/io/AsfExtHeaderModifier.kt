package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.GUID
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger

/**
 * This modifier manipulates an ASF header extension object.
 *
 * @author Christian Laireiter
 */
class AsfExtHeaderModifier(modifiers: List<ChunkModifier>?) : ChunkModifier {
	/**
	 * List of modifiers which are to be applied to contained chunks.
	 */
	private val modifierList: List<ChunkModifier>

	/**
	 * Creates an instance.<br></br>
	 *
	 * @param modifiers modifiers to apply.
	 */
	init {
		assert(modifiers != null)
		modifierList = ArrayList(modifiers)
	}

	/**
	 * Simply copies a chunk from `source` to
	 * `destination`.<br></br>
	 * The method assumes, that the GUID has already been read and will write
	 * the provided one to the destination.<br></br>
	 * The chunk length however will be read and used to determine the amount of
	 * bytes to copy.
	 *
	 * @param guid        GUID of the current CHUNK.
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
	 * {@inheritDoc}
	 */
	override fun isApplicable(guid: GUID): Boolean {
		return GUID.GUID_HEADER_EXTENSION.equals(guid)
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun modify(
		guid: GUID?,
		source: InputStream?,
		destination: OutputStream
	): ModificationResult {
		assert(GUID.GUID_HEADER_EXTENSION == guid)
		source!!

		var difference: Long = 0
		val modders: MutableList<ChunkModifier> = ArrayList(modifierList)
		val occuredGuids: MutableSet<GUID?> = HashSet()
		occuredGuids.add(guid)
		val chunkLen = Utils.readBig64(source)
		val reserved1 = Utils.readGUID(source)
		val reserved2 = Utils.readUINT16(source)
		val dataSize = Utils.readUINT32(source)
		assert(dataSize == 0L || dataSize >= 24)
		assert(chunkLen.subtract(BigInteger.valueOf(46)).toLong() == dataSize)

		/*
		 * Stream buffer for the chunk list
		 */
		val bos = ByteArrayOutputStream()
		/*
		 * Stream which counts read bytes. Dirty but quick way of implementing
		 * this.
		 */
		val cis = CountingInputStream(source)
		while (cis.readCount < dataSize) {
			// read GUID
			val curr = Utils.readGUID(cis)
			var handled = false
			var i = 0
			while (i < modders.size && !handled) {
				if (modders[i].isApplicable(curr)) {
					val modRes = modders[i].modify(curr, cis, bos)
					difference += modRes.byteDifference
					occuredGuids.addAll(modRes.getOccuredGUIDs())
					modders.removeAt(i)
					handled = true
				}
				i++
			}
			if (!handled) {
				occuredGuids.add(curr)
				copyChunk(curr, cis, bos)
			}
		}
		// Now apply the left modifiers.

		/** [org.jaudiotagger.audio.asf.io.AsfExtHeaderModifier] */
		for (curr in modders) {
			// chunks, which were not in the source file, will be added to the
			// destination
			val result = curr.modify(null, null, bos)
			difference += result.byteDifference
			occuredGuids.addAll(result.getOccuredGUIDs())
		}
		destination.write(GUID.GUID_HEADER_EXTENSION.bytes)
		Utils.writeUINT64(chunkLen.add(BigInteger.valueOf(difference)).toLong(), destination)
		destination.write(reserved1.bytes)
		Utils.writeUINT16(reserved2, destination)
		Utils.writeUINT32(dataSize + difference, destination)
		destination.write(bos.toByteArray())
		return ModificationResult(0, difference, occuredGuids)
	}
}
