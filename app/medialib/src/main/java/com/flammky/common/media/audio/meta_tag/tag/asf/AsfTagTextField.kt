package com.flammky.musicplayer.common.media.audio.meta_tag.tag.asf

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.AsfHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.MetadataDescriptor
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils.isBlank
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagTextField
import java.nio.charset.Charset

/**
 * Represents a tag text field for ASF fields.<br></br>
 *
 * @author Christian Laireiter
 */
class AsfTagTextField : AsfTagField, TagTextField {

	private val toWrap
		get() = descriptor

	/**
	 * Creates a tag text field and assigns the string value.
	 *
	 * @param field
	 * ASF field to represent.
	 * @param value
	 * the value to assign.
	 */
	constructor(field: AsfFieldKey?, value: String) : super(field) {
		toWrap.setString(value)
	}

	/**
	 * Creates an instance.
	 *
	 * @param source
	 * The metadata descriptor, whose content is published.<br></br>
	 * Must not be of type [MetadataDescriptor.TYPE_BINARY].
	 */
	constructor(source: MetadataDescriptor) : super(source) {
		require(source.type != MetadataDescriptor.TYPE_BINARY) { "Cannot interpret binary as string." }
	}

	/**
	 * Creates a tag text field and assigns the string value.
	 *
	 * @param fieldKey
	 * The fields identifier.
	 * @param value
	 * the value to assign.
	 */
	constructor(fieldKey: String?, value: String) : super(fieldKey) {
		toWrap.setString(value)
	}

	override var content: String?
		get() = descriptor.getString()
		set(content) {
			descriptor.setString(content.toString())
		}

	override var encoding: Charset?
		get() = AsfHeader.ASF_CHARSET
		set(encoding) {
			require(AsfHeader.ASF_CHARSET == encoding) { "Only UTF-16LE is possible with ASF." }
		}

	/**
	 * @return true if blank or only contains whitespace
	 */
	override val isEmpty: Boolean
		get() = isBlank(
			content
		)
}
