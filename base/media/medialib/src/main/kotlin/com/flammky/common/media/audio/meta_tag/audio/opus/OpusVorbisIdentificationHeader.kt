package com.flammky.common.media.audio.meta_tag.audio.opus

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import java.nio.ByteBuffer
import java.util.logging.Logger


/**
 * - Magic signature: "OpusHead" (64 bits)
 * - Version number (8 bits unsigned): 0x01 for this spec
 * - Channel count 'c' (8 bits unsigned): MUST be > 0
 * - Pre-skip (16 bits unsigned, little endian)
 * - Input sample rate (32 bits unsigned, little endian): informational only
 * - Output gain (16 bits, little endian, signed Q7.8 in dB) to apply when
 * decoding
 * - Channel mapping family (8 bits unsigned)
 * --  0 = one stream: mono or L,R stereo
 * --  1 = channels in vorbis spec order: mono or L,R stereo or ... or FL,C,FR,RL,RR,LFE, ...
 * --  2..254 = reserved (treat as 255)
 * --  255 = no defined channel meaning
 * If channel mapping family > 0
 * - Stream count 'N' (8 bits unsigned): MUST be > 0
 * - Two-channel stream count 'M' (8 bits unsigned): MUST satisfy M <= N, M+N <= 255
 * - Channel mapping (8*c bits)
 * -- one stream index (8 bits unsigned) per channel (255 means silent throughout the file)
 */
class OpusVorbisIdentificationHeader(vorbisData: ByteArray) {
	var isValid = false
	var vorbisVersion: Byte = 0
	var audioChannels: Byte = 0
	var preSkip: Short = 0
	var audioSampleRate = 0
	var outputGain: Short = 0
	var channelMapFamily: Byte = 0
	var streamCount: Byte = 0
	var streamCountTwoChannel: Byte = 0
	var channelMap: ByteArray? = null
	var bitrateMinimal = 0
	var bitrateNominal = 0
	var bitrateMaximal = 0

	init {
		decodeHeader(vorbisData)
	}

	private fun decodeHeader(b: ByteArray) {
		val buf = ByteBuffer.wrap(b)
		val oggHead: String = Utils.readString(buf, 8)
		if (oggHead == OpusHeader.HEAD_CAPTURE_PATTERN) {
			vorbisVersion = buf.get()
			audioChannels = buf.get()
			preSkip = buf.short
			audioSampleRate = buf.int
			outputGain = buf.short
			channelMapFamily = buf.get()
			if (channelMapFamily > 0) {
				streamCount = buf.get()
				streamCountTwoChannel = buf.get()
				channelMap = ByteArray(audioChannels.toInt())
				for (i in 0 until audioChannels) {
					channelMap!![i] = buf.get()
				}
			}
			isValid = true
		}
	}

	override fun toString(): String {
		val sb = StringBuffer("OpusVorbisIdentificationHeader{")
		sb.append("isValid=").append(isValid)
		sb.append(", vorbisVersion=").append(vorbisVersion.toInt())
		sb.append(", audioChannels=").append(audioChannels.toInt())
		sb.append(", preSkip=").append(preSkip.toInt())
		sb.append(", audioSampleRate=").append(audioSampleRate)
		sb.append(", outputGain=").append(outputGain.toInt())
		sb.append(", channelMapFamily=").append(channelMapFamily.toInt())
		sb.append(", streamCount=").append(streamCount.toInt())
		sb.append(", streamCountTwoChannel=").append(streamCountTwoChannel.toInt())
		sb.append(", channelMap=")
		if (channelMap == null) sb.append("null") else {
			sb.append('[')
			for (i in channelMap!!.indices) sb.append(if (i == 0) "" else ", ")
				.append(channelMap!![i].toInt())
			sb.append(']')
		}
		sb.append(", bitrateMinimal=").append(bitrateMinimal)
		sb.append(", bitrateNominal=").append(bitrateNominal)
		sb.append(", bitrateMaximal=").append(bitrateMaximal)
		sb.append('}')
		return sb.toString()
	}

	companion object {
		var logger = Logger.getLogger("org.jaudiotagger.audio.ogg.opus")
	}
}
