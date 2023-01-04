/*
 * Created on 03.05.2015
 * Author: Veselin Markov.
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.dff

import com.flammky.android.core.sdk.VersionHelper
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.InvalidChunkException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.AudioFileReader2
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.GenericAudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.util.logging.Level

class DffFileReader : AudioFileReader2() {
	@Throws(CannotReadException::class, IOException::class, UnsupportedOperationException::class)
	override fun getEncodingInfo(file: Path): GenericAudioHeader {
		return if (VersionHelper.hasOreo()) {
			FileChannel.open(file).use { fc -> getEncodingInfo(fc) }
		} else {
			RandomAccessFile(file.toFile(), "r").use { getEncodingInfo(it.channel) }
		}
	}

	override fun getTag(fc: FileChannel): Tag? {
		return null
	}

	override fun getEncodingInfo(fc: FileChannel): GenericAudioHeader {
		val frm8: Frm8Chunk? = Frm8Chunk
			.readChunk(Utils.readFileDataIntoBufferLE(fc, Frm8Chunk.FRM8_HEADER_LENGTH))
		return if (frm8 != null) {
			DsdChunk.readChunk(Utils.readFileDataIntoBufferLE(fc, DsdChunk.DSD_HEADER_LENGTH))
				?: throw CannotReadException("$fc Not a valid dff file. Missing 'DSD '  after 'FRM8' ")

			PropChunk.readChunk(Utils.readFileDataIntoBufferLE(fc, PropChunk.PROP_HEADER_LENGTH))
				?: throw CannotReadException("$fc Not a valid dff file. Content does not have 'PROP'")

			SndChunk.readChunk(Utils.readFileDataIntoBufferLE(fc, SndChunk.SND_HEADER_LENGTH))
				?: throw CannotReadException("$fc Not a valid dff file. Missing 'SND '  after 'PROP' ")

			var chunk: BaseChunk?
			var fs: FsChunk? = null
			var chnl: ChnlChunk? = null
			var cmpr: CmprChunk?
			var diti: DitiChunk?
			val end: EndChunk?
			var dst: DstChunk? = null
			var frte: FrteChunk? = null
			var id3: Id3Chunk?
			while (true) {
				chunk =
					try {
						BaseChunk.readIdChunk(Utils.readFileDataIntoBufferLE(fc, BaseChunk.ID_LENGHT))
					} catch (ex: InvalidChunkException) {
						continue
					}
				if (chunk is FsChunk) {
					fs = chunk
					fs.readDataChunch(fc)
				} else if (chunk is ChnlChunk) {
					chnl = chunk
					chnl.readDataChunch(fc)
				} else if (chunk is CmprChunk) {
					cmpr = chunk
					cmpr.readDataChunch(fc)
				} else if (chunk is DitiChunk) {
					diti = chunk
					diti.readDataChunch(fc)
				} else if (chunk is EndChunk) {
					end = chunk
					end.readDataChunch(fc)
					break //no more data after the end.
				} else if (chunk is DstChunk) {
					dst = chunk
					dst.readDataChunch(fc)
					frte =
						try {
							BaseChunk
								.readIdChunk(Utils.readFileDataIntoBufferLE(fc, BaseChunk.ID_LENGHT)) as FrteChunk
						} catch (ex: InvalidChunkException) {
							throw CannotReadException(fc.toString() + "Not a valid dft file. Missing 'FRTE' chunk")
						}
					frte?.readDataChunch(fc)
				} else if (chunk is Id3Chunk) {
					id3 = chunk
					id3.readDataChunch(fc)
				}
			}
			if (chnl == null) {
				throw CannotReadException("$fc Not a valid dff file. Missing 'CHNL' chunk")
			}
			if (fs == null) {
				throw CannotReadException("$fc Not a valid dff file. Missing 'FS' chunk")
			}
			if (dst != null && frte == null) {
				throw CannotReadException("$fc Not a valid dst file. Missing 'FRTE' chunk")
			}
			if (end == null && dst == null) {
				throw CannotReadException("$fc Not a valid dff file. Missing 'DSD' end chunk")
			}
			val bitsPerSample = 1
			val channelNumber = chnl.numChannels.toInt()
			val samplingFreqency = fs.sampleRate
			val sampleCount: Long =
				if (dst != null) {
					(frte!!.numFrames / frte.rate!! * samplingFreqency).toLong()
				} else {
					(end!!.dataEnd!! - end.dataStart!!) * (8 / channelNumber)
				}
			buildAudioHeader(
				channelNumber,
				samplingFreqency,
				sampleCount,
				bitsPerSample,
				dst != null
			)
		} else {
			throw CannotReadException("$fc Not a valid dff file. Content does not start with 'FRM8'")
		} //end if frm8
	}

	private fun buildAudioHeader(
		channelNumber: Int,
		samplingFreqency: Int,
		sampleCount: Long,
		bitsPerSample: Int,
		isDST: Boolean
	): GenericAudioHeader {
		val audioHeader = GenericAudioHeader()
		audioHeader.encodingType = "DFF"
		audioHeader.setBitRate(bitsPerSample * samplingFreqency * channelNumber)
		audioHeader.bitsPerSample = bitsPerSample
		audioHeader.channelNumber = channelNumber
		audioHeader.setSamplingRate(samplingFreqency)
		audioHeader.noOfSamples = sampleCount
		audioHeader.preciseTrackLength = (sampleCount.toFloat() / samplingFreqency).toDouble()
		audioHeader.isVariableBitRate = isDST
		logger.log(Level.FINE, "Created audio header: $audioHeader")
		return audioHeader
	}

	@Throws(CannotReadException::class, IOException::class)
	override fun getTag(path: Path): Tag? {
		return null
	}
}
