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
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotWriteException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.readThreeBytesAsChars
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.ogg.util.OggCRCFactory.Companion.computeCRC
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.AbstractID3v1Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.vorbiscomment.VorbisCommentTag.Companion.createNewTag
import org.jaudiotagger.audio.ogg.OggVorbisCommentTagCreator
import org.jaudiotagger.audio.ogg.util.OggPageHeader
import org.jaudiotagger.audio.ogg.util.OggPageHeader.Companion.read
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.logging.Logger

/**
 * Write Vorbis Tag within an ogg
 *
 * VorbisComment holds the tag information within an ogg file
 */
class OggVorbisTagWriter {
	private val tc = OggVorbisCommentTagCreator()
	private val reader = OggVorbisTagReader()

	@Throws(IOException::class, CannotReadException::class, CannotWriteException::class)
	fun delete(raf: RandomAccessFile?, tempRaf: RandomAccessFile?) {
		try {
			reader.read(raf)
		} catch (e: CannotReadException) {
			write(createNewTag(), raf, tempRaf)
			return
		}
		val emptyTag = createNewTag()

		//Go back to start of file
		raf!!.seek(0)
		write(emptyTag, raf, tempRaf)
	}

	@Throws(CannotReadException::class, CannotWriteException::class, IOException::class)
	fun write(tag: Tag?, raf: RandomAccessFile?, rafTemp: RandomAccessFile?) {
		logger.config("Starting to write file:")

		//1st Page:Identification Header
		logger.fine("Read 1st Page:identificationHeader:")
		val pageHeader = read(
			raf!!
		)
		raf.seek(pageHeader.startByte)

		//Write 1st page (unchanged) and place writer pointer at end of data
		rafTemp!!.channel.transferFrom(
			raf.channel,
			0,
			(pageHeader.getPageLength() + OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH + pageHeader.segmentTable!!.size).toLong()
		)
		rafTemp.skipBytes(pageHeader.getPageLength() + OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH + pageHeader.segmentTable!!.size)
		logger.fine("Written identificationHeader:")

		//2nd page:Comment and Setup if there is enough room, may also (although not normally) contain audio frames
		val secondPageHeader = read(
			raf
		)

		//2nd Page:Store the end of Header
		val secondPageHeaderEndPos = raf.filePointer
		logger.fine(
			"Read 2nd Page:comment and setup and possibly audio:Header finishes at file position:$secondPageHeaderEndPos"
		)

		//Get header sizes
		raf.seek(0)
		val vorbisHeaderSizes = reader.readOggVorbisHeaderSizes(raf)

		//Convert the OggVorbisComment header to raw packet data
		val newComment = tc.convert(tag)

		//Compute new comment length(this may need to be spread over multiple pages)
		val newCommentLength = newComment.capacity()

		//Calculate new size of new 2nd page
		val newSecondPageDataLength =
			vorbisHeaderSizes.setupHeaderSize + newCommentLength + vorbisHeaderSizes.extraPacketDataSize
		logger.fine("Old 2nd Page no of packets: " + secondPageHeader.getPacketList().size)
		logger.fine("Old 2nd Page size: " + secondPageHeader.getPageLength())
		logger.fine("Old last packet incomplete: " + secondPageHeader.isLastPacketIncomplete)
		logger.fine("Setup Header Size: " + vorbisHeaderSizes.setupHeaderSize)
		logger.fine("Extra Packets: " + vorbisHeaderSizes.extraPacketList.size)
		logger.fine("Extra Packet Data Size: " + vorbisHeaderSizes.extraPacketDataSize)
		logger.fine("Old comment: " + vorbisHeaderSizes.commentHeaderSize)
		logger.fine(
			"New comment: $newCommentLength"
		)
		logger.fine(
			"New Page Data Size: $newSecondPageDataLength"
		)
		//Second Page containing new vorbis, setup and possibly some extra packets can fit on one page
		if (isCommentAndSetupHeaderFitsOnASinglePage(
				newCommentLength,
				vorbisHeaderSizes.setupHeaderSize,
				vorbisHeaderSizes.extraPacketList
			)
		) {
			//And if comment and setup header originally fitted on both, the length of the 2nd
			//page must be less than maximum size allowed
			//AND
			//there must be two packets with last being complete because they may have
			//elected to split the setup over multiple pages instead of using up whole page - (as long
			//as the last lacing value is 255 they can do this)
			//   OR
			//There are more than the packets in which case have complete setup header and some audio packets
			//we dont care if the last audio packet is split on next page as long as we preserve it
			/** [org.jaudiotagger.audio.ogg.OggVorbisTagWriter] */
			if ((secondPageHeader.getPageLength() < OggPageHeader.MAXIMUM_PAGE_DATA_SIZE)
				&& (secondPageHeader.getPacketList().size == 2 && !secondPageHeader.isLastPacketIncomplete)
				|| (secondPageHeader.getPacketList().size) > 2
			) {
				logger.fine("Header and Setup remain on single page:")
				replaceSecondPageOnly(
					vorbisHeaderSizes,
					newCommentLength,
					newSecondPageDataLength,
					secondPageHeader,
					newComment,
					secondPageHeaderEndPos,
					raf,
					rafTemp
				)
			} else {
				logger.fine("Header and Setup now on single page:")
				replaceSecondPageAndRenumberPageSeqs(
					vorbisHeaderSizes,
					newCommentLength,
					newSecondPageDataLength,
					secondPageHeader,
					newComment,
					raf,
					rafTemp
				)
			}
		} else {
			logger.fine("Header and Setup with shift audio:")
			replacePagesAndRenumberPageSeqs(
				vorbisHeaderSizes,
				newCommentLength,
				secondPageHeader,
				newComment,
				raf,
				rafTemp
			)
		}
	}

