package com.flammky.musicplayer.common.media.audio.meta_tag.audio.flac.metadatablock

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.FieldKey
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidFrameException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagField
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.PictureTypes
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.logging.Logger

/**
 * Picture Block
 *
 *
 * pThis block is for storing pictures associated with the file, most commonly cover art from CDs.
 * There may be more than one PICTURE block in a file. The picture format is similar to the APIC frame in ID3v2.
 * The PICTURE block has a type, MIME type, and UTF-8 description like ID3v2, and supports external linking via URL
 * (though this is discouraged). The differences are that there is no uniqueness constraint on the description field,
 * and the MIME type is mandatory. The FLAC PICTURE block also includes the resolution, color depth, and palette size
 * so that the client can search for a suitable picture without having to scan them all
 *
 * Format:
 * Size in bits Info
 * 32 The picture type according to the ID3v2 APIC frame: (There may only be one each of picture type 1 and 2 in a file)
 * 32 	The length of the MIME type string in bytes.
 * n*8 	The MIME type string, in printable ASCII characters 0x20-0x7e. The MIME type may also be -- to signify that the data part is a URL of the picture instead of the picture data itself.
 * 32 	The length of the description string in bytes.
 * n*8 	The description of the picture, in UTF-8.
 * 32 	The width of the picture in pixels.
 * 32 	The height of the picture in pixels.
 * 32 	The color depth of the picture in bits-per-pixel.
 * 32 	For indexed-color pictures (e.g. GIF), the number of colors used, or 0 for non-indexed pictures.
 * 32 	The length of the picture data in bytes.
 * n*8 	The binary picture data.
 */
class MetadataBlockDataPicture : MetadataBlockData, TagField {
	private var mimeTypeSize = 0
	private var descriptionSize = 0
	private var lengthOfPictureInBytes = 0

	var pictureType = 0
		private set
	var mimeType = ""
		private set
	var description = ""
		private set
	var width = 0
		private set
	var height = 0
		private set
	var colourDepth = 0
		private set
	var indexedColourCount = 0
		private set
	var imageData: ByteArray? = null
		private set


	override val id: String
		get() = FieldKey.COVER_ART.name

	override val rawContent: ByteArray?
		@Throws(UnsupportedEncodingException::class)
		get() = bytes.array()
	override val isBinary: Boolean
		get() = true
	override val isCommon: Boolean
		get() = true
	override val isEmpty: Boolean
		get() = false

	@Throws(IOException::class, InvalidFrameException::class)
	private fun initFromByteBuffer(rawdata: ByteBuffer) {
		//Picture Type
		pictureType = rawdata.int
		if (pictureType >= PictureTypes.instanceOf.getSize()) {
			throw InvalidFrameException("PictureType was:" + pictureType + "but the maximum allowed is " + (PictureTypes.instanceOf.getSize() - 1))
		}

		//MimeType
		mimeTypeSize = rawdata.int
		if (mimeTypeSize < 0) {
			throw InvalidFrameException("PictureType mimeType size was invalid:$mimeTypeSize")
		}
		mimeType = getString(rawdata, mimeTypeSize, StandardCharsets.ISO_8859_1.name())

		//Description
		descriptionSize = rawdata.int
		if (descriptionSize < 0) {
			throw InvalidFrameException("PictureType descriptionSize size was invalid:$mimeTypeSize")
		}
		description = getString(rawdata, descriptionSize, StandardCharsets.UTF_8.name())

		//Image width
		width = rawdata.int

		//Image height
		height = rawdata.int

		//Colour Depth
		colourDepth = rawdata.int

		//Indexed Colour Count
		indexedColourCount = rawdata.int
		lengthOfPictureInBytes = rawdata.int

		//ImageData
		if (lengthOfPictureInBytes > rawdata.remaining()) {
			throw InvalidFrameException("PictureType Size was:" + lengthOfPictureInBytes + " but remaining bytes size " + rawdata.remaining())
		}
		imageData = ByteArray(lengthOfPictureInBytes)
		rawdata[imageData]
		logger.config(
			"Read image:$this"
		)
	}

	/**
	 * Initialize MetaBlockDataPicture from byteBuffer
	 *
	 * @param rawdata
	 * @throws IOException
	 * @throws InvalidFrameException
	 */
	constructor(rawdata: ByteBuffer) {
		initFromByteBuffer(rawdata)
	}

