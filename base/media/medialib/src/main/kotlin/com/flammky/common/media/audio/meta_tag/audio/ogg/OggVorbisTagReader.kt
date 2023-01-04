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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.ogg

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.ogg.util.VorbisHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.ogg.util.VorbisPacketType
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.vorbiscomment.VorbisCommentReader
import org.jaudiotagger.audio.ogg.util.OggPageHeader
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.util.logging.Logger

/**
 * Read Vorbis Comment Tag within ogg
 *
 * Vorbis is the audiostream within an ogg file, Vorbis uses VorbisComments as its tag
 */
open class OggVorbisTagReader {
	private val vorbisCommentReader: VorbisCommentReader

	init {
		vorbisCommentReader = VorbisCommentReader()
	}

	/**
	 * Read the Logical VorbisComment Tag from the file
	 *
	 *
	 * Read the CommenyTag, within an OggVorbis file the VorbisCommentTag is mandatory
	 *
	 * @param raf
	 * @return
	 * @throws CannotReadException
	 * @throws IOException
	 */
	@Throws(CannotReadException::class, IOException::class)
	open fun read(raf: RandomAccessFile?): Tag {
		logger.config("Starting to read ogg vorbis tag from file:")
		val rawVorbisCommentData = readRawPacketData(raf)

		//Begin tag reading
		val tag = vorbisCommentReader.read(rawVorbisCommentData, true, null)
		logger.fine("CompletedReadCommentTag")
		return tag
	}

	open fun read(fc: FileChannel): Tag {
		val rawVorbisCommentData = readRawPacketData(fc)
		//Begin tag reading
		return vorbisCommentReader.read(rawVorbisCommentData, true, null)
	}

	/**
	 * Retrieve the Size of the VorbisComment packet including the oggvorbis header
	 *
	 * @param raf
	 * @return
	 * @throws CannotReadException
	 * @throws IOException
	 */
	@Throws(CannotReadException::class, IOException::class)
	open fun readOggVorbisRawSize(raf: RandomAccessFile?): Int {
		val rawVorbisCommentData = readRawPacketData(raf)
		return rawVorbisCommentData.size + VorbisHeader.FIELD_PACKET_TYPE_LENGTH + VorbisHeader.FIELD_CAPTURE_PATTERN_LENGTH
	}

	/**
	 * Retrieve the raw VorbisComment packet data, does not include the OggVorbis header
	 *
	 * @param raf
	 * @return
	 * @throws CannotReadException if unable to find vorbiscomment header
	 * @throws IOException
	 */
	@Throws(
		CannotReadException::class,
		IOException::class
	)
	open fun readRawPacketData(raf: RandomAccessFile?): ByteArray {
		logger.fine(
			"Read 1st page"
		)
		//1st page = codec infos
		var pageHeader =
			OggPageHeader.read(
				raf!!
			)
		//Skip over data to end of page header 1
		raf.seek(raf.filePointer + pageHeader.getPageLength())
		logger.fine(
			"Read 2nd page"
		)
		//2nd page = comment, may extend to additional pages or not , may also have setup header
		pageHeader =
			OggPageHeader.read(
				raf
			)

		//Now at start of packets on page 2 , check this is the vorbis comment header
		val b =
			ByteArray(VorbisHeader.FIELD_PACKET_TYPE_LENGTH + VorbisHeader.FIELD_CAPTURE_PATTERN_LENGTH)
		raf.read(b)

		if (!isVorbisCommentHeader(b)) {
			throw CannotReadException(
				"Cannot find comment block (no vorbiscomment header)"
			)
		}

		//Convert the comment raw data which maybe over many pages back into raw packet
		return convertToVorbisCommentPacket(pageHeader, raf)
	}

	open fun readRawPacketData(fc: FileChannel): ByteArray {
		//1st page = codec infos
		var pageHeader =
			OggPageHeader.read(fc)
		//Skip over data to end of page header 1
		fc.position(fc.position() + pageHeader.getPageLength())
		//2nd page = comment, may extend to additional pages or not , may also have setup header
		pageHeader = OggPageHeader.read(fc)
		//Now at start of packets on page 2 , check this is the vorbis comment header
		val b = ByteBuffer.allocate(VorbisHeader.FIELD_PACKET_TYPE_LENGTH + VorbisHeader.FIELD_CAPTURE_PATTERN_LENGTH)
		fc.read(b)
		val arr = b.array()
		if (!isVorbisCommentHeader(arr)) {
			throw CannotReadException(
				"Cannot find comment block (no vorbiscomment header)"
			)
		}

		//Convert the comment raw data which maybe over many pages back into raw packet
		return convertToVorbisCommentPacket(pageHeader, fc)
	}

