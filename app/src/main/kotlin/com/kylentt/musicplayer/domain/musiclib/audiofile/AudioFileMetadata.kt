package com.kylentt.musicplayer.domain.musiclib.audiofile

class AudioFileMetadata private constructor() {

	var artist: String = ""
		private set

	var album: String = ""
		private set

	var title: String = ""
		private set

	var bitRate: Int = Int.MIN_VALUE + 1
		private set

	var durationMs: Long = Long.MIN_VALUE + 1
		private set

	var playable: Boolean = false
		private set

	class Builder {
		var album = empty.album
		var artist = empty.artist
		var title = empty.title

		var bitRate = empty.bitRate
		var durationMs = empty.durationMs
		var playable = empty.playable

		fun build(): AudioFileMetadata = AudioFileMetadata()
			.apply {
				album = this@Builder.album
				artist = this@Builder.artist
				title = this@Builder.title
				bitRate = this@Builder.bitRate
				durationMs = this@Builder.durationMs
				playable = this@Builder.playable
			}

		companion object {
			val empty = AudioFileMetadata()
		}
	}
}
