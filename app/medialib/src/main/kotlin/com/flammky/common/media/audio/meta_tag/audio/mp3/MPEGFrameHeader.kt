/**
 * @author : Paul Taylor
 *
 * Version @version:$Id$
 * Date :${DATE}
 *
 * Jaikoz Copyright Copyright (C) 2003 -2005 JThink Ltd
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp3

import com.flammky.musicplayer.common.media.audio.meta_tag.FileConstants
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.InvalidAudioFrameException
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.AbstractTagDisplayFormatter
import java.nio.ByteBuffer

/**
 * Represents a MPEGFrameHeader, an MP3 is made up of a number of frames each frame starts with a four
 * byte frame header.
 */
class MPEGFrameHeader {
	private var mpegBytes: ByteArray = byteArrayOf()
	/**
	 * Gets the mPEGVersion attribute of the MPEGFrame object
	 *
	 * @return The mPEGVersion value
	 */
	/**
	 * The version of this MPEG frame (see the constants)
	 */
	var version = 0
		private set
	var versionAsString: String? = null
		private set
	/**
	 * Gets the layerVersion attribute of the MPEGFrame object
	 *
	 * @return The layerVersion value
	 */
	/**
	 * Contains the mpeg layer of this frame (see constants)
	 */
	var layer = 0
		private set
	var layerAsString: String? = null
		private set

	/**
	 * Bitrate of this frame
	 */
	var bitRate: Int? = null
		private set

	/**
	 * Channel Mode of this Frame (see constants)
	 */
	var channelMode = 0
		private set

	/**
	 * Channel Mode of this Frame As English String
	 */
	var channelModeAsString: String? = null
		private set

	/**
	 * Emphasis of this frame
	 */
	var emphasis = 0
		private set

	/**
	 * Emphasis mode string
	 */
	var emphasisAsString: String? = null
		private set

	/**
	 * Mode Extension
	 */
	var modeExtension: String? = null
		private set

	/**
	 * Flag indicating if this frame has padding byte
	 */
	var isPadding = false
		private set

	/**
	 * Flag indicating if this frame contains copyrighted material
	 */
	var isCopyrighted = false
		private set

	/**
	 * Flag indicating if this frame contains original material
	 */
	var isOriginal = false
		private set

	/**
	 * Flag indicating if this frame is protected
	 */
	var isProtected = false
		private set

	/**
	 * Flag indicating if this frame is private
	 */
	var isPrivate = false
		private set
	var samplingRate: Int? = null
		private set

	/**
	 * Gets the copyrighted attribute of the MPEGFrame object
	 */
	private fun setCopyrighted() {
		isCopyrighted = mpegBytes[BYTE_4].toInt() and MASK_MP3_COPY != 0
	}

	/**
	 * Set the version of this frame as an int value (see constants)
	 * @throws InvalidAudioFrameException
	 */
	@Throws(InvalidAudioFrameException::class)
	private fun setVersion() {
		//MPEG Version
		version = (mpegBytes[BYTE_2].toInt() and MASK_MP3_VERSION shr 3).toByte().toInt()
		versionAsString = mpegVersionMap[version]
		if (versionAsString == null) {
			throw InvalidAudioFrameException("Invalid mpeg version")
		}
	}

	/**
	 * Sets the original attribute of the MPEGFrame object
	 */
	private fun setOriginal() {
		isOriginal = mpegBytes[BYTE_4].toInt() and MASK_MP3_HOME != 0
	}

	/**
	 * Sets the protected attribute of the MPEGFrame object
	 */
	private fun setProtected() {
		isProtected = mpegBytes[BYTE_2].toInt() and MASK_MP3_PROTECTION == 0x00
	}

	/**
	 * Sets the private attribute of the MPEGFrame object
	 */
	private fun setPrivate() {
		isPrivate = mpegBytes[BYTE_3].toInt() and MASK_MP3_PRIVACY != 0
	}

