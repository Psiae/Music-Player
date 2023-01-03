package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair

/**
 * ID3V2 Genre list
 *
 *
 * These are additional genres added in the V2 Specification, they have a string key (RX,CV) rather than a
 * numeric key
 */
enum class ID3V2ExtendedGenreTypes(val description: String) {
	RX("Remix"), CR("Cover");

}
