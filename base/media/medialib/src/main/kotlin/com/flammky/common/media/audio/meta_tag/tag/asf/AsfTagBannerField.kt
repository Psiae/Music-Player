package com.flammky.musicplayer.common.media.audio.meta_tag.tag.asf

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.ContainerType
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.MetadataDescriptor

/**
 * This field represents the image content of the banner image which is stored
 * in the [content branding][ContentBranding] chunk of ASF files.<br></br>
 *
 * @author Christian Laireiter
 */
class AsfTagBannerField : AbstractAsfTagImageField {

	private val toWrap
		get() = descriptor

	/**
	 * Creates an instance with no image data.<br></br>
	 */
	constructor() : super(AsfFieldKey.BANNER_IMAGE)

	/**
	 * Creates an instance with given descriptor as image content.<br></br>
	 *
	 * @param descriptor
	 * image content.
	 */
	constructor(descriptor: MetadataDescriptor) : super(descriptor) {
		assert(descriptor.name == AsfFieldKey.BANNER_IMAGE.fieldName)
	}

	/**
	 * Creates an instance with specified data as image content.
	 *
	 * @param imageData
	 * image content.
	 */
	constructor(imageData: ByteArray) : super(
		MetadataDescriptor(
			ContainerType.CONTENT_BRANDING,
			AsfFieldKey.BANNER_IMAGE.fieldName,
			MetadataDescriptor.TYPE_BINARY
		)
	) {
		toWrap.setBinaryValue(imageData)
	}

	/**
	 * {@inheritDoc}
	 */

	/** [org.jaudiotagger.tag.asf.AsfTagBannerField] */
	override val imageDataSize: Int
		get() = toWrap.rawDataSize

	/**
	 * {@inheritDoc}
	 */
	override val rawImageData: ByteArray
		get() = rawContent
}
