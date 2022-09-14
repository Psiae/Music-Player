package com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.chunk

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.AiffAudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.AiffType
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.AiffUtil
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.chunk.AiffCompressionType.Companion.getByCode
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.Chunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkHeader
import java.io.IOException
import java.nio.ByteBuffer

/**
 * The Common Chunk describes fundamental parameters of the waveform data such as sample rate,
 * bit resolution, and how many channels of digital audio are stored in the FORM AIFF.
 */
class CommonChunk(
	hdr: ChunkHeader,
	chunkData: ByteBuffer,
	private val aiffHeader: AiffAudioHeader
) : Chunk(chunkData, hdr) {

	@Throws(IOException::class)
	override fun readChunk(): Boolean {
		val numChannels = Utils.u(chunkData.short)
		val numSamples = chunkData.int.toLong()
		val bitsPerSample = Utils.u(chunkData.short)
		val sampleRate = AiffUtil.read80BitDouble(chunkData)
		//Compression format, but not necessarily compressed
		val compressionType: String?
		var compressionName: String
		if (aiffHeader.fileType == AiffType.AIFC) {
			// This is a rather special case, but testing did turn up
			// a file that misbehaved in this way.
			if (chunkData.remaining() == 0) {
				return false
			}
			compressionType = Utils.readFourBytesAsChars(chunkData)
			if (compressionType == AiffCompressionType.SOWT.code) {
				aiffHeader.endian =
					AiffAudioHeader.Endian.LITTLE_ENDIAN
			}
			compressionName = Utils.readPascalString(chunkData)
			// Proper handling of compression type should depend
			// on whether raw output is set
			if (compressionType != null) {
				//Is it a known compression type
				val act = getByCode(compressionType)
				if (act != null) {
					compressionName = act.compression
					aiffHeader.isLossless = act.isLossless
					// we assume that the bitrate is not variable, if there is no compression
					if (act === AiffCompressionType.NONE) {
						aiffHeader.isVariableBitRate = false
					}
				} else {
					// We don't know compression type, so we have to assume lossy compression as we know we are using AIFC format
					aiffHeader.isLossless = false
				}
				if (compressionName.isEmpty()) {
					aiffHeader.encodingType = compressionType
				} else {
					aiffHeader.encodingType = compressionName
				}
			}
		} else {
			aiffHeader.isLossless = true
			aiffHeader.encodingType =
				AiffCompressionType.NONE.compression
			// regular AIFF has no variable bit rate AFAIK
			aiffHeader.isVariableBitRate = false
		}
		aiffHeader.bitsPerSample = bitsPerSample
		aiffHeader.setSamplingRate(sampleRate.toInt())
		aiffHeader.channelNumber = numChannels
		aiffHeader.preciseTrackLength = numSamples / sampleRate
		aiffHeader.noOfSamples = numSamples
		return true
	}
}
