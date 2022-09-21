package com.flammky.musicplayer.common.media.audio.meta_tag.audio.ogg.util

/**
 * Vorbis Packet Type
 *
 * In an Vorbis Stream there should be one instance of the three headers, and many audio packets
 */
enum class VorbisPacketType(var type: Int) {
	AUDIO(0), IDENTIFICATION_HEADER(1), COMMENT_HEADER(3), SETUP_HEADER(5);

}
