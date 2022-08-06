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
package com.kylentt.musicplayer.common.media.audio.meta_tag.audio.ogg.util

import com.kylentt.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.kylentt.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getIntLE
import com.kylentt.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.kylentt.musicplayer.common.media.audio.meta_tag.tag.id3.AbstractID3v2Tag.Companion.isId3Tag
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

/**
 * $Id$
 *
 * reference:http://xiph.org/ogg/doc/framing.html
 *
 * @author Raphael Slinckx (KiKiDonK)
 * @version 16 d�cembre 2003
 */
class OggPageHeader(
	/**
	 * @return the raw header data that this pageheader is derived from
	 */
	val rawHeaderData: ByteArray
) {
	private val packetList: MutableList<PacketStartAndLength> = ArrayList()
	private var absoluteGranulePosition = 0.0
	private var pageLength = 0

	val headerType: Byte

	var checkSum = 0

	var isValid = false

	var pageSequence = 0
	var serialNumber = 0


	var segmentTable: ByteArray? = null

	/**
	 * @return true if the last packet on this page extends to the next page
	 */
	var isLastPacketIncomplete = false

	/** Startbyte of this pageHeader in the file
	 *
	 * This is useful for Ogg files that contain unsupported additional data at the start of the file such
	 * as ID3 data
	 */
	var startByte: Long = 0

	init {
		val streamStructureRevision = rawHeaderData[FIELD_STREAM_STRUCTURE_VERSION_POS].toInt()
		headerType = rawHeaderData[FIELD_HEADER_TYPE_FLAG_POS]
		if (streamStructureRevision == 0) {
			absoluteGranulePosition = 0.0
			for (i in 0 until FIELD_ABSOLUTE_GRANULE_LENGTH) {
				absoluteGranulePosition += u(rawHeaderData[i + FIELD_ABSOLUTE_GRANULE_POS].toInt()) * Math.pow(
					2.0,
					(8 * i).toDouble()
				)
			}
			serialNumber = getIntLE(
				rawHeaderData, FIELD_STREAM_SERIAL_NO_POS, 17
			)
			pageSequence = getIntLE(
				rawHeaderData, FIELD_PAGE_SEQUENCE_NO_POS, 21
			)
			checkSum = getIntLE(
				rawHeaderData, FIELD_PAGE_CHECKSUM_POS, 25
			)
			val pageSegments = u(rawHeaderData[FIELD_PAGE_SEGMENTS_POS].toInt())
			segmentTable = ByteArray(rawHeaderData.size - OGG_PAGE_HEADER_FIXED_LENGTH)
			var packetLength = 0
			var segmentLength: Int? = null
			for (i in segmentTable!!.indices) {
				segmentTable!![i] = rawHeaderData[OGG_PAGE_HEADER_FIXED_LENGTH + i]
				segmentLength = u(segmentTable!![i].toInt())
				pageLength += segmentLength
				packetLength += segmentLength
				if (segmentLength < MAXIMUM_SEGMENT_SIZE) {
					packetList.add(PacketStartAndLength(pageLength - packetLength, packetLength))
					packetLength = 0
				}
			}

			//If last segment value is 255 this packet continues onto next page
			//and will not have been added to the packetStartAndEnd list yet
			if (segmentLength != null) {
				if (segmentLength == MAXIMUM_SEGMENT_SIZE) {
					packetList.add(PacketStartAndLength(pageLength - packetLength, packetLength))
					isLastPacketIncomplete = true
				}
			}
			isValid = true
		}
		if (logger.isLoggable(Level.CONFIG)) {
			logger.config(
				"Constructed OggPage:$this"
			)
		}
	}

	private fun u(i: Int): Int {
		return i and 0xFF
	}

	fun getAbsoluteGranulePosition(): Double {
		logger.fine(
			"Number Of Samples: $absoluteGranulePosition"
		)
		return absoluteGranulePosition
	}

	fun getPageLength(): Int {
		logger.finer(
			"This page length: $pageLength"
		)
		return pageLength
	}

	/**
	 * @return a list of packet start position and size within this page.
	 */
	fun getPacketList(): List<PacketStartAndLength> {
		return packetList
	}

	override fun toString(): String {
		var out =
			"Ogg Page Header:isValid:" + isValid + ":type:" + headerType + ":oggPageHeaderLength:" + rawHeaderData.size + ":length:" + pageLength + ":seqNo:" + pageSequence + ":packetIncomplete:" + isLastPacketIncomplete + ":serNum:" + serialNumber
		for (packet in getPacketList()) {
			out += packet.toString()
		}
		return out
	}

	/**
	 * Within the page specifies the start and length of each packet
	 * in the page offset from the end of the pageheader (after the segment table)
	 */
	class PacketStartAndLength(startPosition: Int, length: Int) {
		var startPosition = 0
		var length = 0

		init {
			this.startPosition = startPosition
			this.length = length
		}

		override fun toString(): String {
			return "NextPkt(start:$startPosition:length:$length),"
		}
	}

	/**
	 * This represents all the flags that can be set in the headerType field.
	 * Note these values can be ORED together. For example the last packet in
	 * a file would normally have a value of 0x5 because both the CONTINUED_PACKET
	 * bit and the END_OF_BITSTREAM bit would be set.
	 */
	enum class HeaderTypeFlag(
		/**
		 * @return the value that should be written to file to enable this flag
		 */
		var fileValue: Byte
	) {
		FRESH_PACKET(0x0.toByte()), CONTINUED_PACKET(0x1.toByte()), START_OF_BITSTREAM(0x2.toByte()), END_OF_BITSTREAM(
			0x4.toByte()
		);

	}

	companion object {
		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.audio.ogg.atom")

		//Capture pattern at start of header
		@JvmField
		val CAPTURE_PATTERN =
			byteArrayOf('O'.code.toByte(), 'g'.code.toByte(), 'g'.code.toByte(), 'S'.code.toByte())

		//Ogg Page header is always 27 bytes plus the size of the segment table which is variable
		const val OGG_PAGE_HEADER_FIXED_LENGTH = 27

		//Can have upto 255 segments in a page
		const val MAXIMUM_NO_OF_SEGMENT_SIZE = 255

		//Each segment can be upto 255 bytes
		const val MAXIMUM_SEGMENT_SIZE = 255

		//Maximum size of pageheader (27 + 255 = 282)
		const val MAXIMUM_PAGE_HEADER_SIZE =
			OGG_PAGE_HEADER_FIXED_LENGTH + MAXIMUM_NO_OF_SEGMENT_SIZE

		//Maximum size of page data following the page header (255 * 255 = 65025)
		const val MAXIMUM_PAGE_DATA_SIZE = MAXIMUM_NO_OF_SEGMENT_SIZE * MAXIMUM_SEGMENT_SIZE

		//Maximum size of page includes header and data (282 + 65025 = 65307 bytes)
		const val MAXIMUM_PAGE_SIZE = MAXIMUM_PAGE_HEADER_SIZE + MAXIMUM_PAGE_DATA_SIZE

		//Starting positions of the various attributes
		const val FIELD_CAPTURE_PATTERN_POS = 0
		const val FIELD_STREAM_STRUCTURE_VERSION_POS = 4
		const val FIELD_HEADER_TYPE_FLAG_POS = 5
		const val FIELD_ABSOLUTE_GRANULE_POS = 6
		const val FIELD_STREAM_SERIAL_NO_POS = 14
		const val FIELD_PAGE_SEQUENCE_NO_POS = 18
		const val FIELD_PAGE_CHECKSUM_POS = 22
		const val FIELD_PAGE_SEGMENTS_POS = 26
		const val FIELD_SEGMENT_TABLE_POS = 27

		//Length of various attributes
		const val FIELD_CAPTURE_PATTERN_LENGTH = 4
		const val FIELD_STREAM_STRUCTURE_VERSION_LENGTH = 1
		const val FIELD_HEADER_TYPE_FLAG_LENGTH = 1
		const val FIELD_ABSOLUTE_GRANULE_LENGTH = 8
		const val FIELD_STREAM_SERIAL_NO_LENGTH = 4
		const val FIELD_PAGE_SEQUENCE_NO_LENGTH = 4
		const val FIELD_PAGE_CHECKSUM_LENGTH = 4
		const val FIELD_PAGE_SEGMENTS_LENGTH = 1

		/**
		 * Read next PageHeader from Buffer
		 *
		 * @param byteBuffer
		 * @return
		 * @throws IOException
		 * @throws CannotReadException
		 */
		@Throws(
			IOException::class,
			CannotReadException::class
		)
		fun read(byteBuffer: ByteBuffer): OggPageHeader {
			//byteBuffer
			val start = byteBuffer.position()
			logger.fine(
				"Trying to read OggPage at:$start"
			)
			var b =
				ByteArray(CAPTURE_PATTERN.size)
			byteBuffer[b]
			if (!Arrays.equals(
					b,
					CAPTURE_PATTERN
				)
			) {
				throw CannotReadException(
					ErrorMessage.OGG_HEADER_CANNOT_BE_FOUND.getMsg(
						String(
							b
						)
					)
				)
			}
			byteBuffer.position(start + FIELD_PAGE_SEGMENTS_POS)
			val pageSegments = byteBuffer.get().toInt() and 0xFF //unsigned
			byteBuffer.position(start)
			b =
				ByteArray(OGG_PAGE_HEADER_FIXED_LENGTH + pageSegments)
			byteBuffer[b]

			//Now just after PageHeader, ready for Packet Data
			return OggPageHeader(b)
		}

		/**
		 * Read next PageHeader from file
		 * @param raf
		 * @return
		 * @throws IOException
		 * @throws CannotReadException
		 */
		@JvmStatic
		@Throws(IOException::class, CannotReadException::class)
		fun read(raf: RandomAccessFile): OggPageHeader {
			var start = raf.filePointer
			logger.fine(
				"Trying to read OggPage at:$start"
			)
			var b = ByteArray(CAPTURE_PATTERN.size)
			raf.read(b)
			if (!Arrays.equals(b, CAPTURE_PATTERN)) {
				raf.seek(start)
				if (isId3Tag(raf)) {
					logger.warning(ErrorMessage.OGG_CONTAINS_ID3TAG.getMsg(raf.filePointer - start))
					raf.read(b)
					if (Arrays.equals(b, CAPTURE_PATTERN)) {
						//Go to the end of the ID3 header
						start = raf.filePointer - CAPTURE_PATTERN.size
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
			raf.seek(start + FIELD_PAGE_SEGMENTS_POS)
			val pageSegments = raf.readByte().toInt() and 0xFF //unsigned
			raf.seek(start)
			b = ByteArray(OGG_PAGE_HEADER_FIXED_LENGTH + pageSegments)
			raf.read(b)
			val pageHeader = OggPageHeader(b)
			pageHeader.startByte = start
			//Now just after PageHeader, ready for Packet Data
			return pageHeader
		}
	}
}
