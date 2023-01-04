/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Raphaël Slinckx <raphael@slinckx.net>
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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.flac

import com.flammky.android.core.sdk.VersionHelper
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.flac.metadatablock.BlockType
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.flac.metadatablock.MetadataBlockDataPicture
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.flac.metadatablock.MetadataBlockHeader.Companion.readHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.Hex
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidFrameException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.flac.FlacTag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.vorbiscomment.VorbisCommentReader
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.vorbiscomment.VorbisCommentTag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.vorbiscomment.VorbisCommentTag.Companion.createNewTag
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Read Flac Tag
 */
class FlacTagReader {
	private val vorbisCommentReader = VorbisCommentReader()

	@Throws(CannotReadException::class, IOException::class, NotImplementedError::class)
	fun read(path: Path): FlacTag {
		return if (VersionHelper.hasOreo()) {
			FileChannel.open(path).use { fc ->
				read(fc)
			}
		} else {
			RandomAccessFile(path.toFile(), "r").use { read(it.channel) }
		}
	}

	fun read(fc: FileChannel): FlacTag {
		val flacStream = FlacStreamReader(fc, "")
		flacStream.findStream()

		//Hold the metadata
		var tag: VorbisCommentTag? = null

		val images: MutableList<MetadataBlockDataPicture> = ArrayList()

		//Seems like we have a valid stream
		var isLastBlock = false
		while (!isLastBlock) {
			if (logger.isLoggable(
					Level.CONFIG
				)
			) {
				logger.config(
					fc.toString() + " Looking for MetaBlockHeader at:" + fc.position()
				)
			}

			//Read the header
			val mbh =
				readHeader(
					fc
				)
			if (logger.isLoggable(
					Level.CONFIG
				)
			) {
				logger.config(
					fc.toString() + " Reading MetadataBlockHeader:" + mbh.toString() + " ending at " + fc.position()
				)
			}

			//Is it one containing some sort of metadata, therefore interested in it?

			//JAUDIOTAGGER-466:CBlocktype can be null
			if (mbh.blockType != null) {
				when (mbh.blockType) {
					BlockType.VORBIS_COMMENT -> {
						val commentHeaderRawPacket =
							ByteBuffer.allocate(mbh.dataLength)
						fc.read(commentHeaderRawPacket)
						tag = vorbisCommentReader.read(
							commentHeaderRawPacket.array(),
							false,
							null
						)
					}
					BlockType.PICTURE -> try {
						val mbdp =
							MetadataBlockDataPicture(
								mbh,
								fc
							)
						images.add(mbdp)
					} catch (ioe: IOException) {
						logger.warning(
							fc.toString() + "Unable to read picture metablock, ignoring:" + ioe.message
						)
					} catch (ive: InvalidFrameException) {
						logger.warning(
							fc.toString() + "Unable to read picture metablock, ignoring" + ive.message
						)
					}
					BlockType.SEEKTABLE -> try {
						val pos = fc.position()
						fc.position(pos + mbh.dataLength)
					} catch (ioe: IOException) {
						logger.warning(
							fc.toString() + "Unable to readseek metablock, ignoring:" + ioe.message
						)
					}
					else -> {
						if (logger.isLoggable(
								Level.CONFIG
							)
						) {
							logger.config(
								fc.toString() + "Ignoring MetadataBlock:" + mbh.blockType
							)
						}
						fc.position(fc.position() + mbh.dataLength)
					}
				}
			}
			isLastBlock = mbh.isLastBlock
		}
		logger.config(
			"Audio should start at:" + Hex.asHex(
				fc.position()
			)
		)

		//Note there may not be either a tag or any images, no problem this is valid however to make it easier we
		//just initialize Flac with an empty VorbisTag
		if (tag == null) {
			tag =
				createNewTag()
		}
		return FlacTag(tag, images)
	}

	companion object {
		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.audio.flac")
	}
}
