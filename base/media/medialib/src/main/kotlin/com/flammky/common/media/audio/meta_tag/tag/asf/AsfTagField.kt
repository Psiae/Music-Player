/*
 * Entagged Audio Tag library
 * Copyright (c) 2004-2005 Christian Laireiter <liree@web.de>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.asf

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.MetadataDescriptor
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagField

/**
 * This class encapsulates a
 * [MetadataDescriptor]and provides access
 * to it. <br></br>
 * The metadata descriptor used for construction is copied.
 *
 * @author Christian Laireiter (liree)
 */
open class AsfTagField : TagField, Cloneable {
	/**
	 * Returns the wrapped metadata descriptor (which actually stores the
	 * values).
	 *
	 * @return the wrapped metadata descriptor
	 */
	/**
	 * This descriptor is wrapped.
	 */
	var descriptor: MetadataDescriptor
		protected set

	/**
	 * Creates a tag field.
	 *
	 * @param field
	 * the ASF field that should be represented.
	 */
	constructor(field: AsfFieldKey?) {
		field!!
		descriptor =
			MetadataDescriptor(field.highestContainer, field.fieldName, MetadataDescriptor.TYPE_STRING)
	}

	/**
	 * Creates an instance.
	 *
	 * @param source
	 * The descriptor which should be represented as a
	 * [TagField].
	 */
	constructor(source: MetadataDescriptor?) {
		assert(source != null)
		// XXX Copy ? maybe not really.
		descriptor = source!!.createCopy()
	}

	/**
	 * Creates a tag field.
	 *
	 * @param fieldKey
	 * The field identifier to use.
	 */
	constructor(fieldKey: String?) {
		assert(fieldKey != null)
		descriptor = MetadataDescriptor(
			AsfFieldKey.getAsfFieldKey(fieldKey).highestContainer, fieldKey!!,
			MetadataDescriptor.TYPE_STRING
		)
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(CloneNotSupportedException::class)
	public override fun clone(): Any {
		return super.clone()
	}

	/**
	 * {@inheritDoc}
	 */
	override fun copyContent(field: TagField?) {
		throw UnsupportedOperationException("Not implemented yet.")
	}

	/**
	 * {@inheritDoc}
	 */
	override val id: String
		get() = descriptor.name

	/**
	 * {@inheritDoc}
	 */
	override val rawContent: ByteArray
		get() = descriptor.rawData

	/**
	 * {@inheritDoc}
	 */
	override val isBinary: Boolean
		get() = descriptor.type == MetadataDescriptor.TYPE_BINARY

	/**
	 * {@inheritDoc}
	 */
	override fun isBinary(value: Boolean) {
		if (!value && isBinary) {
			throw UnsupportedOperationException("No conversion supported.")
		}
		descriptor.setBinaryValue(descriptor.rawData)
	}// HashSet is safe against null comparison

	/**
	 * {@inheritDoc}
	 */
	override val isCommon: Boolean
		get() =// HashSet is safe against null comparison
			AsfTag.COMMON_FIELDS.contains(AsfFieldKey.getAsfFieldKey(id))

	/**
	 * {@inheritDoc}
	 */
	override val isEmpty: Boolean
		get() = descriptor.isEmpty()

	/**
	 * {@inheritDoc}
	 */
	override fun toDescriptiveString(): String {
		return descriptor.getString()!!
	}
}