	/**
	 * Get the setBitrate of this frame
	 * @throws InvalidAudioFrameException
	 */
	@Throws(InvalidAudioFrameException::class)
	private fun setBitrate() {
		/* BitRate, get by checking header setBitrate bits and MPEG Version and Layer */
		val bitRateIndex =
			mpegBytes[BYTE_3].toInt() and MASK_MP3_BITRATE or (mpegBytes[BYTE_2].toInt() and MASK_MP3_ID) or (mpegBytes[BYTE_2].toInt() and MASK_MP3_LAYER)
		bitRate = bitrateMap[bitRateIndex]
		if (bitRate == null) {
			throw InvalidAudioFrameException("Invalid bitrate")
		}
	}

	/**
	 * Set the Mpeg channel mode of this frame as a constant (see constants)
	 * @throws InvalidAudioFrameException
	 */
	@Throws(InvalidAudioFrameException::class)
	private fun setChannelMode() {
		channelMode = mpegBytes[BYTE_4].toInt() and MASK_MP3_MODE ushr 6
		channelModeAsString = modeMap[channelMode]
		if (channelModeAsString == null) {
			throw InvalidAudioFrameException("Invalid channel mode")
		}
	}

	/**
	 * Get the setEmphasis mode of this frame in a string representation
	 * @throws InvalidAudioFrameException
	 */
	@Throws(InvalidAudioFrameException::class)
	private fun setEmphasis() {
		emphasis = mpegBytes[BYTE_4].toInt() and MASK_MP3_EMPHASIS
		emphasisAsString = emphasisMap[emphasis]
		if (emphasisAsString == null) {
			throw InvalidAudioFrameException("Invalid emphasis")
		}
	}

	/**
	 * Set whether this frame uses padding bytes
	 */
	private fun setPadding() {
		isPadding = mpegBytes[BYTE_3].toInt() and MASK_MP3_PADDING != 0
	}

	/**
	 * Get the layer version of this frame as a constant int value (see constants)
	 * @throws InvalidAudioFrameException
	 */
	@Throws(InvalidAudioFrameException::class)
	private fun setLayer() {
		layer = mpegBytes[BYTE_2].toInt() and MASK_MP3_LAYER ushr 1
		layerAsString = mpegLayerMap[layer]
		if (layerAsString == null) {
			throw InvalidAudioFrameException("Invalid Layer")
		}
	}

	/**
	 * Sets the string representation of the mode extension of this frame
	 * @throws InvalidAudioFrameException
	 */
	@Throws(InvalidAudioFrameException::class)
	private fun setModeExtension() {
		val index = mpegBytes[BYTE_4].toInt() and MASK_MP3_MODE_EXTENSION shr 4
		if (layer == LAYER_III) {
			modeExtension = modeExtensionLayerIIIMap[index]
			if (modeExtension == null) {
				throw InvalidAudioFrameException("Invalid Mode Extension")
			}
		} else {
			modeExtension = modeExtensionMap[index]
			if (modeExtension == null) {
				throw InvalidAudioFrameException("Invalid Mode Extension")
			}
		}
	}

	/**
	 * set the sampling rate in Hz of this frame
	 * @throws InvalidAudioFrameException
	 */
	@Throws(InvalidAudioFrameException::class)
	private fun setSamplingRate() {
		//Frequency
		val index = mpegBytes[BYTE_3].toInt() and MASK_MP3_FREQUENCY ushr 2
		val samplingRateMapForVersion = samplingRateMap[version]
			?: throw InvalidAudioFrameException(
				"Invalid version"
			)
		samplingRate = samplingRateMapForVersion[index]
		if (samplingRate == null) {
			throw InvalidAudioFrameException("Invalid sampling rate")
		}
	}

