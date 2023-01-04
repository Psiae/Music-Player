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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4

import com.flammky.android.core.sdk.VersionHelper
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadVideoException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.GenericAudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.*
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4BoxHeader.Companion.seekWithinLevel
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SeekableByteChannel
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Logger

/**
 * Read audio info from file.
 *
 *
 * The info is held in the mvdh and mdhd fields as shown below
 * <pre>
 * |--- ftyp
 * |--- moov
 * |......|
 * |......|----- mvdh
 * |......|----- trak
 * |...............|----- mdia
 * |.......................|---- mdhd
 * |.......................|---- minf
 * |..............................|---- smhd
 * |..............................|---- stbl
 * |......................................|--- stsd
 * |.............................................|--- mp4a
 * |......|----- udta
 * |
 * |--- mdat
</pre> *
 */
class Mp4InfoReader {
	@Throws(IOException::class)
	private fun isTrackAtomVideo(
		ftyp: Mp4FtypBox,
		boxHeader: Mp4BoxHeader,
		mvhdBuffer: ByteBuffer
	): Boolean {
		var boxHeader: Mp4BoxHeader? = boxHeader
		boxHeader = seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.MDIA.fieldName)
		if (boxHeader == null) {
			return false
		}
		boxHeader = seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.MDHD.fieldName)
		if (boxHeader == null) {
			return false
		}
		mvhdBuffer.position(mvhdBuffer.position() + boxHeader.dataLength)
		boxHeader = seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.MINF.fieldName)
		if (boxHeader == null) {
			return false
		}
		boxHeader = seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.VMHD.fieldName)
		return boxHeader != null
	}

	@Throws(CannotReadException::class, IOException::class)
	fun read(file: Path): GenericAudioHeader {
		return if (VersionHelper.hasOreo()) {
			Files.newByteChannel(file).use { fc -> read(fc) }
		} else {
			RandomAccessFile(file.toFile(), "r").use { read(it.channel) }
		}
	}

	fun read(sbc: SeekableByteChannel): GenericAudioHeader {
		val info = Mp4AudioHeader()

		//File Identification
		val ftypHeader = seekWithinLevel(
			sbc,
			Mp4AtomIdentifier.FTYP.fieldName
		)
			?: throw CannotReadException(
				ErrorMessage.MP4_FILE_NOT_CONTAINER.msg
			)
		val ftypBuffer =
			ByteBuffer.allocate(ftypHeader.length - Mp4BoxHeader.HEADER_LENGTH)
		sbc.read(ftypBuffer)
		ftypBuffer.rewind()
		val ftyp = Mp4FtypBox(ftypHeader, ftypBuffer)
		ftyp.processData()
		info.brand = ftyp.majorBrand

		//Get to the facts everything we are interested in is within the moov box, so just load data from file
		//once so no more file I/O needed
		val moovHeader = seekWithinLevel(
			sbc,
			Mp4AtomIdentifier.MOOV.fieldName
		)
			?: throw CannotReadException(
				ErrorMessage.MP4_FILE_NOT_AUDIO.msg
			)
		val moovBuffer =
			ByteBuffer.allocate(moovHeader.length - Mp4BoxHeader.HEADER_LENGTH)
		moovBuffer.order(ByteOrder.LITTLE_ENDIAN)
		sbc.read(moovBuffer)
		moovBuffer.rewind()

		//Level 2-Searching for "mvhd" somewhere within "moov", we make a slice after finding header
		//so all get() methods will be relative to mvdh positions
		var boxHeader: Mp4BoxHeader? = seekWithinLevel(moovBuffer, Mp4AtomIdentifier.MVHD.fieldName)
			?: throw CannotReadException(ErrorMessage.MP4_FILE_NOT_AUDIO.msg)
		val mvhdBuffer = moovBuffer.slice()
		val mvhd = Mp4MvhdBox(boxHeader, mvhdBuffer)
		info.preciseTrackLength = mvhd.preciseLength
		//Advance position, TODO should we put this in box code ?
		mvhdBuffer.position(mvhdBuffer.position() + boxHeader!!.dataLength)

		//Level 2-Searching for "trak" within "moov"
		boxHeader = seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.TRAK.fieldName)
		if (boxHeader == null) {
			throw CannotReadException(ErrorMessage.MP4_FILE_NOT_AUDIO.msg)
		}
		val endOfFirstTrackInBuffer = mvhdBuffer.position() + boxHeader.dataLength

		//Level 3-Searching for "mdia" within "trak"
		boxHeader = seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.MDIA.fieldName)
		if (boxHeader == null) {
			throw CannotReadException(ErrorMessage.MP4_FILE_NOT_AUDIO.msg)
		}
		//Level 4-Searching for "mdhd" within "mdia"
		boxHeader = seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.MDHD.fieldName)
		if (boxHeader == null) {
			throw CannotReadException(ErrorMessage.MP4_FILE_NOT_AUDIO.msg)
		}
		val mdhd = Mp4MdhdBox(boxHeader, mvhdBuffer.slice())
		info.setSamplingRate(mdhd.sampleRate)

		//Level 4-Searching for "hdlr" within "mdia"
		/*We dont currently need to process this because contains nothing we want
		mvhdBuffer.position(mvhdBuffer.position() + boxHeader.getDataLength());
		boxHeader = Mp4BoxHeader.seekWithinLevel(mvhdBuffer, Mp4NotMetaFieldKey.HDLR.getFieldName());
		if (boxHeader == null)
		{
				throw new CannotReadException(ErrorMessage.MP4_FILE_NOT_AUDIO.getMsg());
		}
		Mp4HdlrBox hdlr = new Mp4HdlrBox(boxHeader, mvhdBuffer.slice());
		hdlr.processData();
		*/

		//Level 4-Searching for "minf" within "mdia"
		mvhdBuffer.position(mvhdBuffer.position() + boxHeader.dataLength)
		boxHeader = seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.MINF.fieldName)
		if (boxHeader == null) {
			throw CannotReadException(ErrorMessage.MP4_FILE_NOT_AUDIO.msg)
		}

		//Level 5-Searching for "smhd" within "minf"
		//Only an audio track would have a smhd frame
		val pos = mvhdBuffer.position()
		boxHeader = seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.SMHD.fieldName)
		if (boxHeader == null) {
			mvhdBuffer.position(pos)
			boxHeader = seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.VMHD.fieldName)
			//try easy check to confirm that it is video
			if (boxHeader != null) {
				throw CannotReadVideoException(ErrorMessage.MP4_FILE_IS_VIDEO.msg)
			} else {
				throw CannotReadException(ErrorMessage.MP4_FILE_NOT_AUDIO.msg)
			}
		}
		mvhdBuffer.position(pos)

		//Level 5-Searching for "stbl within "minf"
		boxHeader = seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.STBL.fieldName)
		if (boxHeader == null) {
			throw CannotReadException(ErrorMessage.MP4_FILE_NOT_AUDIO.msg)
		}


		//Level 6-Searching for "stsd within "stbl" and process it direct data, dont think these are mandatory so dont throw
		//exception if unable to find
		val positionBeforeStsdSearch = mvhdBuffer.position()
		boxHeader = seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.STSD.fieldName)
		if (boxHeader != null) {
			val stsd = Mp4StsdBox(boxHeader, mvhdBuffer)
			stsd.processData()
			val positionAfterStsdHeaderAndData = mvhdBuffer.position()

			///Level 7-Searching for "mp4a within "stsd"
			boxHeader = seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.MP4A.fieldName)
			if (boxHeader != null) {
				val mp4aBuffer = mvhdBuffer.slice()
				val mp4a = Mp4Mp4aBox(boxHeader, mp4aBuffer)
				mp4a.processData()
				//Level 8-Searching for "esds" within mp4a to get No Of Channels and bitrate
				boxHeader = seekWithinLevel(mp4aBuffer, Mp4AtomIdentifier.ESDS.fieldName)
				if (boxHeader != null) {
					val esds = Mp4EsdsBox(boxHeader, mp4aBuffer.slice())

					//Set Bitrate in kbps
					info.setBitRate(esds.avgBitrate / Utils.KILOBYTE_MULTIPLIER)

					//Set Number of Channels
					info.channelNumber = esds.numberOfChannels
					info.kind = esds.kind
					info.profile = esds.audioProfile
					info.encodingType = EncoderType.AAC.description
				}
			} else {
				//Level 7 -Searching for drms within stsd instead (m4p files)
				mvhdBuffer.position(positionAfterStsdHeaderAndData)
				boxHeader = seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.DRMS.fieldName)
				if (boxHeader != null) {
					val drms = Mp4DrmsBox(boxHeader, mvhdBuffer)
					drms.processData()

					//Level 8-Searching for "esds" within drms to get No Of Channels and bitrate
					boxHeader = seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.ESDS.fieldName)
					if (boxHeader != null) {
						val esds = Mp4EsdsBox(boxHeader, mvhdBuffer.slice())

						//Set Bitrate in kbps
						info.setBitRate(esds.avgBitrate / Utils.KILOBYTE_MULTIPLIER)

						//Set Number of Channels
						info.channelNumber = esds.numberOfChannels
						info.kind = esds.kind
						info.profile = esds.audioProfile
						info.encodingType = EncoderType.DRM_AAC.description
					}
				} else {
					mvhdBuffer.position(positionAfterStsdHeaderAndData)
					boxHeader = seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.ALAC.fieldName)
					if (boxHeader != null) {
						//Process First Alac
						var alac = Mp4AlacBox(boxHeader, mvhdBuffer)
						alac.processData()

						//Level 8-Searching for 2nd "alac" within box that contains the info we really want
						boxHeader =
							seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.ALAC.fieldName)
						if (boxHeader != null) {
							alac = Mp4AlacBox(boxHeader, mvhdBuffer)
							alac.processData()
							info.encodingType = EncoderType.APPLE_LOSSLESS.description
							info.channelNumber = alac.channels
							info.setBitRate(alac.bitRate / Utils.KILOBYTE_MULTIPLIER)
							info.bitsPerSample = alac.sampleSize
						}
					}
				}
			}
		}

		//Level 6-Searching for "stco within "stbl" to get size of audio data
		mvhdBuffer.position(positionBeforeStsdSearch)
		boxHeader = seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.STCO.fieldName)
		if (boxHeader != null) {
			val stco = Mp4StcoBox(boxHeader, mvhdBuffer)
			info.audioDataStartPosition = stco.firstOffSet.toLong()
			info.audioDataEndPosition = sbc.size()
			info.audioDataLength = sbc.size() - stco.firstOffSet
		}

		//Set default channels if couldn't calculate it
		if (info.channelNumber == -1) {
			info.channelNumber = 2
		}

		//Set default bitrate if couldnt calculate it
		if (info.bitRateAsNumber == -1L) {
			info.setBitRate(128)
		}

		//Set default bits per sample if couldn't calculate it
		if (info.bitsPerSample == -1) {
			info.bitsPerSample = 16
		}

		//This is the most likely option if cant find a match
		if (info.encodingType == "") {
			info.encodingType = EncoderType.AAC.description
		}
		logger.config(info.toString())

		//Level 2-Searching for others "trak" within "moov", if we find any traks containing video
		//then reject it if no track if not video then we allow it because many encoders seem to contain all sorts
		//of stuff that you wouldn't expect in an audio track
		mvhdBuffer.position(endOfFirstTrackInBuffer)
		while (mvhdBuffer.hasRemaining()) {
			boxHeader = seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.TRAK.fieldName)
			if (boxHeader != null) {
				if (isTrackAtomVideo(ftyp, boxHeader, mvhdBuffer)) {
					throw CannotReadVideoException(ErrorMessage.MP4_FILE_IS_VIDEO.msg)
				}
			} else {
				break
			}
		}

		//Because Mp4 is container format we set format to encoder
		info.format = info.encodingType

		//Build AtomTree to ensure it is valid, this means we can detect any problems early on
		Mp4AtomTree(sbc, false)
		return info
	}

	companion object {
		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.audio.mp4.atom")
	}
}