	/**
	 * Is this a Vorbis Comment header, check
	 *
	 * Note this check only applies to Vorbis Comments embedded within an OggVorbis File which is why within here
	 *
	 * @param headerData
	 * @return true if the headerData matches a VorbisComment header i.e is a Vorbis header of type COMMENT_HEADER
	 */
	open fun isVorbisCommentHeader(headerData: ByteArray): Boolean {
		val vorbis = String(
			headerData,
			VorbisHeader.FIELD_CAPTURE_PATTERN_POS,
			VorbisHeader.FIELD_CAPTURE_PATTERN_LENGTH,
			StandardCharsets.ISO_8859_1
		)
		return !(headerData[VorbisHeader.FIELD_PACKET_TYPE_POS].toInt() != VorbisPacketType.COMMENT_HEADER.type || vorbis != VorbisHeader.CAPTURE_PATTERN)
	}

	/**
	 * Is this a Vorbis SetupHeader check
	 *
	 * @param headerData
	 * @return true if matches vorbis setupheader
	 */
	fun isVorbisSetupHeader(headerData: ByteArray): Boolean {
		val vorbis = String(
			headerData,
			VorbisHeader.FIELD_CAPTURE_PATTERN_POS,
			VorbisHeader.FIELD_CAPTURE_PATTERN_LENGTH,
			StandardCharsets.ISO_8859_1
		)
		return !(headerData[VorbisHeader.FIELD_PACKET_TYPE_POS].toInt() != VorbisPacketType.SETUP_HEADER.type || vorbis != VorbisHeader.CAPTURE_PATTERN)
	}

	/**
	 * The Vorbis Comment may span multiple pages so we we need to identify the pages they contain and then
	 * extract the packet data from the pages
	 * @param startVorbisCommentPage
	 * @param raf
	 * @throws CannotReadException
	 * @throws IOException
	 * @return
	 */
	@Throws(IOException::class, CannotReadException::class)
	private fun convertToVorbisCommentPacket(
		startVorbisCommentPage: OggPageHeader,
		raf: RandomAccessFile?
	): ByteArray {
		val baos = ByteArrayOutputStream()
		var b =
			ByteArray(startVorbisCommentPage.getPacketList()[0].length - (VorbisHeader.FIELD_PACKET_TYPE_LENGTH + VorbisHeader.FIELD_CAPTURE_PATTERN_LENGTH))
		raf!!.read(b)
		baos.write(b)

		//Because there is at least one other packet (SetupHeaderPacket) this means the Comment Packet has finished
		//on this page so thats all we need and we can return
		if (startVorbisCommentPage.getPacketList().size > 1) {
			logger.config("Comments finish on 2nd Page because there is another packet on this page")
			return baos.toByteArray()
		}

		//There is only the VorbisComment packet on page if it has completed on this page we can return
		if (!startVorbisCommentPage.isLastPacketIncomplete) {
			logger.config("Comments finish on 2nd Page because this packet is complete")
			return baos.toByteArray()
		}

		//The VorbisComment extends to the next page, so should be at end of page already
		//so carry on reading pages until we get to the end of comment
		while (true) {
			logger.config("Reading next page")
			val nextPageHeader = OggPageHeader.read(
				raf
			)
			b = ByteArray(nextPageHeader.getPacketList()[0].length)
			raf.read(b)
			baos.write(b)

			//Because there is at least one other packet (SetupHeaderPacket) this means the Comment Packet has finished
			//on this page so thats all we need and we can return
			if (nextPageHeader.getPacketList().size > 1) {
				logger.config("Comments finish on Page because there is another packet on this page")
				return baos.toByteArray()
			}

			//There is only the VorbisComment packet on page if it has completed on this page we can return
			if (!nextPageHeader.isLastPacketIncomplete) {
				logger.config("Comments finish on Page because this packet is complete")
				return baos.toByteArray()
			}
		}
	}

