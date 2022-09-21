/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Raphaël Slinckx <raphael@slinckx.net>
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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.flac.metadatablock

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.logging.Logger

/**
 * Stream Info
 *
 * This block has information about the whole stream, like sample rate, number of channels, total number of samples,
 * etc. It must be present as the first metadata block in the stream. Other metadata blocks may follow, and ones
 * that the decoder doesn't understand, it will skip.
 * Format:
 * Size in bits Info
 * 16 The minimum block size (in samples) used in the stream.
 * 16 The maximum block size (in samples) used in the stream. (Minimum blocksize == maximum blocksize) implies a fixed-blocksize stream.
 * 24 The minimum frame size (in bytes) used in the stream. May be 0 to imply the value is not known.
 * 24 The maximum frame size (in bytes) used in the stream. May be 0 to imply the value is not known.
 * 20 Sample rate in Hz. Though 20 bits are available, the maximum sample rate is limited by the structure of frame headers to 655350Hz. Also,
 * a value of 0 is invalid.
 * 3 	(number of channels)-1. FLAC supports from 1 to 8 channels
 * 5 	(bits per sample)-1. FLAC supports from 4 to 32 bits per sample. Currently the reference encoder and decoders only support up to 24 bits per sample.
 * 36 	Total samples in stream. 'Samples' means inter-channel sample,
 * i.e. one second of 44.1Khz audio will have 44100 samples regardless of the number of channels.
 * A value of zero here means the number of total samples is unknown.
 * 128 	MD5 signature of the unencoded audio data. This allows the decoder to determine if an error exists in the audio data
 * even when the error does not result in an invalid bitstream.
 * NOTES
 * * FLAC specifies a minimum block size of 16 and a maximum block size of 65535, meaning the bit patterns corresponding to the numbers 0-15 in the minimum blocksize and maximum blocksize fields are invalid.
 */
class MetadataBlockDataStreamInfo(header: MetadataBlockHeader, fc: FileChannel) :
	MetadataBlockData {
	private val minBlockSize: Int
	private val maxBlockSize: Int
	private val minFrameSize: Int
	private val maxFrameSize: Int
	private val samplingRate: Int
	private val samplingRatePerChannel: Int
	private val bitsPerSample: Int
	private val noOfChannels: Int
	private val noOfSamples: Int
	private val trackLength: Float
	private val md5: String
	private var isValid = true
	private val rawdata: ByteBuffer

	init {
		if (header.dataLength < STREAM_INFO_DATA_LENGTH) {
			isValid = false
			throw IOException("MetadataBlockDataStreamInfo HeaderDataSize is invalid:" + header.dataLength)
		}
		rawdata = ByteBuffer.allocate(header.dataLength)
		rawdata.order(ByteOrder.BIG_ENDIAN)
		val bytesRead = fc.read(rawdata)
		if (bytesRead < header.dataLength) {
			isValid = false
			throw IOException("Unable to read required number of bytes, read:" + bytesRead + ":required:" + header.dataLength)
		}
		rawdata.flip()
		minBlockSize = Utils.u(rawdata.short)
		maxBlockSize = Utils.u(rawdata.short)
		minFrameSize = readThreeByteInteger(rawdata.get(), rawdata.get(), rawdata.get())
		maxFrameSize = readThreeByteInteger(rawdata.get(), rawdata.get(), rawdata.get())
		samplingRate = readSamplingRate()
		noOfChannels = readNoOfChannels()
		bitsPerSample = readBitsPerSample()
		noOfSamples = readTotalNumberOfSamples()
		md5 = readMd5()
		trackLength = (noOfSamples.toDouble() / samplingRate).toFloat()
		samplingRatePerChannel = samplingRate / noOfChannels
		rawdata.rewind()
	}

	private fun readMd5(): String {
		val hexChars = CharArray(32) // MD5 is always 32 characters
		if (rawdata.limit() >= 34) {
			for (i in 0..15) {
				val v = rawdata[i + 18].toInt() and 0xFF // Offset 18
				hexChars[i * 2] = hexArray[v ushr 4]
				hexChars[i * 2 + 1] = hexArray[v and 0x0F]
			}
		}
		return String(hexChars)
	}

	override val bytes: ByteBuffer
		get() = rawdata

	override val length: Int
		get() = rawdata.limit()

	override fun toString(): String {
		return "MinBlockSize:" + minBlockSize + "MaxBlockSize:" + maxBlockSize + "MinFrameSize:" + minFrameSize + "MaxFrameSize:" + maxFrameSize + "SampleRateTotal:" + samplingRate + "SampleRatePerChannel:" + samplingRatePerChannel + ":Channel number:" + noOfChannels + ":Bits per sample: " + bitsPerSample + ":TotalNumberOfSamples: " + noOfSamples + ":Length: " + trackLength
	}

	fun getPreciseLength(): Float {
		return trackLength
	}

	fun getNoOfChannels(): Int {
		return noOfChannels
	}

	fun getSamplingRate(): Int {
		return samplingRate
	}

	fun getSamplingRatePerChannel(): Int {
		return samplingRatePerChannel
	}

	fun getEncodingType(): String {
		return "FLAC $bitsPerSample bits"
	}

	fun getBitsPerSample(): Int {
		return bitsPerSample
	}

	fun getNoOfSamples(): Long {
		return noOfSamples.toLong()
	}

	fun getMD5Signature(): String {
		return md5
	}

	fun isValid(): Boolean {
		return isValid
	}

	/**
	 * SOme values are stored as 3 byte integrals (instead of the more usual 2 or 4)
	 *
	 * @param b1
	 * @param b2
	 * @param b3
	 * @return
	 */
	private fun readThreeByteInteger(
		b1: Byte,
		b2: Byte,
		b3: Byte
	): Int {
		return (Utils.u(b1) shl 16) + (Utils.u(b2) shl 8) + Utils.u(b3)
	}

	/**
	 * Sampling rate is stored over 20 bits bytes 10 and 11 and half of bytes 12 so have to mask third one
	 *
	 * @return
	 */
	private fun readSamplingRate(): Int {
		return (Utils.u(rawdata[10]) shl 12) + (Utils.u(rawdata[11]) shl 4) + (Utils.u(
			rawdata[12]
		) and 0xF0 ushr 4)
	}

	/**
	 * Stored in 5th to 7th bits of byte 12
	 */
	private fun readNoOfChannels(): Int {
		return (Utils.u(rawdata[12]) and 0x0E ushr 1) + 1
	}

	/** Stored in last bit of byte 12 and first 4 bits of byte 13  */
	private fun readBitsPerSample(): Int {
		return (Utils.u(rawdata[12]) and 0x01 shl 4) + (Utils.u(
			rawdata[13]
		) and 0xF0 ushr 4) + 1
	}

	/** Stored in second half of byte 13 plus bytes 14 - 17
	 *
	 * @return
	 */
	private fun readTotalNumberOfSamples(): Int {
		var nb = Utils.u(
			rawdata[17]
		)
		nb += Utils.u(rawdata[16]) shl 8
		nb += Utils.u(rawdata[15]) shl 16
		nb += Utils.u(rawdata[14]) shl 24
		nb += Utils.u(rawdata[13]) and 0x0F shl 32
		return nb
	}

	companion object {
		const val STREAM_INFO_DATA_LENGTH = 34

		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.audio.flac.MetadataBlockDataStreamInfo")
		private val hexArray = "0123456789abcdef".toCharArray()
	}
}