	/**
	 * Calculate checkSum over the Page
	 *
	 * @param page
	 */
	private fun calculateChecksumOverPage(page: ByteBuffer) {
		//CRC should be zero before calculating it
		page.putInt(OggPageHeader.FIELD_PAGE_CHECKSUM_POS, 0)

		//Compute CRC over the  page  //TODO shouldnt really use array();
		val crc = computeCRC(page.array())
		for (i in crc.indices) {
			page.put(OggPageHeader.FIELD_PAGE_CHECKSUM_POS + i, crc[i])
		}

		//Rewind to start of Page
		page.rewind()
	}

	/**
	 * Create a second Page, and add comment header to it, but page is incomplete may want to add addition header and need to calculate CRC
	 *
	 * @param vorbisHeaderSizes
	 * @param newCommentLength
	 * @param newSecondPageLength
	 * @param secondPageHeader
	 * @param newComment
	 * @return
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun startCreateBasicSecondPage(
		vorbisHeaderSizes: OggVorbisTagReader.OggVorbisHeaderSizes?,
		newCommentLength: Int,
		newSecondPageLength: Int,
		secondPageHeader: OggPageHeader,
		newComment: ByteBuffer?
	): ByteBuffer {
		logger.fine("WriteOgg Type 1")
		val segmentTable = createSegmentTable(
			newCommentLength,
			vorbisHeaderSizes!!.setupHeaderSize,
			vorbisHeaderSizes.extraPacketList
		)
		val newSecondPageHeaderLength =
			OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH + segmentTable.size
		logger.fine(
			"New second page header length:$newSecondPageHeaderLength"
		)
		logger.fine("No of segments:" + segmentTable.size)
		val secondPageBuffer = ByteBuffer.allocate(newSecondPageLength + newSecondPageHeaderLength)
		secondPageBuffer.order(ByteOrder.LITTLE_ENDIAN)

		//Build the new 2nd page header, can mostly be taken from the original upto the segment length OggS capture
		secondPageBuffer.put(
			secondPageHeader.rawHeaderData,
			0,
			OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH - 1
		)

		//Number of Page Segments
		secondPageBuffer.put(segmentTable.size.toByte())

		//Page segment table
		for (aSegmentTable in segmentTable) {
			secondPageBuffer.put(aSegmentTable)
		}

		//Add New VorbisComment
		secondPageBuffer.put(newComment)
		return secondPageBuffer
	}

	/**
	 * Usually can use this method, previously comment and setup header all fit on page 2
	 * and they still do, so just replace this page. And copy further pages as is.
	 *
	 * @param vorbisHeaderSizes
	 * @param newCommentLength
	 * @param newSecondPageLength
	 * @param secondPageHeader
	 * @param newComment
	 * @param secondPageHeaderEndPos
	 * @param raf
	 * @param rafTemp
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun replaceSecondPageOnly(
		vorbisHeaderSizes: OggVorbisTagReader.OggVorbisHeaderSizes?,
		newCommentLength: Int,
		newSecondPageLength: Int,
		secondPageHeader: OggPageHeader,
		newComment: ByteBuffer?,
		secondPageHeaderEndPos: Long,
		raf: RandomAccessFile?,
		rafTemp: RandomAccessFile?
	) {
		logger.fine("WriteOgg Type 1")
		val secondPageBuffer = startCreateBasicSecondPage(
			vorbisHeaderSizes,
			newCommentLength,
			newSecondPageLength,
			secondPageHeader,
			newComment
		)
		raf!!.seek(secondPageHeaderEndPos)
		//Skip comment header
		raf.skipBytes(vorbisHeaderSizes!!.commentHeaderSize)
		//Read in setup header and extra packets
		raf.channel.read(secondPageBuffer)
		calculateChecksumOverPage(secondPageBuffer)
		rafTemp!!.channel.write(secondPageBuffer)
		rafTemp.channel.transferFrom(
			raf.channel,
			rafTemp.filePointer,
			raf.length() - raf.filePointer
		)
	}

	/**
	 * Previously comment and/or setup header was on a number of pages now can just replace this page fitting all
	 * on 2nd page, and renumber subsequent sequence pages
	 *
	 * @param originalHeaderSizes
	 * @param newCommentLength
	 * @param newSecondPageLength
	 * @param secondPageHeader
	 * @param newComment
	 * @param raf
	 * @param rafTemp
	 * @throws IOException
	 * @throws CannotReadException
	 * @throws CannotWriteException
	 */
	@Throws(IOException::class, CannotReadException::class, CannotWriteException::class)
	private fun replaceSecondPageAndRenumberPageSeqs(
		originalHeaderSizes: OggVorbisTagReader.OggVorbisHeaderSizes?,
		newCommentLength: Int,
		newSecondPageLength: Int,
		secondPageHeader: OggPageHeader,
		newComment: ByteBuffer?,
		raf: RandomAccessFile?,
		rafTemp: RandomAccessFile?
	) {
		logger.fine("WriteOgg Type 2")
		val secondPageBuffer = startCreateBasicSecondPage(
			originalHeaderSizes,
			newCommentLength,
			newSecondPageLength,
			secondPageHeader,
			newComment
		)

		//Add setup header and packets
		val pageSequence = secondPageHeader.pageSequence
		val setupHeaderData = reader.convertToVorbisSetupHeaderPacketAndAdditionalPackets(
			originalHeaderSizes!!.setupHeaderStartPosition,
			raf
		)
		logger.finest(
			setupHeaderData.size.toString() + ":" + secondPageBuffer.position() + ":" + secondPageBuffer.capacity()
		)
		secondPageBuffer.put(setupHeaderData)
		calculateChecksumOverPage(secondPageBuffer)
		rafTemp!!.channel.write(secondPageBuffer)
		writeRemainingPages(pageSequence, raf, rafTemp)
	}

