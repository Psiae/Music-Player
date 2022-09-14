/*
 *  MusicTag Copyright (C)2003,2004
 *
 *  This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 *  General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 *  or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with this library; if not,
 *  you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.AbstractStringStringValuePair
import java.util.*

/**
 * Subclasses Defines ID3 frames for their Tag Version
 *
 * Here we specify how frames are mapped between different Tag Versions
 *
 * @author Paul Taylor
 * @version $Id$
 */
abstract class ID3Frames : AbstractStringStringValuePair() {
	/**
	 * Holds frames whereby multiple occurences are allowed
	 */
	@JvmField
	protected var multipleFrames = TreeSet<String>()

	/**
	 * These frames should be lost if file changes
	 */
	@JvmField
	protected var discardIfFileAlteredFrames = TreeSet<String>()

	/**
	 * These frames are part of the Official Specification for that Tag Version
	 */
	var supportedFrames = TreeSet<String>()
		protected set

	/**
	 * These frames are extensions to the  Specification for that Tag Version
	 */
	@JvmField
	protected var extensionFrames = TreeSet<String>()

	/**
	 * These frames are Common , this is a loose term
	 */
	@JvmField
	protected var commonFrames = TreeSet<String>()

	/**
	 * These frames are Binary
	 */
	@JvmField
	protected var binaryFrames = TreeSet<String>()

	/**
	 * If file changes discard these frames
	 * @param frameID
	 * @return
	 */
	fun isDiscardIfFileAltered(frameID: String): Boolean {
		return discardIfFileAlteredFrames.contains(frameID)
	}

	/**
	 * Are multiple occurrences of frame allowed
	 * @param frameID
	 * @return
	 */
	fun isMultipleAllowed(frameID: String): Boolean {
		return multipleFrames.contains(frameID)
	}

	/**
	 * @param frameID
	 * @return true if frames with this id are part of the specification
	 */
	fun isSupportedFrames(frameID: String): Boolean {
		return supportedFrames.contains(frameID)
	}

	/**
	 * @param frameID
	 * @return true if frames with this id are considered common
	 */
	fun isCommon(frameID: String): Boolean {
		return commonFrames.contains(frameID)
	}

	/**
	 * @param frameID
	 * @return true if frames with this id are binary (non textual data)
	 */
	fun isBinary(frameID: String): Boolean {
		return binaryFrames.contains(frameID)
	}

	/**
	 * @param frameID
	 * @return true if frame is a known extension
	 */
	fun isExtensionFrames(frameID: String): Boolean {
		return extensionFrames.contains(frameID)
	}

	/**
	 *
	 * Allows setting of a special iTunes 12.6 mode, where
	 * [FieldKey.GROUPING] is mapped to the
	 * non-standard frame `GPP1` and [FieldKey.WORK]
	 * to `TIT1` instead of the regular mapping
	 * (`GROUPING -> TIT1, WORK -> TXXX:WORK`).
	 *
	 *
	 * This method is called internally by [TagOptionSingleton.setId3v2ITunes12_6WorkGroupingMode]
	 * and should not be called by framework users directly.
	 *
	 * @param id3v2ITunes12_6Mode true or false
	 */
	abstract fun setITunes12_6WorkGroupingMode(id3v2ITunes12_6Mode: Boolean)

