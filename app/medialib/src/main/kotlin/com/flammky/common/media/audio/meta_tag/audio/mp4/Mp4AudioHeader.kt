package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.GenericAudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4EsdsBox


/**
 * Store some additional attributes useful for Mp4s
 */
class Mp4AudioHeader : GenericAudioHeader() {

	var kind: Mp4EsdsBox.Kind? = null
	var profile: Mp4EsdsBox.AudioProfile? = null
	var brand: String? = null
}
