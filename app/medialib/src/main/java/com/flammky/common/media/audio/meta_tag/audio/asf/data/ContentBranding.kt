package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import java.io.IOException
import java.io.OutputStream
import java.math.BigInteger
import java.nio.charset.StandardCharsets

/**
 * This structure represents the value of the content branding object, which
 * stores the banner image, the banner image URL and the copyright URL.<br></br>
 *
 * @author Christian Laireiter
 */
class ContentBranding(
	pos: Long = 0,
	size: BigInteger = BigInteger.ZERO
) : MetadataContainer(ContainerType.CONTENT_BRANDING, pos, size) {
	/**
	 * Returns the banner image URL.
	 *
	 * @return the banner image URL.
	 */
	/**
	 * This method sets the banner image URL, if `imageURL` is not
	 * blank.<br></br>
	 *
	 * @param imageURL image URL to set.
	 */
	var bannerImageURL: String?
		get() = getValueFor(KEY_BANNER_URL)
		set(imageURL) {
			if (Utils.isBlank(imageURL)) {
				removeDescriptorsByName(KEY_BANNER_URL)
			} else {
				assertDescriptor(KEY_BANNER_URL).setStringValue(imageURL)
			}
		}
	/**
	 * Returns the copyright URL.
	 *
	 * @return the banner image URL.
	 */
	/**
	 * This method sets the copyright URL, if `copyRight` is not
	 * blank.<br></br>
	 *
	 * @param copyRight copyright URL to set.
	 */
	var copyRightURL: String?
		get() = getValueFor(KEY_COPYRIGHT_URL)
		set(copyRight) {
			if (Utils.isBlank(copyRight)) {
				removeDescriptorsByName(KEY_COPYRIGHT_URL)
			} else {
				assertDescriptor(KEY_COPYRIGHT_URL).setStringValue(copyRight)
			}
		}

	override val currentAsfChunkSize: Long
		get() {
			// GUID, size, image type, image data size, image url data size,
			// copyright data size
			var result: Long = 40
			result += assertDescriptor(
				KEY_BANNER_IMAGE,
				MetadataDescriptor.TYPE_BINARY
			).rawDataSize.toLong()
			result += bannerImageURL!!.length.toLong()
			result += copyRightURL!!.length.toLong()
			return result
		}

	/**
	 * Returns the binary image data.
	 *
	 * @return binary image data.
	 */
	val imageData: ByteArray
		get() = assertDescriptor(KEY_BANNER_IMAGE, MetadataDescriptor.TYPE_BINARY).rawData

	/**
	 * Returns the image type.<br></br>
	 *
	 * @return image type
	 * @see .KEY_BANNER_TYPE for known/valid values.
	 */
	val imageType: Long
		get() {
			if (!hasDescriptor(KEY_BANNER_TYPE)) {
				val descriptor = MetadataDescriptor(
					ContainerType.CONTENT_BRANDING,
					KEY_BANNER_TYPE,
					MetadataDescriptor.TYPE_DWORD
				)
				descriptor.setDWordValue(0)
				addDescriptor(descriptor)
			}
			return assertDescriptor(KEY_BANNER_TYPE).getNumber()
		}

	/**
	 * {@inheritDoc}
	 */
	override fun isAddSupported(descriptor: MetadataDescriptor): Boolean {
		return ALLOWED!!.contains(descriptor.name) && super.isAddSupported(descriptor)
	}

	/**
	 * @param imageType
	 * @param imageData
	 */
	fun setImage(imageType: Long, imageData: ByteArray) {
		assert(imageType >= 0 && imageType <= 3)
		assert(imageType > 0 || imageData.size == 0)
		assertDescriptor(KEY_BANNER_TYPE, MetadataDescriptor.TYPE_DWORD).setDWordValue(imageType)
		assertDescriptor(KEY_BANNER_IMAGE, MetadataDescriptor.TYPE_BINARY).setBinaryValue(imageData)
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun writeInto(out: OutputStream): Long {
		val chunkSize = currentAsfChunkSize
		out.write(guid.bytes)
		Utils.writeUINT64(chunkSize, out)
		Utils.writeUINT32(
			imageType, out
		)
		assert(imageType >= 0 && imageType <= 3)
		val imageData = imageData
		assert(imageType > 0 || imageData.size == 0)
		Utils.writeUINT32(imageData.size.toLong(), out)
		out.write(imageData)
		Utils.writeUINT32(
			bannerImageURL!!.length.toLong(), out
		)
		out.write(bannerImageURL!!.toByteArray(StandardCharsets.US_ASCII))
		Utils.writeUINT32(
			copyRightURL!!.length.toLong(), out
		)
		out.write(copyRightURL!!.toByteArray(StandardCharsets.US_ASCII))
		return chunkSize
	}

	companion object {
		/**
		 * Stores the allowed [descriptor][MetadataDescriptor.getName].
		 */
		var ALLOWED: MutableSet<String>? = null

		/**
		 * Descriptor key representing the banner image.
		 */
		const val KEY_BANNER_IMAGE = "BANNER_IMAGE"

		/**
		 * Descriptor key representing the banner image type.<br></br>
		 * <br></br>
		 * **Known/valid values are:**
		 *
		 *  1. 0: there is no image present
		 *  1. 1: there is a BMP image
		 *  1. 2: there is a JPEG image
		 *  1. 3: there is a GIF image
		 *
		 */
		const val KEY_BANNER_TYPE = "BANNER_IMAGE_TYPE"

		/**
		 * Descriptor key representing the banner image URL.
		 */
		const val KEY_BANNER_URL = "BANNER_IMAGE_URL"

		/**
		 * Descriptor key representing the copyright URL.
		 */
		const val KEY_COPYRIGHT_URL = "COPYRIGHT_URL"

		init {
			ALLOWED = HashSet()
			val ALLOWED = ALLOWED!!
			ALLOWED.add(KEY_BANNER_IMAGE)
			ALLOWED.add(KEY_BANNER_TYPE)
			ALLOWED.add(KEY_BANNER_URL)
			ALLOWED.add(KEY_COPYRIGHT_URL)
		}
	}
}