	open protected fun convertToVorbisCommentPacket(
		startVorbisCommentPage: OggPageHeader,
		fc: FileChannel
	): ByteArray {
		val baos = ByteArrayOutputStream()
		var b = ByteBuffer.allocate(startVorbisCommentPage.getPacketList()[0].length - (VorbisHeader.FIELD_PACKET_TYPE_LENGTH + VorbisHeader.FIELD_CAPTURE_PATTERN_LENGTH))
		fc.read(b)
		baos.write(b.array())

		//Because there is at least one other packet (SetupHeaderPacket) this means the Comment Packet has finished
		//on this page so thats all we need and we can return
		if (startVorbisCommentPage.getPacketList().size > 1) {
			return baos.toByteArray()
		}

		//There is only the VorbisComment packet on page if it has completed on this page we can return
		if (!startVorbisCommentPage.isLastPacketIncomplete) {
			return baos.toByteArray()
		}

		//The VorbisComment extends to the next page, so should be at end of page already
		//so carry on reading pages until we get to the end of comment
		while (true) {
			logger.config("Reading next page")
			val nextPageHeader = OggPageHeader.read(fc)
			b = ByteBuffer.allocate(nextPageHeader.getPacketList()[0].length)
			fc.read(b)
			baos.write(b.array())

			//Because there is at least one other packet (SetupHeaderPacket) this means the Comment Packet has finished
			//on this page so thats all we need and we can return
			if (nextPageHeader.getPacketList().size > 1) {
				return baos.toByteArray()
			}

			//There is only the VorbisComment packet on page if it has completed on this page we can return
			if (!nextPageHeader.isLastPacketIncomplete) {
				return baos.toByteArray()
			}
		}
	}

	/**
	 * The Vorbis Setup Header may span multiple(2) pages, athough it doesnt normally. We pass the start of the
	 * file offset of the OggPage it belongs on, it probably won't be first packet.
	 * @param fileOffsetOfStartingOggPage
	 * @param raf
	 * @throws CannotReadException
	 * @throws IOException
	 * @return
	 */
	@Throws(IOException::class, CannotReadException::class)
	fun convertToVorbisSetupHeaderPacket(
		fileOffsetOfStartingOggPage: Long,
		raf: RandomAccessFile
	): ByteArray {
		val baos = ByteArrayOutputStream()

		//Seek to specified offset
		raf.seek(fileOffsetOfStartingOggPage)

		//Read Page
		val setupPageHeader = OggPageHeader.read(raf)

		//Assume that if multiple packets first packet is VorbisComment and second packet
		//is setupheader
		if (setupPageHeader.getPacketList().size > 1) {
			raf.skipBytes(setupPageHeader.getPacketList()[0].length)
		}

		//Now should be at start of next packet, check this is the vorbis setup header
		var b =
			ByteArray(VorbisHeader.FIELD_PACKET_TYPE_LENGTH + VorbisHeader.FIELD_CAPTURE_PATTERN_LENGTH)
		raf.read(b)
		if (!isVorbisSetupHeader(b)) {
			throw CannotReadException("Unable to find setup header(2), unable to write ogg file")
		}

		//Go back to start of setupheader data
		raf.seek(raf.filePointer - (VorbisHeader.FIELD_PACKET_TYPE_LENGTH + VorbisHeader.FIELD_CAPTURE_PATTERN_LENGTH))

		//Read data
		if (setupPageHeader.getPacketList().size > 1) {
			b = ByteArray(setupPageHeader.getPacketList()[1].length)
			raf.read(b)
			baos.write(b)
		} else {
			b = ByteArray(setupPageHeader.getPacketList()[0].length)
			raf.read(b)
			baos.write(b)
		}

		//Return Data
		if (!setupPageHeader.isLastPacketIncomplete || setupPageHeader.getPacketList().size > 2) {
			logger.config("Setupheader finishes on this page")
			return baos.toByteArray()
		}

		//The Setupheader extends to the next page, so should be at end of page already
		//so carry on reading pages until we get to the end of comment
		while (true) {
			logger.config("Reading another page")
			val nextPageHeader = OggPageHeader.read(raf)
			b = ByteArray(nextPageHeader.getPacketList()[0].length)
			raf.read(b)
			baos.write(b)

			//Because there is at least one other packet this means the Setupheader Packet has finished
			//on this page so thats all we need and we can return
			if (nextPageHeader.getPacketList().size > 1) {
				logger.config("Setupheader finishes on this page")
				return baos.toByteArray()
			}

			//There is only the Setupheader packet on page if it has completed on this page we can return
			if (!nextPageHeader.isLastPacketIncomplete) {
				logger.config("Setupheader finish on Page because this packet is complete")
				return baos.toByteArray()
			}
		}
	}

