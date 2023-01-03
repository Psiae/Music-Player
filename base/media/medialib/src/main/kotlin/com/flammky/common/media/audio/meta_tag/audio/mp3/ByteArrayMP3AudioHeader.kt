package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp3

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.InvalidAudioFrameException
import java.nio.ByteBuffer

class ByteArrayMP3AudioHeader(fileBytes: ByteArray) : MP3AudioHeader() {

	init {
		//References to Xing Header
		var header: ByteBuffer

		// This is substantially faster than updating the filechannels position
		var filePointerCount: Long = 0

		// Read into Byte Buffer in Chunks
		val bb = ByteBuffer.wrap(fileBytes)
		var syncFound = false
		do {
			if (MPEGFrameHeader.Companion.isMPEGFrame(bb)) {
				try {
					mp3FrameHeader = MPEGFrameHeader.Companion.parseMPEGHeader(bb)
					syncFound = true
					if (XingFrame.Companion.isXingFrame(bb, mp3FrameHeader)
							.also { header = it!! } != null
					) {
						try {
							// Parses Xing frame without modifying position of main buffer
							mp3XingFrame = XingFrame.Companion.parseXingFrame(header)
						} catch (ex: InvalidAudioFrameException) {
							// We Ignore because even if Xing Header is corrupted
							// doesn't mean file is corrupted
						}
						break
					} else {
						syncFound = isNextFrameValid(bb)
						if (syncFound) {
							break
						}
					}
				} catch (ex: InvalidAudioFrameException) {
					// We Ignore because likely to be incorrect sync bits ,
					// will just continue in loop
				}
			}
			bb.position(bb.position() + 1)
			filePointerCount++
		} while (!syncFound)
		setFileSize(fileBytes.size.toLong())
		mp3StartByte = filePointerCount
		setTimePerFrame()
		setNumberOfFrames()
		setTrackLength()
		setBitRate()
		setEncoder()
	}

	private fun isNextFrameValid(bb: ByteBuffer): Boolean {
		var result = false
		val currentPosition = bb.position()
		bb.position(bb.position() + (mp3FrameHeader?.frameLength ?: 0))
		if (MPEGFrameHeader.isMPEGFrame(bb)) {
			result = try {
				MPEGFrameHeader.Companion.parseMPEGHeader(bb)
				logger.finer("Check next frame confirms is an audio header ")
				true
			} catch (ex: InvalidAudioFrameException) {
				logger.finer("Check next frame has identified this is not an audio header")
				false
			}
		}
		// Set back to the start of the previous frame
		bb.position(currentPosition)
		return result
	}
}
