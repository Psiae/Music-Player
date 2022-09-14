package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom

/**
 * Some mp4s contain null padding at the end of the file, possibly do with gapless playback. This is not really
 * allowable but seeing as seems to cccur in files encoded with iTunes 6 and players such as Winamp and iTunes deal
 * with it we should
 *
 * It isnt actually a box, but it helps to keep as a subclass of this type
 */
class NullPadding(startPosition: Long, fileSize: Long) : Mp4BoxHeader() {
	init {
		filePos = startPosition
		length = (fileSize - startPosition).toInt()
	}
}
