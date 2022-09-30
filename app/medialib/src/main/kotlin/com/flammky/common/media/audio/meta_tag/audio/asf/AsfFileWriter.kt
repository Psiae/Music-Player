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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.AudioFile
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.ChunkContainer
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.MetadataContainer
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io.*
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io.AsfHeaderReader.Companion.readTagHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.TagConverter.distributeMetadata
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotWriteException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.AudioFileWriter
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.asf.AsfTag
import java.io.IOException
import java.io.RandomAccessFile

/**
 * This class writes given tags to ASF files containing WMA content. <br></br>
 * <br></br>
 *
 * @author Christian Laireiter
 */
class AsfFileWriter : AudioFileWriter() {
	/**
	 * {@inheritDoc}
	 */
	@Throws(CannotWriteException::class, IOException::class)
	override fun deleteTag(tag: Tag?, raf: RandomAccessFile?, tempRaf: RandomAccessFile?) {
		writeTag(null, AsfTag(true), raf!!, tempRaf!!)
	}

	private fun searchExistence(
		container: ChunkContainer?,
		metaContainers: Array<MetadataContainer?>?
	): BooleanArray {
		assert(container != null)
		assert(metaContainers != null)
		val result = BooleanArray(metaContainers!!.size)
		for (i in result.indices) {
			result[i] = container!!.hasChunkByGUID(metaContainers[i]!!.containerType.containerGUID)
		}
		return result
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(CannotWriteException::class, IOException::class)
	override fun writeTag(
		audioFile: AudioFile?,
		tag: Tag,
		raf: RandomAccessFile,
		rafTemp: RandomAccessFile
	) {
		/*
		 * Since this implementation should not change the structure of the ASF
		 * file (locations of content description chunks), we need to read the
		 * content description chunk and the extended content description chunk
		 * from the source file. In the second step we need to determine which
		 * modifier (asf header or asf extended header) gets the appropriate
		 * modifiers. The following policies are applied: if the source does not
		 * contain any descriptor, the necessary descriptors are appended to the
		 * header object.
		 *
		 * if the source contains only one descriptor in the header extension
		 * object, and the other type is needed as well, the other one will be
		 * put into the header extension object.
		 *
		 * for each descriptor type, if an object is found, an updater will be
		 * configured.
		 */
		val sourceHeader = readTagHeader(raf)
		raf.seek(0) // Reset for the streamer
		/*
		 * Now createField modifiers for metadata descriptor and extended content
		 * descriptor as implied by the given Tag.
		 */
		// TODO not convinced that we need to copy fields here
		val copy = AsfTag(tag, true)
		val distribution = distributeMetadata(copy)
		val existHeader = searchExistence(sourceHeader, distribution)
		val existExtHeader = searchExistence(sourceHeader!!.extendedHeader, distribution)
		// Modifiers for the asf header object
		val headerModifier: MutableList<ChunkModifier> = ArrayList()
		// Modifiers for the asf header extension object
		val extHeaderModifier: MutableList<ChunkModifier> = ArrayList()
		for (i in distribution.indices) {
			val modifier = WriteableChunkModifer(
				distribution[i]!!
			)
			if (existHeader[i]) {
				// Will remove or modify chunks in ASF header
				headerModifier.add(modifier)
			} else if (existExtHeader[i]) {
				// Will remove or modify chunks in extended header
				extHeaderModifier.add(modifier)
			} else {
				// Objects (chunks) will be added here.
				if (i == 0 || i == 2 || i == 1) {
					// Add content description and extended content description
					// at header for maximum compatibility
					headerModifier.add(modifier)
				} else {
					// For now, the rest should be created at extended header
					// since other positions aren't known.
					extHeaderModifier.add(modifier)
				}
			}
		}
		// only addField an AsfExtHeaderModifier, if there is actually something to
		// change (performance)
		if (!extHeaderModifier.isEmpty()) {
			headerModifier.add(AsfExtHeaderModifier(extHeaderModifier))
		}
		AsfStreamer().createModifiedCopy(
			RandomAccessFileInputstream(raf),
			RandomAccessFileOutputStream(rafTemp),
			headerModifier
		)
	}
}
