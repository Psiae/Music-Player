package com.flammky.musicplayer.common.media.audio.meta_tag.tag.asf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.MetadataDescriptor
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * An `AbstractAsfTagImageField` is an abstract class for representing tag
 * fields containing image data.<br></br>
 *
 * @author Christian Laireiter
 */
abstract class AbstractAsfTagImageField : AsfTagField {
	/**
	 * Creates a image tag field.
	 *
	 * @param field
	 * the ASF field that should be represented.
	 */
	constructor(field: AsfFieldKey?) : super(field)

	/**
	 * Creates an instance.
	 *
	 * @param source
	 * The descriptor which should be represented as a
	 * [TagField].
	 */
	constructor(source: MetadataDescriptor?) : super(source)

	/**
	 * Creates a tag field.
	 *
	 * @param fieldKey
	 * The field identifier to use.
	 */
	constructor(fieldKey: String?) : super(fieldKey)

	/**
	 * This method returns an image instance from the
	 * [image content][.getRawImageData].
	 *
	 * @return the image instance
	 * @throws IOException
	 */
	@get:Throws(IOException::class)
	val image: Bitmap?
		get() = BitmapFactory.decodeStream(ByteArrayInputStream(rawImageData))

	/**
	 * Returns the size of the [image data][.getRawImageData].<br></br>
	 *
	 * @return image data size in bytes.
	 */
	abstract val imageDataSize: Int

	/**
	 * Returns the raw data of the represented image.<br></br>
	 *
	 * @return raw image data
	 */
	abstract val rawImageData: ByteArray
}
