package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.AbstractIntStringValuePair

/**
 * Content Type used by Sysnchronised Lyrics Frame (SYLT)
 */
class SynchronisedLyricsContentType private constructor() : AbstractIntStringValuePair() {
	init {
		idToValue[0x00] = "other"
		idToValue[0x01] = "lyrics"
		idToValue[0x02] = "text transcription"
		idToValue[0x03] = "movement/part name"
		idToValue[0x04] = "events"
		idToValue[0x05] = "chord"
		idToValue[0x06] = "trivia"
		idToValue[0x07] = "URLs to webpages"
		idToValue[0x08] = "URLs to images"
		createMaps()
	}

	companion object {
		private var eventTimingTypes: SynchronisedLyricsContentType? = null

		@JvmStatic
		val instanceOf: SynchronisedLyricsContentType
			get() {
				if (eventTimingTypes == null) {
					eventTimingTypes = SynchronisedLyricsContentType()
				}
				return eventTimingTypes!!
			}
		const val CONTENT_KEY_FIELD_SIZE = 1
	}
}