	/**
	 * Construct picture block by reading from file, the header informs us how many bytes we should be reading from
	 *
	 * @param header
	 * @param fc
	 * @throws IOException
	 * @throws InvalidFrameException
	 */
	constructor(header: MetadataBlockHeader, fc: FileChannel) {
		if (header.dataLength == 0) {
			throw IOException("MetadataBlockDataPicture HeaderDataSize is zero")
		}
		val rawdata = ByteBuffer.allocate(header.dataLength)
		val bytesRead = fc.read(rawdata)
		if (bytesRead < header.dataLength) {
			throw IOException("Unable to read required number of databytes read:" + bytesRead + ":required:" + header.dataLength)
		}
		rawdata.rewind()
		initFromByteBuffer(rawdata)
	}

	/**
	 * Construct new MetadataPicture block
	 *
	 * @param imageData
	 * @param pictureType
	 * @param mimeType
	 * @param description
	 * @param width
	 * @param height
	 * @param colourDepth
	 * @param indexedColouredCount
	 */
	constructor(
		imageData: ByteArray?,
		pictureType: Int,
		mimeType: String?,
		description: String,
		width: Int,
		height: Int,
		colourDepth: Int,
		indexedColouredCount: Int
	) {
		this.pictureType = pictureType
		if (mimeType != null) {
			this.mimeType = mimeType
		}
		this.description = description
		this.width = width
		this.height = height
		this.colourDepth = colourDepth
		indexedColourCount = indexedColouredCount
		this.imageData = imageData
	}

	@Throws(IOException::class)
	private fun getString(rawdata: ByteBuffer, length: Int, charset: String): String {
		val tempbuffer = ByteArray(length)
		rawdata[tempbuffer]
		return String(tempbuffer, Charset.forName(charset))
	}

	override val bytes: ByteBuffer
		get() = try {
			val baos = ByteArrayOutputStream()
			baos.write(Utils.getSizeBEInt32(pictureType))
			baos.write(Utils.getSizeBEInt32(mimeType.toByteArray(StandardCharsets.ISO_8859_1).size))
			baos.write(mimeType.toByteArray(StandardCharsets.ISO_8859_1))
			baos.write(Utils.getSizeBEInt32(description.toByteArray(StandardCharsets.UTF_8).size))
			baos.write(description.toByteArray(StandardCharsets.UTF_8))
			baos.write(Utils.getSizeBEInt32(width))
			baos.write(Utils.getSizeBEInt32(height))
			baos.write(Utils.getSizeBEInt32(colourDepth))
			baos.write(
				Utils.getSizeBEInt32(
					indexedColourCount
				)
			)
			baos.write(Utils.getSizeBEInt32(imageData!!.size))
			baos.write(imageData)
			ByteBuffer.wrap(baos.toByteArray())
		} catch (ioe: IOException) {
			throw RuntimeException(ioe.message)
		}
	override val length: Int
		get() = bytes.limit()

	/**
	 * @return true if imagedata  is held as a url rather than actually being imagedata
	 */
	val isImageUrl: Boolean
		get() = mimeType == IMAGE_IS_URL

	/**
	 * @return the image url if there is otherwise return an empty String
	 */
	fun getImageUrl(): String {
		return if (isImageUrl) {
			String(imageData!!, 0, imageData!!.size, StandardCharsets.ISO_8859_1)
		} else {
			""
		}
	}

	override fun toDescriptiveString(): String {
		return """		${PictureTypes.instanceOf.getValueForId(pictureType)}
		mimeType:size:$mimeTypeSize:$mimeType
		description:size:$descriptionSize:$description
		width:$width
		height:$height
		colourdepth:$colourDepth
		indexedColourCount:${indexedColourCount}
		image size in bytes:$lengthOfPictureInBytes/${imageData!!.size}
"""
	}

	/**
	 * This method copies the data of the given field to the current data.<br></br>
	 *
	 * @param field The field containing the data to be taken.
	 */
	override fun copyContent(field: TagField?) {
		throw UnsupportedOperationException()
	}

	/**
	 * This method will set the field to represent binary data.<br></br>
	 *
	 * Some implementations may support conversions.<br></br>
	 * As of now (Octobre 2005) there is no implementation really using this
	 * method to perform useful operations.
	 *
	 * @param b `true`, if the field contains binary data.
	 */
	@Deprecated(
		"""As for now is of no use. Implementations should use another
                  way of setting this property."""
	)
	override fun isBinary(b: Boolean) {
		//Do nothing, always true
	}

	companion object {
		const val IMAGE_IS_URL = "-->"

		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.audio.flac.MetadataBlockDataPicture")
	}
}
