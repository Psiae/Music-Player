package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3

/**
 * Orders frame Ids so that the most important frames are writtne first
 */
class ID3v22PreferredFrameOrderComparator private constructor() : Comparator<String> {
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
		return obj is ID3v22PreferredFrameOrderComparator
	}

	companion object {
		private var comparator: ID3v22PreferredFrameOrderComparator? = null
		private val frameIdsInPreferredOrder: MutableList<String> = ArrayList()

		init {
			//these are the key ones we want at the top
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_UNIQUE_FILE_ID)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_TITLE)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_ARTIST)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_ALBUM)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_TORY)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_GENRE)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_COMPOSER)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_CONDUCTOR)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_CONTENT_GROUP_DESC)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_TRACK)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_TYER)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_TDAT)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_TIME)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_BPM)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_ISRC)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_TORY)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_ACCOMPANIMENT)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_TITLE_REFINEMENT)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_UNSYNC_LYRICS)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_USER_DEFINED_INFO)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_USER_DEFINED_URL)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_URL_ARTIST_WEB)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_URL_COMMERCIAL)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_URL_COPYRIGHT)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_URL_FILE_WEB)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_URL_OFFICIAL_RADIO)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_URL_PAYMENT)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_URL_PUBLISHERS)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_URL_COMMERCIAL)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_LYRICIST)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_MEDIA_TYPE)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_IPLS)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_LANGUAGE)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_TITLE_SORT_ORDER_ITUNES)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_PLAYLIST_DELAY)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_PLAY_COUNTER)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_POPULARIMETER)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_PUBLISHER)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_ALBUM_ARTIST_SORT_ORDER_ITUNES)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_COMPOSER_SORT_ORDER_ITUNES)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_IS_COMPILATION)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_TITLE_SORT_ORDER_ITUNES)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_ARTIST_SORT_ORDER_ITUNES)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_ALBUM_SORT_ORDER_ITUNES)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_ALBUM_ARTIST_SORT_ORDER_ITUNES)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_COMPOSER_SORT_ORDER_ITUNES)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_COMMENT)

			//Not so bothered about these
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_TRDA)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_COPYRIGHTINFO)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_ENCODEDBY)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_EQUALISATION)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_EVENT_TIMING_CODES)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_FILE_TYPE)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_HW_SW_SETTINGS)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_INITIAL_KEY)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_LENGTH)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_LINKED_INFO)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_TSIZ)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_MPEG_LOCATION_LOOKUP_TABLE)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_ORIGARTIST)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_ORIG_FILENAME)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_ORIG_LYRICIST)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_ORIG_TITLE)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_RECOMMENDED_BUFFER_SIZE)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_REMIXED)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_REVERB)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_SET)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_SYNC_LYRIC)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_SYNC_TEMPO)

			//Want this near the end because can cause problems with unsyncing
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_ATTACHED_PICTURE)

			//Itunes doesnt seem to like these, and of little use so put right at end
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_MUSIC_CD_ID)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_AUDIO_ENCRYPTION)
			frameIdsInPreferredOrder.add(ID3v22Frames.FRAME_ID_V2_GENERAL_ENCAPS_OBJECT)
		}

		@JvmStatic
		val instanceof: ID3v22PreferredFrameOrderComparator
			get() {
				if (comparator == null) {
					comparator = ID3v22PreferredFrameOrderComparator()
				}
				return comparator!!
			}
	}
}
