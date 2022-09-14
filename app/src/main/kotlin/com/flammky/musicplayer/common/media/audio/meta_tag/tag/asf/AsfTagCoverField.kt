package com.flammky.musicplayer.common.media.audio.meta_tag.tag.asf

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.AsfHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.MetadataDescriptor
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getIntLE
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getSizeLEInt32
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.ImageFormats
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.ImageFormats.getMimeTypeForBinarySignature
import java.io.ByteArrayOutputStream
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.logging.Logger

/**
 * Encapsulates the WM/Pictures provides some convenience methods for decoding
 * the binary data it contains
 *
 * The value of a WM/Pictures metadata descriptor is as follows:
 *
 * byte0 Picture Type byte1-4 Length of the image data mime type encoded as
 * UTF-16LE null byte null byte description encoded as UTF-16LE (optional) null
 * byte null byte image data
 */
class AsfTagCoverField : AbstractAsfTagImageField {

	private val toWrap
		get() = descriptor

	/**
	 * Description
	 */
	var description: String? = null
		private set

	/**
	 * We need this to retrieve the buffered image, if required
	 */
	private var endOfName = 0

	/**
	 * Image Data Size as read
	 */
	override var imageDataSize = 0
		private set

	/**
	 * Mimetype of binary
	 */
	var mimeType: String? = null
		private set

	/**
	 * Picture Type
	 */
	var pictureType = 0
		private set

	/**
	 * Create New Image Field
	 *
	 * @param imageData
	 * @param pictureType
	 * @param description
	 * @param mimeType
	 */
	constructor(
		imageData: ByteArray, pictureType: Int,
		description: String?, mimeType: String?
	) : super(
		MetadataDescriptor(
			AsfFieldKey.COVER_ART.fieldName,
			MetadataDescriptor.TYPE_BINARY
		)
	) {
		this.descriptor
			.setBinaryValue(
				createRawContent(
					imageData, pictureType, description,
					mimeType
				)
			)
	}

	/**
	 * Creates an instance from a metadata descriptor
	 *
	 * @param source
	 * The metadata descriptor, whose content is published.<br></br>
	 */
	constructor(source: MetadataDescriptor) : super(source) {
		require(source.name == AsfFieldKey.COVER_ART.fieldName) { "Descriptor description must be WM/Picture" }
		require(source.type == MetadataDescriptor.TYPE_BINARY) { "Descriptor type must be binary" }
		try {
			processRawContent()
		} catch (uee: UnsupportedEncodingException) {
			// Should never happen
			throw RuntimeException(uee) // NOPMD by Christian Laireiter on 5/9/09 5:45 PM
		}
	}

	private fun createRawContent(
		data: ByteArray, pictureType: Int,
		description: String?, mimeType: String?
	): ByteArray { // NOPMD by Christian Laireiter on 5/9/09 5:46 PM
		var mimeType = mimeType
		this.description = description
		imageDataSize = data.size
		this.pictureType = pictureType
		this.mimeType = mimeType

		// Get Mimetype from data if not already setField
		if (mimeType == null) {
			mimeType = getMimeTypeForBinarySignature(data)
			// Couldnt identify lets default to png because probably error in
			// code because not 100% sure how to identify
			// formats
			if (mimeType == null) {
				LOGGER.warning(
					ErrorMessage.GENERAL_UNIDENITIFED_IMAGE_FORMAT
						.msg
				)
				mimeType = ImageFormats.MIME_TYPE_PNG
			}
		}
		val baos = ByteArrayOutputStream()

		// PictureType
		baos.write(pictureType)

		// ImageDataSize
		baos.write(getSizeLEInt32(data.size), 0, 4)

		// mimetype
		val mimeTypeData: ByteArray
		mimeTypeData = try {
			mimeType.toByteArray(charset(AsfHeader.ASF_CHARSET.name()))
		} catch (uee: UnsupportedEncodingException) {
			// Should never happen
			throw RuntimeException(
				"Unable to find encoding:" // NOPMD by Christian Laireiter on 5/9/09 5:45 PM
					+ AsfHeader.ASF_CHARSET.name()
			)
		}
		baos.write(mimeTypeData, 0, mimeTypeData.size)

		// Seperator
		baos.write(0x00)
		baos.write(0x00)

		// description
		if (description != null && description.length > 0) {
			val descriptionData: ByteArray
			descriptionData = try {
				description.toByteArray(
					charset(
						AsfHeader.ASF_CHARSET
							.name()
					)
				)
			} catch (uee: UnsupportedEncodingException) {
				// Should never happen
				throw RuntimeException(
					"Unable to find encoding:" // NOPMD by Christian Laireiter on 5/9/09 5:45 PM
						+ AsfHeader.ASF_CHARSET.name()
				)
			}
			baos.write(descriptionData, 0, descriptionData.size)
		}

		// Seperator (always write whther or not we have descriptor field)
		baos.write(0x00)
		baos.write(0x00)

		// Image data
		baos.write(data, 0, data.size)
		return baos.toByteArray()
	}

	/**
	 * @return the raw image data only
	 */
	override val rawImageData: ByteArray
		get() {
			val baos = ByteArrayOutputStream()
			baos.write(rawContent, endOfName, toWrap.rawDataSize - endOfName)
			return baos.toByteArray()
		}

	@Throws(UnsupportedEncodingException::class)
	private fun processRawContent() {
		// PictureType
		pictureType = this.rawContent[0].toInt()

		// ImageDataSize
		imageDataSize = getIntLE(this.rawContent, 1, 2)

		// Set Count to after picture type,datasize and two byte nulls
		var count = 5
		mimeType = null
		description = null // Optional
		var endOfMimeType = 0
		while (count < this.rawContent.size - 1) {
			if (rawContent.get(count).toInt() == 0 && rawContent.get(count + 1)
					.toInt() == 0
			) {
				if (mimeType == null) {
					mimeType =
						String(
							rawContent, 5, count - 5,
							Charset.forName("UTF-16LE")
						)
					endOfMimeType = count + 2
				} else if (description == null) {
					description =
						String(
							rawContent, endOfMimeType, count - endOfMimeType,
							Charset.forName("UTF-16LE")
						)
					endOfName = count + 2
					break
				}
			}
			count += 2 // keep on two byte word boundary
		}
	}

	companion object {
		/**
		 * Logger Object
		 */
		val LOGGER = Logger
			.getLogger("org.jaudiotagger.audio.asf.tag")
	}
}
