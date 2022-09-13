package com.kylentt.musicplayer.common.media.audio.meta_tag.audio.aiff

import com.kylentt.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.kylentt.musicplayer.common.media.audio.meta_tag.audio.generic.AudioFileReader2
import com.kylentt.musicplayer.common.media.audio.meta_tag.audio.generic.GenericAudioHeader
import com.kylentt.musicplayer.common.media.audio.meta_tag.tag.Tag
import java.io.IOException
import java.nio.file.Path

/**
 * Reads Audio and Metadata information contained in Aiff file.
 */
class AiffFileReader : AudioFileReader2() {

	@Throws(CannotReadException::class, IOException::class)
	override fun getEncodingInfo(file: Path): GenericAudioHeader {
		return AiffInfoReader(file.toString()).read(file)
	}

	@Throws(CannotReadException::class, IOException::class)
	override fun getTag(path: Path): Tag {
		return AiffTagReader(path.toString()).read(path)
	}
}
