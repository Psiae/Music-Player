/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Raphaël Slinckx <raphael@slinckx.net>
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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.ogg.util

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.SupportedFileFormat
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.GenericAudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v2TagBase.Companion.isId3Tag
import org.jaudiotagger.audio.ogg.util.OggPageHeader
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import java.util.logging.Logger

/**
 * Read encoding info, only implemented for vorbis streams
 */
class OggInfoReader {

	@Throws(CannotReadException::class, IOException::class)
	fun read(fc: FileChannel): GenericAudioHeader {
		var start = fc.position()
		val info = GenericAudioHeader()
		val oldPos: Long

		//Check start of file does it have Ogg pattern
		var b = ByteBuffer.allocate(OggPageHeader.Companion.CAPTURE_PATTERN.size)
		fc.read(b)
		if (!Arrays.equals(b.array(), OggPageHeader.Companion.CAPTURE_PATTERN)) {
			fc.position(0)
			if (isId3Tag(fc)) {
				fc.read(b)
				if (Arrays.equals(b.array(), OggPageHeader.Companion.CAPTURE_PATTERN)) {
					start = fc.position()
				}
			} else {
				throw CannotReadException(
					ErrorMessage.OGG_HEADER_CANNOT_BE_FOUND.getMsg(String(b.array()))
				)
			}
		}

		//Now work backwards from file looking for the last ogg page, it reads the granule position for this last page
		//which must be set.
		//TODO should do buffering to cut down the number of file reads
		fc.position(start)
		var pcmSamplesNumber = -1.0
		fc.position(fc.size() - 2)
		while (fc.position() >= 4) {
			if (ByteBuffer.allocate(1).apply { fc.read(this) }.get(0) ==
				OggPageHeader.Companion.CAPTURE_PATTERN.get(3)
			) {
				fc.position(fc.position() - OggPageHeader.Companion.FIELD_CAPTURE_PATTERN_LENGTH)
				val ogg = ByteBuffer.allocate(3)
				fc.read(ogg)
				if (ogg[0] == OggPageHeader.Companion.CAPTURE_PATTERN.get(
						0
					) && ogg[1] == OggPageHeader.Companion.CAPTURE_PATTERN.get(
						1
					) && ogg[2] == OggPageHeader.Companion.CAPTURE_PATTERN.get(
						2
					)
				) {
					fc.position(fc.position() - 3)
					oldPos = fc.position()
					fc.position(fc.position() + OggPageHeader.Companion.FIELD_PAGE_SEGMENTS_POS)
					val pageSegments =
						ByteBuffer.allocate(1).apply { fc.read(this) }.get(0).toInt() and 0xFF //Unsigned
					fc.position(oldPos)
					b =
						ByteBuffer.allocate(OggPageHeader.Companion.OGG_PAGE_HEADER_FIXED_LENGTH + pageSegments)
					fc.read(b)
					val pageHeader = OggPageHeader(b.array())
					fc.position(0)
					pcmSamplesNumber = pageHeader.getAbsoluteGranulePosition().toDouble()
					break
				}
			}
			fc.position(fc.position() - 2)
		}
		if (pcmSamplesNumber == -1.0) {
			//According to spec a value of -1 indicates no packet finished on this page, this should not occur
			throw CannotReadException(ErrorMessage.OGG_VORBIS_NO_SETUP_BLOCK.msg)
		}

		//1st page = Identification Header
		val pageHeader: OggPageHeader = OggPageHeader.Companion.read(fc)
		val vorbisData = ByteBuffer.allocate(pageHeader.getPageLength())
		if (vorbisData.array().size < OggPageHeader.Companion.OGG_PAGE_HEADER_FIXED_LENGTH) {
			throw CannotReadException("Invalid Identification header for this Ogg File")
		}
		fc.read(vorbisData)
		val vorbisIdentificationHeader = VorbisIdentificationHeader(vorbisData.array())

		//Map to generic encodingInfo
		info.preciseTrackLength =
			((pcmSamplesNumber / vorbisIdentificationHeader.samplingRate).toFloat().toDouble())
		info.channelNumber = vorbisIdentificationHeader.channelNumber
		info.setSamplingRate(vorbisIdentificationHeader.samplingRate)
		info.encodingType = (vorbisIdentificationHeader.encodingType)
		info.format = (SupportedFileFormat.OGG.displayName)

		//According to Wikipedia Vorbis Page, Vorbis only works on 16bits 44khz
		info.bitsPerSample = 16

		//TODO this calculation should be done within identification header
		if (vorbisIdentificationHeader.nominalBitrate != 0 && vorbisIdentificationHeader.maxBitrate == vorbisIdentificationHeader.nominalBitrate && vorbisIdentificationHeader.minBitrate == vorbisIdentificationHeader.nominalBitrate) {
			//CBR (in kbps)
			info.setBitRate(vorbisIdentificationHeader.nominalBitrate / 1000)
			info.isVariableBitRate = false
		} else if (vorbisIdentificationHeader.nominalBitrate != 0 && vorbisIdentificationHeader.maxBitrate == 0 && vorbisIdentificationHeader.minBitrate == 0) {
			//Average vbr (in kpbs)
			info.setBitRate(vorbisIdentificationHeader.nominalBitrate / 1000)
			info.isVariableBitRate = true
		} else {
			//TODO need to remove comment from raf.getLength()
			info.setBitRate(computeBitrate(info.trackLength!!, fc.size()))
			info.isVariableBitRate = true
		}
		return info
	}

