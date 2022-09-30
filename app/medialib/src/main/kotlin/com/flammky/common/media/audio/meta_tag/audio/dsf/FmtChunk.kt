package com.flammky.musicplayer.common.media.audio.meta_tag.audio.dsf

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.SupportedFileFormat
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.GenericAudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.IffHeaderChunk
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Created by Paul on 25/01/2016.
 */
class FmtChunk private constructor(dataBuffer: ByteBuffer) {
	private val chunkSizeLength: Long

	init {
		chunkSizeLength = dataBuffer.long
	}

	@Throws(IOException::class)
	fun readChunkData(dsd: DsdChunk, fc: FileChannel): GenericAudioHeader {
		val sizeExcludingChunkHeader =
			chunkSizeLength - (IffHeaderChunk.SIGNATURE_LENGTH + DsdChunk.CHUNKSIZE_LENGTH)
		val audioData = Utils.readFileDataIntoBufferLE(fc, sizeExcludingChunkHeader.toInt())
		return readAudioInfo(dsd, audioData)
	}

	/**
	 * @param audioInfoChunk contains the bytes from "format version" up to "reserved"
	 * fields
	 * @return an empty [GenericAudioHeader] if audioInfoChunk has less
	 * than 40 bytes, the read data otherwise. Never `null`.
	 */
	private fun readAudioInfo(dsd: DsdChunk, audioInfoChunk: ByteBuffer): GenericAudioHeader {
		val audioHeader = GenericAudioHeader()
		if (audioInfoChunk.limit() < FMT_CHUNK_MIN_DATA_SIZE_) {
			logger.log(
				Level.WARNING,
				"Not enough bytes supplied for Generic audio header. Returning an empty one."
			)
			return audioHeader
		}
		audioInfoChunk.order(ByteOrder.LITTLE_ENDIAN)
		val version = audioInfoChunk.int
		val formatId = audioInfoChunk.int
		val channelType = audioInfoChunk.int
		val channelNumber = audioInfoChunk.int
		val samplingFreqency = audioInfoChunk.int
		val bitsPerSample = audioInfoChunk.int
		val sampleCount = audioInfoChunk.long
		val blocksPerSample = audioInfoChunk.int
		audioHeader.encodingType = "DSF"
		audioHeader.format =
			SupportedFileFormat.DSF.displayName
		audioHeader.setBitRate(bitsPerSample * samplingFreqency * channelNumber)
		audioHeader.bitsPerSample = bitsPerSample
		audioHeader.channelNumber = channelNumber
		audioHeader.setSamplingRate(samplingFreqency)
		audioHeader.noOfSamples = sampleCount
		audioHeader.preciseTrackLength = (sampleCount.toFloat() / samplingFreqency).toDouble()
		audioHeader.isVariableBitRate = false
		return audioHeader
	}

	companion object {
		var logger = Logger.getLogger("org.jaudiotagger.audio.dsf.FmtChunk")
		const val FMT_CHUNK_MIN_DATA_SIZE_ = 40
		fun readChunkHeader(dataBuffer: ByteBuffer): FmtChunk? {
			val type = Utils.readFourBytesAsChars(dataBuffer)
			return if (DsfChunkType.FORMAT.code == type) {
				FmtChunk(dataBuffer)
			} else null
		}
	}
}