	/**
	 * CommentHeader extends over multiple pages OR Comment Header doesnt but it's got larger causing some extra
	 * packets to be shifted onto another page.
	 *
	 * @param originalHeaderSizes
	 * @param newCommentLength
	 * @param secondPageHeader
	 * @param newComment
	 * @param raf
	 * @param rafTemp
	 * @throws IOException
	 * @throws CannotReadException
	 * @throws CannotWriteException
	 */
	@Throws(IOException::class, CannotReadException::class, CannotWriteException::class)
	private fun replacePagesAndRenumberPageSeqs(
		originalHeaderSizes: OggVorbisTagReader.OggVorbisHeaderSizes?,
		newCommentLength: Int,
		secondPageHeader: OggPageHeader,
		newComment: ByteBuffer?,
		raf: RandomAccessFile?,
		rafTemp: RandomAccessFile?
	) {
		var pageSequence = secondPageHeader.pageSequence

		//We need to work out how to split the newcommentlength over the pages
		val noOfCompletePagesNeededForComment =
			newCommentLength / OggPageHeader.MAXIMUM_PAGE_DATA_SIZE
		logger.config(
			"Comment requires:$noOfCompletePagesNeededForComment complete pages"
		)

		//Create the Pages
		var newCommentOffset = 0
		if (noOfCompletePagesNeededForComment > 0) {
			for (i in 0 until noOfCompletePagesNeededForComment) {
				//Create ByteBuffer for the New page
				val segmentTable = createSegments(OggPageHeader.MAXIMUM_PAGE_DATA_SIZE, false)
				val pageHeaderLength =
					OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH + segmentTable.size
				val pageBuffer =
					ByteBuffer.allocate(pageHeaderLength + OggPageHeader.MAXIMUM_PAGE_DATA_SIZE)
				pageBuffer.order(ByteOrder.LITTLE_ENDIAN)

				//Now create the page basing it on the existing 2ndpageheader
				pageBuffer.put(
					secondPageHeader.rawHeaderData,
					0,
					OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH - 1
				)
				//Number of Page Segments
				pageBuffer.put(segmentTable.size.toByte())
				//Page segment table
				for (aSegmentTable in segmentTable) {
					pageBuffer.put(aSegmentTable)
				}
				//Get next bit of Comment
				val nextPartOfComment = newComment!!.slice()
				nextPartOfComment.limit(OggPageHeader.MAXIMUM_PAGE_DATA_SIZE)
				pageBuffer.put(nextPartOfComment)

				//Recalculate Page Sequence Number
				pageBuffer.putInt(OggPageHeader.FIELD_PAGE_SEQUENCE_NO_POS, pageSequence)
				pageSequence++

				//Set Header Flag to indicate continuous (except for first flag)
				if (i != 0) {
					pageBuffer.put(
						OggPageHeader.FIELD_HEADER_TYPE_FLAG_POS,
						OggPageHeader.HeaderTypeFlag.CONTINUED_PACKET.fileValue
					)
				}
				calculateChecksumOverPage(pageBuffer)
				rafTemp!!.channel.write(pageBuffer)
				newCommentOffset += OggPageHeader.MAXIMUM_PAGE_DATA_SIZE
				newComment.position(newCommentOffset)
			}
		}
		val lastPageCommentPacketSize = newCommentLength % OggPageHeader.MAXIMUM_PAGE_DATA_SIZE
		logger.fine(
			"Last comment packet size:$lastPageCommentPacketSize"
		)

		//End of comment and setup header cannot fit on the last page
		if (!isCommentAndSetupHeaderFitsOnASinglePage(
				lastPageCommentPacketSize,
				originalHeaderSizes!!.setupHeaderSize,
				originalHeaderSizes.extraPacketList
			)
		) {
			logger.fine("WriteOgg Type 3")

			//Write the last part of comment only (its possible it might be the only comment)
			run {
				val segmentTable = createSegments(lastPageCommentPacketSize, true)
				val pageHeaderLength =
					OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH + segmentTable.size
				val pageBuffer = ByteBuffer.allocate(lastPageCommentPacketSize + pageHeaderLength)
				pageBuffer.order(ByteOrder.LITTLE_ENDIAN)
				pageBuffer.put(
					secondPageHeader.rawHeaderData,
					0,
					OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH - 1
				)
				pageBuffer.put(segmentTable.size.toByte())
				for (aSegmentTable in segmentTable) {
					pageBuffer.put(aSegmentTable)
				}
				newComment!!.position(newCommentOffset)
				pageBuffer.put(newComment.slice())
				pageBuffer.putInt(OggPageHeader.FIELD_PAGE_SEQUENCE_NO_POS, pageSequence)
				if (noOfCompletePagesNeededForComment > 0) {
					pageBuffer.put(
						OggPageHeader.FIELD_HEADER_TYPE_FLAG_POS,
						OggPageHeader.HeaderTypeFlag.CONTINUED_PACKET.fileValue
					)
				}
				logger.fine(
					"Writing Last Comment Page $pageSequence to file"
				)
				pageSequence++
				calculateChecksumOverPage(pageBuffer)
				rafTemp!!.channel.write(pageBuffer)
			}

			//Now write header and extra packets onto next page
			run {
				val segmentTable = this.createSegmentTable(
					originalHeaderSizes.setupHeaderSize,
					originalHeaderSizes.extraPacketList
				)
				val pageHeaderLength =
					OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH + segmentTable.size
				val setupHeaderData = reader.convertToVorbisSetupHeaderPacketAndAdditionalPackets(
					originalHeaderSizes.setupHeaderStartPosition,
					raf
				)
				val pageBuffer = ByteBuffer.allocate(setupHeaderData.size + pageHeaderLength)
				pageBuffer.order(ByteOrder.LITTLE_ENDIAN)
				pageBuffer.put(
					secondPageHeader.rawHeaderData,
					0,
					OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH - 1
				)
				pageBuffer.put(segmentTable.size.toByte())
				for (aSegmentTable in segmentTable) {
					pageBuffer.put(aSegmentTable)
				}
				pageBuffer.put(setupHeaderData)
				pageBuffer.putInt(OggPageHeader.FIELD_PAGE_SEQUENCE_NO_POS, pageSequence)
				//pageBuffer.put(OggPageHeader.FIELD_HEADER_TYPE_FLAG_POS, OggPageHeader.HeaderTypeFlag.CONTINUED_PACKET.getFileValue());
				logger.fine(
					"Writing Setup Header and packets Page $pageSequence to file"
				)
				calculateChecksumOverPage(pageBuffer)
				rafTemp!!.channel.write(pageBuffer)
			}
		} else {
			//End of Comment and SetupHeader and extra packets can fit on one page
			logger.fine("WriteOgg Type 4")

			//Create last header page
			val newSecondPageDataLength =
				originalHeaderSizes.setupHeaderSize + lastPageCommentPacketSize + originalHeaderSizes.extraPacketDataSize
			newComment!!.position(newCommentOffset)
			val lastComment = newComment.slice()
			val lastHeaderBuffer = startCreateBasicSecondPage(
				originalHeaderSizes,
				lastPageCommentPacketSize,
				newSecondPageDataLength,
				secondPageHeader,
				lastComment
			)
			//Now find the setupheader which is on a different page
			raf!!.seek(originalHeaderSizes.setupHeaderStartPosition)

			//Add setup Header and Extra Packets (although it will fit in this page, it may be over multiple pages in its original form
			//so need to use this function to convert to raw data
			val setupHeaderData = reader.convertToVorbisSetupHeaderPacketAndAdditionalPackets(
				originalHeaderSizes.setupHeaderStartPosition,
				raf
			)
			lastHeaderBuffer.put(setupHeaderData)

			//Page Sequence No
			lastHeaderBuffer.putInt(OggPageHeader.FIELD_PAGE_SEQUENCE_NO_POS, pageSequence)

			//Set Header Flag to indicate continuous (contains end of comment)
			lastHeaderBuffer.put(
				OggPageHeader.FIELD_HEADER_TYPE_FLAG_POS,
				OggPageHeader.HeaderTypeFlag.CONTINUED_PACKET.fileValue
			)
			calculateChecksumOverPage(lastHeaderBuffer)
			rafTemp!!.channel.write(lastHeaderBuffer)
		}

		//Write the rest of the original file
		writeRemainingPages(pageSequence, raf, rafTemp)
	}