	/**
	 * Gets the number of channels
	 *
	 * @return The setChannelMode value
	 */
	val numberOfChannels: Int
		get() = when (channelMode) {
			MODE_DUAL_CHANNEL -> 2
			MODE_JOINT_STEREO -> 2
			MODE_MONO -> 1
			MODE_STEREO -> 2
			else -> 0
		}

	/**
	 * Gets the paddingLength attribute of the MPEGFrame object
	 *
	 * @return The paddingLength value
	 */
	val paddingLength: Int
		get() = if (isPadding) {
			1
		} else {
			0
		}

	/*
	 * Gets this frame length in bytes, value should always be rounded down to the nearest byte (not rounded up)
	 *
	 * Calculation is Bitrate (scaled to bps) divided by sampling frequency (in Hz), The larger the bitrate the larger
	 * the frame but the more samples per second the smaller the value, also have to take into account frame padding
	 * Have to multiple by a coefficient constant depending upon the layer it is encoded in,

	 */
	val frameLength: Int
		get() = when (version) {
			VERSION_2, VERSION_2_5 -> when (layer) {
				LAYER_I -> (LAYER_I_FRAME_SIZE_COEFFICIENT * (bitRate!! * SCALE_BY_THOUSAND) / samplingRate!! + paddingLength) * LAYER_I_SLOT_SIZE
				LAYER_II -> LAYER_II_FRAME_SIZE_COEFFICIENT * (bitRate!! * SCALE_BY_THOUSAND) / samplingRate!! + paddingLength * LAYER_II_SLOT_SIZE
				LAYER_III -> if (channelMode == MODE_MONO) {
					LAYER_III_FRAME_SIZE_COEFFICIENT / 2 * (bitRate!! * SCALE_BY_THOUSAND) / samplingRate!! + paddingLength * LAYER_III_SLOT_SIZE
				} else {
					LAYER_III_FRAME_SIZE_COEFFICIENT * (bitRate!! * SCALE_BY_THOUSAND) / samplingRate!! + paddingLength * LAYER_III_SLOT_SIZE
				}
				else -> throw RuntimeException("Mp3 Unknown Layer:$layer")
			}
			VERSION_1 -> when (layer) {
				LAYER_I -> (LAYER_I_FRAME_SIZE_COEFFICIENT * (bitRate!! * SCALE_BY_THOUSAND) / samplingRate!! + paddingLength) * LAYER_I_SLOT_SIZE
				LAYER_II -> LAYER_II_FRAME_SIZE_COEFFICIENT * (bitRate!! * SCALE_BY_THOUSAND) / samplingRate!! + paddingLength * LAYER_II_SLOT_SIZE
				LAYER_III -> LAYER_III_FRAME_SIZE_COEFFICIENT * (bitRate!! * SCALE_BY_THOUSAND) / samplingRate!! + paddingLength * LAYER_III_SLOT_SIZE
				else -> throw RuntimeException("Mp3 Unknown Layer:$layer")
			}
			else -> throw RuntimeException("Mp3 Unknown Version:$version")
		}

	/**
	 * Get the number of samples in a frame, all frames in a file have a set number of samples as defined by their MPEG Versiona
	 * and Layer
	 * @return
	 */
	val noOfSamples: Int
		get() {
			val noOfSamples = samplesPerFrameMap[version]!![layer]
			return noOfSamples!!
		}
	val isVariableBitRate: Boolean
		get() = false

	/**
	 * Hide Constructor
	 * @throws InvalidAudioFrameException
	 */
	private constructor()

	/**
	 * Try and create a new MPEG frame with the given byte array and decodes its contents
	 * If decoding header causes a problem it is not a valid header
	 *
	 * @param b the array of bytes representing this mpeg frame
	 * @throws InvalidAudioFrameException if does not match expected format
	 */
	private constructor(b: ByteArray) {
		mpegBytes = b
		setBitrate()
		setVersion()
		setLayer()
		setProtected()
		setSamplingRate()
		setPadding()
		setPrivate()
		setChannelMode()
		setModeExtension()
		setCopyrighted()
		setOriginal()
		setEmphasis()
	}

