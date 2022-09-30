package com.flammky.musicplayer.common.media.audio.meta_tag.audio.dsf

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagOptionSingleton
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v22Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v23Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.ID3V2Version

/**
 * Created by Paul on 28/01/2016.
 */
object Dsf {
	@JvmStatic
	fun createDefaultTag(): Tag {
		if (TagOptionSingleton.instance.iD3V2Version === ID3V2Version.ID3_V24) {
			return ID3v24Tag()
		} else if (TagOptionSingleton.instance.iD3V2Version === ID3V2Version.ID3_V23) {
			return ID3v23Tag()
		} else if (TagOptionSingleton.instance.iD3V2Version === ID3V2Version.ID3_V22) {
			return ID3v22Tag()
		}
		//Default in case not set somehow
		return ID3v24Tag()
	}
}
