package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.Chunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.ContentBranding
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.GUID
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import java.io.IOException
import java.io.InputStream

/**
 * This reader is used to read the content branding object of ASF streams.<br></br>
 *
 * @author Christian Laireiter
 * @see ContainerType.CONTENT_BRANDING
 *
 * @see ContentBranding
 */
class ContentBrandingReader
/**
 * Should not be used for now.
 */
protected constructor() : ChunkReader {
	/**
	 * {@inheritDoc}
	 */
	override fun canFail(): Boolean {
		return false
	}

	/**
	 * {@inheritDoc}
	 */
	override val applyingIds: Array<GUID>
		get() = APPLYING.clone()

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun read(guid: GUID?, stream: InputStream, streamPosition: Long): Chunk? {
		assert(GUID.GUID_CONTENT_BRANDING.equals(guid))
		val chunkSize = Utils.readBig64(stream)
		val imageType = Utils.readUINT32(stream)
		assert(imageType >= 0 && imageType <= 3) { imageType }
		val imageDataSize = Utils.readUINT32(stream)
		assert(imageType > 0 || imageDataSize == 0L) { imageDataSize }
		assert(imageDataSize < Int.MAX_VALUE)
		val imageData = Utils.readBinary(stream, imageDataSize)
		val copyRightUrlLen = Utils.readUINT32(stream)
		val copyRight = String(Utils.readBinary(stream, copyRightUrlLen))
		val imageUrlLen = Utils.readUINT32(stream)
		val imageUrl = String(Utils.readBinary(stream, imageUrlLen))
		val result = ContentBranding(streamPosition, chunkSize)
		result.setImage(imageType, imageData)
		result.copyRightURL = copyRight
		result.bannerImageURL = imageUrl
		return result
	}

	companion object {
		/**
		 * The GUID this reader [applies to][.getApplyingIds]
		 */
		private val APPLYING = arrayOf(GUID.GUID_CONTENT_BRANDING)
	}
}
