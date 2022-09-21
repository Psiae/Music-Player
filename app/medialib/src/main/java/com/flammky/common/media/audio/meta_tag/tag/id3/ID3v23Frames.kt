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
 * Defines ID3v23 frames and collections that categorise frames within an ID3v23 tag.
 *
 * You can include frames here that are not officially supported as long as they can be used within an
 * ID3v23Tag
 *
 * @author Paul Taylor
 * @version $Id$
 */
class ID3v23Frames private constructor() : ID3Frames() {
	/**
	 * Maps from Generic key to ID3 key
	 */
	protected var tagFieldToId3 = EnumMap<FieldKey, ID3v23FieldKey>(FieldKey::class.java)

	/**
	 * Maps from ID3 key to Generic key
	 */
	protected var id3ToTagField = EnumMap<ID3v23FieldKey, FieldKey>(ID3v23FieldKey::class.java)

	init {
		// The defined v23 frames,
		supportedFrames.add(FRAME_ID_V3_ACCOMPANIMENT)
		supportedFrames.add(FRAME_ID_V3_ALBUM)
		supportedFrames.add(FRAME_ID_V3_ARTIST)
		supportedFrames.add(FRAME_ID_V3_ATTACHED_PICTURE)
		supportedFrames.add(FRAME_ID_V3_AUDIO_ENCRYPTION)
		supportedFrames.add(FRAME_ID_V3_BPM)
		supportedFrames.add(FRAME_ID_V3_CHAPTER)
		supportedFrames.add(FRAME_ID_V3_CHAPTER_TOC)
		supportedFrames.add(FRAME_ID_V3_COMMENT)
		supportedFrames.add(FRAME_ID_V3_COMMERCIAL_FRAME)
		supportedFrames.add(FRAME_ID_V3_COMPOSER)
		supportedFrames.add(FRAME_ID_V3_CONDUCTOR)
		supportedFrames.add(FRAME_ID_V3_CONTENT_GROUP_DESC)
		supportedFrames.add(FRAME_ID_V3_COPYRIGHTINFO)
		supportedFrames.add(FRAME_ID_V3_ENCODEDBY)
		supportedFrames.add(FRAME_ID_V3_ENCRYPTION)
		supportedFrames.add(FRAME_ID_V3_EQUALISATION)
		supportedFrames.add(FRAME_ID_V3_EVENT_TIMING_CODES)
		supportedFrames.add(FRAME_ID_V3_FILE_OWNER)
		supportedFrames.add(FRAME_ID_V3_FILE_TYPE)
		supportedFrames.add(FRAME_ID_V3_GENERAL_ENCAPS_OBJECT)
		supportedFrames.add(FRAME_ID_V3_GENRE)
		supportedFrames.add(FRAME_ID_V3_GROUP_ID_REG)
		supportedFrames.add(FRAME_ID_V3_HW_SW_SETTINGS)
		supportedFrames.add(FRAME_ID_V3_INITIAL_KEY)
		supportedFrames.add(FRAME_ID_V3_INVOLVED_PEOPLE)
		supportedFrames.add(FRAME_ID_V3_ISRC)
		supportedFrames.add(FRAME_ID_V3_ITUNES_GROUPING)
		supportedFrames.add(FRAME_ID_V3_LANGUAGE)
		supportedFrames.add(FRAME_ID_V3_LENGTH)
		supportedFrames.add(FRAME_ID_V3_LINKED_INFO)
		supportedFrames.add(FRAME_ID_V3_LYRICIST)
		supportedFrames.add(FRAME_ID_V3_MEDIA_TYPE)
		supportedFrames.add(FRAME_ID_V3_MPEG_LOCATION_LOOKUP_TABLE)
		supportedFrames.add(FRAME_ID_V3_MOVEMENT)
		supportedFrames.add(FRAME_ID_V3_MOVEMENT_NO)
		supportedFrames.add(FRAME_ID_V3_MUSIC_CD_ID)
		supportedFrames.add(FRAME_ID_V3_ORIGARTIST)
		supportedFrames.add(FRAME_ID_V3_ORIG_FILENAME)
		supportedFrames.add(FRAME_ID_V3_ORIG_LYRICIST)
		supportedFrames.add(FRAME_ID_V3_ORIG_TITLE)
		supportedFrames.add(FRAME_ID_V3_OWNERSHIP)
		supportedFrames.add(FRAME_ID_V3_PLAYLIST_DELAY)
		supportedFrames.add(FRAME_ID_V3_PLAY_COUNTER)
		supportedFrames.add(FRAME_ID_V3_POPULARIMETER)
		supportedFrames.add(FRAME_ID_V3_POSITION_SYNC)
		supportedFrames.add(FRAME_ID_V3_PRIVATE)
		supportedFrames.add(FRAME_ID_V3_PUBLISHER)
		supportedFrames.add(FRAME_ID_V3_RADIO_NAME)
		supportedFrames.add(FRAME_ID_V3_RADIO_OWNER)
		supportedFrames.add(FRAME_ID_V3_RECOMMENDED_BUFFER_SIZE)
		supportedFrames.add(FRAME_ID_V3_RELATIVE_VOLUME_ADJUSTMENT)
		supportedFrames.add(FRAME_ID_V3_REMIXED)
		supportedFrames.add(FRAME_ID_V3_REVERB)
		supportedFrames.add(FRAME_ID_V3_SET)
		supportedFrames.add(FRAME_ID_V3_SET_SUBTITLE)
		supportedFrames.add(FRAME_ID_V3_SYNC_LYRIC)
		supportedFrames.add(FRAME_ID_V3_SYNC_TEMPO)
		supportedFrames.add(FRAME_ID_V3_TDAT)
		supportedFrames.add(FRAME_ID_V3_TERMS_OF_USE)
		supportedFrames.add(FRAME_ID_V3_TIME)
		supportedFrames.add(FRAME_ID_V3_TITLE)
		supportedFrames.add(FRAME_ID_V3_TITLE_REFINEMENT)
		supportedFrames.add(FRAME_ID_V3_TORY)
		supportedFrames.add(FRAME_ID_V3_TRACK)
		supportedFrames.add(FRAME_ID_V3_TRDA)
		supportedFrames.add(FRAME_ID_V3_TSIZ)
		supportedFrames.add(FRAME_ID_V3_TYER)
		supportedFrames.add(FRAME_ID_V3_UNIQUE_FILE_ID)
		supportedFrames.add(FRAME_ID_V3_UNSYNC_LYRICS)
		supportedFrames.add(FRAME_ID_V3_URL_ARTIST_WEB)
		supportedFrames.add(FRAME_ID_V3_URL_COMMERCIAL)
		supportedFrames.add(FRAME_ID_V3_URL_COPYRIGHT)
		supportedFrames.add(FRAME_ID_V3_URL_FILE_WEB)
		supportedFrames.add(FRAME_ID_V3_URL_OFFICIAL_RADIO)
		supportedFrames.add(FRAME_ID_V3_URL_PAYMENT)
		supportedFrames.add(FRAME_ID_V3_URL_PUBLISHERS)
		supportedFrames.add(FRAME_ID_V3_URL_SOURCE_WEB)
		supportedFrames.add(FRAME_ID_V3_USER_DEFINED_INFO)
		supportedFrames.add(FRAME_ID_V3_USER_DEFINED_URL)

		//Extension
		extensionFrames.add(FRAME_ID_V3_IS_COMPILATION)
		extensionFrames.add(FRAME_ID_V3_TITLE_SORT_ORDER_ITUNES)
		extensionFrames.add(FRAME_ID_V3_ARTIST_SORT_ORDER_ITUNES)
		extensionFrames.add(FRAME_ID_V3_ALBUM_SORT_ORDER_ITUNES)
		extensionFrames.add(FRAME_ID_V3_TITLE_SORT_ORDER_MUSICBRAINZ)
		extensionFrames.add(FRAME_ID_V3_ARTIST_SORT_ORDER_MUSICBRAINZ)
		extensionFrames.add(FRAME_ID_V3_ALBUM_SORT_ORDER_MUSICBRAINZ)
		extensionFrames.add(FRAME_ID_V3_ALBUM_ARTIST_SORT_ORDER_ITUNES)
		extensionFrames.add(FRAME_ID_V3_COMPOSER_SORT_ORDER_ITUNES)

		//Common
		commonFrames.add(FRAME_ID_V3_ARTIST)
		commonFrames.add(FRAME_ID_V3_ALBUM)
		commonFrames.add(FRAME_ID_V3_TITLE)
		commonFrames.add(FRAME_ID_V3_GENRE)
		commonFrames.add(FRAME_ID_V3_TRACK)
		commonFrames.add(FRAME_ID_V3_TYER)
		commonFrames.add(FRAME_ID_V3_COMMENT)

		//Binary
		binaryFrames.add(FRAME_ID_V3_ATTACHED_PICTURE)
		binaryFrames.add(FRAME_ID_V3_AUDIO_ENCRYPTION)
		binaryFrames.add(FRAME_ID_V3_ENCRYPTION)
		binaryFrames.add(FRAME_ID_V3_EQUALISATION)
		binaryFrames.add(FRAME_ID_V3_EVENT_TIMING_CODES)
		binaryFrames.add(FRAME_ID_V3_GENERAL_ENCAPS_OBJECT)
		binaryFrames.add(FRAME_ID_V3_RELATIVE_VOLUME_ADJUSTMENT)
		binaryFrames.add(FRAME_ID_V3_RECOMMENDED_BUFFER_SIZE)
		binaryFrames.add(FRAME_ID_V3_UNIQUE_FILE_ID)

		// Map frameid to a name
		idToValue[FRAME_ID_V3_ACCOMPANIMENT] = "Text: Band/Orchestra/Accompaniment"
		idToValue[FRAME_ID_V3_ALBUM] = "Text: Album/Movie/Show title"
		idToValue[FRAME_ID_V3_ARTIST] =
			"Text: Lead artist(s)/Lead performer(s)/Soloist(s)/Performing group"
		idToValue[FRAME_ID_V3_ATTACHED_PICTURE] =
			"Attached picture"
		idToValue[FRAME_ID_V3_AUDIO_ENCRYPTION] = "Audio encryption"
		idToValue[FRAME_ID_V3_BPM] =
			"Text: BPM (Beats Per Minute)"
		idToValue[FRAME_ID_V3_CHAPTER] =
			"Chapter"
		idToValue[FRAME_ID_V3_CHAPTER_TOC] = "Chapter TOC"
		idToValue[FRAME_ID_V3_COMMENT] = "Comments"
		idToValue[FRAME_ID_V3_COMMERCIAL_FRAME] = ""
		idToValue[FRAME_ID_V3_COMPOSER] =
			"Text: Composer"
		idToValue[FRAME_ID_V3_CONDUCTOR] = "Text: Conductor/Performer refinement"
		idToValue[FRAME_ID_V3_CONTENT_GROUP_DESC] =
			"Text: Content group description"
		idToValue[FRAME_ID_V3_COPYRIGHTINFO] = "Text: Copyright message"
		idToValue[FRAME_ID_V3_ENCODEDBY] =
			"Text: Encoded by"
		idToValue[FRAME_ID_V3_ENCRYPTION] =
			"Encryption method registration"
		idToValue[FRAME_ID_V3_EQUALISATION] =
			"Equalization"
		idToValue[FRAME_ID_V3_EVENT_TIMING_CODES] =
			"Event timing codes"
		idToValue[FRAME_ID_V3_FILE_OWNER] =
			""
		idToValue[FRAME_ID_V3_FILE_TYPE] =
			"Text: File type"
		idToValue[FRAME_ID_V3_GENERAL_ENCAPS_OBJECT] = "General encapsulated datatype"
		idToValue[FRAME_ID_V3_GENRE] = "Text: Content type"
		idToValue[FRAME_ID_V3_GROUP_ID_REG] =
			""
		idToValue[FRAME_ID_V3_HW_SW_SETTINGS] =
			"Text: Software/hardware and settings used for encoding"
		idToValue[FRAME_ID_V3_INITIAL_KEY] = "Text: Initial key"
		idToValue[FRAME_ID_V3_INVOLVED_PEOPLE] =
			"Involved people list"
		idToValue[FRAME_ID_V3_ISRC] =
			"Text: ISRC (International Standard Recording Code)"
		idToValue[FRAME_ID_V3_ITUNES_GROUPING] =
			"Text: iTunes Grouping"
		idToValue[FRAME_ID_V3_LANGUAGE] = "Text: Language(s)"
		idToValue[FRAME_ID_V3_LENGTH] =
			"Text: Length"
		idToValue[FRAME_ID_V3_LINKED_INFO] = "Linked information"
		idToValue[FRAME_ID_V3_LYRICIST] =
			"Text: Lyricist/text writer"
		idToValue[FRAME_ID_V3_MEDIA_TYPE] =
			"Text: Media type"
		idToValue[FRAME_ID_V3_MOVEMENT] =
			"Text: Movement"
		idToValue[FRAME_ID_V3_MOVEMENT_NO] = "Text: Movement No"
		idToValue[FRAME_ID_V3_MPEG_LOCATION_LOOKUP_TABLE] = "MPEG location lookup table"
		idToValue[FRAME_ID_V3_MUSIC_CD_ID] = "Music CD Identifier"
		idToValue[FRAME_ID_V3_ORIGARTIST] = "Text: Original artist(s)/performer(s)"
		idToValue[FRAME_ID_V3_ORIG_FILENAME] =
			"Text: Original filename"
		idToValue[FRAME_ID_V3_ORIG_LYRICIST] =
			"Text: Original Lyricist(s)/text writer(s)"
		idToValue[FRAME_ID_V3_ORIG_TITLE] = "Text: Original album/Movie/Show title"
		idToValue[FRAME_ID_V3_OWNERSHIP] =
			""
		idToValue[FRAME_ID_V3_PLAYLIST_DELAY] = "Text: Playlist delay"
		idToValue[FRAME_ID_V3_PLAY_COUNTER] =
			"Play counter"
		idToValue[FRAME_ID_V3_POPULARIMETER] = "Popularimeter"
		idToValue[FRAME_ID_V3_POSITION_SYNC] = "Position Sync"
		idToValue[FRAME_ID_V3_PRIVATE] =
			"Private frame"
		idToValue[FRAME_ID_V3_PUBLISHER] =
			"Text: Publisher"
		idToValue[FRAME_ID_V3_RADIO_NAME] =
			""
		idToValue[FRAME_ID_V3_RADIO_OWNER] = ""
		idToValue[FRAME_ID_V3_RECOMMENDED_BUFFER_SIZE] =
			"Recommended buffer size"
		idToValue[FRAME_ID_V3_RELATIVE_VOLUME_ADJUSTMENT] = "Relative volume adjustment"
		idToValue[FRAME_ID_V3_REMIXED] =
			"Text: Interpreted, remixed, or otherwise modified by"
		idToValue[FRAME_ID_V3_REVERB] =
			"Reverb"
		idToValue[FRAME_ID_V3_SET] =
			"Text: Part of a setField"
		idToValue[FRAME_ID_V3_SET_SUBTITLE] =
			"Text: SubTitle"
		idToValue[FRAME_ID_V3_SYNC_LYRIC] =
			"Synchronized lyric/text"
		idToValue[FRAME_ID_V3_SYNC_TEMPO] = "Synced tempo codes"
		idToValue[FRAME_ID_V3_TDAT] =
			"Text: Date"
		idToValue[FRAME_ID_V3_TERMS_OF_USE] =
			""
		idToValue[FRAME_ID_V3_TIME] = "Text: Time"
		idToValue[FRAME_ID_V3_TITLE] =
			"Text: Title/Songname/Content description"
		idToValue[FRAME_ID_V3_TITLE_REFINEMENT] =
			"Text: Subtitle/Description refinement"
		idToValue[FRAME_ID_V3_TORY] =
			"Text: Original release year"
		idToValue[FRAME_ID_V3_TRACK] = "Text: Track number/Position in setField"
		idToValue[FRAME_ID_V3_TRDA] = "Text: Recording dates"
		idToValue[FRAME_ID_V3_TSIZ] =
			"Text: Size"
		idToValue[FRAME_ID_V3_TYER] = "Text: Year"
		idToValue[FRAME_ID_V3_UNIQUE_FILE_ID] =
			"Unique file identifier"
		idToValue[FRAME_ID_V3_UNSYNC_LYRICS] =
			"Unsychronized lyric/text transcription"
		idToValue[FRAME_ID_V3_URL_ARTIST_WEB] = "URL: Official artist/performer webpage"
		idToValue[FRAME_ID_V3_URL_COMMERCIAL] =
			"URL: Commercial information"
		idToValue[FRAME_ID_V3_URL_COPYRIGHT] =
			"URL: Copyright/Legal information"
		idToValue[FRAME_ID_V3_URL_FILE_WEB] = "URL: Official audio file webpage"
		idToValue[FRAME_ID_V3_URL_OFFICIAL_RADIO] =
			"Official Radio"
		idToValue[FRAME_ID_V3_URL_PAYMENT] =
			"URL: Payment"
		idToValue[FRAME_ID_V3_URL_PUBLISHERS] =
			"URL: Publishers official webpage"
		idToValue[FRAME_ID_V3_URL_SOURCE_WEB] =
			"URL: Official audio source webpage"
		idToValue[FRAME_ID_V3_USER_DEFINED_INFO] = "User defined text information frame"
		idToValue[FRAME_ID_V3_USER_DEFINED_URL] =
			"User defined URL link frame"
		idToValue[FRAME_ID_V3_IS_COMPILATION] = "Is Compilation"
		idToValue[FRAME_ID_V3_TITLE_SORT_ORDER_ITUNES] =
			"Text: title sort order"
		idToValue[FRAME_ID_V3_ARTIST_SORT_ORDER_ITUNES] = "Text: artist sort order"
		idToValue[FRAME_ID_V3_ALBUM_SORT_ORDER_ITUNES] =
			"Text: album sort order"
		idToValue[FRAME_ID_V3_TITLE_SORT_ORDER_MUSICBRAINZ] = "Text: title sort order"
		idToValue[FRAME_ID_V3_ARTIST_SORT_ORDER_MUSICBRAINZ] = "Text: artist sort order"
		idToValue[FRAME_ID_V3_ALBUM_SORT_ORDER_MUSICBRAINZ] = "Text: album sort order"
		idToValue[FRAME_ID_V3_ALBUM_ARTIST_SORT_ORDER_ITUNES] =
			"Text:Album Artist Sort Order Frame"
		idToValue[FRAME_ID_V3_COMPOSER_SORT_ORDER_ITUNES] =
			"Text:Composer Sort Order Frame"
		createMaps()
		multipleFrames.add(FRAME_ID_V3_USER_DEFINED_INFO)
		multipleFrames.add(FRAME_ID_V3_USER_DEFINED_URL)
		multipleFrames.add(FRAME_ID_V3_ATTACHED_PICTURE)
		multipleFrames.add(FRAME_ID_V3_PRIVATE)
		multipleFrames.add(FRAME_ID_V3_COMMENT)
		multipleFrames.add(FRAME_ID_V3_UNIQUE_FILE_ID)
		multipleFrames.add(FRAME_ID_V3_UNSYNC_LYRICS)
		multipleFrames.add(FRAME_ID_V3_POPULARIMETER)
		multipleFrames.add(FRAME_ID_V3_GENERAL_ENCAPS_OBJECT)
		multipleFrames.add(FRAME_ID_V3_URL_ARTIST_WEB)
		discardIfFileAlteredFrames.add(FRAME_ID_V3_EVENT_TIMING_CODES)
		discardIfFileAlteredFrames.add(FRAME_ID_V3_EQUALISATION)
		discardIfFileAlteredFrames.add(FRAME_ID_V3_MPEG_LOCATION_LOOKUP_TABLE)
		discardIfFileAlteredFrames.add(FRAME_ID_V3_POSITION_SYNC)
		discardIfFileAlteredFrames.add(FRAME_ID_V3_SYNC_LYRIC)
		discardIfFileAlteredFrames.add(FRAME_ID_V3_SYNC_TEMPO)
		discardIfFileAlteredFrames.add(FRAME_ID_V3_RELATIVE_VOLUME_ADJUSTMENT)
		discardIfFileAlteredFrames.add(FRAME_ID_V3_EVENT_TIMING_CODES)
		discardIfFileAlteredFrames.add(FRAME_ID_V3_ENCODEDBY)
		discardIfFileAlteredFrames.add(FRAME_ID_V3_LENGTH)
		discardIfFileAlteredFrames.add(FRAME_ID_V3_TSIZ)

		//Mapping from generic key
		tagFieldToId3[FieldKey.ACOUSTID_FINGERPRINT] =
			ID3v23FieldKey.ACOUSTID_FINGERPRINT
		tagFieldToId3[FieldKey.ACOUSTID_ID] =
			ID3v23FieldKey.ACOUSTID_ID
		tagFieldToId3[FieldKey.ALBUM] =
			ID3v23FieldKey.ALBUM
		tagFieldToId3[FieldKey.ALBUM_ARTIST] =
			ID3v23FieldKey.ALBUM_ARTIST
		tagFieldToId3[FieldKey.ALBUM_ARTIST_SORT] =
			ID3v23FieldKey.ALBUM_ARTIST_SORT
		tagFieldToId3[FieldKey.ALBUM_ARTISTS] =
			ID3v23FieldKey.ALBUM_ARTISTS
		tagFieldToId3[FieldKey.ALBUM_ARTISTS_SORT] =
			ID3v23FieldKey.ALBUM_ARTISTS_SORT
		tagFieldToId3[FieldKey.ALBUM_SORT] =
			ID3v23FieldKey.ALBUM_SORT
		tagFieldToId3[FieldKey.ALBUM_YEAR] = ID3v23FieldKey.ALBUM_YEAR
		tagFieldToId3[FieldKey.AMAZON_ID] =
			ID3v23FieldKey.AMAZON_ID
		tagFieldToId3[FieldKey.ARRANGER] =
			ID3v23FieldKey.ARRANGER
		tagFieldToId3[FieldKey.ARRANGER_SORT] =
			ID3v23FieldKey.ARRANGER_SORT
		tagFieldToId3[FieldKey.ARTIST] = ID3v23FieldKey.ARTIST
		tagFieldToId3[FieldKey.ARTISTS] =
			ID3v23FieldKey.ARTISTS
		tagFieldToId3[FieldKey.ARTISTS_SORT] =
			ID3v23FieldKey.ARTISTS_SORT
		tagFieldToId3[FieldKey.ARTIST_SORT] =
			ID3v23FieldKey.ARTIST_SORT
		tagFieldToId3[FieldKey.BARCODE] =
			ID3v23FieldKey.BARCODE
		tagFieldToId3[FieldKey.BPM] =
			ID3v23FieldKey.BPM
		tagFieldToId3[FieldKey.CATALOG_NO] =
			ID3v23FieldKey.CATALOG_NO
		tagFieldToId3[FieldKey.CHOIR] =
			ID3v23FieldKey.CHOIR
		tagFieldToId3[FieldKey.CHOIR_SORT] = ID3v23FieldKey.CHOIR_SORT
		tagFieldToId3[FieldKey.CLASSICAL_CATALOG] =
			ID3v23FieldKey.CLASSICAL_CATALOG
		tagFieldToId3[FieldKey.CLASSICAL_NICKNAME] =
			ID3v23FieldKey.CLASSICAL_NICKNAME
		tagFieldToId3[FieldKey.COMMENT] = ID3v23FieldKey.COMMENT
		tagFieldToId3[FieldKey.COMPOSER] = ID3v23FieldKey.COMPOSER
		tagFieldToId3[FieldKey.COMPOSER_SORT] =
			ID3v23FieldKey.COMPOSER_SORT
		tagFieldToId3[FieldKey.CONDUCTOR] =
			ID3v23FieldKey.CONDUCTOR
		tagFieldToId3[FieldKey.CONDUCTOR_SORT] =
			ID3v23FieldKey.CONDUCTOR_SORT
		tagFieldToId3[FieldKey.COPYRIGHT] =
			ID3v23FieldKey.COPYRIGHT
		tagFieldToId3[FieldKey.COUNTRY] =
			ID3v23FieldKey.COUNTRY
		tagFieldToId3[FieldKey.COVER_ART] = ID3v23FieldKey.COVER_ART
		tagFieldToId3[FieldKey.CUSTOM1] =
			ID3v23FieldKey.CUSTOM1
		tagFieldToId3[FieldKey.CUSTOM2] =
			ID3v23FieldKey.CUSTOM2
		tagFieldToId3[FieldKey.CUSTOM3] =
			ID3v23FieldKey.CUSTOM3
		tagFieldToId3[FieldKey.CUSTOM4] =
			ID3v23FieldKey.CUSTOM4
		tagFieldToId3[FieldKey.CUSTOM5] =
			ID3v23FieldKey.CUSTOM5
		tagFieldToId3[FieldKey.DISC_NO] =
			ID3v23FieldKey.DISC_NO
		tagFieldToId3[FieldKey.DISC_SUBTITLE] =
			ID3v23FieldKey.DISC_SUBTITLE
		tagFieldToId3[FieldKey.DISC_TOTAL] = ID3v23FieldKey.DISC_NO
		tagFieldToId3[FieldKey.DJMIXER] =
			ID3v23FieldKey.DJMIXER
		tagFieldToId3[FieldKey.DJMIXER_SORT] =
			ID3v23FieldKey.DJMIXER_SORT
		tagFieldToId3[FieldKey.MOOD_ELECTRONIC] =
			ID3v23FieldKey.MOOD_ELECTRONIC
		tagFieldToId3[FieldKey.ENCODER] =
			ID3v23FieldKey.ENCODER
		tagFieldToId3[FieldKey.ENGINEER] =
			ID3v23FieldKey.ENGINEER
		tagFieldToId3[FieldKey.ENGINEER_SORT] =
			ID3v23FieldKey.ENGINEER_SORT
		tagFieldToId3[FieldKey.ENSEMBLE] = ID3v23FieldKey.ENSEMBLE
		tagFieldToId3[FieldKey.ENSEMBLE_SORT] =
			ID3v23FieldKey.ENSEMBLE_SORT
		tagFieldToId3[FieldKey.FBPM] =
			ID3v23FieldKey.FBPM
		tagFieldToId3[FieldKey.GENRE] = ID3v23FieldKey.GENRE
		tagFieldToId3[FieldKey.GROUP] =
			ID3v23FieldKey.GROUP
		tagFieldToId3[FieldKey.GROUPING] =
			ID3v23FieldKey.GROUPING
		tagFieldToId3[FieldKey.INSTRUMENT] =
			ID3v23FieldKey.INSTRUMENT
		tagFieldToId3[FieldKey.INVOLVEDPEOPLE] =
			ID3v23FieldKey.INVOLVEDPEOPLE
		tagFieldToId3[FieldKey.IPI] =
			ID3v23FieldKey.IPI
		tagFieldToId3[FieldKey.ISRC] =
			ID3v23FieldKey.ISRC
		tagFieldToId3[FieldKey.ISWC] = ID3v23FieldKey.ISWC
		tagFieldToId3[FieldKey.IS_CLASSICAL] =
			ID3v23FieldKey.IS_CLASSICAL
		tagFieldToId3[FieldKey.IS_COMPILATION] =
			ID3v23FieldKey.IS_COMPILATION
		tagFieldToId3[FieldKey.IS_GREATEST_HITS] =
			ID3v23FieldKey.IS_GREATEST_HITS
		tagFieldToId3[FieldKey.IS_HD] = ID3v23FieldKey.IS_HD
		tagFieldToId3[FieldKey.IS_LIVE] = ID3v23FieldKey.IS_LIVE
		tagFieldToId3[FieldKey.IS_SOUNDTRACK] =
			ID3v23FieldKey.IS_SOUNDTRACK
		tagFieldToId3[FieldKey.ITUNES_GROUPING] =
			ID3v23FieldKey.ITUNES_GROUPING
		tagFieldToId3[FieldKey.JAIKOZ_ID] =
			ID3v23FieldKey.JAIKOZ_ID
		tagFieldToId3[FieldKey.KEY] =
			ID3v23FieldKey.KEY
		tagFieldToId3[FieldKey.LANGUAGE] =
			ID3v23FieldKey.LANGUAGE
		tagFieldToId3[FieldKey.LYRICIST] =
			ID3v23FieldKey.LYRICIST
		tagFieldToId3[FieldKey.LYRICIST_SORT] =
			ID3v23FieldKey.LYRICIST_SORT
		tagFieldToId3[FieldKey.LYRICS] =
			ID3v23FieldKey.LYRICS
		tagFieldToId3[FieldKey.MEDIA] =
			ID3v23FieldKey.MEDIA
		tagFieldToId3[FieldKey.MIXER] =
			ID3v23FieldKey.MIXER
		tagFieldToId3[FieldKey.MIXER_SORT] = ID3v23FieldKey.MIXER_SORT
		tagFieldToId3[FieldKey.MOOD] =
			ID3v23FieldKey.MOOD
		tagFieldToId3[FieldKey.MOOD_ACOUSTIC] =
			ID3v23FieldKey.MOOD_ACOUSTIC
		tagFieldToId3[FieldKey.MOOD_AGGRESSIVE] =
			ID3v23FieldKey.MOOD_AGGRESSIVE
		tagFieldToId3[FieldKey.MOOD_AROUSAL] =
			ID3v23FieldKey.MOOD_AROUSAL
		tagFieldToId3[FieldKey.MOOD_DANCEABILITY] =
			ID3v23FieldKey.MOOD_DANCEABILITY
		tagFieldToId3[FieldKey.MOOD_HAPPY] =
			ID3v23FieldKey.MOOD_HAPPY
		tagFieldToId3[FieldKey.MOOD_INSTRUMENTAL] =
			ID3v23FieldKey.MOOD_INSTRUMENTAL
		tagFieldToId3[FieldKey.MOOD_PARTY] =
			ID3v23FieldKey.MOOD_PARTY
		tagFieldToId3[FieldKey.MOOD_RELAXED] =
			ID3v23FieldKey.MOOD_RELAXED
		tagFieldToId3[FieldKey.MOOD_SAD] =
			ID3v23FieldKey.MOOD_SAD
		tagFieldToId3[FieldKey.MOOD_VALENCE] =
			ID3v23FieldKey.MOOD_VALENCE
		tagFieldToId3[FieldKey.MOVEMENT] =
			ID3v23FieldKey.MOVEMENT
		tagFieldToId3[FieldKey.MOVEMENT_NO] =
			ID3v23FieldKey.MOVEMENT_NO
		tagFieldToId3[FieldKey.MOVEMENT_TOTAL] =
			ID3v23FieldKey.MOVEMENT_TOTAL
		tagFieldToId3[FieldKey.MUSICBRAINZ_ARTISTID] =
			ID3v23FieldKey.MUSICBRAINZ_ARTISTID
		tagFieldToId3[FieldKey.MUSICBRAINZ_DISC_ID] =
			ID3v23FieldKey.MUSICBRAINZ_DISC_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_ORIGINAL_RELEASE_ID] =
			ID3v23FieldKey.MUSICBRAINZ_ORIGINAL_RELEASEID
		tagFieldToId3[FieldKey.MUSICBRAINZ_RELEASEARTISTID] =
			ID3v23FieldKey.MUSICBRAINZ_RELEASEARTISTID
		tagFieldToId3[FieldKey.MUSICBRAINZ_RELEASEID] =
			ID3v23FieldKey.MUSICBRAINZ_RELEASEID
		tagFieldToId3[FieldKey.MUSICBRAINZ_RELEASE_COUNTRY] =
			ID3v23FieldKey.MUSICBRAINZ_RELEASE_COUNTRY
		tagFieldToId3[FieldKey.MUSICBRAINZ_RELEASE_GROUP_ID] =
			ID3v23FieldKey.MUSICBRAINZ_RELEASE_GROUP_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_RELEASE_STATUS] =
			ID3v23FieldKey.MUSICBRAINZ_RELEASE_STATUS
		tagFieldToId3[FieldKey.MUSICBRAINZ_RELEASE_TRACK_ID] =
			ID3v23FieldKey.MUSICBRAINZ_RELEASE_TRACK_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_RELEASE_TYPE] =
			ID3v23FieldKey.MUSICBRAINZ_RELEASE_TYPE
		tagFieldToId3[FieldKey.MUSICBRAINZ_TRACK_ID] =
			ID3v23FieldKey.MUSICBRAINZ_TRACK_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK] =
			ID3v23FieldKey.MUSICBRAINZ_WORK
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_ID] =
			ID3v23FieldKey.MUSICBRAINZ_WORK_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_RECORDING_WORK_ID] =
			ID3v23FieldKey.MUSICBRAINZ_RECORDING_WORK_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL1_ID] =
			ID3v23FieldKey.MUSICBRAINZ_WORK_PART_LEVEL1_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL2_ID] =
			ID3v23FieldKey.MUSICBRAINZ_WORK_PART_LEVEL2_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL3_ID] =
			ID3v23FieldKey.MUSICBRAINZ_WORK_PART_LEVEL3_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL4_ID] =
			ID3v23FieldKey.MUSICBRAINZ_WORK_PART_LEVEL4_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL5_ID] =
			ID3v23FieldKey.MUSICBRAINZ_WORK_PART_LEVEL5_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL6_ID] =
			ID3v23FieldKey.MUSICBRAINZ_WORK_PART_LEVEL6_ID
		tagFieldToId3[FieldKey.MUSICIP_ID] = ID3v23FieldKey.MUSICIP_ID
		tagFieldToId3[FieldKey.OCCASION] =
			ID3v23FieldKey.OCCASION
		tagFieldToId3[FieldKey.OPUS] = ID3v23FieldKey.OPUS
		tagFieldToId3[FieldKey.ORCHESTRA] =
			ID3v23FieldKey.ORCHESTRA
		tagFieldToId3[FieldKey.ORCHESTRA_SORT] =
			ID3v23FieldKey.ORCHESTRA_SORT
		tagFieldToId3[FieldKey.ORIGINAL_ALBUM] =
			ID3v23FieldKey.ORIGINAL_ALBUM
		tagFieldToId3[FieldKey.ORIGINALRELEASEDATE] =
			ID3v23FieldKey.ORIGINALRELEASEDATE
		tagFieldToId3[FieldKey.ORIGINAL_ARTIST] =
			ID3v23FieldKey.ORIGINAL_ARTIST
		tagFieldToId3[FieldKey.ORIGINAL_LYRICIST] =
			ID3v23FieldKey.ORIGINAL_LYRICIST
		tagFieldToId3[FieldKey.ORIGINAL_YEAR] =
			ID3v23FieldKey.ORIGINAL_YEAR
		tagFieldToId3[FieldKey.OVERALL_WORK] =
			ID3v23FieldKey.OVERALL_WORK
		tagFieldToId3[FieldKey.PART] = ID3v23FieldKey.PART
		tagFieldToId3[FieldKey.PART_NUMBER] = ID3v23FieldKey.PART_NUMBER
		tagFieldToId3[FieldKey.PART_TYPE] = ID3v23FieldKey.PART_TYPE
		tagFieldToId3[FieldKey.PERFORMER] =
			ID3v23FieldKey.PERFORMER
		tagFieldToId3[FieldKey.PERFORMER_NAME] =
			ID3v23FieldKey.PERFORMER_NAME
		tagFieldToId3[FieldKey.PERFORMER_NAME_SORT] =
			ID3v23FieldKey.PERFORMER_NAME_SORT
		tagFieldToId3[FieldKey.PERIOD] = ID3v23FieldKey.PERIOD
		tagFieldToId3[FieldKey.PRODUCER] =
			ID3v23FieldKey.PRODUCER
		tagFieldToId3[FieldKey.PRODUCER_SORT] =
			ID3v23FieldKey.PRODUCER_SORT
		tagFieldToId3[FieldKey.QUALITY] =
			ID3v23FieldKey.QUALITY
		tagFieldToId3[FieldKey.RANKING] =
			ID3v23FieldKey.RANKING
		tagFieldToId3[FieldKey.RATING] = ID3v23FieldKey.RATING
		tagFieldToId3[FieldKey.RECORD_LABEL] =
			ID3v23FieldKey.RECORD_LABEL
		tagFieldToId3[FieldKey.RECORDINGDATE] =
			ID3v23FieldKey.RECORDINGDATE
		tagFieldToId3[FieldKey.RECORDINGSTARTDATE] =
			ID3v23FieldKey.RECORDINGSTARTDATE
		tagFieldToId3[FieldKey.RECORDINGENDDATE] =
			ID3v23FieldKey.RECORDINGENDDATE
		tagFieldToId3[FieldKey.RECORDINGLOCATION] =
			ID3v23FieldKey.RECORDINGLOCATION
		tagFieldToId3[FieldKey.REMIXER] =
			ID3v23FieldKey.REMIXER
		tagFieldToId3[FieldKey.ROONALBUMTAG] = ID3v23FieldKey.ROONALBUMTAG
		tagFieldToId3[FieldKey.ROONTRACKTAG] = ID3v23FieldKey.ROONTRACKTAG
		tagFieldToId3[FieldKey.SCRIPT] = ID3v23FieldKey.SCRIPT
		tagFieldToId3[FieldKey.SECTION] = ID3v23FieldKey.SECTION
		tagFieldToId3[FieldKey.SINGLE_DISC_TRACK_NO] =
			ID3v23FieldKey.SINGLE_DISC_TRACK_NO
		tagFieldToId3[FieldKey.SONGKONG_ID] = ID3v23FieldKey.SONGKONG_ID
		tagFieldToId3[FieldKey.SUBTITLE] =
			ID3v23FieldKey.SUBTITLE
		tagFieldToId3[FieldKey.TAGS] = ID3v23FieldKey.TAGS
		tagFieldToId3[FieldKey.TEMPO] = ID3v23FieldKey.TEMPO
		tagFieldToId3[FieldKey.TIMBRE] = ID3v23FieldKey.TIMBRE
		tagFieldToId3[FieldKey.TITLE] =
			ID3v23FieldKey.TITLE
		tagFieldToId3[FieldKey.TITLE_MOVEMENT] =
			ID3v23FieldKey.TITLE_MOVEMENT
		tagFieldToId3[FieldKey.TITLE_SORT] =
			ID3v23FieldKey.TITLE_SORT
		tagFieldToId3[FieldKey.TONALITY] =
			ID3v23FieldKey.TONALITY
		tagFieldToId3[FieldKey.TRACK] =
			ID3v23FieldKey.TRACK
		tagFieldToId3[FieldKey.TRACK_TOTAL] =
			ID3v23FieldKey.TRACK_TOTAL
		tagFieldToId3[FieldKey.URL_DISCOGS_ARTIST_SITE] =
			ID3v23FieldKey.URL_DISCOGS_ARTIST_SITE
		tagFieldToId3[FieldKey.URL_DISCOGS_RELEASE_SITE] =
			ID3v23FieldKey.URL_DISCOGS_RELEASE_SITE
		tagFieldToId3[FieldKey.URL_LYRICS_SITE] =
			ID3v23FieldKey.URL_LYRICS_SITE
		tagFieldToId3[FieldKey.URL_OFFICIAL_ARTIST_SITE] =
			ID3v23FieldKey.URL_OFFICIAL_ARTIST_SITE
		tagFieldToId3[FieldKey.URL_OFFICIAL_RELEASE_SITE] =
			ID3v23FieldKey.URL_OFFICIAL_RELEASE_SITE
		tagFieldToId3[FieldKey.URL_WIKIPEDIA_ARTIST_SITE] =
			ID3v23FieldKey.URL_WIKIPEDIA_ARTIST_SITE
		tagFieldToId3[FieldKey.URL_WIKIPEDIA_RELEASE_SITE] =
			ID3v23FieldKey.URL_WIKIPEDIA_RELEASE_SITE
		tagFieldToId3[FieldKey.WORK] =
			ID3v23FieldKey.WORK
		tagFieldToId3[FieldKey.MUSICBRAINZ_RECORDING_WORK] =
			ID3v23FieldKey.MUSICBRAINZ_RECORDING_WORK
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL1] =
			ID3v23FieldKey.WORK_PART_LEVEL1
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL1_TYPE] =
			ID3v23FieldKey.WORK_PART_LEVEL1_TYPE
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL2] =
			ID3v23FieldKey.WORK_PART_LEVEL2
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL2_TYPE] =
			ID3v23FieldKey.WORK_PART_LEVEL2_TYPE
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL3] =
			ID3v23FieldKey.WORK_PART_LEVEL3
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL3_TYPE] =
			ID3v23FieldKey.WORK_PART_LEVEL3_TYPE
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL4] =
			ID3v23FieldKey.WORK_PART_LEVEL4
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL4_TYPE] =
			ID3v23FieldKey.WORK_PART_LEVEL4_TYPE
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL5] =
			ID3v23FieldKey.WORK_PART_LEVEL5
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL5_TYPE] =
			ID3v23FieldKey.WORK_PART_LEVEL5_TYPE
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL6] =
			ID3v23FieldKey.WORK_PART_LEVEL6
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL6_TYPE] =
			ID3v23FieldKey.WORK_PART_LEVEL6_TYPE
		tagFieldToId3[FieldKey.VERSION] =
			ID3v23FieldKey.VERSION
		tagFieldToId3[FieldKey.WORK_TYPE] =
			ID3v23FieldKey.WORK_TYPE
		tagFieldToId3[FieldKey.YEAR] =
			ID3v23FieldKey.YEAR
		populateId3ToTagField()
	}

	private fun populateId3ToTagField() {
		for ((key, value) in tagFieldToId3) {
			id3ToTagField[value] = key
		}
	}

	override fun setITunes12_6WorkGroupingMode(id3v2ITunes12_6Mode: Boolean) {
		if (id3v2ITunes12_6Mode) {
			tagFieldToId3[FieldKey.WORK] =
				ID3v23FieldKey.GROUPING
			tagFieldToId3[FieldKey.GROUPING] =
				ID3v23FieldKey.ITUNES_GROUPING
		} else {
			tagFieldToId3[FieldKey.WORK] =
				ID3v23FieldKey.WORK
			tagFieldToId3[FieldKey.GROUPING] =
				ID3v23FieldKey.GROUPING
		}
		populateId3ToTagField()
	}

	/**
	 * @param genericKey
	 * @return id3 key for generic key
	 */
	fun getId3KeyFromGenericKey(genericKey: FieldKey): ID3v23FieldKey? {
		return tagFieldToId3[genericKey]
	}

	/**
	 * Get generic key for ID3 field key
	 * @param fieldKey
	 * @return
	 */
	fun getGenericKeyFromId3(fieldKey: ID3v23FieldKey): FieldKey? {
		return id3ToTagField[fieldKey]
	}

	companion object {
		/**
		 * Define all frames that are valid within ID3v23
		 * Frame IDs beginning with T are text frames, and with W are url frames
		 */
		const val FRAME_ID_V3_ACCOMPANIMENT = "TPE2"
		const val FRAME_ID_V3_ALBUM = "TALB"
		const val FRAME_ID_V3_ARTIST = "TPE1"
		const val FRAME_ID_V3_ATTACHED_PICTURE = "APIC"
		const val FRAME_ID_V3_AUDIO_ENCRYPTION = "AENC"
		const val FRAME_ID_V3_BPM = "TBPM"
		const val FRAME_ID_V3_CHAPTER = ID3v2ChapterFrames.FRAME_ID_CHAPTER
		const val FRAME_ID_V3_CHAPTER_TOC = ID3v2ChapterFrames.FRAME_ID_TABLE_OF_CONTENT
		const val FRAME_ID_V3_COMMENT = "COMM"
		const val FRAME_ID_V3_COMMERCIAL_FRAME = "COMR"
		const val FRAME_ID_V3_COMPOSER = "TCOM"
		const val FRAME_ID_V3_CONDUCTOR = "TPE3"
		const val FRAME_ID_V3_CONTENT_GROUP_DESC = "TIT1"
		const val FRAME_ID_V3_COPYRIGHTINFO = "TCOP"
		const val FRAME_ID_V3_ENCODEDBY = "TENC"
		const val FRAME_ID_V3_ENCRYPTION = "ENCR"

		/**
		 * The ID3v2 frame identifier
		 *
		 * @return the ID3v2 frame identifier  for this frame type
		 */
		const val FRAME_ID_V3_EQUALISATION = "EQUA"
		const val FRAME_ID_V3_EVENT_TIMING_CODES = "ETCO"
		const val FRAME_ID_V3_FILE_OWNER = "TOWN"
		const val FRAME_ID_V3_FILE_TYPE = "TFLT"
		const val FRAME_ID_V3_GENERAL_ENCAPS_OBJECT = "GEOB"
		const val FRAME_ID_V3_GENRE = "TCON"
		const val FRAME_ID_V3_GROUP_ID_REG = "GRID"
		const val FRAME_ID_V3_HW_SW_SETTINGS = "TSSE"
		const val FRAME_ID_V3_INITIAL_KEY = "TKEY"

		/**
		 * The ID3v2 frame identifier
		 *
		 * @return the ID3v2 frame identifier  for this frame type
		 */
		const val FRAME_ID_V3_INVOLVED_PEOPLE = "IPLS"
		const val FRAME_ID_V3_ISRC = "TSRC"
		const val FRAME_ID_V3_ITUNES_GROUPING = "GRP1"
		const val FRAME_ID_V3_LANGUAGE = "TLAN"
		const val FRAME_ID_V3_LENGTH = "TLEN"
		const val FRAME_ID_V3_LINKED_INFO = "LINK"
		const val FRAME_ID_V3_LYRICIST = "TEXT"
		const val FRAME_ID_V3_MEDIA_TYPE = "TMED"
		const val FRAME_ID_V3_MOVEMENT = "MVNM"
		const val FRAME_ID_V3_MOVEMENT_NO = "MVIN"
		const val FRAME_ID_V3_MPEG_LOCATION_LOOKUP_TABLE = "MLLT"
		const val FRAME_ID_V3_MUSIC_CD_ID = "MCDI"
		const val FRAME_ID_V3_ORIGARTIST = "TOPE"
		const val FRAME_ID_V3_ORIG_FILENAME = "TOFN"
		const val FRAME_ID_V3_ORIG_LYRICIST = "TOLY"
		const val FRAME_ID_V3_ORIG_TITLE = "TOAL"
		const val FRAME_ID_V3_OWNERSHIP = "OWNE"
		const val FRAME_ID_V3_PLAYLIST_DELAY = "TDLY"
		const val FRAME_ID_V3_PLAY_COUNTER = "PCNT"
		const val FRAME_ID_V3_POPULARIMETER = "POPM"
		const val FRAME_ID_V3_POSITION_SYNC = "POSS"
		const val FRAME_ID_V3_PRIVATE = "PRIV"
		const val FRAME_ID_V3_PUBLISHER = "TPUB"
		const val FRAME_ID_V3_RADIO_NAME = "TRSN"
		const val FRAME_ID_V3_RADIO_OWNER = "TRSO"
		const val FRAME_ID_V3_RECOMMENDED_BUFFER_SIZE = "RBUF"

		/**
		 * The ID3v2 frame identifier
		 *
		 * @return the ID3v2 frame identifier  for this frame type
		 */
		const val FRAME_ID_V3_RELATIVE_VOLUME_ADJUSTMENT = "RVAD"
		const val FRAME_ID_V3_REMIXED = "TPE4"
		const val FRAME_ID_V3_REVERB = "RVRB"
		const val FRAME_ID_V3_SET = "TPOS"
		const val FRAME_ID_V3_SYNC_LYRIC = "SYLT"
		const val FRAME_ID_V3_SYNC_TEMPO = "SYTC"

		/**
		 * The ID3v2 frame identifier
		 *
		 * @return the ID3v2 frame identifier  for this frame type
		 */
		const val FRAME_ID_V3_TDAT = "TDAT"
		const val FRAME_ID_V3_TERMS_OF_USE = "USER"

		/**
		 * The ID3v2 frame identifier
		 *
		 * @return the ID3v2 frame identifier  for this frame type
		 */
		const val FRAME_ID_V3_TIME = "TIME"
		const val FRAME_ID_V3_TITLE = "TIT2"
		const val FRAME_ID_V3_TITLE_REFINEMENT = "TIT3"

		/**
		 * The ID3v2 frame identifier
		 *
		 * @return the ID3v2 frame identifier  for this frame type
		 */
		const val FRAME_ID_V3_TORY = "TORY"
		const val FRAME_ID_V3_TRACK = "TRCK"

		/**
		 * The ID3v2 frame identifier
		 *
		 * @return the ID3v2 frame identifier  for this frame type
		 */
		const val FRAME_ID_V3_TRDA = "TRDA"


		/**
		 * The ID3v2 frame identifier
		 *
		 * @return the ID3v2 frame identifier  for this frame type
		 */
		const val FRAME_ID_V3_TSIZ = "TSIZ"

		/**
		 * The ID3v2 frame identifier
		 *
		 * @return the ID3v2 frame identifier  for this frame type
		 */
		const val FRAME_ID_V3_TYER = "TYER"
		const val FRAME_ID_V3_UNIQUE_FILE_ID = "UFID"
		const val FRAME_ID_V3_UNSYNC_LYRICS = "USLT"
		const val FRAME_ID_V3_URL_ARTIST_WEB = "WOAR"
		const val FRAME_ID_V3_URL_COMMERCIAL = "WCOM"
		const val FRAME_ID_V3_URL_COPYRIGHT = "WCOP"
		const val FRAME_ID_V3_URL_FILE_WEB = "WOAF"
		const val FRAME_ID_V3_URL_OFFICIAL_RADIO = "WORS"
		const val FRAME_ID_V3_URL_PAYMENT = "WPAY"
		const val FRAME_ID_V3_URL_PUBLISHERS = "WPUB"
		const val FRAME_ID_V3_URL_SOURCE_WEB = "WOAS"
		const val FRAME_ID_V3_USER_DEFINED_INFO = "TXXX"
		const val FRAME_ID_V3_USER_DEFINED_URL = "WXXX"
		const val FRAME_ID_V3_IS_COMPILATION = "TCMP"
		const val FRAME_ID_V3_TITLE_SORT_ORDER_ITUNES = "TSOT"
		const val FRAME_ID_V3_ARTIST_SORT_ORDER_ITUNES = "TSOP"
		const val FRAME_ID_V3_ALBUM_SORT_ORDER_ITUNES = "TSOA"

		/**
		 * The ID3v2 frame identifier
		 *
		 * @return the ID3v2 frame identifier  for this frame type
		 */
		const val FRAME_ID_V3_TITLE_SORT_ORDER_MUSICBRAINZ = "XSOT"


		/**
		 * The ID3v2 frame identifier
		 *
		 * @return the ID3v2 frame identifier  for this frame type
		 */
		const val FRAME_ID_V3_ARTIST_SORT_ORDER_MUSICBRAINZ = "XSOP"

		/**
		 * The ID3v2 frame identifier
		 *
		 * @return the ID3v2 frame identifier  for this frame type
		 */
		const val FRAME_ID_V3_ALBUM_SORT_ORDER_MUSICBRAINZ = "XSOA"
		const val FRAME_ID_V3_ALBUM_ARTIST_SORT_ORDER_ITUNES = "TSO2"
		const val FRAME_ID_V3_COMPOSER_SORT_ORDER_ITUNES = "TSOC"
		const val FRAME_ID_V3_SET_SUBTITLE = "TSST"
		private var id3v23Frames: ID3v23Frames? = null

		@JvmStatic
		val instanceOf: ID3v23Frames
			get() {
				if (id3v23Frames == null) {
					id3v23Frames = ID3v23Frames()
				}
				return id3v23Frames!!
			}
	}
}
