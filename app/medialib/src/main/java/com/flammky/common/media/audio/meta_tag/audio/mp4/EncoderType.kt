package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4

/**
 * Encoder Type actually identifies the format of the audio within the mp4. This is because
 * mp4 container can be used to hold different types of files.
 */
enum class EncoderType(val description: String) {
	AAC("Aac"), DRM_AAC("Aac (Drm)"), APPLE_LOSSLESS("Alac");

}
