package com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.field

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getSizeBEInt32
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4BoxHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagField
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagTextField
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.Mp4FieldKey
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.Mp4TagField
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.atom.Mp4DataBox
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.atom.Mp4MeanBox
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.atom.Mp4NameBox
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Represents reverse dns field, used for custom information
 *
 *
 * Originally only used by Itunes for information that was iTunes specific but now used in a wide range of uses,
 * for example Musicbrainz uses it for many of its fields.
 *
 * These fields have a more complex setup
 * Box ----  shows this is a reverse dns metadata field
 * Box mean  the issuer in the form of reverse DNS domain (e.g com.apple.iTunes)
 * Box name  descriptor identifying the type of contents
 * Box data  contents
 *
 * The raw data passed starts from the mean box
 */
class Mp4TagReverseDnsField : Mp4TagField, TagTextField {
	protected var dataSize = 0
	/**
	 * @return the issuer
	 */
	/**
	 * Set the issuer, usually reverse dns of the Companies domain
	 *
	 * @param issuer
	 */
	//Issuer
	var issuer: String? = null
	/**
	 * @return the descriptor
	 */
	/**
	 * Set the descriptor for the data (what type of data it is)
	 *
	 * @param descriptor
	 */
	//Descriptor
	var descriptor: String? = null

	//Data Content,
	//TODO assuming always text at the moment
	override var content: String? = null

	/**
	 * Construct from existing file data
	 *
	 * @param parentHeader
	 * @param data
	 * @throws UnsupportedEncodingException
	 */
	constructor(parentHeader: Mp4BoxHeader?, data: ByteBuffer) : super(parentHeader, data)

	/**
	 * Newly created Reverse Dns field
	 *
	 * @param id
	 * @param content
	 */
	constructor(id: Mp4FieldKey, content: String?) : super(id.fieldName) {
		issuer = id.issuer
		descriptor = id.identifier
		this.content = content
	}

	/**
	 * Newly created Reverse Dns field bypassing the Mp4TagField enum for creation of temporary reverse dns fields
	 * @param fieldName
	 * @param issuer
	 * @param identifier
	 * @param content
	 */
	constructor(fieldName: String?, issuer: String?, identifier: String?, content: String?) : super(
		fieldName
	) {
		this.issuer = issuer
		descriptor = identifier
		this.content = content
	}

	override val fieldType: Mp4FieldType
		get() = Mp4FieldType.TEXT


	@Throws(UnsupportedEncodingException::class)
	override fun build(data: ByteBuffer) {
		//Read mean box, set the issuer and skip over data
		val meanBoxHeader = Mp4BoxHeader(data)
		val meanBox = Mp4MeanBox(meanBoxHeader, data)
		issuer = meanBox.issuer
		data.position(data.position() + meanBoxHeader.dataLength)

		//Read name box, identify what type of field it is
		val nameBoxHeader = Mp4BoxHeader(data)
		val nameBox = Mp4NameBox(nameBoxHeader, data)
		descriptor = nameBox.name
		data.position(data.position() + nameBoxHeader.dataLength)

		//Issue 198:There is not actually a data atom there cannot cant be because no room for one
		if (parentHeader?.dataLength == meanBoxHeader.length + nameBoxHeader.length) {
			id = IDENTIFIER + ":" + issuer + ":" + descriptor
			content = ""
			logger.warning(ErrorMessage.MP4_REVERSE_DNS_FIELD_HAS_NO_DATA.getMsg(id))
		} else {
			//Read data box, identify the data
			val dataBoxHeader = Mp4BoxHeader(data)
			val dataBox = Mp4DataBox(dataBoxHeader, data)
			content = dataBox.content
			data.position(data.position() + dataBoxHeader.dataLength)

			//Now calculate the id which in order to be unique needs to use all htree values
			id = IDENTIFIER + ":" + issuer + ":" + descriptor
		}
	}

	override fun copyContent(field: TagField?) {
		if (field is Mp4TagReverseDnsField) {
			issuer = field.issuer
			descriptor = field.descriptor
			content = field.content
		}
	}

	override val dataBytes: ByteArray
		get() = content
			?.toByteArray(encoding!!)
			?: byteArrayOf()


	/* Not allowed */
	override var encoding: Charset?
		get() = StandardCharsets.UTF_8
		set(s) {
			/* Not allowed */
		}//This should never happen as were not actually writing to/from a file//Create Meanbox data

	//Create Namebox data

	//Create DataBox data if we have data only
	//Now wrap with reversedns box
	/**
	 * Convert back to raw content, includes ----,mean,name and data atom as views as one thing externally
	 *
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	@get:Throws(UnsupportedEncodingException::class)
	override val rawContent: ByteArray
		get() = try {
			val baos = ByteArrayOutputStream()

			//Create Meanbox data
			val issuerRawData = issuer!!.toByteArray(encoding!!)
			baos.write(getSizeBEInt32(Mp4BoxHeader.HEADER_LENGTH + Mp4MeanBox.PRE_DATA_LENGTH + issuerRawData.size))
			baos.write(Mp4MeanBox.IDENTIFIER.toByteArray(StandardCharsets.ISO_8859_1))
			baos.write(byteArrayOf(0, 0, 0, 0))
			baos.write(issuerRawData)

			//Create Namebox data
			val nameRawData = descriptor!!.toByteArray(encoding!!)
			baos.write(getSizeBEInt32(Mp4BoxHeader.HEADER_LENGTH + Mp4NameBox.PRE_DATA_LENGTH + nameRawData.size))
			baos.write(Mp4NameBox.IDENTIFIER.toByteArray(StandardCharsets.ISO_8859_1))
			baos.write(byteArrayOf(0, 0, 0, 0))
			baos.write(nameRawData)

			//Create DataBox data if we have data only
			if (content!!.length > 0) {
				baos.write(rawContentDataOnly)
			}
			//Now wrap with reversedns box
			val outerbaos = ByteArrayOutputStream()
			outerbaos.write(getSizeBEInt32(Mp4BoxHeader.HEADER_LENGTH + baos.size()))
			outerbaos.write(IDENTIFIER.toByteArray(StandardCharsets.ISO_8859_1))
			outerbaos.write(baos.toByteArray())
			outerbaos.toByteArray()
		} catch (ioe: IOException) {
			//This should never happen as were not actually writing to/from a file
			throw RuntimeException(ioe)
		}

	override val rawContentDataOnly: ByteArray
		get() {
			logger.fine("Getting Raw data for:" + id)
			return try {
				//Create DataBox data
				val baos = ByteArrayOutputStream()
				val dataRawData = content!!.toByteArray(encoding!!)
				baos.write(getSizeBEInt32(Mp4BoxHeader.HEADER_LENGTH + Mp4DataBox.PRE_DATA_LENGTH + dataRawData.size))
				baos.write(Mp4DataBox.IDENTIFIER.toByteArray(StandardCharsets.ISO_8859_1))
				baos.write(byteArrayOf(0))
				baos.write(byteArrayOf(0, 0, fieldType.fileClassId.toByte()))
				baos.write(byteArrayOf(0, 0, 0, 0))
				baos.write(dataRawData)
				baos.toByteArray()
			} catch (ioe: IOException) {
				//This should never happen as were not actually writing to/from a file
				throw RuntimeException(ioe)
			}
		}

	override val isBinary: Boolean
		get() = false
	override val isEmpty: Boolean
		get() = "" == content!!.trim { it <= ' ' }

	override fun toDescriptiveString(): String {
		return content!!
	}

	companion object {
		const val IDENTIFIER = "----"
	}
}
