package com.flammky.common.media.audio.meta_tag.audio.opus


import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.GenericAudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v2TagBase
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
class OpusInfoReader {
	@Throws(CannotReadException::class, IOException::class)
	fun read(raf: RandomAccessFile): GenericAudioHeader {
		var start = raf.filePointer
		val info = GenericAudioHeader()
		logger.fine("Started")

		//Check start of file does it have Ogg pattern
		val b = ByteArray(OggPageHeader.CAPTURE_PATTERN.size)
		raf.read(b)
		if (!Arrays.equals(b, OggPageHeader.CAPTURE_PATTERN)) {
			raf.seek(0)
			if (ID3v2TagBase.isId3Tag(raf)) {
				raf.read(b)
				if (Arrays.equals(b, OggPageHeader.CAPTURE_PATTERN)) {
					start = raf.filePointer
				}
			} else {
				throw CannotReadException(ErrorMessage.OGG_HEADER_CANNOT_BE_FOUND.getMsg(String(b)))
			}
		}
		raf.seek(start)

		//1st page = Identification Header
		val pageHeader = OggPageHeader.read(raf)
		val vorbisData = ByteArray(pageHeader.getPageLength())
		raf.read(vorbisData)
		val opusIdHeader = OpusVorbisIdentificationHeader(vorbisData)

		//Map to generic encodingInfo
		info.channelNumber = opusIdHeader.audioChannels.toInt()
		info.setSamplingRate(opusIdHeader.audioSampleRate)
		info.encodingType = "Opus Vorbis 1.0"

		// find last Opus Header
		val last = lastValidHeader(raf)
			?: throw CannotReadException("Opus file contains ID and Comment headers but no audio content")
		info.noOfSamples = (last.getAbsoluteGranulePosition() - opusIdHeader.preSkip).toLong()
		info.preciseTrackLength = (info.noOfSamples!! / 48000.0)
		return info
	}

	fun read(fc: FileChannel): GenericAudioHeader {
		var start = fc.position()
		val info = GenericAudioHeader()
		logger.fine("Started")

		//Check start of file does it have Ogg pattern
		val b = ByteBuffer.allocate(OggPageHeader.CAPTURE_PATTERN.size)
		fc.read(b)
		if (!Arrays.equals(b.array(), OggPageHeader.CAPTURE_PATTERN)) {
			fc.position(0)
			if (ID3v2TagBase.isId3Tag(fc)) {
				fc.read(b)
				if (Arrays.equals(b.array(), OggPageHeader.CAPTURE_PATTERN)) {
					start = fc.position()
				}
			} else {
				throw CannotReadException(ErrorMessage.OGG_HEADER_CANNOT_BE_FOUND.getMsg(String(b.array())))
			}
		}

		fc.position(start)
		//1st page = Identification Header
		val pageHeader = OggPageHeader.read(fc)
		val vorbisData = ByteBuffer.allocate(pageHeader.getPageLength())
		fc.read(vorbisData)
		val opusIdHeader = OpusVorbisIdentificationHeader(vorbisData.array())

		//Map to generic encodingInfo
		info.channelNumber = opusIdHeader.audioChannels.toInt()
		info.setSamplingRate(opusIdHeader.audioSampleRate)
		info.encodingType = "Opus Vorbis 1.0"

		// find last Opus Header
		val last = lastValidHeader(fc)
			?: throw CannotReadException("Opus file contains ID and Comment headers but no audio content")
		info.noOfSamples = (last.getAbsoluteGranulePosition() - opusIdHeader.preSkip).toLong()
		info.preciseTrackLength = (info.noOfSamples!! / 48000.0)
		return info
	}

	@Throws(IOException::class)
	private fun lastValidHeader(raf: RandomAccessFile): OggPageHeader? {
		var best: OggPageHeader? = null
		while (true) {
			try {
				val candidate = OggPageHeader.read(raf)
				raf.seek(raf.filePointer + candidate.getPageLength())
				if (candidate.isValid && !candidate.isLastPacketIncomplete) {
					best = candidate
				}
			} catch (ignored: CannotReadException) {
				break
			}
		}
		return best
	}

	@Throws(IOException::class)
	private fun lastValidHeader(fc: FileChannel): OggPageHeader? {
		var best: OggPageHeader? = null
		while (true) {
			try {
				val candidate = OggPageHeader.read(fc)
				fc.position(fc.position() + candidate.getPageLength())
				if (candidate.isValid && !candidate.isLastPacketIncomplete) {
					best = candidate
				}
			} catch (ignored: CannotReadException) {
				break
			}
		}
		return best
	}

	companion object {
		var logger = Logger.getLogger("org.jaudiotagger.audio.opus.atom")
	}
}

