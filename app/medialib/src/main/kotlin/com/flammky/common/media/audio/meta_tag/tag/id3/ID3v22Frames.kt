/*
 * Jaudiotagger Copyright (C)2004,2005
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 * or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not,
 * you can getFields a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.FieldKey
import java.util.*

/**
 * Defines ID3v22 frames and collections that categorise frames within an ID3v22 tag.
 *
 * You can include frames here that are not officially supported as long as they can be used within an
 * ID3v22Tag
 *
 * @author Paul Taylor
 * @version $Id$
 */
class ID3v22Frames private constructor() : ID3Frames() {
	/**
	 * Maps from Generic key to ID3 key
	 */
	protected var tagFieldToId3 = EnumMap<FieldKey, ID3v22FieldKey>(
		FieldKey::class.java
	)

	/**
	 * Maps from ID3 key to Generic key
	 */
	protected var id3ToTagField = EnumMap<ID3v22FieldKey, FieldKey>(
		ID3v22FieldKey::class.java
	)

	init {
		// The defined v22 frames
		supportedFrames.add(FRAME_ID_V2_ACCOMPANIMENT)
		supportedFrames.add(FRAME_ID_V2_ALBUM)
		supportedFrames.add(FRAME_ID_V2_ARTIST)
		supportedFrames.add(FRAME_ID_V2_ATTACHED_PICTURE)
		supportedFrames.add(FRAME_ID_V2_AUDIO_ENCRYPTION)
		supportedFrames.add(FRAME_ID_V2_BPM)
		supportedFrames.add(FRAME_ID_V2_COMMENT)
		supportedFrames.add(FRAME_ID_V2_COMPOSER)
		supportedFrames.add(FRAME_ID_V2_ENCRYPTED_FRAME)
		supportedFrames.add(FRAME_ID_V2_CONDUCTOR)
		supportedFrames.add(FRAME_ID_V2_CONTENT_GROUP_DESC)
		supportedFrames.add(FRAME_ID_V2_COPYRIGHTINFO)
		supportedFrames.add(FRAME_ID_V2_ENCODEDBY)
		supportedFrames.add(FRAME_ID_V2_EQUALISATION)
		supportedFrames.add(FRAME_ID_V2_EVENT_TIMING_CODES)
		supportedFrames.add(FRAME_ID_V2_FILE_TYPE)
		supportedFrames.add(FRAME_ID_V2_GENERAL_ENCAPS_OBJECT)
		supportedFrames.add(FRAME_ID_V2_GENRE)
		supportedFrames.add(FRAME_ID_V2_HW_SW_SETTINGS)
		supportedFrames.add(FRAME_ID_V2_INITIAL_KEY)
		supportedFrames.add(FRAME_ID_V2_IPLS)
		supportedFrames.add(FRAME_ID_V2_ISRC)
		supportedFrames.add(FRAME_ID_V2_ITUNES_GROUPING)
		supportedFrames.add(FRAME_ID_V2_LANGUAGE)
		supportedFrames.add(FRAME_ID_V2_LENGTH)
		supportedFrames.add(FRAME_ID_V2_LINKED_INFO)
		supportedFrames.add(FRAME_ID_V2_LYRICIST)
		supportedFrames.add(FRAME_ID_V2_MEDIA_TYPE)
		supportedFrames.add(FRAME_ID_V2_MOVEMENT)
		supportedFrames.add(FRAME_ID_V2_MOVEMENT_NO)
		supportedFrames.add(FRAME_ID_V2_MPEG_LOCATION_LOOKUP_TABLE)
		supportedFrames.add(FRAME_ID_V2_MUSIC_CD_ID)
		supportedFrames.add(FRAME_ID_V2_ORIGARTIST)
		supportedFrames.add(FRAME_ID_V2_ORIG_FILENAME)
		supportedFrames.add(FRAME_ID_V2_ORIG_LYRICIST)
		supportedFrames.add(FRAME_ID_V2_ORIG_TITLE)
		supportedFrames.add(FRAME_ID_V2_PLAYLIST_DELAY)
		supportedFrames.add(FRAME_ID_V2_PLAY_COUNTER)
		supportedFrames.add(FRAME_ID_V2_POPULARIMETER)
		supportedFrames.add(FRAME_ID_V2_PUBLISHER)
		supportedFrames.add(FRAME_ID_V2_RECOMMENDED_BUFFER_SIZE)
		supportedFrames.add(FRAME_ID_V2_RELATIVE_VOLUME_ADJUSTMENT)
		supportedFrames.add(FRAME_ID_V2_REMIXED)
		supportedFrames.add(FRAME_ID_V2_REVERB)
		supportedFrames.add(FRAME_ID_V2_SET)
		supportedFrames.add(FRAME_ID_V2_SYNC_LYRIC)
		supportedFrames.add(FRAME_ID_V2_SYNC_TEMPO)
		supportedFrames.add(FRAME_ID_V2_TDAT)
		supportedFrames.add(FRAME_ID_V2_TIME)
		supportedFrames.add(FRAME_ID_V2_TITLE)
		supportedFrames.add(FRAME_ID_V2_TITLE_REFINEMENT)
		supportedFrames.add(FRAME_ID_V2_TORY)
		supportedFrames.add(FRAME_ID_V2_TRACK)
		supportedFrames.add(FRAME_ID_V2_TRDA)
		supportedFrames.add(FRAME_ID_V2_TSIZ)
		supportedFrames.add(FRAME_ID_V2_TYER)
		supportedFrames.add(FRAME_ID_V2_UNIQUE_FILE_ID)
		supportedFrames.add(FRAME_ID_V2_UNSYNC_LYRICS)
		supportedFrames.add(FRAME_ID_V2_URL_ARTIST_WEB)
		supportedFrames.add(FRAME_ID_V2_URL_COMMERCIAL)
		supportedFrames.add(FRAME_ID_V2_URL_COPYRIGHT)
		supportedFrames.add(FRAME_ID_V2_URL_FILE_WEB)
		supportedFrames.add(FRAME_ID_V2_URL_OFFICIAL_RADIO)
		supportedFrames.add(FRAME_ID_V2_URL_PAYMENT)
		supportedFrames.add(FRAME_ID_V2_URL_PUBLISHERS)
		supportedFrames.add(FRAME_ID_V2_URL_SOURCE_WEB)
		supportedFrames.add(FRAME_ID_V2_USER_DEFINED_INFO)
		supportedFrames.add(FRAME_ID_V2_USER_DEFINED_URL)

		//Extension
		extensionFrames.add(FRAME_ID_V2_IS_COMPILATION)
		extensionFrames.add(FRAME_ID_V2_TITLE_SORT_ORDER_ITUNES)
		extensionFrames.add(FRAME_ID_V2_ARTIST_SORT_ORDER_ITUNES)
		extensionFrames.add(FRAME_ID_V2_ALBUM_SORT_ORDER_ITUNES)
		extensionFrames.add(FRAME_ID_V2_ALBUM_ARTIST_SORT_ORDER_ITUNES)
		extensionFrames.add(FRAME_ID_V2_COMPOSER_SORT_ORDER_ITUNES)

		//Common
		commonFrames.add(FRAME_ID_V2_ARTIST)
		commonFrames.add(FRAME_ID_V2_ALBUM)
		commonFrames.add(FRAME_ID_V2_TITLE)
		commonFrames.add(FRAME_ID_V2_GENRE)
		commonFrames.add(FRAME_ID_V2_TRACK)
		commonFrames.add(FRAME_ID_V2_TYER)
		commonFrames.add(FRAME_ID_V2_COMMENT)

		//Binary
		binaryFrames.add(FRAME_ID_V2_ATTACHED_PICTURE)
		binaryFrames.add(FRAME_ID_V2_AUDIO_ENCRYPTION)
		binaryFrames.add(FRAME_ID_V2_ENCRYPTED_FRAME)
		binaryFrames.add(FRAME_ID_V2_EQUALISATION)
		binaryFrames.add(FRAME_ID_V2_EVENT_TIMING_CODES)
		binaryFrames.add(FRAME_ID_V2_GENERAL_ENCAPS_OBJECT)
		binaryFrames.add(FRAME_ID_V2_RELATIVE_VOLUME_ADJUSTMENT)
		binaryFrames.add(FRAME_ID_V2_RECOMMENDED_BUFFER_SIZE)
		binaryFrames.add(FRAME_ID_V2_UNIQUE_FILE_ID)

		// Map frameid to a name
		idToValue[FRAME_ID_V2_ACCOMPANIMENT] =
			"Text: Band/Orchestra/Accompaniment"
		idToValue[FRAME_ID_V2_ALBUM] = "Text: Album/Movie/Show title"
		idToValue[FRAME_ID_V2_ARTIST] =
			"Text: Lead artist(s)/Lead performer(s)/Soloist(s)/Performing group"
		idToValue[FRAME_ID_V2_ATTACHED_PICTURE] = "Attached picture"
		idToValue[FRAME_ID_V2_AUDIO_ENCRYPTION] =
			"Audio encryption"
		idToValue[FRAME_ID_V2_BPM] =
			"Text: BPM (Beats Per Minute)"
		idToValue[FRAME_ID_V2_COMMENT] =
			"Comments"
		idToValue[FRAME_ID_V2_COMPOSER] =
			"Text: Composer"
		idToValue[FRAME_ID_V2_CONDUCTOR] = "Text: Conductor/Performer refinement"
		idToValue[FRAME_ID_V2_CONTENT_GROUP_DESC] = "Text: Content group description"
		idToValue[FRAME_ID_V2_COPYRIGHTINFO] = "Text: Copyright message"
		idToValue[FRAME_ID_V2_ENCODEDBY] =
			"Text: Encoded by"
		idToValue[FRAME_ID_V2_ENCRYPTED_FRAME] =
			"Encrypted meta frame"
		idToValue[FRAME_ID_V2_EQUALISATION] = "Equalization"
		idToValue[FRAME_ID_V2_EVENT_TIMING_CODES] =
			"Event timing codes"
		idToValue[FRAME_ID_V2_FILE_TYPE] = "Text: File type"
		idToValue[FRAME_ID_V2_GENERAL_ENCAPS_OBJECT] = "General encapsulated datatype"
		idToValue[FRAME_ID_V2_GENRE] = "Text: Content type"
		idToValue[FRAME_ID_V2_HW_SW_SETTINGS] =
			"Text: Software/hardware and settings used for encoding"
		idToValue[FRAME_ID_V2_INITIAL_KEY] =
			"Text: Initial key"
		idToValue[FRAME_ID_V2_IPLS] = "Involved people list"
		idToValue[FRAME_ID_V2_ISRC] = "Text: ISRC (International Standard Recording Code)"
		idToValue[FRAME_ID_V2_ITUNES_GROUPING] =
			"iTunes Grouping"
		idToValue[FRAME_ID_V2_LANGUAGE] = "Text: Language(s)"
		idToValue[FRAME_ID_V2_LENGTH] =
			"Text: Length"
		idToValue[FRAME_ID_V2_LINKED_INFO] =
			"Linked information"
		idToValue[FRAME_ID_V2_LYRICIST] =
			"Text: Lyricist/text writer"
		idToValue[FRAME_ID_V2_MEDIA_TYPE] =
			"Text: Media type"
		idToValue[FRAME_ID_V2_MOVEMENT] =
			"Text: Movement"
		idToValue[FRAME_ID_V2_MOVEMENT_NO] =
			"Text: Movement No"
		idToValue[FRAME_ID_V2_MPEG_LOCATION_LOOKUP_TABLE] =
			"MPEG location lookup table"
		idToValue[FRAME_ID_V2_MUSIC_CD_ID] = "Music CD Identifier"
		idToValue[FRAME_ID_V2_ORIGARTIST] =
			"Text: Original artist(s)/performer(s)"
		idToValue[FRAME_ID_V2_ORIG_FILENAME] =
			"Text: Original filename"
		idToValue[FRAME_ID_V2_ORIG_LYRICIST] =
			"Text: Original Lyricist(s)/text writer(s)"
		idToValue[FRAME_ID_V2_ORIG_TITLE] =
			"Text: Original album/Movie/Show title"
		idToValue[FRAME_ID_V2_PLAYLIST_DELAY] =
			"Text: Playlist delay"
		idToValue[FRAME_ID_V2_PLAY_COUNTER] =
			"Play counter"
		idToValue[FRAME_ID_V2_POPULARIMETER] =
			"Popularimeter"
		idToValue[FRAME_ID_V2_PUBLISHER] =
			"Text: Publisher"
		idToValue[FRAME_ID_V2_RECOMMENDED_BUFFER_SIZE] = "Recommended buffer size"
		idToValue[FRAME_ID_V2_RELATIVE_VOLUME_ADJUSTMENT] = "Relative volume adjustment"
		idToValue[FRAME_ID_V2_REMIXED] =
			"Text: Interpreted, remixed, or otherwise modified by"
		idToValue[FRAME_ID_V2_REVERB] =
			"Reverb"
		idToValue[FRAME_ID_V2_SET] = "Text: Part of a setField"
		idToValue[FRAME_ID_V2_SET_SUBTITLE] = "Text: Set subtitle"
		idToValue[FRAME_ID_V2_SYNC_LYRIC] = "Synchronized lyric/text"
		idToValue[FRAME_ID_V2_SYNC_TEMPO] =
			"Synced tempo codes"
		idToValue[FRAME_ID_V2_TDAT] = "Text: Date"
		idToValue[FRAME_ID_V2_TIME] =
			"Text: Time"
		idToValue[FRAME_ID_V2_TITLE] =
			"Text: Title/Songname/Content description"
		idToValue[FRAME_ID_V2_TITLE_REFINEMENT] =
			"Text: Subtitle/Description refinement"
		idToValue[FRAME_ID_V2_TORY] =
			"Text: Original release year"
		idToValue[FRAME_ID_V2_TRACK] =
			"Text: Track number/Position in setField"
		idToValue[FRAME_ID_V2_TRDA] =
			"Text: Recording dates"
		idToValue[FRAME_ID_V2_TSIZ] =
			"Text: Size"
		idToValue[FRAME_ID_V2_TYER] =
			"Text: Year"
		idToValue[FRAME_ID_V2_UNIQUE_FILE_ID] = "Unique file identifier"
		idToValue[FRAME_ID_V2_UNSYNC_LYRICS] = "Unsychronized lyric/text transcription"
		idToValue[FRAME_ID_V2_URL_ARTIST_WEB] =
			"URL: Official artist/performer webpage"
		idToValue[FRAME_ID_V2_URL_COMMERCIAL] = "URL: Commercial information"
		idToValue[FRAME_ID_V2_URL_COPYRIGHT] = "URL: Copyright/Legal information"
		idToValue[FRAME_ID_V2_URL_FILE_WEB] = "URL: Official audio file webpage"
		idToValue[FRAME_ID_V2_URL_OFFICIAL_RADIO] = "URL: Official radio station"
		idToValue[FRAME_ID_V2_URL_PAYMENT] =
			"URL: Official payment site"
		idToValue[FRAME_ID_V2_URL_PUBLISHERS] = "URL: Publishers official webpage"
		idToValue[FRAME_ID_V2_URL_SOURCE_WEB] = "URL: Official audio source webpage"
		idToValue[FRAME_ID_V2_USER_DEFINED_INFO] = "User defined text information frame"
		idToValue[FRAME_ID_V2_USER_DEFINED_URL] =
			"User defined URL link frame"
		idToValue[FRAME_ID_V2_IS_COMPILATION] =
			"Is Compilation"
		idToValue[FRAME_ID_V2_TITLE_SORT_ORDER_ITUNES] = "Text: title sort order"
		idToValue[FRAME_ID_V2_ARTIST_SORT_ORDER_ITUNES] =
			"Text: artist sort order"
		idToValue[FRAME_ID_V2_ALBUM_SORT_ORDER_ITUNES] =
			"Text: album sort order"
		idToValue[FRAME_ID_V2_ALBUM_ARTIST_SORT_ORDER_ITUNES] = "Text:Album Artist Sort Order Frame"
		idToValue[FRAME_ID_V2_COMPOSER_SORT_ORDER_ITUNES] =
			"Text:Composer Sort Order Frame"
		createMaps()
		multipleFrames.add(FRAME_ID_V2_ATTACHED_PICTURE)
		multipleFrames.add(FRAME_ID_V2_UNIQUE_FILE_ID)
		multipleFrames.add(FRAME_ID_V2_POPULARIMETER)
		multipleFrames.add(FRAME_ID_V2_USER_DEFINED_INFO)
		multipleFrames.add(FRAME_ID_V2_USER_DEFINED_URL)
		multipleFrames.add(FRAME_ID_V2_COMMENT)
		multipleFrames.add(FRAME_ID_V2_UNSYNC_LYRICS)
		multipleFrames.add(FRAME_ID_V2_GENERAL_ENCAPS_OBJECT)
		multipleFrames.add(FRAME_ID_V2_URL_ARTIST_WEB)

		//Mapping generic key to id3v22 key
		tagFieldToId3[FieldKey.ACOUSTID_FINGERPRINT] =
			ID3v22FieldKey.ACOUSTID_FINGERPRINT
		tagFieldToId3[FieldKey.ACOUSTID_ID] = ID3v22FieldKey.ACOUSTID_ID
		tagFieldToId3[FieldKey.ALBUM] =
			ID3v22FieldKey.ALBUM
		tagFieldToId3[FieldKey.ALBUM_ARTIST] = ID3v22FieldKey.ALBUM_ARTIST
		tagFieldToId3[FieldKey.ALBUM_ARTIST_SORT] =
			ID3v22FieldKey.ALBUM_ARTIST_SORT
		tagFieldToId3[FieldKey.ALBUM_ARTISTS] =
			ID3v22FieldKey.ALBUM_ARTISTS
		tagFieldToId3[FieldKey.ALBUM_ARTISTS_SORT] =
			ID3v22FieldKey.ALBUM_ARTISTS_SORT
		tagFieldToId3[FieldKey.ALBUM_SORT] = ID3v22FieldKey.ALBUM_SORT
		tagFieldToId3[FieldKey.ALBUM_YEAR] =
			ID3v22FieldKey.ALBUM_YEAR
		tagFieldToId3[FieldKey.AMAZON_ID] =
			ID3v22FieldKey.AMAZON_ID
		tagFieldToId3[FieldKey.ARRANGER] =
			ID3v22FieldKey.ARRANGER
		tagFieldToId3[FieldKey.ARRANGER_SORT] =
			ID3v22FieldKey.ARRANGER_SORT
		tagFieldToId3[FieldKey.ARTIST] = ID3v22FieldKey.ARTIST
		tagFieldToId3[FieldKey.ARTISTS] =
			ID3v22FieldKey.ARTISTS
		tagFieldToId3[FieldKey.ARTISTS_SORT] =
			ID3v22FieldKey.ARTISTS_SORT
		tagFieldToId3[FieldKey.ARTIST_SORT] =
			ID3v22FieldKey.ARTIST_SORT
		tagFieldToId3[FieldKey.BARCODE] = ID3v22FieldKey.BARCODE
		tagFieldToId3[FieldKey.BPM] =
			ID3v22FieldKey.BPM
		tagFieldToId3[FieldKey.CATALOG_NO] =
			ID3v22FieldKey.CATALOG_NO
		tagFieldToId3[FieldKey.CHOIR] =
			ID3v22FieldKey.CHOIR
		tagFieldToId3[FieldKey.CHOIR_SORT] = ID3v22FieldKey.CHOIR_SORT
		tagFieldToId3[FieldKey.CLASSICAL_CATALOG] =
			ID3v22FieldKey.CLASSICAL_CATALOG
		tagFieldToId3[FieldKey.CLASSICAL_NICKNAME] =
			ID3v22FieldKey.CLASSICAL_NICKNAME
		tagFieldToId3[FieldKey.COMMENT] =
			ID3v22FieldKey.COMMENT
		tagFieldToId3[FieldKey.COMPOSER] =
			ID3v22FieldKey.COMPOSER
		tagFieldToId3[FieldKey.COMPOSER_SORT] =
			ID3v22FieldKey.COMPOSER_SORT
		tagFieldToId3[FieldKey.CONDUCTOR] =
			ID3v22FieldKey.CONDUCTOR
		tagFieldToId3[FieldKey.CONDUCTOR_SORT] =
			ID3v22FieldKey.CONDUCTOR_SORT
		tagFieldToId3[FieldKey.COUNTRY] = ID3v22FieldKey.COUNTRY
		tagFieldToId3[FieldKey.COPYRIGHT] =
			ID3v22FieldKey.COPYRIGHT
		tagFieldToId3[FieldKey.COVER_ART] = ID3v22FieldKey.COVER_ART
		tagFieldToId3[FieldKey.CUSTOM1] =
			ID3v22FieldKey.CUSTOM1
		tagFieldToId3[FieldKey.CUSTOM2] =
			ID3v22FieldKey.CUSTOM2
		tagFieldToId3[FieldKey.CUSTOM3] =
			ID3v22FieldKey.CUSTOM3
		tagFieldToId3[FieldKey.CUSTOM4] = ID3v22FieldKey.CUSTOM4
		tagFieldToId3[FieldKey.CUSTOM5] = ID3v22FieldKey.CUSTOM5
		tagFieldToId3[FieldKey.DISC_NO] =
			ID3v22FieldKey.DISC_NO
		tagFieldToId3[FieldKey.DISC_SUBTITLE] =
			ID3v22FieldKey.DISC_SUBTITLE
		tagFieldToId3[FieldKey.DISC_TOTAL] =
			ID3v22FieldKey.DISC_NO
		tagFieldToId3[FieldKey.DJMIXER] =
			ID3v22FieldKey.DJMIXER
		tagFieldToId3[FieldKey.DJMIXER_SORT] =
			ID3v22FieldKey.DJMIXER_SORT
		tagFieldToId3[FieldKey.ENCODER] =
			ID3v22FieldKey.ENCODER
		tagFieldToId3[FieldKey.ENGINEER] =
			ID3v22FieldKey.ENGINEER
		tagFieldToId3[FieldKey.ENGINEER_SORT] =
			ID3v22FieldKey.ENGINEER_SORT
		tagFieldToId3[FieldKey.ENSEMBLE] =
			ID3v22FieldKey.ENSEMBLE
		tagFieldToId3[FieldKey.ENSEMBLE_SORT] =
			ID3v22FieldKey.ENSEMBLE_SORT
		tagFieldToId3[FieldKey.FBPM] = ID3v22FieldKey.FBPM
		tagFieldToId3[FieldKey.GENRE] =
			ID3v22FieldKey.GENRE
		tagFieldToId3[FieldKey.GROUP] =
			ID3v22FieldKey.GROUP
		tagFieldToId3[FieldKey.GROUPING] =
			ID3v22FieldKey.GROUPING
		tagFieldToId3[FieldKey.INSTRUMENT] = ID3v22FieldKey.INSTRUMENT
		tagFieldToId3[FieldKey.INVOLVEDPEOPLE] =
			ID3v22FieldKey.INVOLVEDPEOPLE
		tagFieldToId3[FieldKey.IPI] =
			ID3v22FieldKey.IPI
		tagFieldToId3[FieldKey.ISRC] =
			ID3v22FieldKey.ISRC
		tagFieldToId3[FieldKey.ISWC] =
			ID3v22FieldKey.ISWC
		tagFieldToId3[FieldKey.IS_CLASSICAL] =
			ID3v22FieldKey.IS_CLASSICAL
		tagFieldToId3[FieldKey.IS_COMPILATION] =
			ID3v22FieldKey.IS_COMPILATION
		tagFieldToId3[FieldKey.IS_GREATEST_HITS] =
			ID3v22FieldKey.IS_GREATEST_HITS
		tagFieldToId3[FieldKey.IS_HD] =
			ID3v22FieldKey.IS_HD
		tagFieldToId3[FieldKey.IS_LIVE] =
			ID3v22FieldKey.IS_LIVE
		tagFieldToId3[FieldKey.IS_SOUNDTRACK] =
			ID3v22FieldKey.IS_SOUNDTRACK
		tagFieldToId3[FieldKey.ITUNES_GROUPING] =
			ID3v22FieldKey.ITUNES_GROUPING
		tagFieldToId3[FieldKey.JAIKOZ_ID] = ID3v22FieldKey.JAIKOZ_ID
		tagFieldToId3[FieldKey.KEY] =
			ID3v22FieldKey.KEY
		tagFieldToId3[FieldKey.LANGUAGE] =
			ID3v22FieldKey.LANGUAGE
		tagFieldToId3[FieldKey.LYRICIST] =
			ID3v22FieldKey.LYRICIST
		tagFieldToId3[FieldKey.LYRICIST_SORT] =
			ID3v22FieldKey.LYRICIST_SORT
		tagFieldToId3[FieldKey.LYRICS] =
			ID3v22FieldKey.LYRICS
		tagFieldToId3[FieldKey.MEDIA] =
			ID3v22FieldKey.MEDIA
		tagFieldToId3[FieldKey.MIXER] =
			ID3v22FieldKey.MIXER
		tagFieldToId3[FieldKey.MIXER_SORT] =
			ID3v22FieldKey.MIXER_SORT
		tagFieldToId3[FieldKey.MOOD] =
			ID3v22FieldKey.MOOD
		tagFieldToId3[FieldKey.MOOD_ACOUSTIC] =
			ID3v22FieldKey.MOOD_ACOUSTIC
		tagFieldToId3[FieldKey.MOOD_AGGRESSIVE] =
			ID3v22FieldKey.MOOD_AGGRESSIVE
		tagFieldToId3[FieldKey.MOOD_AROUSAL] =
			ID3v22FieldKey.MOOD_AROUSAL
		tagFieldToId3[FieldKey.MOOD_DANCEABILITY] =
			ID3v22FieldKey.MOOD_DANCEABILITY
		tagFieldToId3[FieldKey.MOOD_ELECTRONIC] =
			ID3v22FieldKey.MOOD_ELECTRONIC
		tagFieldToId3[FieldKey.MOOD_HAPPY] =
			ID3v22FieldKey.MOOD_HAPPY
		tagFieldToId3[FieldKey.MOOD_INSTRUMENTAL] =
			ID3v22FieldKey.MOOD_INSTRUMENTAL
		tagFieldToId3[FieldKey.MOOD_PARTY] = ID3v22FieldKey.MOOD_PARTY
		tagFieldToId3[FieldKey.MOOD_RELAXED] =
			ID3v22FieldKey.MOOD_RELAXED
		tagFieldToId3[FieldKey.MOOD_SAD] =
			ID3v22FieldKey.MOOD_SAD
		tagFieldToId3[FieldKey.MOOD_VALENCE] =
			ID3v22FieldKey.MOOD_VALENCE
		tagFieldToId3[FieldKey.MOVEMENT] =
			ID3v22FieldKey.MOVEMENT
		tagFieldToId3[FieldKey.MOVEMENT_NO] =
			ID3v22FieldKey.MOVEMENT_NO
		tagFieldToId3[FieldKey.MOVEMENT_TOTAL] =
			ID3v22FieldKey.MOVEMENT_TOTAL
		tagFieldToId3[FieldKey.MUSICBRAINZ_ARTISTID] =
			ID3v22FieldKey.MUSICBRAINZ_ARTISTID
		tagFieldToId3[FieldKey.MUSICBRAINZ_DISC_ID] =
			ID3v22FieldKey.MUSICBRAINZ_DISC_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_ORIGINAL_RELEASE_ID] =
			ID3v22FieldKey.MUSICBRAINZ_ORIGINAL_RELEASEID
		tagFieldToId3[FieldKey.MUSICBRAINZ_RELEASEARTISTID] =
			ID3v22FieldKey.MUSICBRAINZ_RELEASEARTISTID
		tagFieldToId3[FieldKey.MUSICBRAINZ_RELEASEID] =
			ID3v22FieldKey.MUSICBRAINZ_RELEASEID
		tagFieldToId3[FieldKey.MUSICBRAINZ_RELEASE_COUNTRY] =
			ID3v22FieldKey.MUSICBRAINZ_RELEASE_COUNTRY
		tagFieldToId3[FieldKey.MUSICBRAINZ_RELEASE_GROUP_ID] =
			ID3v22FieldKey.MUSICBRAINZ_RELEASE_GROUP_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_RELEASE_STATUS] =
			ID3v22FieldKey.MUSICBRAINZ_RELEASE_STATUS
		tagFieldToId3[FieldKey.MUSICBRAINZ_RELEASE_TRACK_ID] =
			ID3v22FieldKey.MUSICBRAINZ_RELEASE_TRACK_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_RELEASE_TYPE] =
			ID3v22FieldKey.MUSICBRAINZ_RELEASE_TYPE
		tagFieldToId3[FieldKey.MUSICBRAINZ_TRACK_ID] =
			ID3v22FieldKey.MUSICBRAINZ_TRACK_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK] =
			ID3v22FieldKey.MUSICBRAINZ_WORK
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_ID] =
			ID3v22FieldKey.MUSICBRAINZ_WORK_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_RECORDING_WORK_ID] =
			ID3v22FieldKey.MUSICBRAINZ_RECORDING_WORK_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL1_ID] =
			ID3v22FieldKey.MUSICBRAINZ_WORK_PART_LEVEL1_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL2_ID] =
			ID3v22FieldKey.MUSICBRAINZ_WORK_PART_LEVEL2_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL3_ID] =
			ID3v22FieldKey.MUSICBRAINZ_WORK_PART_LEVEL3_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL4_ID] =
			ID3v22FieldKey.MUSICBRAINZ_WORK_PART_LEVEL4_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL5_ID] =
			ID3v22FieldKey.MUSICBRAINZ_WORK_PART_LEVEL5_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL6_ID] =
			ID3v22FieldKey.MUSICBRAINZ_WORK_PART_LEVEL6_ID
		tagFieldToId3[FieldKey.MUSICIP_ID] =
			ID3v22FieldKey.MUSICIP_ID
		tagFieldToId3[FieldKey.OCCASION] =
			ID3v22FieldKey.OCCASION
		tagFieldToId3[FieldKey.OPUS] = ID3v22FieldKey.OPUS
		tagFieldToId3[FieldKey.ORCHESTRA] =
			ID3v22FieldKey.ORCHESTRA
		tagFieldToId3[FieldKey.ORCHESTRA_SORT] =
			ID3v22FieldKey.ORCHESTRA_SORT
		tagFieldToId3[FieldKey.ORIGINAL_ALBUM] =
			ID3v22FieldKey.ORIGINAL_ALBUM
		tagFieldToId3[FieldKey.ORIGINALRELEASEDATE] =
			ID3v22FieldKey.ORIGINALRELEASEDATE
		tagFieldToId3[FieldKey.ORIGINAL_ARTIST] =
			ID3v22FieldKey.ORIGINAL_ARTIST
		tagFieldToId3[FieldKey.ORIGINAL_LYRICIST] =
			ID3v22FieldKey.ORIGINAL_LYRICIST
		tagFieldToId3[FieldKey.ORIGINAL_YEAR] =
			ID3v22FieldKey.ORIGINAL_YEAR
		tagFieldToId3[FieldKey.OVERALL_WORK] =
			ID3v22FieldKey.OVERALL_WORK
		tagFieldToId3[FieldKey.PART] =
			ID3v22FieldKey.PART
		tagFieldToId3[FieldKey.PART_NUMBER] =
			ID3v22FieldKey.PART_NUMBER
		tagFieldToId3[FieldKey.PART_TYPE] = ID3v22FieldKey.PART_TYPE
		tagFieldToId3[FieldKey.PERFORMER] = ID3v22FieldKey.PERFORMER
		tagFieldToId3[FieldKey.PERFORMER_NAME] =
			ID3v22FieldKey.PERFORMER_NAME
		tagFieldToId3[FieldKey.PERFORMER_NAME_SORT] =
			ID3v22FieldKey.PERFORMER_NAME_SORT
		tagFieldToId3[FieldKey.PERIOD] =
			ID3v22FieldKey.PERIOD
		tagFieldToId3[FieldKey.PRODUCER] =
			ID3v22FieldKey.PRODUCER
		tagFieldToId3[FieldKey.PRODUCER_SORT] =
			ID3v22FieldKey.PRODUCER_SORT
		tagFieldToId3[FieldKey.QUALITY] =
			ID3v22FieldKey.QUALITY
		tagFieldToId3[FieldKey.RANKING] =
			ID3v22FieldKey.RANKING
		tagFieldToId3[FieldKey.RATING] =
			ID3v22FieldKey.RATING
		tagFieldToId3[FieldKey.RECORD_LABEL] =
			ID3v22FieldKey.RECORD_LABEL
		tagFieldToId3[FieldKey.RECORDINGDATE] =
			ID3v22FieldKey.RECORDINGDATE
		tagFieldToId3[FieldKey.RECORDINGSTARTDATE] =
			ID3v22FieldKey.RECORDINGSTARTDATE
		tagFieldToId3[FieldKey.RECORDINGENDDATE] =
			ID3v22FieldKey.RECORDINGENDDATE
		tagFieldToId3[FieldKey.RECORDINGLOCATION] =
			ID3v22FieldKey.RECORDINGLOCATION
		tagFieldToId3[FieldKey.REMIXER] =
			ID3v22FieldKey.REMIXER
		tagFieldToId3[FieldKey.ROONALBUMTAG] =
			ID3v22FieldKey.ROONALBUMTAG
		tagFieldToId3[FieldKey.ROONTRACKTAG] = ID3v22FieldKey.ROONTRACKTAG
		tagFieldToId3[FieldKey.SCRIPT] =
			ID3v22FieldKey.SCRIPT
		tagFieldToId3[FieldKey.SECTION] = ID3v22FieldKey.SECTION
		tagFieldToId3[FieldKey.SINGLE_DISC_TRACK_NO] =
			ID3v22FieldKey.SINGLE_DISC_TRACK_NO
		tagFieldToId3[FieldKey.SONGKONG_ID] =
			ID3v22FieldKey.SONGKONG_ID
		tagFieldToId3[FieldKey.SUBTITLE] = ID3v22FieldKey.SUBTITLE
		tagFieldToId3[FieldKey.TAGS] =
			ID3v22FieldKey.TAGS
		tagFieldToId3[FieldKey.TEMPO] =
			ID3v22FieldKey.TEMPO
		tagFieldToId3[FieldKey.TIMBRE] =
			ID3v22FieldKey.TIMBRE
		tagFieldToId3[FieldKey.TITLE] = ID3v22FieldKey.TITLE
		tagFieldToId3[FieldKey.TITLE_MOVEMENT] =
			ID3v22FieldKey.TITLE_MOVEMENT
		tagFieldToId3[FieldKey.TITLE_SORT] = ID3v22FieldKey.TITLE_SORT
		tagFieldToId3[FieldKey.TONALITY] = ID3v22FieldKey.TONALITY
		tagFieldToId3[FieldKey.TRACK] =
			ID3v22FieldKey.TRACK
		tagFieldToId3[FieldKey.TRACK_TOTAL] = ID3v22FieldKey.TRACK_TOTAL
		tagFieldToId3[FieldKey.URL_DISCOGS_ARTIST_SITE] =
			ID3v22FieldKey.URL_DISCOGS_ARTIST_SITE
		tagFieldToId3[FieldKey.URL_DISCOGS_RELEASE_SITE] =
			ID3v22FieldKey.URL_DISCOGS_RELEASE_SITE
		tagFieldToId3[FieldKey.URL_LYRICS_SITE] =
			ID3v22FieldKey.URL_LYRICS_SITE
		tagFieldToId3[FieldKey.URL_OFFICIAL_ARTIST_SITE] =
			ID3v22FieldKey.URL_OFFICIAL_ARTIST_SITE
		tagFieldToId3[FieldKey.URL_OFFICIAL_RELEASE_SITE] =
			ID3v22FieldKey.URL_OFFICIAL_RELEASE_SITE
		tagFieldToId3[FieldKey.URL_WIKIPEDIA_ARTIST_SITE] =
			ID3v22FieldKey.URL_WIKIPEDIA_ARTIST_SITE
		tagFieldToId3[FieldKey.URL_WIKIPEDIA_RELEASE_SITE] =
			ID3v22FieldKey.URL_WIKIPEDIA_RELEASE_SITE
		tagFieldToId3[FieldKey.VERSION] =
			ID3v22FieldKey.VERSION
		tagFieldToId3[FieldKey.WORK] =
			ID3v22FieldKey.WORK
		tagFieldToId3[FieldKey.MUSICBRAINZ_RECORDING_WORK] =
			ID3v22FieldKey.MUSICBRAINZ_RECORDING_WORK
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL1] =
			ID3v22FieldKey.WORK_PART_LEVEL1
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL1_TYPE] =
			ID3v22FieldKey.WORK_PART_LEVEL1_TYPE
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL2] =
			ID3v22FieldKey.WORK_PART_LEVEL2
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL2_TYPE] =
			ID3v22FieldKey.WORK_PART_LEVEL2_TYPE
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL3] =
			ID3v22FieldKey.WORK_PART_LEVEL3
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL3_TYPE] =
			ID3v22FieldKey.WORK_PART_LEVEL3_TYPE
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL4] =
			ID3v22FieldKey.WORK_PART_LEVEL4
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL4_TYPE] =
			ID3v22FieldKey.WORK_PART_LEVEL4_TYPE
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL5] =
			ID3v22FieldKey.WORK_PART_LEVEL5
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL5_TYPE] =
			ID3v22FieldKey.WORK_PART_LEVEL5_TYPE
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL6] =
			ID3v22FieldKey.WORK_PART_LEVEL6
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL6_TYPE] =
			ID3v22FieldKey.WORK_PART_LEVEL6_TYPE
		tagFieldToId3[FieldKey.WORK_TYPE] = ID3v22FieldKey.WORK_TYPE
		tagFieldToId3[FieldKey.YEAR] =
			ID3v22FieldKey.YEAR
		populateId3ToTagField()
	}

	private fun populateId3ToTagField() {
		for ((key, value) in tagFieldToId3) {
			id3ToTagField[value] = key
		}
	}

	override fun setITunes12_6WorkGroupingMode(on: Boolean) {
		if (on) {
			tagFieldToId3[FieldKey.WORK] =
				ID3v22FieldKey.GROUPING
			tagFieldToId3[FieldKey.GROUPING] =
				ID3v22FieldKey.ITUNES_GROUPING
		} else {
			tagFieldToId3[FieldKey.WORK] = ID3v22FieldKey.WORK
			tagFieldToId3[FieldKey.GROUPING] =
				ID3v22FieldKey.GROUPING
		}
		populateId3ToTagField()
	}

	/**
	 * @param genericKey
	 * @return id3 key for generic key
	 */
	fun getId3KeyFromGenericKey(genericKey: FieldKey): ID3v22FieldKey? {
		return tagFieldToId3[genericKey]
	}

	/**
	 * Get generic key for ID3 field key
	 * @param fieldKey
	 * @return
	 */
	fun getGenericKeyFromId3(fieldKey: ID3v22FieldKey): FieldKey? {
		return id3ToTagField[fieldKey]
	}

	companion object {
		//V2 Frames (only 3 chars)
		const val FRAME_ID_V2_ACCOMPANIMENT = "TP2"
		const val FRAME_ID_V2_ALBUM = "TAL"
		const val FRAME_ID_V2_ARTIST = "TP1"

		/**
		 * The ID3v2 frame identifier
		 *
		 * @return the ID3v2 frame identifier  for this frame type
		 */
		const val FRAME_ID_V2_ATTACHED_PICTURE = "PIC"
		const val FRAME_ID_V2_AUDIO_ENCRYPTION = "CRA"
		const val FRAME_ID_V2_BPM = "TBP"
		const val FRAME_ID_V2_COMMENT = "COM"
		const val FRAME_ID_V2_COMPOSER = "TCM"
		const val FRAME_ID_V2_CONDUCTOR = "TP3"
		const val FRAME_ID_V2_CONTENT_GROUP_DESC = "TT1"
		const val FRAME_ID_V2_COPYRIGHTINFO = "TCR"
		const val FRAME_ID_V2_ENCODEDBY = "TEN"

		/**
		 * The ID3v2 frame identifier
		 *
		 * @return the ID3v2 frame identifier  for this frame type
		 */
		const val FRAME_ID_V2_ENCRYPTED_FRAME = "CRM"
		const val FRAME_ID_V2_EQUALISATION = "EQU"
		const val FRAME_ID_V2_EVENT_TIMING_CODES = "ETC"
		const val FRAME_ID_V2_FILE_TYPE = "TFT"
		const val FRAME_ID_V2_GENERAL_ENCAPS_OBJECT = "GEO"
		const val FRAME_ID_V2_GENRE = "TCO"
		const val FRAME_ID_V2_HW_SW_SETTINGS = "TSS"
		const val FRAME_ID_V2_INITIAL_KEY = "TKE"
		const val FRAME_ID_V2_IPLS = "IPL"
		const val FRAME_ID_V2_ISRC = "TRC"
		const val FRAME_ID_V2_ITUNES_GROUPING = "GP1"
		const val FRAME_ID_V2_LANGUAGE = "TLA"
		const val FRAME_ID_V2_LENGTH = "TLE"
		const val FRAME_ID_V2_LINKED_INFO = "LNK"
		const val FRAME_ID_V2_LYRICIST = "TXT"
		const val FRAME_ID_V2_MEDIA_TYPE = "TMT"
		const val FRAME_ID_V2_MOVEMENT = "MVN"
		const val FRAME_ID_V2_MOVEMENT_NO = "MVI"
		const val FRAME_ID_V2_MPEG_LOCATION_LOOKUP_TABLE = "MLL"
		const val FRAME_ID_V2_MUSIC_CD_ID = "MCI"
		const val FRAME_ID_V2_ORIGARTIST = "TOA"
		const val FRAME_ID_V2_ORIG_FILENAME = "TOF"
		const val FRAME_ID_V2_ORIG_LYRICIST = "TOL"
		const val FRAME_ID_V2_ORIG_TITLE = "TOT"
		const val FRAME_ID_V2_PLAYLIST_DELAY = "TDY"
		const val FRAME_ID_V2_PLAY_COUNTER = "CNT"
		const val FRAME_ID_V2_POPULARIMETER = "POP"
		const val FRAME_ID_V2_PUBLISHER = "TPB"
		const val FRAME_ID_V2_RECOMMENDED_BUFFER_SIZE = "BUF"
		const val FRAME_ID_V2_RELATIVE_VOLUME_ADJUSTMENT = "RVA"
		const val FRAME_ID_V2_REMIXED = "TP4"
		const val FRAME_ID_V2_REVERB = "REV"
		const val FRAME_ID_V2_SET = "TPA"
		const val FRAME_ID_V2_SET_SUBTITLE = "TPS" //Note this is non-standard
		const val FRAME_ID_V2_SYNC_LYRIC = "SLT"
		const val FRAME_ID_V2_SYNC_TEMPO = "STC"
		const val FRAME_ID_V2_TDAT = "TDA"
		const val FRAME_ID_V2_TIME = "TIM"
		const val FRAME_ID_V2_TITLE = "TT2"
		const val FRAME_ID_V2_TITLE_REFINEMENT = "TT3"
		const val FRAME_ID_V2_TORY = "TOR"
		const val FRAME_ID_V2_TRACK = "TRK"
		const val FRAME_ID_V2_TRDA = "TRD"
		const val FRAME_ID_V2_TSIZ = "TSI"
		const val FRAME_ID_V2_TYER = "TYE"
		const val FRAME_ID_V2_UNIQUE_FILE_ID = "UFI"
		const val FRAME_ID_V2_UNSYNC_LYRICS = "ULT"
		const val FRAME_ID_V2_URL_ARTIST_WEB = "WAR"
		const val FRAME_ID_V2_URL_COMMERCIAL = "WCM"
		const val FRAME_ID_V2_URL_COPYRIGHT = "WCP"
		const val FRAME_ID_V2_URL_FILE_WEB = "WAF"
		const val FRAME_ID_V2_URL_OFFICIAL_RADIO = "WRS"
		const val FRAME_ID_V2_URL_PAYMENT = "WPAY"
		const val FRAME_ID_V2_URL_PUBLISHERS = "WPB"
		const val FRAME_ID_V2_URL_SOURCE_WEB = "WAS"
		const val FRAME_ID_V2_USER_DEFINED_INFO = "TXX"
		const val FRAME_ID_V2_USER_DEFINED_URL = "WXX"
		const val FRAME_ID_V2_IS_COMPILATION = "TCP"
		const val FRAME_ID_V2_TITLE_SORT_ORDER_ITUNES = "TST"
		const val FRAME_ID_V2_ARTIST_SORT_ORDER_ITUNES = "TSP"
		const val FRAME_ID_V2_ALBUM_SORT_ORDER_ITUNES = "TSA"
		const val FRAME_ID_V2_ALBUM_ARTIST_SORT_ORDER_ITUNES = "TS2"
		const val FRAME_ID_V2_COMPOSER_SORT_ORDER_ITUNES = "TSC"
		private var id3v22Frames: ID3v22Frames? = null

		@JvmStatic
		val instanceOf: ID3v22Frames
			get() {
				if (id3v22Frames == null) {
					id3v22Frames = ID3v22Frames()
				}
				return id3v22Frames!!
			}
	}
}