	/**
	 * The Vorbis Setup Header may span multiple(2) pages, athough it doesnt normally. We pass the start of the
	 * file offset of the OggPage it belongs on, it probably won't be first packet, also returns any addditional
	 * packets that immediately follow the setup header in original file
	 * @param fileOffsetOfStartingOggPage
	 * @param raf
	 * @throws CannotReadException
	 * @throws IOException
	 * @return
	 */
	@Throws(IOException::class, CannotReadException::class)
	fun convertToVorbisSetupHeaderPacketAndAdditionalPackets(
		fileOffsetOfStartingOggPage: Long,
		raf: RandomAccessFile?
	): ByteArray {
		val baos = ByteArrayOutputStream()

		//Seek to specified offset
		raf!!.seek(fileOffsetOfStartingOggPage)

		//Read Page
		val setupPageHeader = OggPageHeader.read(
			raf
		)

		//Assume that if multiple packets first packet is VorbisComment and second packet
		//is setupheader
		if (setupPageHeader.getPacketList().size > 1) {
			raf.skipBytes(setupPageHeader.getPacketList()[0].length)
		}

		//Now should be at start of next packet, check this is the vorbis setup header
		var b =
			ByteArray(VorbisHeader.FIELD_PACKET_TYPE_LENGTH + VorbisHeader.FIELD_CAPTURE_PATTERN_LENGTH)
		raf.read(b)
		if (!isVorbisSetupHeader(b)) {
			throw CannotReadException("Unable to find setup header(2), unable to write ogg file")
		}

		//Go back to start of setupheader data
		raf.seek(raf.filePointer - (VorbisHeader.FIELD_PACKET_TYPE_LENGTH + VorbisHeader.FIELD_CAPTURE_PATTERN_LENGTH))

		//Read data
		if (setupPageHeader.getPacketList().size > 1) {
			b = ByteArray(setupPageHeader.getPacketList()[1].length)
			raf.read(b)
			baos.write(b)
		} else {
			b = ByteArray(setupPageHeader.getPacketList()[0].length)
			raf.read(b)
			baos.write(b)
		}

		//Return Data
		if (!setupPageHeader.isLastPacketIncomplete || setupPageHeader.getPacketList().size > 2) {
			logger.config("Setupheader finishes on this page")
			if (setupPageHeader.getPacketList().size > 2) {
				for (i in 2 until setupPageHeader.getPacketList().size) {
					b = ByteArray(setupPageHeader.getPacketList()[i].length)
					raf.read(b)
					baos.write(b)
				}
			}
			return baos.toByteArray()
		}

		//The Setupheader extends to the next page, so should be at end of page already
		//so carry on reading pages until we get to the end of comment
		while (true) {
			logger.config("Reading another page")
			val nextPageHeader = OggPageHeader.read(
				raf
			)
			b = ByteArray(nextPageHeader.getPacketList()[0].length)
			raf.read(b)
			baos.write(b)

			//Because there is at least one other packet this means the Setupheader Packet has finished
			//on this page so thats all we need and we can return
			if (nextPageHeader.getPacketList().size > 1) {
				logger.config("Setupheader finishes on this page")
				return baos.toByteArray()
			}

			//There is only the Setupheader packet on page if it has completed on this page we can return
			if (!nextPageHeader.isLastPacketIncomplete) {
				logger.config("Setupheader finish on Page because this packet is complete")
				return baos.toByteArray()
			}
		}
	}