	companion object {
		/**
		 * Mapping from v22 to v23
		 */
		val convertv22Tov23: MutableMap<String, String> = LinkedHashMap()
		val convertv23Tov22: MutableMap<String?, String> = LinkedHashMap()
		val forcev22Tov23: MutableMap<String, String> = LinkedHashMap()
		val forcev23Tov22: MutableMap<String, String> = LinkedHashMap()
		val convertv23Tov24: MutableMap<String, String> = LinkedHashMap()
		val convertv24Tov23: Map<String, String> = LinkedHashMap()
		val forcev23Tov24: MutableMap<String, String> = LinkedHashMap()
		val forcev24Tov23: MutableMap<String, String> = LinkedHashMap()
		private fun loadID3v23ID3v24Mapping() {
			// Define the mapping from v23 to v24 only maps values where
			// the v23 ID is not a v24 ID and where the translation from v23 to v24
			// ID does not affect the framebody.
			//This one way allows us to convert XSOT to TSOT,XSOP to TSOP and XSOA - TSOA but in the other direction gets converted to TSOT,TSOP,TSOA
			convertv23Tov24[ID3v23Frames.FRAME_ID_V3_TITLE_SORT_ORDER_MUSICBRAINZ] =
				ID3v24Frames.FRAME_ID_TITLE_SORT_ORDER
			convertv23Tov24[ID3v23Frames.FRAME_ID_V3_ARTIST_SORT_ORDER_MUSICBRAINZ] =
				ID3v24Frames.FRAME_ID_ARTIST_SORT_ORDER
			convertv23Tov24[ID3v23Frames.FRAME_ID_V3_ALBUM_SORT_ORDER_MUSICBRAINZ] =
				ID3v24Frames.FRAME_ID_ALBUM_SORT_ORDER

			// No others exist because most v23 mappings are identical to v24 therefore no mapping required and the ones that
			// are different need to be forced.

			// Force v23 to v24 These are deprecated and need to do a forced mapping
			forcev23Tov24[ID3v23Frames.FRAME_ID_V3_RELATIVE_VOLUME_ADJUSTMENT] =
				ID3v24Frames.FRAME_ID_RELATIVE_VOLUME_ADJUSTMENT2
			forcev23Tov24[ID3v23Frames.FRAME_ID_V3_EQUALISATION] =
				ID3v24Frames.FRAME_ID_EQUALISATION2
			forcev23Tov24[ID3v23Frames.FRAME_ID_V3_INVOLVED_PEOPLE] =
				ID3v24Frames.FRAME_ID_INVOLVED_PEOPLE
			forcev23Tov24[ID3v23Frames.FRAME_ID_V3_TDAT] =
				ID3v24Frames.FRAME_ID_YEAR
			forcev23Tov24[ID3v23Frames.FRAME_ID_V3_TIME] =
				ID3v24Frames.FRAME_ID_YEAR
			forcev23Tov24[ID3v23Frames.FRAME_ID_V3_TORY] =
				ID3v24Frames.FRAME_ID_ORIGINAL_RELEASE_TIME
			forcev23Tov24[ID3v23Frames.FRAME_ID_V3_TRDA] = ID3v24Frames.FRAME_ID_YEAR
			forcev23Tov24[ID3v23Frames.FRAME_ID_V3_TYER] =
				ID3v24Frames.FRAME_ID_YEAR

			//Note Force v24 to v23, TDRC is a 1M relationship handled specially.
			// @TODO EQUALISATION
			forcev24Tov23[ID3v24Frames.FRAME_ID_RELATIVE_VOLUME_ADJUSTMENT2] =
				ID3v23Frames.FRAME_ID_V3_RELATIVE_VOLUME_ADJUSTMENT
			//Used to be a special frame now a text frame
			forcev24Tov23[ID3v24Frames.FRAME_ID_INVOLVED_PEOPLE] =
				ID3v23Frames.FRAME_ID_V3_INVOLVED_PEOPLE
			//No Mood frame in v23 so use a TXXX frame
			forcev24Tov23[ID3v24Frames.FRAME_ID_MOOD] =
				ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO
			//Release time can be mapped to release year (but can only hold year)
			forcev24Tov23[ID3v24Frames.FRAME_ID_ORIGINAL_RELEASE_TIME] =
				ID3v23Frames.FRAME_ID_V3_TORY
		}

		private fun loadID3v22ID3v23Mapping() {
			val iterator: Iterator<String>
			var key: String
			var value: String?

			// All v22 ids were renamed in v23, but are essentially the same
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_ACCOMPANIMENT] =
				ID3v23Frames.FRAME_ID_V3_ACCOMPANIMENT
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_ALBUM] =
				ID3v23Frames.FRAME_ID_V3_ALBUM
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_ARTIST] =
				ID3v23Frames.FRAME_ID_V3_ARTIST
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_AUDIO_ENCRYPTION] =
				ID3v23Frames.FRAME_ID_V3_AUDIO_ENCRYPTION
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_BPM] =
				ID3v23Frames.FRAME_ID_V3_BPM
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_COMMENT] =
				ID3v23Frames.FRAME_ID_V3_COMMENT
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_COMMENT] =
				ID3v23Frames.FRAME_ID_V3_COMMENT
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_COMPOSER] =
				ID3v23Frames.FRAME_ID_V3_COMPOSER
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_CONDUCTOR] =
				ID3v23Frames.FRAME_ID_V3_CONDUCTOR
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_CONTENT_GROUP_DESC] =
				ID3v23Frames.FRAME_ID_V3_CONTENT_GROUP_DESC
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_COPYRIGHTINFO] =
				ID3v23Frames.FRAME_ID_V3_COPYRIGHTINFO
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_ENCODEDBY] =
				ID3v23Frames.FRAME_ID_V3_ENCODEDBY
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_EQUALISATION] =
				ID3v23Frames.FRAME_ID_V3_EQUALISATION
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_EVENT_TIMING_CODES] =
				ID3v23Frames.FRAME_ID_V3_EVENT_TIMING_CODES
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_FILE_TYPE] =
				ID3v23Frames.FRAME_ID_V3_FILE_TYPE
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_GENERAL_ENCAPS_OBJECT] =
				ID3v23Frames.FRAME_ID_V3_GENERAL_ENCAPS_OBJECT
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_GENRE] =
				ID3v23Frames.FRAME_ID_V3_GENRE
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_HW_SW_SETTINGS] =
				ID3v23Frames.FRAME_ID_V3_HW_SW_SETTINGS
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_INITIAL_KEY] =
				ID3v23Frames.FRAME_ID_V3_INITIAL_KEY
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_IPLS] =
				ID3v23Frames.FRAME_ID_V3_INVOLVED_PEOPLE
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_ISRC] =
				ID3v23Frames.FRAME_ID_V3_ISRC
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_ITUNES_GROUPING] =
				ID3v23Frames.FRAME_ID_V3_ITUNES_GROUPING
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_LANGUAGE] =
				ID3v23Frames.FRAME_ID_V3_LANGUAGE
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_LENGTH] =
				ID3v23Frames.FRAME_ID_V3_LENGTH
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_LINKED_INFO] =
				ID3v23Frames.FRAME_ID_V3_LINKED_INFO
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_LYRICIST] =
				ID3v23Frames.FRAME_ID_V3_LYRICIST
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_MEDIA_TYPE] =
				ID3v23Frames.FRAME_ID_V3_MEDIA_TYPE
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_MOVEMENT] =
				ID3v23Frames.FRAME_ID_V3_MOVEMENT
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_MOVEMENT_NO] =
				ID3v23Frames.FRAME_ID_V3_MOVEMENT_NO
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_MPEG_LOCATION_LOOKUP_TABLE] =
				ID3v23Frames.FRAME_ID_V3_MPEG_LOCATION_LOOKUP_TABLE
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_MUSIC_CD_ID] =
				ID3v23Frames.FRAME_ID_V3_MUSIC_CD_ID
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_ORIGARTIST] =
				ID3v23Frames.FRAME_ID_V3_ORIGARTIST
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_ORIG_FILENAME] =
				ID3v23Frames.FRAME_ID_V3_ORIG_FILENAME
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_ORIG_LYRICIST] =
				ID3v23Frames.FRAME_ID_V3_ORIG_LYRICIST
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_ORIG_TITLE] =
				ID3v23Frames.FRAME_ID_V3_ORIG_TITLE
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_PLAYLIST_DELAY] =
				ID3v23Frames.FRAME_ID_V3_PLAYLIST_DELAY
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_PLAY_COUNTER] =
				ID3v23Frames.FRAME_ID_V3_PLAY_COUNTER
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_PLAY_COUNTER] =
				ID3v23Frames.FRAME_ID_V3_PLAY_COUNTER
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_POPULARIMETER] =
				ID3v23Frames.FRAME_ID_V3_POPULARIMETER
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_PUBLISHER] =
				ID3v23Frames.FRAME_ID_V3_PUBLISHER
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_RECOMMENDED_BUFFER_SIZE] =
				ID3v23Frames.FRAME_ID_V3_RECOMMENDED_BUFFER_SIZE
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_RECOMMENDED_BUFFER_SIZE] =
				ID3v23Frames.FRAME_ID_V3_RECOMMENDED_BUFFER_SIZE
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_RELATIVE_VOLUME_ADJUSTMENT] =
				ID3v23Frames.FRAME_ID_V3_RELATIVE_VOLUME_ADJUSTMENT
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_REMIXED] =
				ID3v23Frames.FRAME_ID_V3_REMIXED
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_REVERB] =
				ID3v23Frames.FRAME_ID_V3_REVERB
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_SET] =
				ID3v23Frames.FRAME_ID_V3_SET
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_SET_SUBTITLE] =
				ID3v23Frames.FRAME_ID_V3_SET_SUBTITLE
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_SYNC_LYRIC] =
				ID3v23Frames.FRAME_ID_V3_SYNC_LYRIC
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_SYNC_TEMPO] =
				ID3v23Frames.FRAME_ID_V3_SYNC_TEMPO
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_TDAT] =
				ID3v23Frames.FRAME_ID_V3_TDAT
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_TIME] =
				ID3v23Frames.FRAME_ID_V3_TIME
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_TITLE_REFINEMENT] =
				ID3v23Frames.FRAME_ID_V3_TITLE_REFINEMENT
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_TORY] =
				ID3v23Frames.FRAME_ID_V3_TORY
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_TRACK] =
				ID3v23Frames.FRAME_ID_V3_TRACK
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_TRDA] =
				ID3v23Frames.FRAME_ID_V3_TRDA
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_TSIZ] =
				ID3v23Frames.FRAME_ID_V3_TSIZ
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_TYER] =
				ID3v23Frames.FRAME_ID_V3_TYER
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_UNIQUE_FILE_ID] =
				ID3v23Frames.FRAME_ID_V3_UNIQUE_FILE_ID
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_UNIQUE_FILE_ID] =
				ID3v23Frames.FRAME_ID_V3_UNIQUE_FILE_ID
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_UNSYNC_LYRICS] =
				ID3v23Frames.FRAME_ID_V3_UNSYNC_LYRICS
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_URL_ARTIST_WEB] =
				ID3v23Frames.FRAME_ID_V3_URL_ARTIST_WEB
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_URL_COMMERCIAL] =
				ID3v23Frames.FRAME_ID_V3_URL_COMMERCIAL
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_URL_COPYRIGHT] =
				ID3v23Frames.FRAME_ID_V3_URL_COPYRIGHT
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_URL_FILE_WEB] =
				ID3v23Frames.FRAME_ID_V3_URL_FILE_WEB
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_URL_OFFICIAL_RADIO] =
				ID3v23Frames.FRAME_ID_V3_URL_OFFICIAL_RADIO
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_URL_PAYMENT] =
				ID3v23Frames.FRAME_ID_V3_URL_PAYMENT
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_URL_PUBLISHERS] =
				ID3v23Frames.FRAME_ID_V3_URL_PUBLISHERS
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_URL_SOURCE_WEB] =
				ID3v23Frames.FRAME_ID_V3_URL_SOURCE_WEB
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_USER_DEFINED_INFO] =
				ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_USER_DEFINED_URL] =
				ID3v23Frames.FRAME_ID_V3_USER_DEFINED_URL
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_TITLE] =
				ID3v23Frames.FRAME_ID_V3_TITLE
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_IS_COMPILATION] =
				ID3v23Frames.FRAME_ID_V3_IS_COMPILATION
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_TITLE_SORT_ORDER_ITUNES] =
				ID3v23Frames.FRAME_ID_V3_TITLE_SORT_ORDER_ITUNES
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_ARTIST_SORT_ORDER_ITUNES] =
				ID3v23Frames.FRAME_ID_V3_ARTIST_SORT_ORDER_ITUNES
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_ALBUM_SORT_ORDER_ITUNES] =
				ID3v23Frames.FRAME_ID_V3_ALBUM_SORT_ORDER_ITUNES
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_ALBUM_ARTIST_SORT_ORDER_ITUNES] =
				ID3v23Frames.FRAME_ID_V3_ALBUM_ARTIST_SORT_ORDER_ITUNES
			convertv22Tov23[ID3v22Frames.FRAME_ID_V2_COMPOSER_SORT_ORDER_ITUNES] =
				ID3v23Frames.FRAME_ID_V3_COMPOSER_SORT_ORDER_ITUNES

			// v23 to v22 The translation is both way
			iterator = convertv22Tov23.keys.iterator()
			while (iterator.hasNext()) {
				key = iterator.next()
				value = convertv22Tov23[key]
				convertv23Tov22[value] = key
			}

			//This one way translation allows us to convert XSOT to TST, but in the other direction gets converted to TSOT
			convertv23Tov22[ID3v23Frames.FRAME_ID_V3_TITLE_SORT_ORDER_MUSICBRAINZ] =
				ID3v22Frames.FRAME_ID_V2_TITLE_SORT_ORDER_ITUNES
			convertv23Tov22[ID3v23Frames.FRAME_ID_V3_ARTIST_SORT_ORDER_MUSICBRAINZ] =
				ID3v22Frames.FRAME_ID_V2_ARTIST_SORT_ORDER_ITUNES
			convertv23Tov22[ID3v23Frames.FRAME_ID_V3_ALBUM_SORT_ORDER_MUSICBRAINZ] =
				ID3v22Frames.FRAME_ID_V2_ALBUM_SORT_ORDER_ITUNES

			//TODO What does CRM Map to ?
			// Force v22 to v23,  Extra fields in v23 version
			forcev22Tov23[ID3v22Frames.FRAME_ID_V2_ATTACHED_PICTURE] =
				ID3v23Frames.FRAME_ID_V3_ATTACHED_PICTURE

			// Force v23 to v22
			forcev23Tov22[ID3v23Frames.FRAME_ID_V3_ATTACHED_PICTURE] =
				ID3v22Frames.FRAME_ID_V2_ATTACHED_PICTURE
		}

		init {
			loadID3v22ID3v23Mapping()
			loadID3v23ID3v24Mapping()
		}
	}
}