	/**
	 * Write all the remaining pages as they are except that the page sequence needs to be modified.
	 *
	 * @param pageSequence
	 * @param raf
	 * @param rafTemp
	 * @throws IOException
	 * @throws CannotReadException
	 * @throws CannotWriteException
	 */
	@Throws(IOException::class, CannotReadException::class, CannotWriteException::class)
	fun writeRemainingPages(pageSequence: Int, raf: RandomAccessFile?, rafTemp: RandomAccessFile?) {
		var pageSequence = pageSequence
		val startAudio = raf!!.filePointer
		val startAudioWritten = rafTemp!!.filePointer

		//TODO there is a risk we wont have enough memory to create these buffers
		val bb = ByteBuffer.allocate((raf.length() - raf.filePointer).toInt())
		val bbTemp = ByteBuffer.allocate((raf.length() - raf.filePointer).toInt())

		//Read in the rest of the data into bytebuffer and rewind it to start
		raf.channel.read(bb)
		bb.rewind()
		var bytesToDiscard: Long = 0
		while (bb.hasRemaining()) {
			var nextPage: OggPageHeader? = null
			try {
				nextPage = read(bb)
			} catch (cre: CannotReadException) {
				//Go back to where were
				bb.position(bb.position() - OggPageHeader.CAPTURE_PATTERN.size)
				//#117:Ogg file with invalid ID3v1 tag at end remove and save
				if (readThreeBytesAsChars(bb) == AbstractID3v1Tag.TAG) {
					bytesToDiscard = (bb.remaining() + AbstractID3v1Tag.TAG.length).toLong()
					break
				} else {
					throw cre
				}
			}
			//Create buffer large enough for next page (header and data) and set byte order to LE so we can use
			//putInt method
			val nextPageHeaderBuffer =
				ByteBuffer.allocate(nextPage.rawHeaderData.size + nextPage.getPageLength())
			nextPageHeaderBuffer.order(ByteOrder.LITTLE_ENDIAN)
			nextPageHeaderBuffer.put(nextPage.rawHeaderData)
			val data = bb.slice()
			data.limit(nextPage.getPageLength())
			nextPageHeaderBuffer.put(data)
			nextPageHeaderBuffer.putInt(OggPageHeader.FIELD_PAGE_SEQUENCE_NO_POS, ++pageSequence)
			calculateChecksumOverPage(nextPageHeaderBuffer)
			bb.position(bb.position() + nextPage.getPageLength())
			nextPageHeaderBuffer.rewind()
			bbTemp.put(nextPageHeaderBuffer)
		}
		//Now just write as a single IO operation
		bbTemp.flip()
		rafTemp.channel.write(bbTemp)
		//Check we have written all the data (minus any invalid Tag at end)
		if (raf.length() - startAudio != rafTemp.length() + bytesToDiscard - startAudioWritten) {
			throw CannotWriteException(
				"File written counts don't match, file not written:"
					+ "origAudioLength:" + (raf.length() - startAudio)
					+ ":newAudioLength:" + (rafTemp.length() + bytesToDiscard - startAudioWritten)
					+ ":bytesDiscarded:" + bytesToDiscard
			)
		}
	}

