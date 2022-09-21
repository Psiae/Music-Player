package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3

/**
 * Orders frame Ids so that the most important frames are writtne first
 */
class ID3v23PreferredFrameOrderComparator private constructor() : Comparator<String> {
	/**
	 *
	 * @param frameId1
	 * @param frameId2
	 * @return
	 */
	override fun compare(frameId1: String, frameId2: String): Int {
		var frameId1Index = frameIdsInPreferredOrder.indexOf(frameId1)
		if (frameId1Index == -1) {
			frameId1Index = Int.MAX_VALUE
		}
		var frameId2Index = frameIdsInPreferredOrder.indexOf(frameId2)

		//Because othwerwise returns -1 whihc would be tags in list went to top of list
		if (frameId2Index == -1) {
			frameId2Index = Int.MAX_VALUE
		}

		//To have determinable ordering AND because if returns equal Treese considers as equal
		return if (frameId1Index == frameId2Index) {
			frameId1.compareTo(frameId2)
		} else frameId1Index - frameId2Index
	}

	override fun equals(obj: Any?): Boolean {
		return obj is ID3v23PreferredFrameOrderComparator
	}

	companion object {
		private var comparator: ID3v23PreferredFrameOrderComparator? = null
		private val frameIdsInPreferredOrder: MutableList<String> = ArrayList()

		init {
			//these are the key ones we want at the top
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_UNIQUE_FILE_ID)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_TITLE)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_ARTIST)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_ALBUM)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_TORY)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_GENRE)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_COMPOSER)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_CONDUCTOR)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_CONTENT_GROUP_DESC)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_TRACK)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_TYER)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_TDAT)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_TIME)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_BPM)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_ISRC)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_TORY)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_ACCOMPANIMENT)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_TITLE_REFINEMENT)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_UNSYNC_LYRICS)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_URL)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_URL_ARTIST_WEB)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_URL_COMMERCIAL)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_URL_COPYRIGHT)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_URL_FILE_WEB)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_URL_OFFICIAL_RADIO)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_URL_PAYMENT)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_URL_PUBLISHERS)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_URL_COMMERCIAL)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_LYRICIST)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_MEDIA_TYPE)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_INVOLVED_PEOPLE)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_LANGUAGE)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_TITLE_SORT_ORDER_ITUNES)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_PLAYLIST_DELAY)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_PLAY_COUNTER)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_POPULARIMETER)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_PUBLISHER)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_ALBUM_ARTIST_SORT_ORDER_ITUNES)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_COMPOSER_SORT_ORDER_ITUNES)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_IS_COMPILATION)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_TITLE_SORT_ORDER_ITUNES)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_ARTIST_SORT_ORDER_ITUNES)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_ALBUM_SORT_ORDER_ITUNES)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_TITLE_SORT_ORDER_MUSICBRAINZ)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_ARTIST_SORT_ORDER_MUSICBRAINZ)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_ALBUM_SORT_ORDER_MUSICBRAINZ)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_ALBUM_ARTIST_SORT_ORDER_ITUNES)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_COMPOSER_SORT_ORDER_ITUNES)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_COMMENT)


			//Not so bothered about these
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_TRDA)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_COMMERCIAL_FRAME)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_COPYRIGHTINFO)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_ENCODEDBY)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_ENCRYPTION)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_EQUALISATION)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_EVENT_TIMING_CODES)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_FILE_OWNER)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_FILE_TYPE)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_GROUP_ID_REG)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_HW_SW_SETTINGS)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_INITIAL_KEY)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_LENGTH)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_LINKED_INFO)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_TSIZ)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_MPEG_LOCATION_LOOKUP_TABLE)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_ORIGARTIST)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_ORIG_FILENAME)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_ORIG_LYRICIST)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_ORIG_TITLE)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_OWNERSHIP)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_POSITION_SYNC)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_RADIO_NAME)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_RADIO_OWNER)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_RECOMMENDED_BUFFER_SIZE)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_REMIXED)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_REVERB)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_SET)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_SYNC_LYRIC)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_SYNC_TEMPO)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_TERMS_OF_USE)

			//Want this near the end because can cause problems with unsyncing
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_ATTACHED_PICTURE)

			//Itunes doesnt seem to like these, and of little use so put right at end
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_PRIVATE)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_MUSIC_CD_ID)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_AUDIO_ENCRYPTION)
			frameIdsInPreferredOrder.add(ID3v23Frames.FRAME_ID_V3_GENERAL_ENCAPS_OBJECT)
		}

		@JvmStatic
		val instanceof: ID3v23PreferredFrameOrderComparator?
			get() {
				if (comparator == null) {
					comparator = ID3v23PreferredFrameOrderComparator()
				}
				return comparator
			}
	}
}
