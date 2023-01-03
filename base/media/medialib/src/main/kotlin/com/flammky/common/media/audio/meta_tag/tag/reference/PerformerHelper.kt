package com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference

import java.util.*

/**
 * The PERFORMER field is formatted differently depending on whether writing to the ID3 format that utilises multiple fields or the simple
 * TEXT/VALUE fields used by VorbisComments and similar formats, the role is always set to lowercase.
 */
object PerformerHelper {
	fun formatForId3(artist: String, attributes: String): String {
		return attributes.lowercase(Locale.getDefault()) + '\u0000' + artist
	}

	fun formatForNonId3(artist: String, attributes: String): String {
		return artist + " (" + attributes.lowercase(Locale.getDefault()) + ")"
	}
}
