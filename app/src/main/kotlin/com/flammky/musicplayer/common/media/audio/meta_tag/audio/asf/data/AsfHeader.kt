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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import java.math.BigInteger
import java.nio.charset.Charset

/**
 * Each ASF file starts with a so called header. <br></br>
 * This header contains other chunks. Each chunk starts with a 16 byte GUID
 * followed by the length (in bytes) of the chunk (including GUID). The length
 * number takes 8 bytes and is unsigned. Finally the chunk's data appears. <br></br>
 *
 * @author Christian Laireiter
 */
class AsfHeader
/**
 * Creates an instance.
 *
 * @param pos      see [Chunk.position]
 * @param chunkLen see [Chunk.chunkLength]
 * @param chunkCnt
 */(
	pos: Long, chunkLen: BigInteger?,
	/**
	 * An ASF header contains multiple chunks. <br></br>
	 * The count of those is stored here.
	 */
	val chunkCount: Long
) : ChunkContainer(GUID.GUID_HEADER, pos, chunkLen) {
	/**
	 * Returns the amount of chunks, when this instance was created.<br></br>
	 * If chunks have been added, this won't be reflected with this call.<br></br>
	 * For that use [.getChunks].
	 *
	 * @return Chunkcount at instance creation.
	 */

	/**
	 * This method looks for an content description object in this header
	 * instance, if not found there, it tries to get one from a contained ASF
	 * header extension object.
	 *
	 * @return content description if found, `null` otherwise.
	 */
	fun findContentDescription(): ContentDescription? {
		var result: ContentDescription? = contentDescription
		if (result == null && extendedHeader != null) {
			result = extendedHeader!!.contentDescription
		}
		return result
	}

	/**
	 * This method looks for an extended content description object in this
	 * header instance, if not found there, it tries to get one from a contained
	 * ASF header extension object.
	 *
	 * @return extended content description if found, `null`
	 * otherwise.
	 */
	fun findExtendedContentDescription(): MetadataContainer? {
		var result: MetadataContainer? = extendedContentDescription
		if (result == null && extendedHeader != null) {
			result = extendedHeader!!.extendedContentDescription
		}
		return result
	}

	/**
	 * This method searches for a metadata container of the given type.<br></br>
	 *
	 * @param type the type of the container to look up.
	 * @return a container of specified type, of `null` if not
	 * contained.
	 */
	fun findMetadataContainer(type: ContainerType): MetadataContainer? {
		var result = getFirst(type.containerGUID, MetadataContainer::class.java) as? MetadataContainer
		if (result == null) {
			result = extendedHeader?.getFirst(
				type.containerGUID, MetadataContainer::class.java
			) as? MetadataContainer
		}
		return result
	}

	/**
	 * This method returns the first audio stream chunk found in the asf file or
	 * stream.
	 *
	 * @return Returns the audioStreamChunk.
	 */
	val audioStreamChunk: AudioStreamChunk?
		get() {
			var result: AudioStreamChunk? = null
			val streamChunks = assertChunkList(GUID.GUID_STREAM)
			var i = 0
			while (i < streamChunks.size && result == null) {
				if (streamChunks[i] is AudioStreamChunk) {
					result = streamChunks[i] as AudioStreamChunk
				}
				i++
			}
			return result
		}

	/**
	 * @return Returns the contentDescription.
	 */
	val contentDescription: ContentDescription
		get() = getFirst(
			GUID.GUID_CONTENTDESCRIPTION,
			ContentDescription::class.java
		) as ContentDescription

	/**
	 * @return Returns the encodingChunk.
	 */
	val encodingChunk: EncodingChunk
		get() = getFirst(GUID.GUID_ENCODING, EncodingChunk::class.java) as EncodingChunk

	/**
	 * @return Returns the encodingChunk.
	 */
	val encryptionChunk: EncryptionChunk
		get() = getFirst(
			GUID.GUID_CONTENT_ENCRYPTION,
			EncryptionChunk::class.java
		) as EncryptionChunk

	/**
	 * @return Returns the tagHeader.
	 */
	val extendedContentDescription: MetadataContainer
		get() = getFirst(
			GUID.GUID_EXTENDED_CONTENT_DESCRIPTION,
			MetadataContainer::class.java
		) as MetadataContainer

	/**
	 * @return Returns the extended header.
	 */
	val extendedHeader: AsfExtendedHeader?
		get() = getFirst(
			GUID.GUID_HEADER_EXTENSION,
			AsfExtendedHeader::class.java
		) as AsfExtendedHeader

	/**
	 * @return Returns the fileHeader.
	 */
	val fileHeader: FileHeader
		get() = getFirst(GUID.GUID_FILE, FileHeader::class.java) as FileHeader

	/**
	 * @return Returns the streamBitratePropertiesChunk.
	 */
	val streamBitratePropertiesChunk: StreamBitratePropertiesChunk
		get() = getFirst(
			GUID.GUID_STREAM_BITRATE_PROPERTIES,
			StreamBitratePropertiesChunk::class.java
		) as StreamBitratePropertiesChunk

	/**
	 * {@inheritDoc}
	 */
	override fun prettyPrint(prefix: String): String {
		val result = StringBuilder(
			super.prettyPrint(
				prefix,
				prefix + "  | : Contains: \"" + chunkCount + "\" chunks" + Utils.LINE_SEPARATOR
			)
		)
		return result.toString()
	}

	companion object {
		/**
		 * The charset &quot;UTF-16LE&quot; is mandatory for ASF handling.
		 */
		@JvmField
		val ASF_CHARSET: Charset = Charset.forName("UTF-16LE") //$NON-NLS-1$

		/**
		 * Byte sequence representing the zero term character.
		 */
		@JvmField
		val ZERO_TERM = byteArrayOf(0, 0)

		init {
			val MULTI_CHUNKS: MutableSet<GUID> = HashSet()
			MULTI_CHUNKS.add(GUID.GUID_STREAM)
		}
	}
}
