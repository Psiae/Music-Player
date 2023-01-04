package com.flammky.common.media.audio.meta_tag.audio.opus


import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.AudioFileReader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.GenericAudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import org.jaudiotagger.audio.ogg.util.OggPageHeader
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.util.logging.Logger


/**
 * Read Ogg Opus File Tag and Encoding information
 */
class OpusFileReader : AudioFileReader() {
	private val ir: OpusInfoReader = OpusInfoReader()
	private val vtr: OpusVorbisTagReader = OpusVorbisTagReader()

	override fun getEncodingInfo(raf: RandomAccessFile): GenericAudioHeader {
		return ir.read(raf)
	}

	override fun getEncodingInfo(fc: FileChannel): GenericAudioHeader {
		return ir.read(fc)
	}

	override fun getTag(raf: RandomAccessFile): Tag {
		return vtr.read(raf)
	}

	override fun getTag(fc: FileChannel): Tag? {
		return vtr.read(fc)
	}


	/**
	 * Return count Ogg Page header, count starts from zero
	 *
	 *
	 * count=0; should return PageHeader that contains Vorbis Identification Header
	 * count=1; should return Pageheader that contains VorbisComment and possibly SetupHeader
	 * count>=2; should return PageHeader containing remaining VorbisComment,SetupHeader and/or Audio
	 *
	 * @param raf
	 * @param count
	 * @return
	 * @throws CannotReadException
	 * @throws IOException
	 */
	@Throws(CannotReadException::class, IOException::class)
	fun readOggPageHeader(raf: RandomAccessFile, count: Int): OggPageHeader {
		var count = count
		var pageHeader = OggPageHeader.read(raf)
		while (count > 0) {
			raf.seek(raf.filePointer + pageHeader.getPageLength())
			pageHeader = OggPageHeader.read(raf)
			count--
		}
		return pageHeader
	}

	/**
	 * Summarize all the ogg headers in a file
	 *
	 *
	 * A useful utility function
	 *
	 * @param oggFile
	 * @throws CannotReadException
	 * @throws IOException
	 */
	@Throws(CannotReadException::class, IOException::class)
	fun summarizeOggPageHeaders(oggFile: File?) {
		val raf = RandomAccessFile(oggFile, "r")
		while (raf.filePointer < raf.length()) {
			println("pageHeader starts at absolute file position:" + raf.filePointer)
			val pageHeader = OggPageHeader.read(raf)
			println("pageHeader finishes at absolute file position:" + raf.filePointer)
			println(
				"""
								$pageHeader

								""".trimIndent()
			)
			raf.seek(raf.filePointer + pageHeader.getPageLength())
		}
		println("Raf File Pointer at:" + raf.filePointer + "File Size is:" + raf.length())
		raf.close()
	}

	/**
	 * Summarizes the first five pages, normally all we are interested in
	 *
	 * @param oggFile
	 * @throws CannotReadException
	 * @throws IOException
	 */
	@Throws(CannotReadException::class, IOException::class)
	fun shortSummarizeOggPageHeaders(oggFile: File?) {
		val raf = RandomAccessFile(oggFile, "r")
		var i = 0
		while (raf.filePointer < raf.length()) {
			println("pageHeader starts at absolute file position:" + raf.filePointer)
			val pageHeader = OggPageHeader.read(raf)
			println("pageHeader finishes at absolute file position:" + raf.filePointer)
			println(
				"""
								$pageHeader

								""".trimIndent()
			)
			raf.seek(raf.filePointer + pageHeader.getPageLength())
			i++
			if (i >= 5) {
				break
			}
		}
		println("Raf File Pointer at:" + raf.filePointer + "File Size is:" + raf.length())
		raf.close()
	}

	companion object {
		var logger = Logger.getLogger(OpusFileReader::class.java.getPackage().name)
	}
}