	/**
	 * This method creates a new segment table for the second page (header).
	 *
	 * @param newCommentLength  The length of the Vorbis Comment
	 * @param setupHeaderLength The length of Setup Header, zero if comment String extends
	 * over multiple pages and this is not the last page.
	 * @param extraPackets      If there are packets immediately after setup header in same page, they
	 * need including in the segment table
	 * @return new segment table.
	 */
	private fun createSegmentTable(
		newCommentLength: Int,
		setupHeaderLength: Int,
		extraPackets: List<OggPageHeader.PacketStartAndLength?>?
	): ByteArray {
		logger.finest(
			"Create SegmentTable CommentLength:$newCommentLength:SetupHeaderLength:$setupHeaderLength"
		)
		val resultBaos = ByteArrayOutputStream()
		val newStart: ByteArray
		val restShouldBe: ByteArray
		var nextPacket: ByteArray

		//Vorbis Comment
		if (setupHeaderLength == 0) {
			//Comment Stream continues onto next page so last lacing value can be 255
			newStart = createSegments(newCommentLength, false)
			return newStart
		} else {
			//Comment Stream finishes on this page so if is a multiple of 255
			//have to add an extra entry.
			newStart = createSegments(newCommentLength, true)
		}

		//Setup Header, should be closed
		restShouldBe = if (extraPackets!!.size > 0) {
			createSegments(setupHeaderLength, true)
		} else {
			createSegments(setupHeaderLength, false)
		}
		logger.finest("Created " + newStart.size + " segments for header")
		logger.finest("Created " + restShouldBe.size + " segments for setup")
		try {
			resultBaos.write(newStart)
			resultBaos.write(restShouldBe)
			if (extraPackets.size > 0) {
				//Packets are being copied literally not converted from a length, so always pass
				//false parameter, TODO is this statement correct
				logger.finer("Creating segments for " + extraPackets.size + " packets")
				for (packet in extraPackets) {
					nextPacket = createSegments(packet!!.length, false)
					resultBaos.write(nextPacket)
				}
			}
		} catch (ioe: IOException) {
			throw RuntimeException("Unable to create segment table:" + ioe.message)
		}
		return resultBaos.toByteArray()
	}