	/**
	 * Calculate the size of the packet data for the comment and setup headers
	 *
	 * @param raf
	 * @return
	 * @throws CannotReadException
	 * @throws IOException
	 */
	@Throws(CannotReadException::class, IOException::class)
	fun readOggVorbisHeaderSizes(raf: RandomAccessFile?): OggVorbisHeaderSizes {
		logger.fine("Started to read comment and setup header sizes:")

		//Stores filepointers so return file in same state
		val filepointer = raf!!.filePointer

		//Extra Packets on same page as setup header
		var extraPackets: List<OggPageHeader.PacketStartAndLength> = ArrayList()
		val commentHeaderStartPosition: Long
		val setupHeaderStartPosition: Long
		var commentHeaderSize = 0
		var setupHeaderSize: Int
		//1st page = codec infos
		var pageHeader = OggPageHeader.read(
			raf
		)
		//Skip over data to end of page header 1
		raf.seek(raf.filePointer + pageHeader.getPageLength())

		//2nd page = comment, may extend to additional pages or not , may also have setup header
		pageHeader = OggPageHeader.read(
			raf
		)
		commentHeaderStartPosition =
			raf.filePointer - (OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH + pageHeader.segmentTable!!.size)

		//Now at start of packets on page 2 , check this is the vorbis comment header
		var b =
			ByteArray(VorbisHeader.FIELD_PACKET_TYPE_LENGTH + VorbisHeader.FIELD_CAPTURE_PATTERN_LENGTH)
		raf.read(b)
		if (!isVorbisCommentHeader(b)) {
			throw CannotReadException("Cannot find comment block (no vorbiscomment header)")
		}
		raf.seek(raf.filePointer - (VorbisHeader.FIELD_PACKET_TYPE_LENGTH + VorbisHeader.FIELD_CAPTURE_PATTERN_LENGTH))
		logger.config("Found start of comment header at:" + raf.filePointer)

		//Calculate Comment Size (not inc header)
		while (true) {
			val packetList = pageHeader.getPacketList()
			commentHeaderSize += packetList[0].length
			raf.skipBytes(packetList[0].length)

			//If this page contains multiple packets or if this last packet is complete then the Comment header
			//end son this page and we can break
			if (packetList.size > 1 || !pageHeader.isLastPacketIncomplete) {
				//done comment size
				logger.config("Found end of comment:size:" + commentHeaderSize + "finishes at file position:" + raf.filePointer)
				break
			}
			pageHeader = OggPageHeader.read(
				raf
			)
		}

		//If there are no more packets on this page we need to go to next page to get the setup header
		val packet: OggPageHeader.PacketStartAndLength
		if (pageHeader.getPacketList().size == 1) {
			pageHeader = OggPageHeader.read(
				raf
			)
			var packetList = pageHeader.getPacketList()
			packet = pageHeader.getPacketList()[0]

			//Now at start of next packet , check this is the vorbis setup header
			b =
				ByteArray(VorbisHeader.FIELD_PACKET_TYPE_LENGTH + VorbisHeader.FIELD_CAPTURE_PATTERN_LENGTH)
			raf.read(b)
			if (!isVorbisSetupHeader(b)) {
				throw CannotReadException(ErrorMessage.OGG_VORBIS_NO_VORBIS_HEADER_FOUND.msg)
			}
			raf.seek(raf.filePointer - (VorbisHeader.FIELD_PACKET_TYPE_LENGTH + VorbisHeader.FIELD_CAPTURE_PATTERN_LENGTH))
			logger.config("Found start of vorbis setup header at file position:" + raf.filePointer)

			//Set this to the  start of the OggPage that setupheader was found on
			setupHeaderStartPosition =
				raf.filePointer - (OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH + pageHeader.segmentTable!!.size)

			//Add packet data to size to the setup header size
			setupHeaderSize = packet.length
			logger.fine("Adding:" + packet.length + " to setup header size")

			//Skip over the packet data
			raf.skipBytes(packet.length)

			//If there are other packets that follow this one, or if the last packet is complete then we must have
			//got the size of the setup header.
			if (packetList.size > 1 || !pageHeader.isLastPacketIncomplete) {
				logger.config("Found end of setupheader:size:" + setupHeaderSize + "finishes at:" + raf.filePointer)
				if (packetList.size > 1) {
					extraPackets = packetList.subList(1, packetList.size)
				}
			} else {
				pageHeader = OggPageHeader.read(
					raf
				)
				packetList = pageHeader.getPacketList()
				while (true) {
					setupHeaderSize += packetList[0].length
					logger.fine("Adding:" + packetList[0].length + " to setup header size")
					raf.skipBytes(packetList[0].length)
					if (packetList.size > 1 || !pageHeader.isLastPacketIncomplete) {
						//done setup size
						logger.fine("Found end of setupheader:size:" + setupHeaderSize + "finishes at:" + raf.filePointer)
						if (packetList.size > 1) {
							extraPackets = packetList.subList(1, packetList.size)
						}
						break
					}
					//Continues onto another page
					pageHeader = OggPageHeader.read(
						raf
					)
				}
			}
		} else {
			packet = pageHeader.getPacketList()[1]
			var packetList = pageHeader.getPacketList()

			//Now at start of next packet , check this is the vorbis setup header
			b =
				ByteArray(VorbisHeader.FIELD_PACKET_TYPE_LENGTH + VorbisHeader.FIELD_CAPTURE_PATTERN_LENGTH)
			raf.read(b)
			if (!isVorbisSetupHeader(b)) {
				logger.warning("Expecting but got:" + String(b) + "at " + (raf.filePointer - b.size))
				throw CannotReadException(ErrorMessage.OGG_VORBIS_NO_VORBIS_HEADER_FOUND.msg)
			}
			raf.seek(raf.filePointer - (VorbisHeader.FIELD_PACKET_TYPE_LENGTH + VorbisHeader.FIELD_CAPTURE_PATTERN_LENGTH))
			logger.config("Found start of vorbis setup header at file position:" + raf.filePointer)

			//Set this to the  start of the OggPage that setupheader was found on
			setupHeaderStartPosition =
				(raf.filePointer - (OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH + pageHeader.segmentTable!!.size)
					- pageHeader.getPacketList()[0].length)

			//Add packet data to size to the setup header size
			setupHeaderSize = packet.length
			logger.fine("Adding:" + packet.length + " to setup header size")

			//Skip over the packet data
			raf.skipBytes(packet.length)

			//If there are other packets that follow this one, or if the last packet is complete then we must have
			//got the size of the setup header.
			if (packetList.size > 2 || !pageHeader.isLastPacketIncomplete) {
				logger.fine("Found end of setupheader:size:" + setupHeaderSize + "finishes at:" + raf.filePointer)
				if (packetList.size > 2) {
					extraPackets = packetList.subList(2, packetList.size)
				}
			} else {
				pageHeader = OggPageHeader.read(
					raf
				)
				packetList = pageHeader.getPacketList()
				while (true) {
					setupHeaderSize += packetList[0].length
					logger.fine("Adding:" + packetList[0].length + " to setup header size")
					raf.skipBytes(packetList[0].length)
					if (packetList.size > 1 || !pageHeader.isLastPacketIncomplete) {
						//done setup size
						logger.fine("Found end of setupheader:size:" + setupHeaderSize + "finishes at:" + raf.filePointer)
						if (packetList.size > 1) {
							extraPackets = packetList.subList(1, packetList.size)
						}
						break
					}
					//Continues onto another page
					pageHeader = OggPageHeader.read(
						raf
					)
				}
			}
		}

		//Reset filepointer to location that it was in at start of method
		raf.seek(filepointer)
		return OggVorbisHeaderSizes(
			commentHeaderStartPosition,
			setupHeaderStartPosition,
			commentHeaderSize,
			setupHeaderSize,
			extraPackets
		)
	}

