package com.flammky.musicplayer.common.media.audio.meta_tag.audio

/**
 * Representation of AudioHeader
 *
 *
 * Contains info about the Audio Header
 */
interface AudioHeader {
	/**
	 * @return the audio file type
	 */
	val encodingType: String?

	/**
	 * @return the ByteRate of the Audio, this is the total average amount of bytes of data sampled per second
	 */
	val byteRate: Int?

	/**
	 * @return the BitRate of the Audio, this is the amount of kilobits of data sampled per second
	 */
	val bitRate: String?

	/**
	 * @return bitRate as a number, this is the amount of kilobits of data sampled per second
	 */
	val bitRateAsNumber: Long?

	/**
	 *
	 * @return length of the audio data in bytes, exactly what this means depends on the audio format
	 *
	 * TODO currently only used by Wav/Aiff/Flac/Mp4
	 */
	val audioDataLength: Long?

	/**
	 *
	 * @return the location in the file where the audio samples start
	 *
	 * TODO currently only used by Wav/Aiff/Flac/Mp4
	 */
	val audioDataStartPosition: Long?

	/**
	 *
	 * @return the location in the file where the audio samples end
	 *
	 * TODO currently only used by Wav/Aiff/Flac/Mp4
	 */
	val audioDataEndPosition: Long?

	/**
	 * @return the Sampling rate, the number of samples taken per second
	 */
	val sampleRate: String?

	/**
	 * @return he Sampling rate, the number of samples taken per second
	 */
	val sampleRateAsNumber: Int?

	/**
	 * @return the format
	 */
	val format: String?

	/**
	 * @return the number of channels (i.e 1 = Mono, 2 = Stereo)
	 */
	val channels: String?

	/**
	 * @return if the sampling bitRate is variable or constant
	 */
	val isVariableBitRate: Boolean?

	/**
	 * @return track length in seconds
	 */
	val trackLength: Int?

	/**
	 *
	 * @return track length as float
	 */
	val preciseTrackLength: Double?

	/**
	 * @return the number of bits in each sample
	 */
	val bitsPerSample: Int?

	/**
	 *
	 * @return if the audio codec is lossless or lossy
	 */
	val isLossless: Boolean?

	/**
	 *
	 * @return the total number of samples, this can usually be used in conjunction with the
	 * sample rate to determine the track duration
	 */
	val noOfSamples: Long?
}