	/**
	 * This method creates a new segment table for the second half of setup header
	 *
	 * @param setupHeaderLength The length of Setup Header, zero if comment String extends
	 * over multiple pages and this is not the last page.
	 * @param extraPackets      If there are packets immediately after setup header in same page, they
	 * need including in the segment table
	 * @return new segment table.
	 */
	private fun createSegmentTable(
		setupHeaderLength: Int,
		extraPackets: List<OggPageHeader.PacketStartAndLength?>?
	): ByteArray {
		val resultBaos = ByteArrayOutputStream()
		val restShouldBe: ByteArray
		var nextPacket: ByteArray

		//Setup Header
		restShouldBe = createSegments(setupHeaderLength, true)
		try {
			resultBaos.write(restShouldBe)
			if (extraPackets!!.size > 0) {
				//Packets are being copied literally not converted from a length, so always pass
				//false parameter, TODO is this statement correct
				for (packet in extraPackets) {
					nextPacket = createSegments(packet!!.length, false)
					resultBaos.write(nextPacket)
				}
			}
		} catch (ioe: IOException) {
			throw RuntimeException("Unable to create segment table:" + ioe.message)
		}
		return resultBaos.toByteArray()
	}

	/**
	 * This method creates a byte array of values whose sum should
	 * be the value of `length`.<br></br>
	 *
	 * @param length     Size of the page which should be
	 * represented as 255 byte packets.
	 * @param quitStream If true and a length is a multiple of 255 we need another
	 * segment table entry with the value of 0. Else it's the last stream of the
	 * table which is already ended.
	 * @return Array of packet sizes. However only the last packet will
	 * differ from 255.
	 */
	//TODO if pass is data of max length (65025 bytes) and have quitStream==true
	//this will return 256 segments which is illegal, should be checked somewhere
	private fun createSegments(length: Int, quitStream: Boolean): ByteArray {
		logger.finest(
			"Create Segments for length:$length:QuitStream:$quitStream"
		)
		//It is valid to have nil length packets
		if (length == 0) {
			val result = ByteArray(1)
			result[0] = 0x00.toByte()
			return result
		}
		val result =
			ByteArray(length / OggPageHeader.MAXIMUM_SEGMENT_SIZE + if (length % OggPageHeader.MAXIMUM_SEGMENT_SIZE == 0 && !quitStream) 0 else 1)
		var i = 0
		while (i < result.size - 1) {
			result[i] = 0xFF.toByte()
			i++
		}
		result[result.size - 1] = (length - i * OggPageHeader.MAXIMUM_SEGMENT_SIZE).toByte()
		return result
	}

