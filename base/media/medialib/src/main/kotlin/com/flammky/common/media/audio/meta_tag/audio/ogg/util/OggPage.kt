package com.flammky.common.media.audio.meta_tag.audio.ogg.util


import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.ogg.util.OggCRCFactory.Companion.computeCRC
import org.jaudiotagger.audio.ogg.util.OggPageHeader
import org.jaudiotagger.audio.ogg.util.OggPageHeader.Companion.read
import java.nio.ByteBuffer


class OggPage(val header: OggPageHeader, val content: ByteBuffer) {

	init {
		fixCksum()
	}

	fun write(buf: ByteBuffer) {
		header.write(buf)
		buf.put(content.duplicate())
	}

	fun setSequenceNo(seqNo: Int) {
		header.pageSequence = seqNo
		fixCksum()
	}

	private fun fixCksum() {
		val temp = ByteBuffer.allocate(size())
		write(temp)

		//CRC should be zero before calculating it
		temp.putInt(OggPageHeader.FIELD_PAGE_CHECKSUM_POS, 0)
		val crc = computeCRC(temp.array())
		val cksum: Int = Utils.getIntLE(crc)
		header.checkSum = cksum
	}

	fun size(): Int {
		return (OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH // header
			+ header.segmentTable!!.size // variable segment table
			+ header.getPageLength()) // content
	}

	companion object {
		@Throws(CannotReadException::class)
		fun parse(buf: ByteBuffer): OggPage {
			val header = read(buf)
			val content = buf.slice()
			content.limit(header.getPageLength())
			Utils.skip(buf, header.getPageLength())
			return OggPage(header, content)
		}
	}
}

