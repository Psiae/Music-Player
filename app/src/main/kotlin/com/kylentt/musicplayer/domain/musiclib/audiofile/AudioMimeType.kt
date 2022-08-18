package com.kylentt.musicplayer.domain.musiclib.audiofile

sealed class AudioMimeType(
	open val extensions: List<String>,
	open val typeString: String
) {

	object AAC : AudioMimeType(
		extensions = listOf("aac"),
		typeString = "audio/aac"
	)

	object BASIC : AudioMimeType(
		extensions = listOf("snd", "au"),
		typeString = "audio/basic"
	)

	object FLAC : AudioMimeType(
		extensions = listOf("flac"),
		typeString = "audio/flac"
	)

	object MP4 : AudioMimeType(
		extensions = listOf("m4a"),
		typeString = "audio/mp4"
	)

	object MPEG : AudioMimeType(
		extensions = listOf("mp2", "mp3"),
		typeString = "audio/mpeg"
	)

	object OGG : AudioMimeType(
		extensions = listOf("oga", "ogg", "opus", "spx"),
		typeString = "audio/ogg"
	)

	object X_AIFF : AudioMimeType(
		extensions = listOf("aif", "aiff", "aifc"),
		typeString = "audio/x-aiff"
	)

	object X_WAV : AudioMimeType(
		extensions = listOf("wav"),
		typeString = "audio/x-wav"
	)

	data class OTHER(
		override val extensions: List<String>,
		override val typeString: String
	) : AudioMimeType(extensions, typeString)

	object Any : AudioMimeType(
		extensions = listOf("*"),
		typeString = "audio/*"
	)
}
