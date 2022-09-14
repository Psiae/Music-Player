/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Rapha�l Slinckx <raphael@slinckx.net>
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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.AudioHeader
import kotlin.math.roundToInt

/**
 * This class represents a structure for storing and retrieving information
 * about the codec respectively the encoding parameters.<br></br>
 * Most of the parameters are available for nearly each audio format. Some
 * others would result in standard values.<br></br>
 * **Consider:** None of the setter methods will actually affect the audio
 * file. This is just a structure for retrieving information, not manipulating
 * the audio file.<br></br>
 *
 * @author Raphael Slinckx
 */
open class GenericAudioHeader : AudioHeader {
	private var mAudioDataLength: Long? = null
	private var mAudioDataStartPosition: Long? = null
	private var mAudioDataEndPosition: Long? = null
	private var mBitRate: Int? = null
	private var mNoOfChannels: Int? = null
	private var mSamplingRate: Int? = null
	private var mBitsPerSample: Int? = null
	private var mEncodingType: String? = null
	private var mFormat: String? = null
	private var mIsVbr: Boolean? = null
	private var mIsLossless: Boolean? = null
	private var mTrackLength: Double? = null
	private var mNoOfSamples: Long? = null
	private var mByteRate: Int? = null

	var channelNumber: Int?
		get() = mNoOfChannels
		set(channelMode) {
			mNoOfChannels = channelMode
		}

	override var audioDataLength: Long?
		get() = mAudioDataLength
		set(value) {
			mAudioDataLength = value
		}

	override var audioDataStartPosition: Long?
		get() = mAudioDataStartPosition
		set(value) {
			mAudioDataStartPosition = value
		}

	override var audioDataEndPosition: Long?
		get() = mAudioDataEndPosition
		set(value) {
			mAudioDataEndPosition = value
		}

	override var byteRate: Int?
		get() = mByteRate
		set(value) {
			mByteRate = value
		}

	override var encodingType: String?
		get() = mEncodingType
		set(value) {
			mEncodingType = value
		}

	override var format: String?
		get() = mFormat
		set(value) {
			mFormat = value
		}

	override var isLossless: Boolean
		get() = mIsLossless ?: false
		set(value) {
			mIsLossless = value
		}

	override var isVariableBitRate: Boolean
		get() = mIsVbr ?: false
		set(value) {
			mIsVbr = value
		}

	override var noOfSamples: Long?
		get() = mNoOfSamples
		set(value) {
			mNoOfSamples = value
		}

	override var preciseTrackLength: Double?
		get() = mTrackLength
		set(value) {
			mTrackLength = value
		}

	override val bitRateAsNumber: Long?
		get() = mBitRate?.toLong()

	override val sampleRate: String?
		get() = mSamplingRate?.toString()

	override val sampleRateAsNumber: Int?
		get() = mSamplingRate

	override val channels: String?
		get() = mNoOfChannels?.toString()

	override val bitRate: String?
		get() = mBitRate?.toString()

	override val trackLength: Int?
		get() = preciseTrackLength?.roundToInt()

	override var bitsPerSample: Int
		get() = mBitsPerSample ?: -1
		set(value) {
			mBitsPerSample = value
		}

	/**
	 * This Method sets the bitRate in &quot;Kbps&quot;.<br></br>
	 *
	 * @param bitRate bitRate in kbps.
	 */
	fun setBitRate(bitRate: Int) {
		mBitRate = bitRate
	}

	/**
	 * Sets the Sampling rate in &quot;Hz&quot;<br></br>
	 *
	 * @param samplingRate Sample rate.
	 */
	fun setSamplingRate(samplingRate: Int) {
		mSamplingRate = samplingRate
	}

	/*
	* Sets the ByteRate (per second)
	*
	* @params ByteRate
	*/
	fun setByteRate(byteRate: Int) {
		mByteRate = byteRate
	}

	/**
	 * Pretty prints this encoding info
	 *
	 * @see Object.toString
	 */
	override fun toString(): String {
		val out = StringBuilder()
		out.append("Audio Header content:\n")
		if (audioDataLength != null) {
			out.append("\taudioDataLength:$audioDataLength\n")
		}
		if (audioDataStartPosition != null) {
			out.append("\taudioDataStartPosition:$audioDataStartPosition\n")
		}
		if (audioDataEndPosition != null) {
			out.append("\taudioDataEndPosition:$audioDataEndPosition\n")
		}
		if (byteRate != null) {
			out.append("\tbyteRate:$byteRate\n")
		}
		if (bitRate != null) {
			out.append("\tbitRate:$bitRate\n")
		}
		if (sampleRateAsNumber != null) {
			out.append("\tsamplingRate:$sampleRateAsNumber\n")
		}
		if (bitsPerSample != null) {
			out.append("\tbitsPerSample:$bitsPerSample\n")
		}
		if (noOfSamples != null) {
			out.append("\ttotalNoSamples:$noOfSamples\n")
		}
		if (channelNumber != null) {
			out.append("\tnumberOfChannels:$channelNumber\n")
		}
		if (format != null) {
			out.append("\tformat:$format\n")
		}
		if (encodingType != null) {
			out.append("\tencodingType:$encodingType\n")
		}
		if (mIsVbr != null) {
			out.append("\tisVbr:$mIsVbr\n")
		}
		if (mIsLossless != null) {
			out.append("\tisLossless:$mIsLossless\n")
		}
		if (trackLength != null) {
			out.append("\ttrackDuration:$trackLength\n")
		}
		return out.toString()
	}
}