	/**
	 * @return a string represntation
	 */
	override fun toString(): String {
		return """MPEG Frame Header:
	frame length:${frameLength}
	version:$versionAsString
	layer:$layerAsString
	channelMode:$channelModeAsString
	noOfSamples:${noOfSamples}
	samplingRate:$samplingRate
	isPadding:$isPadding
	isProtected:$isProtected
	isPrivate:$isPrivate
	isCopyrighted:$isCopyrighted
	isOriginal:$isCopyrighted
	isVariableBitRate${isVariableBitRate}
	header as binary:
	${AbstractTagDisplayFormatter.displayAsBinary(mpegBytes[BYTE_1])} 	${
			AbstractTagDisplayFormatter.displayAsBinary(
				mpegBytes[BYTE_2]
			)
		} 	${AbstractTagDisplayFormatter.displayAsBinary(mpegBytes[BYTE_3])} 	${
			AbstractTagDisplayFormatter.displayAsBinary(
				mpegBytes[BYTE_4]
			)
		}
"""
	}

	companion object {
		/**
		 * Constants for MP3 Frame header, each frame has a basic header of
		 * 4 bytes
		 */
		private const val BYTE_1 = 0
		private const val BYTE_2 = 1
		private const val BYTE_3 = 2
		private const val BYTE_4 = 3
		const val HEADER_SIZE = 4

		/**
		 * Sync Value to identify the start of an MPEGFrame
		 */
		const val SYNC_SIZE = 2
		const val SYNC_BYTE1 = 0xFF
		const val SYNC_BYTE2 = 0xE0
		const val SYNC_BIT_ANDSAMPING_BYTE3 = 0xFC
		private val header = ByteArray(HEADER_SIZE)

		/**
		 * Constants for MPEG Version
		 */
		val mpegVersionMap: MutableMap<Int, String> = HashMap()
		const val VERSION_2_5 = 0
		const val VERSION_2 = 2
		const val VERSION_1 = 3

		init {
			mpegVersionMap[VERSION_2_5] = "MPEG-2.5"
			mpegVersionMap[VERSION_2] = "MPEG-2"
			mpegVersionMap[VERSION_1] =
				"MPEG-1"
		}

		/**
		 * Constants for MPEG Layer
		 */
		val mpegLayerMap: MutableMap<Int, String> = HashMap()
		const val LAYER_I = 3
		const val LAYER_II = 2
		const val LAYER_III = 1

		init {
			mpegLayerMap[LAYER_I] =
				"Layer 1"
			mpegLayerMap[LAYER_II] = "Layer 2"
			mpegLayerMap[LAYER_III] =
				"Layer 3"
		}

		/**
		 * Slot Size is dependent on Layer
		 */
		const val LAYER_I_SLOT_SIZE = 4
		const val LAYER_II_SLOT_SIZE = 1
		const val LAYER_III_SLOT_SIZE = 1

		/**
		 * Bit Rates, the setBitrate varies for different Version and Layer
		 */
		private val bitrateMap: MutableMap<Int, Int> = HashMap()

		init {
			// MPEG-1, Layer I (E)
			bitrateMap[0x1E] = 32
			bitrateMap[0x2E] = 64
			bitrateMap[0x3E] =
				96
			bitrateMap[0x4E] =
				128
			bitrateMap[0x5E] = 160
			bitrateMap[0x6E] = 192
			bitrateMap[0x7E] = 224
			bitrateMap[0x8E] =
				256
			bitrateMap[0x9E] =
				288
			bitrateMap[0xAE] = 320
			bitrateMap[0xBE] = 352
			bitrateMap[0xCE] = 384
			bitrateMap[0xDE] = 416
			bitrateMap[0xEE] =
				448
			// MPEG-1, Layer II (C)
			bitrateMap[0x1C] =
				32
			bitrateMap[0x2C] =
				48
			bitrateMap[0x3C] =
				56
			bitrateMap[0x4C] =
				64
			bitrateMap[0x5C] = 80
			bitrateMap[0x6C] = 96
			bitrateMap[0x7C] =
				112
			bitrateMap[0x8C] =
				128
			bitrateMap[0x9C] = 160
			bitrateMap[0xAC] =
				192
			bitrateMap[0xBC] =
				224
			bitrateMap[0xCC] = 256
			bitrateMap[0xDC] =
				320
			bitrateMap[0xEC] =
				384
			// MPEG-1, Layer III (A)
			bitrateMap[0x1A] = 32
			bitrateMap[0x2A] = 40
			bitrateMap[0x3A] =
				48
			bitrateMap[0x4A] =
				56
			bitrateMap[0x5A] =
				64
			bitrateMap[0x6A] =
				80
			bitrateMap[0x7A] = 96
			bitrateMap[0x8A] = 112
			bitrateMap[0x9A] =
				128
			bitrateMap[0xAA] =
				160
			bitrateMap[0xBA] =
				192
			bitrateMap[0xCA] =
				224
			bitrateMap[0xDA] =
				256
			bitrateMap[0xEA] =
				320
			// MPEG-2, Layer I (6)
			bitrateMap[0x16] = 32
			bitrateMap[0x26] = 48
			bitrateMap[0x36] = 56
			bitrateMap[0x46] = 64
			bitrateMap[0x56] = 80
			bitrateMap[0x66] = 96
			bitrateMap[0x76] =
				112
			bitrateMap[0x86] = 128
			bitrateMap[0x96] =
				144
			bitrateMap[0xA6] = 160
			bitrateMap[0xB6] =
				176
			bitrateMap[0xC6] = 192
			bitrateMap[0xD6] = 224
			bitrateMap[0xE6] = 256
			// MPEG-2, Layer II (4)
			bitrateMap[0x14] = 8
			bitrateMap[0x24] =
				16
			bitrateMap[0x34] =
				24
			bitrateMap[0x44] = 32
			bitrateMap[0x54] = 40
			bitrateMap[0x64] =
				48
			bitrateMap[0x74] =
				56
			bitrateMap[0x84] =
				64
			bitrateMap[0x94] = 80
			bitrateMap[0xA4] =
				96
			bitrateMap[0xB4] =
				112
			bitrateMap[0xC4] = 128
			bitrateMap[0xD4] =
				144
			bitrateMap[0xE4] = 160
			// MPEG-2, Layer III (2)
			bitrateMap[0x12] = 8
			bitrateMap[0x22] =
				16
			bitrateMap[0x32] = 24
			bitrateMap[0x42] = 32
			bitrateMap[0x52] =
				40
			bitrateMap[0x62] = 48
			bitrateMap[0x72] = 56
			bitrateMap[0x82] =
				64
			bitrateMap[0x92] =
				80
			bitrateMap[0xA2] =
				96
			bitrateMap[0xB2] = 112
			bitrateMap[0xC2] = 128
			bitrateMap[0xD2] =
				144
			bitrateMap[0xE2] = 160
		}

		/**
		 * Constants for Channel mode
		 */
		val modeMap: MutableMap<Int, String> = HashMap()
		const val MODE_STEREO = 0
		const val MODE_JOINT_STEREO = 1
		const val MODE_DUAL_CHANNEL = 2
		const val MODE_MONO = 3

		init {
			modeMap[MODE_STEREO] = "Stereo"
			modeMap[MODE_JOINT_STEREO] = "Joint Stereo"
			modeMap[MODE_DUAL_CHANNEL] = "Dual"
			modeMap[MODE_MONO] = "Mono"
		}

		/**
		 * Constants for Emphasis
		 */
		private val emphasisMap: MutableMap<Int, String> = HashMap()
		const val EMPHASIS_NONE = 0
		const val EMPHASIS_5015MS = 1
		const val EMPHASIS_RESERVED = 2
		const val EMPHASIS_CCITT = 3

		init {
			emphasisMap[EMPHASIS_NONE] = "None"
			emphasisMap[EMPHASIS_5015MS] =
				"5015MS"
			emphasisMap[EMPHASIS_RESERVED] =
				"Reserved"
			emphasisMap[EMPHASIS_CCITT] =
				"CCITT"
		}

		private val modeExtensionMap: MutableMap<Int, String> = HashMap()
		private const val MODE_EXTENSION_NONE = 0
		private const val MODE_EXTENSION_ONE = 1
		private const val MODE_EXTENSION_TWO = 2
		private const val MODE_EXTENSION_THREE = 3
		private val modeExtensionLayerIIIMap: MutableMap<Int, String> = HashMap()
		private const val MODE_EXTENSION_OFF_OFF = 0
		private const val MODE_EXTENSION_ON_OFF = 1
		private const val MODE_EXTENSION_OFF_ON = 2
		private const val MODE_EXTENSION_ON_ON = 3

		init {
			modeExtensionMap[MODE_EXTENSION_NONE] =
				"4-31"
			modeExtensionMap[MODE_EXTENSION_ONE] = "8-31"
			modeExtensionMap[MODE_EXTENSION_TWO] =
				"12-31"
			modeExtensionMap[MODE_EXTENSION_THREE] =
				"16-31"
			modeExtensionLayerIIIMap[MODE_EXTENSION_OFF_OFF] =
				"off-off"
			modeExtensionLayerIIIMap[MODE_EXTENSION_ON_OFF] =
				"on-off"
			modeExtensionLayerIIIMap[MODE_EXTENSION_OFF_ON] =
				"off-on"
			modeExtensionLayerIIIMap[MODE_EXTENSION_ON_ON] =
				"on-on"
		}

		/**
		 * Sampling Rate in Hz
		 */
		private val samplingRateMap: MutableMap<Int, Map<Int, Int>> = HashMap()
		private val samplingV1Map: MutableMap<Int, Int> = HashMap()
		private val samplingV2Map: MutableMap<Int, Int> = HashMap()
		private val samplingV25Map: MutableMap<Int, Int> = HashMap()

		init {
			samplingV1Map[0] =
				44100
			samplingV1Map[1] =
				48000
			samplingV1Map[2] = 32000
			samplingV2Map[0] =
				22050
			samplingV2Map[1] = 24000
			samplingV2Map[2] = 16000
			samplingV25Map[0] =
				11025
			samplingV25Map[1] =
				12000
			samplingV25Map[2] = 8000
			samplingRateMap[VERSION_1] =
				samplingV1Map
			samplingRateMap[VERSION_2] =
				samplingV2Map
			samplingRateMap[VERSION_2_5] =
				samplingV25Map
		}

		/* Samples Per Frame */
		private val samplesPerFrameMap: MutableMap<Int, Map<Int, Int>> = HashMap()
		private val samplesPerFrameV1Map: MutableMap<Int, Int> = HashMap()
		private val samplesPerFrameV2Map: MutableMap<Int, Int> = HashMap()
		private val samplesPerFrameV25Map: MutableMap<Int, Int> = HashMap()

		init {
			samplesPerFrameV1Map[LAYER_I] = 384
			samplesPerFrameV1Map[LAYER_II] =
				1152
			samplesPerFrameV1Map[LAYER_III] = 1152
			samplesPerFrameV2Map[LAYER_I] = 384
			samplesPerFrameV2Map[LAYER_II] =
				1152
			samplesPerFrameV2Map[LAYER_III] =
				1152
			samplesPerFrameV25Map[LAYER_I] =
				384
			samplesPerFrameV25Map[LAYER_II] = 1152
			samplesPerFrameV25Map[LAYER_III] =
				1152
			samplesPerFrameMap[VERSION_1] =
				samplesPerFrameV1Map
			samplesPerFrameMap[VERSION_2] =
				samplesPerFrameV2Map
			samplesPerFrameMap[VERSION_2_5] =
				samplesPerFrameV25Map
		}

		private const val SCALE_BY_THOUSAND = 1000
		private const val LAYER_I_FRAME_SIZE_COEFFICIENT = 12
		private const val LAYER_II_FRAME_SIZE_COEFFICIENT = 144
		private const val LAYER_III_FRAME_SIZE_COEFFICIENT = 144

		/**
		 * MP3 Frame Header bit mask
		 */
		private const val MASK_MP3_ID = FileConstants.BIT3

		/**
		 * MP3 version, confusingly for MP3s the version is 1.
		 */
		private const val MASK_MP3_VERSION = FileConstants.BIT4 or FileConstants.BIT3

		/**
		 * MP3 Layer, for MP3s the Layer is 3
		 */
		private const val MASK_MP3_LAYER = FileConstants.BIT2 or FileConstants.BIT1

		/**
		 * Does it include a CRC Checksum at end of header, this can be used to check the header.
		 */
		private const val MASK_MP3_PROTECTION = FileConstants.BIT0

		/**
		 * The setBitrate of this MP3
		 */
		private const val MASK_MP3_BITRATE =
			FileConstants.BIT7 or FileConstants.BIT6 or FileConstants.BIT5 or FileConstants.BIT4

		/**
		 * The sampling/frequency rate
		 */
		private const val MASK_MP3_FREQUENCY = FileConstants.BIT3 + FileConstants.BIT2

		/**
		 * An extra padding bit is sometimes used to make sure frames are exactly the right length
		 */
		private const val MASK_MP3_PADDING = FileConstants.BIT1

		/**
		 * Private bit set, for application specific
		 */
		private const val MASK_MP3_PRIVACY = FileConstants.BIT0

		/**
		 * Channel Mode, Stero/Mono/Dual Channel
		 */
		private const val MASK_MP3_MODE = FileConstants.BIT7 or FileConstants.BIT6

		/**
		 * MP3 Frame Header bit mask
		 */
		private const val MASK_MP3_MODE_EXTENSION = FileConstants.BIT5 or FileConstants.BIT4

		/**
		 * MP3 Frame Header bit mask
		 */
		private const val MASK_MP3_COPY = FileConstants.BIT3

		/**
		 * MP3 Frame Header bit mask
		 */
		private const val MASK_MP3_HOME = FileConstants.BIT2

		/**
		 * MP3 Frame Header bit mask
		 */
		private const val MASK_MP3_EMPHASIS = FileConstants.BIT1 or FileConstants.BIT0

		/**
		 * Parse the MPEGFrameHeader of an MP3File, file pointer returns at end of the frame header
		 *
		 * @param bb the byte buffer containing the header
		 * @return
		 * @throws InvalidAudioFrameException if there is no header at this point
		 */
		@Throws(InvalidAudioFrameException::class)
		fun parseMPEGHeader(bb: ByteBuffer): MPEGFrameHeader {
			val position = bb.position()
			bb[header, 0, HEADER_SIZE]
			bb.position(position)
			return MPEGFrameHeader(header)
		}

		/**
		 * Gets the MPEGFrame attribute of the MPEGFrame object
		 *
		 * @param bb
		 * @return The mPEGFrame value
		 */
		fun isMPEGFrame(bb: ByteBuffer): Boolean {
			val position = bb.position()
			return bb[position].toInt() and SYNC_BYTE1 == SYNC_BYTE1 && bb[position + 1].toInt() and SYNC_BYTE2 == SYNC_BYTE2 && bb[position + 2].toInt() and SYNC_BIT_ANDSAMPING_BYTE3 != SYNC_BIT_ANDSAMPING_BYTE3
		}
	}
}
