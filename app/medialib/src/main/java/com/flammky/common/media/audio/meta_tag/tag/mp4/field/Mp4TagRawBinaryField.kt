package com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.field

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getSizeBEInt32
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4BoxHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagField
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.Mp4TagField
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * Represents raw binary data
 *
 *
 * We use this when we find an atom under the ilst atom that we do not recognise , that does not
 * follow standard conventions in order to save the data without modification so it can be safetly
 * written back to file
 */

/** [org.jaudiotagger.tag.mp4.field.Mp4TagRawBinaryField] */
class Mp4TagRawBinaryField(header: Mp4BoxHeader, raw: ByteBuffer) : Mp4TagField(header.id) {

	var dataSize: Int
		protected set

	var data: ByteArray = byteArrayOf()

	/**
	 * Construct binary field from rawdata of audio file
	 *
	 * @param header
	 * @param raw
	 * @throws UnsupportedEncodingException
	 */
	init {
		dataSize = header.dataLength
		build(raw)
	}

	override val fieldType: Mp4FieldType
		get() = Mp4FieldType.IMPLICIT

	override val dataBytes: ByteArray
		get() = data

	/**
	 * Build from data
	 *
	 *
	 * After returning buffers position will be after the end of this atom
	 *
	 * @param raw
	 */
	override fun build(raw: ByteBuffer) {
		//Read the raw data into byte array
		data = ByteArray(dataSize)
		for (i in data.indices) {
			data[i] = raw.get()
		}
	}

	override val isBinary: Boolean
		get() = true
	override val isEmpty: Boolean
		get() = data.size == 0

	override fun copyContent(field: TagField?) {
		throw UnsupportedOperationException("not done")
	}

	//This should never happen as were not actually writing to/from a file
	@get:Throws(UnsupportedEncodingException::class)
	override val rawContent: ByteArray
		get() {
			logger.fine("Getting Raw data for: $id")
			return try {
				val outerbaos = ByteArrayOutputStream()
				outerbaos.write(getSizeBEInt32(Mp4BoxHeader.HEADER_LENGTH + dataSize))
				outerbaos.write(id?.toByteArray(StandardCharsets.ISO_8859_1))
				outerbaos.write(data)
				outerbaos.toByteArray()
			} catch (ioe: IOException) {
				//This should never happen as were not actually writing to/from a file
				throw RuntimeException(ioe)
			}
		}

	override fun toDescriptiveString(): String = "Mp4TagRawBinaryField@${hashCode()}"
}