	@Throws(CannotReadException::class, IOException::class)
	fun read(raf: RandomAccessFile): GenericAudioHeader {
		var start = raf.filePointer
		val info = GenericAudioHeader()
		logger.fine("Started")
		val oldPos: Long

		//Check start of file does it have Ogg pattern
		var b = ByteArray(OggPageHeader.Companion.CAPTURE_PATTERN.size)
		raf.read(b)
		if (!Arrays.equals(b, OggPageHeader.Companion.CAPTURE_PATTERN)) {
			raf.seek(0)
			if (isId3Tag(raf)) {
				raf.read(b)
				if (Arrays.equals(b, OggPageHeader.Companion.CAPTURE_PATTERN)) {
					start = raf.filePointer
				}
			} else {
				throw CannotReadException(
					ErrorMessage.OGG_HEADER_CANNOT_BE_FOUND.getMsg(
						String(
							b
						)
					)
				)
			}
		}

		//Now work backwards from file looking for the last ogg page, it reads the granule position for this last page
		//which must be set.
		//TODO should do buffering to cut down the number of file reads
		raf.seek(start)
		var pcmSamplesNumber = -1.0
		raf.seek(raf.length() - 2)
		while (raf.filePointer >= 4) {
			if (raf.read() == OggPageHeader.Companion.CAPTURE_PATTERN.get(3).toInt()) {
				raf.seek(raf.filePointer - OggPageHeader.Companion.FIELD_CAPTURE_PATTERN_LENGTH)
				val ogg = ByteArray(3)
				raf.readFully(ogg)
				if (ogg[0] == OggPageHeader.Companion.CAPTURE_PATTERN.get(
						0
					) && ogg[1] == OggPageHeader.Companion.CAPTURE_PATTERN.get(
						1
					) && ogg[2] == OggPageHeader.Companion.CAPTURE_PATTERN.get(
						2
					)
				) {
					raf.seek(raf.filePointer - 3)
					oldPos = raf.filePointer
					raf.seek(raf.filePointer + OggPageHeader.Companion.FIELD_PAGE_SEGMENTS_POS)
					val pageSegments = raf.readByte().toInt() and 0xFF //Unsigned
					raf.seek(oldPos)
					b =
						ByteArray(OggPageHeader.Companion.OGG_PAGE_HEADER_FIXED_LENGTH + pageSegments)
					raf.readFully(b)
					val pageHeader = OggPageHeader(b)
					raf.seek(0)
					pcmSamplesNumber = pageHeader.getAbsoluteGranulePosition().toDouble()
					break
				}
			}
			raf.seek(raf.filePointer - 2)
		}
		if (pcmSamplesNumber == -1.0) {
			//According to spec a value of -1 indicates no packet finished on this page, this should not occur
			throw CannotReadException(ErrorMessage.OGG_VORBIS_NO_SETUP_BLOCK.msg)
		}

		//1st page = Identification Header
		val pageHeader: OggPageHeader = OggPageHeader.read(raf)
		val vorbisData = ByteArray(pageHeader.getPageLength())
		if (vorbisData.size < OggPageHeader.Companion.OGG_PAGE_HEADER_FIXED_LENGTH) {
			throw CannotReadException("Invalid Identification header for this Ogg File: ${vorbisData.size}")
		}
		raf.read(vorbisData)
		val vorbisIdentificationHeader = VorbisIdentificationHeader(vorbisData)

		//Map to generic encodingInfo
		info.preciseTrackLength =
			((pcmSamplesNumber / vorbisIdentificationHeader.samplingRate).toFloat().toDouble())
		info.channelNumber = vorbisIdentificationHeader.channelNumber
		info.setSamplingRate(vorbisIdentificationHeader.samplingRate)
		info.encodingType = (vorbisIdentificationHeader.encodingType)
		info.format = (SupportedFileFormat.OGG.displayName)

		//According to Wikipedia Vorbis Page, Vorbis only works on 16bits 44khz
		info.bitsPerSample = 16

		//TODO this calculation should be done within identification header
		if (vorbisIdentificationHeader.nominalBitrate != 0 && vorbisIdentificationHeader.maxBitrate == vorbisIdentificationHeader.nominalBitrate && vorbisIdentificationHeader.minBitrate == vorbisIdentificationHeader.nominalBitrate) {
			//CBR (in kbps)
			info.setBitRate(vorbisIdentificationHeader.nominalBitrate / 1000)
			info.isVariableBitRate = false
		} else if (vorbisIdentificationHeader.nominalBitrate != 0 && vorbisIdentificationHeader.maxBitrate == 0 && vorbisIdentificationHeader.minBitrate == 0) {
			//Average vbr (in kpbs)
			info.setBitRate(vorbisIdentificationHeader.nominalBitrate / 1000)
			info.isVariableBitRate = true
		} else {
			//TODO need to remove comment from raf.getLength()
			info.setBitRate(computeBitrate(info.trackLength!!, raf.length()))
			info.isVariableBitRate = true
		}
		return info
	}

	private fun computeBitrate(length: Int, size: Long): Int {
		//Protect against audio less than 0.5 seconds that can be rounded to zero causing Arithmetic Exception
		var length = length
		if (length == 0) {
			length = 1
		}
		return (size / Utils.KILOBYTE_MULTIPLIER * Utils.BITS_IN_BYTE_MULTIPLIER / length).toInt()
	}

	companion object {
		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.audio.ogg.atom")
	}
}
