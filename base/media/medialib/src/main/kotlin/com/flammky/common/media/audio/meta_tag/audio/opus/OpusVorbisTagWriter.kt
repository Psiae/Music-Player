package com.flammky.common.media.audio.meta_tag.audio.opus


import com.flammky.common.media.audio.meta_tag.audio.ogg.util.OggPage
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotWriteException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.vorbiscomment.VorbisCommentTag.Companion.createNewTag
import org.jaudiotagger.audio.ogg.OggVorbisCommentTagCreator
import org.jaudiotagger.audio.ogg.util.OggPageHeader
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.logging.Logger


/**
 * Write Vorbis Tag within an ogg
 *
 *
 * VorbisComment holds the tag information within an ogg file
 */
class OpusVorbisTagWriter {
//	private val tc =
//		OggVorbisCommentTagCreator(ByteArray(0), OpusHeader.TAGS_CAPTURE_PATTERN_AS_BYTES, false)

	private val tc = OggVorbisCommentTagCreator()

	private val reader: OpusVorbisTagReader = OpusVorbisTagReader()
	@Throws(IOException::class, CannotReadException::class, CannotWriteException::class)
	fun delete(raf: RandomAccessFile, tempRaf: RandomAccessFile) {
		try {
			reader.read(raf)
		} catch (e: CannotReadException) {
			write(createNewTag(), raf, tempRaf)
			return
		}
		val emptyTag = createNewTag()

		//Go back to start of file
		raf.seek(0)
		write(emptyTag, raf, tempRaf)
	}

	@Throws(CannotReadException::class, CannotWriteException::class, IOException::class)
	fun write(tag: Tag?, raf: RandomAccessFile, rafTemp: RandomAccessFile) {
		logger.config("Starting to write file: $raf")
		val fi = raf.channel
		val fo = rafTemp.channel

		//1st Page:Identification Header
		logger.fine("Read 1st Page: identificationHeader")
		val originalHeaders: MutableList<OggPage> = readPages(fi).toMutableList()

		//Convert the OggVorbisComment header to raw packet data
		val newComment = tc.convert(tag)

		// write identification header
		val identPage: OggPage = originalHeaders.removeAt(0)
		writePage(fo, identPage)

		// second page is OpusTags, skip all OpusTags
		originalHeaders.removeAt(0) // skip first tag page
		while (originalHeaders[0].header.isContinuedPage) {
			originalHeaders.removeAt(0) // skip continued tag pages
		}
		val fullPagesNeeded = newComment.capacity() / OggPageHeader.MAXIMUM_PAGE_DATA_SIZE
		val pagesRemainder = newComment.capacity() % OggPageHeader.MAXIMUM_PAGE_DATA_SIZE
		val streamNo: Int = identPage.header.serialNumber
		var sequenceNo = 1
		for (page in 0 until fullPagesNeeded) {
			val header: OggPageHeader = OggPageHeader.createCommentHeader(
				OggPageHeader.MAXIMUM_PAGE_DATA_SIZE,
				page != 0,
				streamNo,
				sequenceNo++
			)
			val content = newComment.slice()
			content.limit(OggPageHeader.MAXIMUM_PAGE_DATA_SIZE)
			writePage(fo, OggPage(header, content))
			Utils.skip(newComment, OggPageHeader.MAXIMUM_PAGE_DATA_SIZE)
		}
		if (pagesRemainder > 0) {
			val header: OggPageHeader = OggPageHeader.createCommentHeader(
				pagesRemainder,
				fullPagesNeeded > 0,
				streamNo,
				sequenceNo++
			)
			val content = newComment.slice()
			writePage(fo, OggPage(header, content))
		}
		for (page in originalHeaders) {
			page.setSequenceNo(sequenceNo++)
			writePage(fo, page)
		}
	}

	@Throws(IOException::class)
	private fun writePage(fo: FileChannel, oggPage: OggPage) {
		val buf = ByteBuffer.allocate(oggPage.size())
		oggPage.write(buf)
		buf.rewind()
		fo.write(buf)
	}

	@Throws(IOException::class, CannotReadException::class)
	private fun readPages(fi: FileChannel): List<OggPage> {
		val output: MutableList<OggPage> = ArrayList<OggPage>()
		val buf: ByteBuffer = Utils.fetchFromChannel(fi, fi.size().toInt())
		while (buf.remaining() > 0) {
			output.add(OggPage.parse(buf))
		}
		return output
	}

	companion object {
		var logger = Logger.getLogger("org.jaudiotagger.audio.opus")
	}
}
