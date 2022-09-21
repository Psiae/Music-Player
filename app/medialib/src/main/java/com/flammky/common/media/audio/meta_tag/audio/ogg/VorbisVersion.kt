package com.flammky.musicplayer.common.media.audio.meta_tag.audio.ogg

/**
 * Vorbis Version
 *
 * Ordinal is used to map from internal representation
 */
enum class VorbisVersion(  //The display name for this version
	private val displayName: String
) {
	VERSION_ONE("Ogg Vorbis v1");

	override fun toString(): String {
		return displayName
	}
}