	/**
	 * Find the length of the raw packet data and the start position of the ogg page header they start in
	 * for the two OggVorbisHeader we need to know about when writing data (sizes included vorbis header)
	 */
	class OggVorbisHeaderSizes internal constructor(
		/**
		 * @return the start position in the file of the ogg header which contains the start of the Vorbis Comment
		 */
		val commentHeaderStartPosition: Long,
		/**
		 * @return the start position in the file of the ogg header which contains the start of the Setup Header
		 */
		val setupHeaderStartPosition: Long,
		/**
		 * @return the size of the raw packet data for the vorbis comment header (includes vorbis header)
		 */
		val commentHeaderSize: Int,
		/**
		 * @return he size of the raw packet data for the vorbis setup header (includes vorbis header)
		 */
		val setupHeaderSize: Int, val extraPacketList: List<OggPageHeader.PacketStartAndLength>
	) {

		/**
		 * Return the size required by all the extra packets on same page as setup header, usually there are
		 * no packets immediately after the setup packet.
		 *
		 * @return extra data size required for additional packets on same page
		 */
		val extraPacketDataSize: Int
			get() {
				var extraPacketSize = 0
				for (packet in extraPacketList) {
					extraPacketSize += packet.length
				}
				return extraPacketSize
			}
	}

	companion object {
		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.audio.ogg")
	}
}
