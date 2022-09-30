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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.AsfHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.ContainerType
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.ContainerType.Companion.areInCorrectOrder
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.ContainerType.Companion.ordered
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.MetadataContainer
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.MetadataContainerFactory.Companion.getInstance
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.MetadataDescriptor
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.FieldKey
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.asf.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.GenreTypes

/**
 * This class provides functionality to convert
 * [AsfHeader]objects into
 * [Tag]objects.<br></br>
 *
 * @author Christian Laireiter (liree)
 */
object TagConverter {
	/**
	 * This method assigns those tags of `tag` which are defined to
	 * be common by jaudiotagger. <br></br>
	 *
	 * @param tag         The tag from which the values are gathered. <br></br>
	 * Assigned values are: <br></br>
	 * @param description The extended content description which should receive the
	 * values. <br></br>
	 * **Warning: ** the common values will be replaced.
	 */
	fun assignCommonTagValues(tag: Tag, description: MetadataContainer) {
		assert(description.containerType === ContainerType.EXTENDED_CONTENT)
		var tmp: MetadataDescriptor
		if (!Utils.isBlank(tag.getFirst(FieldKey.ALBUM))) {
			tmp = MetadataDescriptor(
				description.containerType,
				AsfFieldKey.ALBUM.fieldName,
				MetadataDescriptor.TYPE_STRING
			)
			tmp.setStringValue(tag.getFirst(FieldKey.ALBUM))
			description.removeDescriptorsByName(tmp.name)
			description.addDescriptor(tmp)
		} else {
			description.removeDescriptorsByName(AsfFieldKey.ALBUM.fieldName)
		}
		if (!Utils.isBlank(tag.getFirst(FieldKey.TRACK))) {
			tmp = MetadataDescriptor(
				description.containerType,
				AsfFieldKey.TRACK.fieldName,
				MetadataDescriptor.TYPE_STRING
			)
			tmp.setStringValue(tag.getFirst(FieldKey.TRACK))
			description.removeDescriptorsByName(tmp.name)
			description.addDescriptor(tmp)
		} else {
			description.removeDescriptorsByName(AsfFieldKey.TRACK.fieldName)
		}
		if (!Utils.isBlank(tag.getFirst(FieldKey.YEAR))) {
			tmp = MetadataDescriptor(
				description.containerType,
				AsfFieldKey.YEAR.fieldName,
				MetadataDescriptor.TYPE_STRING
			)
			tmp.setStringValue(tag.getFirst(FieldKey.YEAR))
			description.removeDescriptorsByName(tmp.name)
			description.addDescriptor(tmp)
		} else {
			description.removeDescriptorsByName(AsfFieldKey.YEAR.fieldName)
		}
		if (!Utils.isBlank(tag.getFirst(FieldKey.GENRE))) {
			// Write Genre String value
			tmp = MetadataDescriptor(
				description.containerType,
				AsfFieldKey.GENRE.fieldName,
				MetadataDescriptor.TYPE_STRING
			)
			tmp.setStringValue(tag.getFirst(FieldKey.GENRE))
			description.removeDescriptorsByName(tmp.name)
			description.addDescriptor(tmp)
			val genreNum = tag.getFirst(FieldKey.GENRE)?.let { GenreTypes.instanceOf.getIdForName(it) }
			// ..and if it is one of the standard genre types used the id as
			// well
			if (genreNum != null) {
				tmp = MetadataDescriptor(
					description.containerType,
					AsfFieldKey.GENRE_ID.fieldName,
					MetadataDescriptor.TYPE_STRING
				)
				tmp.setStringValue("($genreNum)")
				description.removeDescriptorsByName(tmp.name)
				description.addDescriptor(tmp)
			} else {
				description.removeDescriptorsByName(AsfFieldKey.GENRE_ID.fieldName)
			}
		} else {
			description.removeDescriptorsByName(AsfFieldKey.GENRE.fieldName)
			description.removeDescriptorsByName(AsfFieldKey.GENRE_ID.fieldName)
		}
	}

	/**
	 * This method creates a [Tag]and fills it with the contents of the
	 * given [AsfHeader].<br></br>
	 *
	 * @param source The ASF header which contains the information. <br></br>
	 * @return A Tag with all its values.
	 */
	@JvmStatic
	fun createTagOf(source: AsfHeader): AsfTag {
		// TODO do we need to copy here.
		val result = AsfTag(true)
		for (i in ContainerType.values().indices) {
			val current = source.findMetadataContainer(ContainerType.values()[i])
			if (current != null) {
				val descriptors = current.getDescriptors()
				for (descriptor in descriptors) {
					var toAdd: AsfTagField
					toAdd = if (descriptor.type == MetadataDescriptor.TYPE_BINARY) {
						when (descriptor.name) {
							AsfFieldKey.COVER_ART.fieldName -> {
								AsfTagCoverField(descriptor)
							}
							AsfFieldKey.BANNER_IMAGE.fieldName -> {
								AsfTagBannerField(descriptor)
							}
							else -> {
								AsfTagField(descriptor)
							}
						}
					} else {
						AsfTagTextField(descriptor)
					}
					result.addField(toAdd)
				}
			}
		}
		return result
	}

	/**
	 * This method distributes the tags fields among the
	 * [ContainerType.getOrdered] [ containers][MetadataContainer].
	 *
	 * @param tag the tag with the fields to distribute.
	 * @return distribution
	 */
	@JvmStatic
	fun distributeMetadata(tag: AsfTag): Array<MetadataContainer?> {
		val asfFields = tag.asfFields
		val createContainers = getInstance().createContainers(ordered)
		var assigned: Boolean
		var current: AsfTagField
		while (asfFields.hasNext()) {
			current = asfFields.next()
			assigned = false
			var i = 0
			while (!assigned && i < createContainers.size) {
				if (areInCorrectOrder(
						createContainers[i]!!.containerType,
						AsfFieldKey.getAsfFieldKey(current.id).highestContainer
					)
				) {
					if (createContainers[i]!!.isAddSupported(current.descriptor)) {
						createContainers[i]!!.addDescriptor(current.descriptor)
						assigned = true
					}
				}
				i++
			}
			assert(assigned)
		}
		return createContainers
	}
}
