package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3

/**
 * Orders frame Ids so that the most important frames are writtne first
 */
class ID3v24PreferredFrameOrderComparator private constructor() : Comparator<String> {
	/**
	 *
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
		return obj is ID3v24PreferredFrameOrderComparator
	}

	companion object {
		private var comparator: ID3v24PreferredFrameOrderComparator? = null
		private val frameIdsInPreferredOrder: MutableList<String> = ArrayList()

		init {
			//these are the key ones we want at the top
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_UNIQUE_FILE_ID)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_TITLE)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_ARTIST)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_ALBUM)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_ALBUM_SORT_ORDER)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_GENRE)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_COMPOSER)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_CONDUCTOR)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_CONTENT_GROUP_DESC)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_TRACK)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_YEAR)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_ACCOMPANIMENT)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_BPM)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_ISRC)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_TITLE_SORT_ORDER)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_TITLE_REFINEMENT)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_UNSYNC_LYRICS)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_USER_DEFINED_INFO)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_USER_DEFINED_URL)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_URL_ARTIST_WEB)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_URL_COMMERCIAL)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_URL_COPYRIGHT)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_URL_FILE_WEB)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_URL_OFFICIAL_RADIO)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_URL_PAYMENT)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_URL_PUBLISHERS)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_URL_COMMERCIAL)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_LYRICIST)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_MEDIA_TYPE)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_INVOLVED_PEOPLE)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_LANGUAGE)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_ARTIST_SORT_ORDER)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_PLAYLIST_DELAY)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_PLAY_COUNTER)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_POPULARIMETER)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_PUBLISHER)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_ALBUM_ARTIST_SORT_ORDER_ITUNES)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_COMPOSER_SORT_ORDER_ITUNES)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_IS_COMPILATION)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_COMMENT)

			//Not so bothered about these
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_AUDIO_SEEK_POINT_INDEX)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_COMMERCIAL_FRAME)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_COPYRIGHTINFO)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_ENCODEDBY)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_ENCODING_TIME)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_ENCRYPTION)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_EQUALISATION2)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_EVENT_TIMING_CODES)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_FILE_OWNER)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_FILE_TYPE)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_GROUP_ID_REG)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_HW_SW_SETTINGS)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_INITIAL_KEY)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_LENGTH)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_LINKED_INFO)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_MOOD)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_MPEG_LOCATION_LOOKUP_TABLE)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_MUSICIAN_CREDITS)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_ORIGARTIST)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_ORIGINAL_RELEASE_TIME)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_ORIG_FILENAME)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_ORIG_LYRICIST)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_ORIG_TITLE)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_OWNERSHIP)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_POSITION_SYNC)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_PRODUCED_NOTICE)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_RADIO_NAME)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_RADIO_OWNER)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_RECOMMENDED_BUFFER_SIZE)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_RELATIVE_VOLUME_ADJUSTMENT2)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_RELEASE_TIME)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_REMIXED)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_REVERB)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_SEEK)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_SET)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_SET_SUBTITLE)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_SIGNATURE)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_SYNC_LYRIC)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_SYNC_TEMPO)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_TAGGING_TIME)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_TERMS_OF_USE)

			//Want this near the end because can cause problems with unsyncing
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_ATTACHED_PICTURE)

			//Itunes doesnt seem to like these, and of little use so put right at end
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_PRIVATE)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_MUSIC_CD_ID)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_AUDIO_ENCRYPTION)
			frameIdsInPreferredOrder.add(ID3v24Frames.FRAME_ID_GENERAL_ENCAPS_OBJECT)
		}

		@JvmStatic
		val instanceof: ID3v24PreferredFrameOrderComparator?
			get() {
				if (comparator == null) {
					comparator = ID3v24PreferredFrameOrderComparator()
				}
				return comparator
			}
	}
}