	/**
	 * @param commentLength
	 * @param setupHeaderLength
	 * @param extraPacketList
	 * @return true if there is enough room to fit the comment and the setup headers on one page taking into
	 * account the maximum no of segments allowed per page and zero lacing values.
	 */
	private fun isCommentAndSetupHeaderFitsOnASinglePage(
		commentLength: Int,
		setupHeaderLength: Int,
		extraPacketList: List<OggPageHeader.PacketStartAndLength?>?
	): Boolean {
		var totalDataSize = 0
		if (commentLength == 0) {
			totalDataSize++
		} else {
			totalDataSize = commentLength / OggPageHeader.MAXIMUM_SEGMENT_SIZE + 1
			if (commentLength % OggPageHeader.MAXIMUM_SEGMENT_SIZE == 0) {
				totalDataSize++
			}
		}
		logger.finest(
			"Require:$totalDataSize segments for comment"
		)
		if (setupHeaderLength == 0) {
			totalDataSize++
		} else {
			totalDataSize += setupHeaderLength / OggPageHeader.MAXIMUM_SEGMENT_SIZE + 1
			if (setupHeaderLength % OggPageHeader.MAXIMUM_SEGMENT_SIZE == 0) {
				totalDataSize++
			}
		}
		logger.finest(
			"Require:$totalDataSize segments for comment plus setup"
		)
		for (extraPacket in extraPacketList!!) {
			if (extraPacket!!.length == 0) {
				totalDataSize++
			} else {
				totalDataSize += extraPacket.length / OggPageHeader.MAXIMUM_SEGMENT_SIZE + 1
				if (extraPacket.length % OggPageHeader.MAXIMUM_SEGMENT_SIZE == 0) {
					totalDataSize++
				}
			}
		}
		logger.finest(
			"Total No Of Segment If New Comment And Header Put On One Page:$totalDataSize"
		)
		return totalDataSize <= OggPageHeader.MAXIMUM_NO_OF_SEGMENT_SIZE
	}

	companion object {
		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.audio.ogg")
	}
}
