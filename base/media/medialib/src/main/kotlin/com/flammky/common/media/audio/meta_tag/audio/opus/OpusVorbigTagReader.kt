package com.flammky.common.media.audio.meta_tag.audio.opus


import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.ogg.OggVorbisTagReader
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.vorbiscomment.VorbisCommentReader
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.vorbiscomment.VorbisCommentTag
import org.jaudiotagger.audio.ogg.util.OggPageHeader
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.util.logging.Logger


class OpusVorbisTagReader : OggVorbisTagReader() {
	private val tagReader: VorbisCommentReader = VorbisCommentReader()

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
	override fun read(raf: RandomAccessFile?): Tag {
		logger.config("Starting to read ogg vorbis tag from file:")
		val rawVorbisCommentData = readRawPacketData(raf)

		//Begin tag reading
		val tag: VorbisCommentTag = tagReader.read(rawVorbisCommentData, false, null)
		logger.fine("CompletedReadCommentTag")
		return tag
	}

	override fun read(fc: FileChannel): Tag {
		logger.config("Starting to read ogg vorbis tag from file:")
		val rawVorbisCommentData = readRawPacketData(fc)

		//Begin tag reading
		val tag: VorbisCommentTag = tagReader.read(rawVorbisCommentData, false, null)
		logger.fine("CompletedReadCommentTag")
		return tag
	}

	/**
	 * Retrieve the raw VorbisComment packet data, does not include the OggVorbis header
	 *
	 * @param raf
	 * @return
	 * @throws CannotReadException if unable to find vorbiscomment header
	 * @throws IOException
	 */
	@Throws(CannotReadException::class, IOException::class)
	override fun readRawPacketData(raf: RandomAccessFile?): ByteArray {
		logger.fine("Read 1st page")
		//1st page = codec infos
		var pageHeader: OggPageHeader = OggPageHeader.read(raf!!)
		//Skip over data to end of page header 1
		raf.seek(raf.filePointer + pageHeader.getPageLength())
		logger.fine("Read 2nd page")
		//2nd page = comment, may extend to additional pages or not , may also have setup header
		pageHeader = OggPageHeader.read(raf)

		//Now at start of packets on page 2 , check this is the OpusTags comment header
		val b = ByteArray(OpusHeader.TAGS_CAPTURE_PATTERN_LENGTH)
		raf.read(b)
		if (!isVorbisCommentHeader(b)) {
			throw CannotReadException("Cannot find comment block (no vorbiscomment header)")
		}

		//Convert the comment raw data which maybe over many pages back into raw packet
		return convertToVorbisCommentPacket(pageHeader, raf)
	}

	override fun readRawPacketData(fc: FileChannel): ByteArray {
		//1st page = codec infos
		var pageHeader: OggPageHeader = OggPageHeader.read(fc)
		//Skip over data to end of page header 1
		fc.position(fc.position() + pageHeader.getPageLength())
		logger.fine("Read 2nd page")
		//2nd page = comment, may extend to additional pages or not , may also have setup header
		pageHeader = OggPageHeader.read(fc)

		//Now at start of packets on page 2 , check this is the OpusTags comment header
		val b = ByteBuffer.allocate(OpusHeader.TAGS_CAPTURE_PATTERN_LENGTH)
		fc.read(b)
		if (!isVorbisCommentHeader(b.array())) {
			throw CannotReadException("Cannot find comment block (no vorbiscomment header)")
		}

		//Convert the comment raw data which maybe over many pages back into raw packet
		return convertToVorbisCommentPacket(pageHeader, fc)
	}

	@Throws(IOException::class, CannotReadException::class)
	protected fun convertToVorbisCommentPacket(
		startPage: OggPageHeader,
		raf: RandomAccessFile
	): ByteArray {
		val baos = ByteArrayOutputStream()

		// read the rest of the first page
		var packet = ByteArray(
			startPage.getPacketList().get(0).length - OpusHeader.TAGS_CAPTURE_PATTERN_LENGTH
		)
		raf.read(packet)
		baos.write(packet)
		if (startPage.getPacketList().size > 1 || !startPage.isLastPacketIncomplete) {
			return baos.toByteArray()
		}

		//The VorbisComment can extend to the next page, so carry on reading pages until we get to the end of comment
		while (true) {
			logger.config("Reading comment page")
			val nextPageHeader: OggPageHeader = OggPageHeader.read(raf)
			packet = ByteArray(nextPageHeader.getPacketList().get(0).length)
			raf.read(packet)
			baos.write(packet)

			//Because there is at least one other packet (SetupHeaderPacket) this means the Comment Packet has finished
			//on this page so that's all we need and we can return
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

	override fun convertToVorbisCommentPacket(
		startPage: OggPageHeader,
		fc: FileChannel
	): ByteArray {
		val baos = ByteArrayOutputStream()

		// read the rest of the first page
		var packet = ByteBuffer.allocate(startPage.getPacketList().get(0).length - OpusHeader.TAGS_CAPTURE_PATTERN_LENGTH)
		fc.read(packet)
		baos.write(packet.array())
		if (startPage.getPacketList().size > 1 || !startPage.isLastPacketIncomplete) {
			return baos.toByteArray()
		}

		//The VorbisComment can extend to the next page, so carry on reading pages until we get to the end of comment
		while (true) {
			logger.config("Reading comment page")
			val nextPageHeader: OggPageHeader = OggPageHeader.read(fc)
			packet = ByteBuffer.allocate(nextPageHeader.getPacketList().get(0).length)
			fc.read(packet)
			baos.write(packet.array())

			//Because there is at least one other packet (SetupHeaderPacket) this means the Comment Packet has finished
			//on this page so that's all we need and we can return
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

	override fun isVorbisCommentHeader(headerData: ByteArray): Boolean {
		val opusTags = String(
			headerData,
			OpusHeader.TAGS_CAPTURE_PATTERN_POS,
			OpusHeader.TAGS_CAPTURE_PATTERN_LENGTH,
			Charset.forName("ISO_8859_1")
		)
		return opusTags == OpusHeader.TAGS_CAPTURE_PATTERN
	}

	companion object {
		var logger: Logger = Logger.getLogger("org.jaudiotagger.audio.ogg.opus")
	}
}
