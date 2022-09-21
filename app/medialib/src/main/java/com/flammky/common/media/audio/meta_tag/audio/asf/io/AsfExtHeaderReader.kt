package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.AsfExtendedHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.GUID
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import java.io.IOException
import java.io.InputStream
import java.math.BigInteger

/**
 * This reader reads an ASF header extension object from an [InputStream]
 * and creates an [AsfExtendedHeader] object.<br></br>
 *
 * @author Christian Laireiter
 */
class AsfExtHeaderReader
/**
 * Creates a reader instance, which only utilizes the given list of chunk
 * readers.<br></br>
 *
 * @param toRegister    List of [ChunkReader] class instances, which are to be
 * utilized by the instance.
 * @param readChunkOnce if `true`, each chunk type (identified by chunk
 * GUID) will handled only once, if a reader is available, other
 * chunks will be discarded.
 */
	(toRegister: List<Class<out ChunkReader?>>, readChunkOnce: Boolean) :
	ChunkContainerReader<AsfExtendedHeader>(toRegister, readChunkOnce) {
	/**
	 * {@inheritDoc}
	 */
	override fun canFail(): Boolean {
		return false
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun createContainer(
		streamPosition: Long,
		chunkLength: BigInteger,
		stream: InputStream
	): AsfExtendedHeader {
		Utils.readGUID(stream) // First reserved field (should be a specific
		// GUID.
		Utils.readUINT16(stream) // Second reserved field (should always be 6)
		val extensionSize = Utils.readUINT32(stream)
		assert(extensionSize == 0L || extensionSize >= 24)
		assert(chunkLength.subtract(BigInteger.valueOf(46)).toLong() == extensionSize)
		return AsfExtendedHeader(streamPosition, chunkLength)
	}

	/**
	 * {@inheritDoc}
	 */
	override val applyingIds: Array<GUID>
		get() = APPLYING.clone()

	companion object {
		/**
		 * The GUID this reader [applies to][.getApplyingIds]
		 */
		private val APPLYING = arrayOf(GUID.GUID_HEADER_EXTENSION)
	}
}
