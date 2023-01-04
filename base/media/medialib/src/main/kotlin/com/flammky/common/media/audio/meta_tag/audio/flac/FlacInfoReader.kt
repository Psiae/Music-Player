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
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.SupportedFileFormat
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.flac.metadatablock.BlockType
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.flac.metadatablock.MetadataBlockDataStreamInfo
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.flac.metadatablock.MetadataBlockHeader.Companion.readHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.util.logging.Logger

/**
 * Read info from Flac file
 */
class FlacInfoReader {
	@Throws(CannotReadException::class, IOException::class, NotImplementedError::class)
	fun read(path: Path): FlacAudioHeader {
		return if (VersionHelper.hasOreo()) {
			FileChannel.open(path).use { fc ->
				read(fc)
			}
		} else {
			RandomAccessFile(path.toFile(), "r").use { read(it.channel) }
		}
	}

	fun read(fc: FileChannel): FlacAudioHeader {
		val flacStream = FlacStreamReader(fc, "")
		flacStream.findStream()
		var mbdsi: MetadataBlockDataStreamInfo? = null
		var isLastBlock = false

		//Search for StreamInfo Block, but even after we found it we still have to continue through all
		//the metadata blocks so that we can find the start of the audio frames which we need to calculate
		//the bitrate
		while (isLastBlock == false) {
			val mbh = readHeader(fc)
			if (mbh.blockType === BlockType.STREAMINFO) {
				//See #253:MetadataBlockDataStreamInfo exception when bytes length is 0
				if (mbh.dataLength == 0) {
					throw CannotReadException(
						"$fc:FLAC StreamInfo has zeo data length"
					)
				}
				mbdsi = MetadataBlockDataStreamInfo(mbh, fc)
				if (!mbdsi.isValid()) {
					throw CannotReadException(
						"$fc:FLAC StreamInfo not valid"
					)
				}
			} else {
				fc.position(fc.position() + mbh.dataLength)
			}
			isLastBlock = mbh.isLastBlock
		}

		//Audio continues from this point to end of file (normally - TODO might need to allow for an ID3v1 tag at file end ?)
		val streamStart = fc.position()
		if (mbdsi == null) {
			throw CannotReadException(
				"$fc:Unable to find Flac StreamInfo"
			)
		}
		val info = FlacAudioHeader()
		info.noOfSamples = mbdsi.getNoOfSamples()
		info.preciseTrackLength = mbdsi.getPreciseLength().toDouble()
		info.channelNumber = mbdsi.getNoOfChannels()
		info.setSamplingRate(mbdsi.getSamplingRate())
		info.bitsPerSample = mbdsi.getBitsPerSample()
		info.encodingType = mbdsi.getEncodingType()
		info.format =
			SupportedFileFormat.FLAC.displayName
		info.isLossless = true
		info.md5 = mbdsi.getMD5Signature()
		info.audioDataLength = fc.size() - streamStart
		info.audioDataStartPosition = streamStart
		info.audioDataEndPosition = fc.size()
		info.setBitRate(computeBitrate(info.audioDataLength!!, mbdsi.getPreciseLength()))
		return info
	}

	private fun computeBitrate(size: Long, length: Float): Int {
		return (size / Utils.KILOBYTE_MULTIPLIER * Utils.BITS_IN_BYTE_MULTIPLIER / length).toInt()
	}

	/**
	 * Count the number of metadatablocks, useful for debugging
	 *
	 * @param f
	 * @return
	 * @throws CannotReadException
	 * @throws IOException
	 */
	@Throws(CannotReadException::class, IOException::class, UnsupportedOperationException::class)
	fun countMetaBlocks(f: File): Int {
		if (!VersionHelper.hasOreo()) TODO("Require API <= 26")

		FileChannel.open(f.toPath()).use { fc ->
			val flacStream = FlacStreamReader(fc, f.toPath().toString() + " ")
			flacStream.findStream()
			var isLastBlock = false
			var count = 0
			while (!isLastBlock) {
				val mbh = readHeader(fc)
				logger.config(f.toString() + ":Found block:" + mbh.blockType)
				fc.position(fc.position() + mbh.dataLength)
				isLastBlock = mbh.isLastBlock
				count++
			}
			return count
		}
	}

	companion object {
		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.audio.flac")
	}
}
