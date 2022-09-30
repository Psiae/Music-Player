package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.FieldKey
import java.util.*

/** [org.jaudiotagger.tag.id3.ID3v24Frames] */
/**
 * Defines ID3v24 frames and collections that categorise frames.
 *
 *
 * You can include frames here that are not officially supported as long as they can be used within an
 * ID3v24Tag
 *
 * @author Paul Taylor
 * @version $Id$
 */
class ID3v24Frames private constructor() : ID3Frames() {
	protected var tagFieldToId3 = EnumMap<FieldKey, ID3v24FieldKey>(FieldKey::class.java)

	/**
	 * Maps from ID3 key to Generic key
	 */
	protected var id3ToTagField = EnumMap<ID3v24FieldKey, FieldKey>(ID3v24FieldKey::class.java)

	init {
		supportedFrames.add(FRAME_ID_ACCOMPANIMENT)
		supportedFrames.add(FRAME_ID_ALBUM)
		supportedFrames.add(FRAME_ID_ALBUM_SORT_ORDER)
		supportedFrames.add(FRAME_ID_ARTIST)
		supportedFrames.add(FRAME_ID_ATTACHED_PICTURE)
		supportedFrames.add(FRAME_ID_AUDIO_ENCRYPTION)
		supportedFrames.add(FRAME_ID_AUDIO_SEEK_POINT_INDEX)
		supportedFrames.add(FRAME_ID_BPM)
		supportedFrames.add(FRAME_ID_CHAPTER)
		supportedFrames.add(FRAME_ID_CHAPTER_TOC)
		supportedFrames.add(FRAME_ID_COMMENT)
		supportedFrames.add(FRAME_ID_COMMERCIAL_FRAME)
		supportedFrames.add(FRAME_ID_COMPOSER)
		supportedFrames.add(FRAME_ID_CONDUCTOR)
		supportedFrames.add(FRAME_ID_CONTENT_GROUP_DESC)
		supportedFrames.add(FRAME_ID_COPYRIGHTINFO)
		supportedFrames.add(FRAME_ID_ENCODEDBY)
		supportedFrames.add(FRAME_ID_ENCODING_TIME)
		supportedFrames.add(FRAME_ID_ENCRYPTION)
		supportedFrames.add(FRAME_ID_EQUALISATION2)
		supportedFrames.add(FRAME_ID_EVENT_TIMING_CODES)
		supportedFrames.add(FRAME_ID_FILE_OWNER)
		supportedFrames.add(FRAME_ID_FILE_TYPE)
		supportedFrames.add(FRAME_ID_GENERAL_ENCAPS_OBJECT)
		supportedFrames.add(FRAME_ID_GENRE)
		supportedFrames.add(FRAME_ID_GROUP_ID_REG)
		supportedFrames.add(FRAME_ID_HW_SW_SETTINGS)
		supportedFrames.add(FRAME_ID_INITIAL_KEY)
		supportedFrames.add(FRAME_ID_INVOLVED_PEOPLE)
		supportedFrames.add(FRAME_ID_ISRC)
		supportedFrames.add(FRAME_ID_ITUNES_GROUPING)
		supportedFrames.add(FRAME_ID_LANGUAGE)
		supportedFrames.add(FRAME_ID_LENGTH)
		supportedFrames.add(FRAME_ID_LINKED_INFO)
		supportedFrames.add(FRAME_ID_LYRICIST)
		supportedFrames.add(FRAME_ID_MEDIA_TYPE)
		supportedFrames.add(FRAME_ID_MOOD)
		supportedFrames.add(FRAME_ID_MOVEMENT)
		supportedFrames.add(FRAME_ID_MOVEMENT_NO)
		supportedFrames.add(FRAME_ID_MPEG_LOCATION_LOOKUP_TABLE)
		supportedFrames.add(FRAME_ID_MUSIC_CD_ID)
		supportedFrames.add(FRAME_ID_MUSICIAN_CREDITS)
		supportedFrames.add(FRAME_ID_ORIGARTIST)
		supportedFrames.add(FRAME_ID_ORIGINAL_RELEASE_TIME)
		supportedFrames.add(FRAME_ID_ORIG_FILENAME)
		supportedFrames.add(FRAME_ID_ORIG_LYRICIST)
		supportedFrames.add(FRAME_ID_ORIG_TITLE)
		supportedFrames.add(FRAME_ID_OWNERSHIP)
		supportedFrames.add(FRAME_ID_ARTIST_SORT_ORDER)
		supportedFrames.add(FRAME_ID_PLAYLIST_DELAY)
		supportedFrames.add(FRAME_ID_PLAY_COUNTER)
		supportedFrames.add(FRAME_ID_POPULARIMETER)
		supportedFrames.add(FRAME_ID_POSITION_SYNC)
		supportedFrames.add(FRAME_ID_PRIVATE)
		supportedFrames.add(FRAME_ID_PRODUCED_NOTICE)
		supportedFrames.add(FRAME_ID_PUBLISHER)
		supportedFrames.add(FRAME_ID_RADIO_NAME)
		supportedFrames.add(FRAME_ID_RADIO_OWNER)
		supportedFrames.add(FRAME_ID_RECOMMENDED_BUFFER_SIZE)
		supportedFrames.add(FRAME_ID_RELATIVE_VOLUME_ADJUSTMENT2)
		supportedFrames.add(FRAME_ID_RELEASE_TIME)
		supportedFrames.add(FRAME_ID_REMIXED)
		supportedFrames.add(FRAME_ID_REVERB)
		supportedFrames.add(FRAME_ID_SEEK)
		supportedFrames.add(FRAME_ID_SET)
		supportedFrames.add(FRAME_ID_SET_SUBTITLE)
		supportedFrames.add(FRAME_ID_SIGNATURE)
		supportedFrames.add(FRAME_ID_SYNC_LYRIC)
		supportedFrames.add(FRAME_ID_SYNC_TEMPO)
		supportedFrames.add(FRAME_ID_TAGGING_TIME)
		supportedFrames.add(FRAME_ID_TERMS_OF_USE)
		supportedFrames.add(FRAME_ID_TITLE)
		supportedFrames.add(FRAME_ID_TITLE_REFINEMENT)
		supportedFrames.add(FRAME_ID_TITLE_SORT_ORDER)
		supportedFrames.add(FRAME_ID_TRACK)
		supportedFrames.add(FRAME_ID_UNIQUE_FILE_ID)
		supportedFrames.add(FRAME_ID_UNSYNC_LYRICS)
		supportedFrames.add(FRAME_ID_URL_ARTIST_WEB)
		supportedFrames.add(FRAME_ID_URL_COMMERCIAL)
		supportedFrames.add(FRAME_ID_URL_COPYRIGHT)
		supportedFrames.add(FRAME_ID_URL_FILE_WEB)
		supportedFrames.add(FRAME_ID_URL_OFFICIAL_RADIO)
		supportedFrames.add(FRAME_ID_URL_PAYMENT)
		supportedFrames.add(FRAME_ID_URL_PUBLISHERS)
		supportedFrames.add(FRAME_ID_URL_SOURCE_WEB)
		supportedFrames.add(FRAME_ID_USER_DEFINED_INFO)
		supportedFrames.add(FRAME_ID_USER_DEFINED_URL)
		supportedFrames.add(FRAME_ID_YEAR)

		//Extension
		extensionFrames.add(FRAME_ID_IS_COMPILATION)
		extensionFrames.add(FRAME_ID_ALBUM_ARTIST_SORT_ORDER_ITUNES)
		extensionFrames.add(FRAME_ID_COMPOSER_SORT_ORDER_ITUNES)

		//Common
		commonFrames.add(FRAME_ID_ARTIST)
		commonFrames.add(FRAME_ID_ALBUM)
		commonFrames.add(FRAME_ID_TITLE)
		commonFrames.add(FRAME_ID_GENRE)
		commonFrames.add(FRAME_ID_TRACK)
		commonFrames.add(FRAME_ID_YEAR)
		commonFrames.add(FRAME_ID_COMMENT)

		//Binary
		binaryFrames.add(FRAME_ID_ATTACHED_PICTURE)
		binaryFrames.add(FRAME_ID_AUDIO_ENCRYPTION)
		binaryFrames.add(FRAME_ID_ENCRYPTION)
		binaryFrames.add(FRAME_ID_EQUALISATION2)
		binaryFrames.add(FRAME_ID_EVENT_TIMING_CODES)
		binaryFrames.add(FRAME_ID_GENERAL_ENCAPS_OBJECT)
		binaryFrames.add(FRAME_ID_RELATIVE_VOLUME_ADJUSTMENT2)
		binaryFrames.add(FRAME_ID_RECOMMENDED_BUFFER_SIZE)
		binaryFrames.add(FRAME_ID_UNIQUE_FILE_ID)
		// Map frameid to a name
		idToValue[FRAME_ID_ACCOMPANIMENT] = "Text: Band/Orchestra/Accompaniment"
		idToValue[FRAME_ID_ALBUM] = "Text: Album/Movie/Show title"
		idToValue[FRAME_ID_ALBUM_SORT_ORDER] = "Album sort order"
		idToValue[FRAME_ID_ARTIST] =
			"Text: Lead artist(s)/Lead performer(s)/Soloist(s)/Performing group"
		idToValue[FRAME_ID_ATTACHED_PICTURE] = "Attached picture"
		idToValue[FRAME_ID_AUDIO_ENCRYPTION] = "Audio encryption"
		idToValue[FRAME_ID_AUDIO_SEEK_POINT_INDEX] = "Audio seek point index"
		idToValue[FRAME_ID_BPM] = "Text: BPM (Beats Per Minute)"
		idToValue[FRAME_ID_CHAPTER] = "Chapter"
		idToValue[FRAME_ID_CHAPTER_TOC] = "Chapter TOC"
		idToValue[FRAME_ID_COMMENT] = "Comments"
		idToValue[FRAME_ID_COMMERCIAL_FRAME] = "Commercial Frame"
		idToValue[FRAME_ID_COMPOSER] = "Text: Composer"
		idToValue[FRAME_ID_CONDUCTOR] = "Text: Conductor/Performer refinement"
		idToValue[FRAME_ID_CONTENT_GROUP_DESC] = "Text: Content group description"
		idToValue[FRAME_ID_COPYRIGHTINFO] = "Text: Copyright message"
		idToValue[FRAME_ID_ENCODEDBY] = "Text: Encoded by"
		idToValue[FRAME_ID_ENCODING_TIME] = "Text: Encoding time"
		idToValue[FRAME_ID_ENCRYPTION] = "Encryption method registration"
		idToValue[FRAME_ID_EQUALISATION2] = "Equalization (2)"
		idToValue[FRAME_ID_EVENT_TIMING_CODES] = "Event timing codes"
		idToValue[FRAME_ID_FILE_OWNER] = "Text:File Owner"
		idToValue[FRAME_ID_FILE_TYPE] = "Text: File type"
		idToValue[FRAME_ID_GENERAL_ENCAPS_OBJECT] = "General encapsulated datatype"
		idToValue[FRAME_ID_GENRE] = "Text: Content type"
		idToValue[FRAME_ID_GROUP_ID_REG] = "Group ID Registration"
		idToValue[FRAME_ID_HW_SW_SETTINGS] = "Text: Software/hardware and settings used for encoding"
		idToValue[FRAME_ID_INITIAL_KEY] = "Text: Initial key"
		idToValue[FRAME_ID_INVOLVED_PEOPLE] = "Involved people list"
		idToValue[FRAME_ID_ISRC] = "Text: ISRC (International Standard Recording Code)"
		idToValue[FRAME_ID_ITUNES_GROUPING] = "iTunes Grouping"
		idToValue[FRAME_ID_LANGUAGE] = "Text: Language(s)"
		idToValue[FRAME_ID_LENGTH] = "Text: Length"
		idToValue[FRAME_ID_LINKED_INFO] = "Linked information"
		idToValue[FRAME_ID_LYRICIST] = "Text: Lyricist/text writer"
		idToValue[FRAME_ID_MEDIA_TYPE] = "Text: Media type"
		idToValue[FRAME_ID_MOOD] = "Text: Mood"
		idToValue[FRAME_ID_MOVEMENT] = "Text: Movement"
		idToValue[FRAME_ID_MOVEMENT_NO] = "Text: Movement No"
		idToValue[FRAME_ID_MPEG_LOCATION_LOOKUP_TABLE] = "MPEG location lookup table"
		idToValue[FRAME_ID_MUSIC_CD_ID] = "Music CD Identifier"
		idToValue[FRAME_ID_ORIGARTIST] = "Text: Original artist(s)/performer(s)"
		idToValue[FRAME_ID_ORIGINAL_RELEASE_TIME] = "Text: Original release time"
		idToValue[FRAME_ID_ORIG_FILENAME] = "Text: Original filename"
		idToValue[FRAME_ID_ORIG_LYRICIST] = "Text: Original Lyricist(s)/text writer(s)"
		idToValue[FRAME_ID_ORIG_TITLE] = "Text: Original album/Movie/Show title"
		idToValue[FRAME_ID_OWNERSHIP] = "Ownership"
		idToValue[FRAME_ID_ARTIST_SORT_ORDER] = "Performance Sort Order"
		idToValue[FRAME_ID_PLAYLIST_DELAY] = "Text: Playlist delay"
		idToValue[FRAME_ID_PLAY_COUNTER] = "Play counter"
		idToValue[FRAME_ID_POPULARIMETER] = "Popularimeter"
		idToValue[FRAME_ID_POSITION_SYNC] = "Position Sync"
		idToValue[FRAME_ID_PRIVATE] = "Private frame"
		idToValue[FRAME_ID_PRODUCED_NOTICE] = "Produced Notice"
		idToValue[FRAME_ID_PUBLISHER] = "Text: Publisher"
		idToValue[FRAME_ID_RADIO_NAME] = "Text: Radio Name"
		idToValue[FRAME_ID_RADIO_OWNER] = "Text: Radio Owner"
		idToValue[FRAME_ID_RECOMMENDED_BUFFER_SIZE] = "Recommended buffer size"
		idToValue[FRAME_ID_RELATIVE_VOLUME_ADJUSTMENT2] = "Relative volume adjustment(2)"
		idToValue[FRAME_ID_RELEASE_TIME] = "Release Time"
		idToValue[FRAME_ID_REMIXED] = "Text: Interpreted, remixed, or otherwise modified by"
		idToValue[FRAME_ID_REVERB] = "Reverb"
		idToValue[FRAME_ID_SEEK] = "Seek"
		idToValue[FRAME_ID_SET] = "Text: Part of a setField"
		idToValue[FRAME_ID_SET_SUBTITLE] = "Text: Set subtitle"
		idToValue[FRAME_ID_SIGNATURE] = "Signature"
		idToValue[FRAME_ID_SYNC_LYRIC] = "Synchronized lyric/text"
		idToValue[FRAME_ID_SYNC_TEMPO] = "Synced tempo codes"
		idToValue[FRAME_ID_TAGGING_TIME] = "Text: Tagging time"
		idToValue[FRAME_ID_TERMS_OF_USE] = "Terms of Use"
		idToValue[FRAME_ID_TITLE] = "Text: title"
		idToValue[FRAME_ID_TITLE_REFINEMENT] = "Text: Subtitle/Description refinement"
		idToValue[FRAME_ID_TITLE_SORT_ORDER] = "Text: title sort order"
		idToValue[FRAME_ID_TRACK] = "Text: Track number/Position in setField"
		idToValue[FRAME_ID_UNIQUE_FILE_ID] = "Unique file identifier"
		idToValue[FRAME_ID_UNSYNC_LYRICS] = "Unsychronized lyric/text transcription"
		idToValue[FRAME_ID_URL_ARTIST_WEB] = "URL: Official artist/performer webpage"
		idToValue[FRAME_ID_URL_COMMERCIAL] = "URL: Commercial information"
		idToValue[FRAME_ID_URL_COPYRIGHT] = "URL: Copyright/Legal information"
		idToValue[FRAME_ID_URL_FILE_WEB] = "URL: Official audio file webpage"
		idToValue[FRAME_ID_URL_OFFICIAL_RADIO] = "URL: Official Radio website"
		idToValue[FRAME_ID_URL_PAYMENT] = "URL: Payment for this recording "
		idToValue[FRAME_ID_URL_PUBLISHERS] = "URL: Publishers official webpage"
		idToValue[FRAME_ID_URL_SOURCE_WEB] = "URL: Official audio source webpage"
		idToValue[FRAME_ID_USER_DEFINED_INFO] = "User defined text information frame"
		idToValue[FRAME_ID_USER_DEFINED_URL] = "User defined URL link frame"
		idToValue[FRAME_ID_YEAR] = "Text:Year"
		idToValue[FRAME_ID_IS_COMPILATION] = "Is Compilation"
		idToValue[FRAME_ID_ALBUM_ARTIST_SORT_ORDER_ITUNES] = "Text:Album Artist Sort Order Frame"
		idToValue[FRAME_ID_COMPOSER_SORT_ORDER_ITUNES] = "Text:Composer Sort Order Frame"
		createMaps()
		multipleFrames.add(FRAME_ID_USER_DEFINED_INFO)
		multipleFrames.add(FRAME_ID_USER_DEFINED_URL)
		multipleFrames.add(FRAME_ID_ATTACHED_PICTURE)
		multipleFrames.add(FRAME_ID_PRIVATE)
		multipleFrames.add(FRAME_ID_COMMENT)
		multipleFrames.add(FRAME_ID_UNIQUE_FILE_ID)
		multipleFrames.add(FRAME_ID_UNSYNC_LYRICS)
		multipleFrames.add(FRAME_ID_POPULARIMETER)
		multipleFrames.add(FRAME_ID_GENERAL_ENCAPS_OBJECT)
		multipleFrames.add(FRAME_ID_URL_ARTIST_WEB)
		multipleFrames.add(FRAME_ID_RELATIVE_VOLUME_ADJUSTMENT2)
		discardIfFileAlteredFrames.add(FRAME_ID_EVENT_TIMING_CODES)
		discardIfFileAlteredFrames.add(FRAME_ID_MPEG_LOCATION_LOOKUP_TABLE)
		discardIfFileAlteredFrames.add(FRAME_ID_POSITION_SYNC)
		discardIfFileAlteredFrames.add(FRAME_ID_SYNC_LYRIC)
		discardIfFileAlteredFrames.add(FRAME_ID_SYNC_TEMPO)
		discardIfFileAlteredFrames.add(FRAME_ID_EVENT_TIMING_CODES)
		discardIfFileAlteredFrames.add(FRAME_ID_ENCODEDBY)
		discardIfFileAlteredFrames.add(FRAME_ID_LENGTH)
		tagFieldToId3[FieldKey.ACOUSTID_FINGERPRINT] = ID3v24FieldKey.ACOUSTID_FINGERPRINT
		tagFieldToId3[FieldKey.ACOUSTID_ID] = ID3v24FieldKey.ACOUSTID_ID
		tagFieldToId3[FieldKey.ALBUM] = ID3v24FieldKey.ALBUM
		tagFieldToId3[FieldKey.ALBUM_ARTIST] = ID3v24FieldKey.ALBUM_ARTIST
		tagFieldToId3[FieldKey.ALBUM_ARTIST_SORT] = ID3v24FieldKey.ALBUM_ARTIST_SORT
		tagFieldToId3[FieldKey.ALBUM_ARTISTS] = ID3v24FieldKey.ALBUM_ARTISTS
		tagFieldToId3[FieldKey.ALBUM_ARTISTS_SORT] = ID3v24FieldKey.ALBUM_ARTISTS_SORT
		tagFieldToId3[FieldKey.ALBUM_SORT] = ID3v24FieldKey.ALBUM_SORT
		tagFieldToId3[FieldKey.ALBUM_YEAR] = ID3v24FieldKey.ALBUM_YEAR
		tagFieldToId3[FieldKey.AMAZON_ID] = ID3v24FieldKey.AMAZON_ID
		tagFieldToId3[FieldKey.ARRANGER] = ID3v24FieldKey.ARRANGER
		tagFieldToId3[FieldKey.ARRANGER_SORT] = ID3v24FieldKey.ARRANGER_SORT
		tagFieldToId3[FieldKey.ARTIST] = ID3v24FieldKey.ARTIST
		tagFieldToId3[FieldKey.ARTISTS] = ID3v24FieldKey.ARTISTS
		tagFieldToId3[FieldKey.ARTISTS_SORT] = ID3v24FieldKey.ARTISTS_SORT
		tagFieldToId3[FieldKey.ARTIST_SORT] = ID3v24FieldKey.ARTIST_SORT
		tagFieldToId3[FieldKey.BARCODE] = ID3v24FieldKey.BARCODE
		tagFieldToId3[FieldKey.BPM] = ID3v24FieldKey.BPM
		tagFieldToId3[FieldKey.CATALOG_NO] = ID3v24FieldKey.CATALOG_NO
		tagFieldToId3[FieldKey.CHOIR] = ID3v24FieldKey.CHOIR
		tagFieldToId3[FieldKey.CHOIR_SORT] = ID3v24FieldKey.CHOIR_SORT
		tagFieldToId3[FieldKey.CLASSICAL_CATALOG] = ID3v24FieldKey.CLASSICAL_CATALOG
		tagFieldToId3[FieldKey.CLASSICAL_NICKNAME] = ID3v24FieldKey.CLASSICAL_NICKNAME
		tagFieldToId3[FieldKey.COMMENT] = ID3v24FieldKey.COMMENT
		tagFieldToId3[FieldKey.COMPOSER] = ID3v24FieldKey.COMPOSER
		tagFieldToId3[FieldKey.COMPOSER_SORT] = ID3v24FieldKey.COMPOSER_SORT
		tagFieldToId3[FieldKey.CONDUCTOR] = ID3v24FieldKey.CONDUCTOR
		tagFieldToId3[FieldKey.CONDUCTOR_SORT] = ID3v24FieldKey.CONDUCTOR_SORT
		tagFieldToId3[FieldKey.COPYRIGHT] = ID3v24FieldKey.COPYRIGHT
		tagFieldToId3[FieldKey.COUNTRY] = ID3v24FieldKey.COUNTRY
		tagFieldToId3[FieldKey.COVER_ART] = ID3v24FieldKey.COVER_ART
		tagFieldToId3[FieldKey.CUSTOM1] = ID3v24FieldKey.CUSTOM1
		tagFieldToId3[FieldKey.CUSTOM2] = ID3v24FieldKey.CUSTOM2
		tagFieldToId3[FieldKey.CUSTOM3] = ID3v24FieldKey.CUSTOM3
		tagFieldToId3[FieldKey.CUSTOM4] = ID3v24FieldKey.CUSTOM4
		tagFieldToId3[FieldKey.CUSTOM5] = ID3v24FieldKey.CUSTOM5
		tagFieldToId3[FieldKey.DISC_NO] = ID3v24FieldKey.DISC_NO
		tagFieldToId3[FieldKey.DISC_SUBTITLE] = ID3v24FieldKey.DISC_SUBTITLE
		tagFieldToId3[FieldKey.DISC_TOTAL] = ID3v24FieldKey.DISC_NO
		tagFieldToId3[FieldKey.DJMIXER] = ID3v24FieldKey.DJMIXER
		tagFieldToId3[FieldKey.DJMIXER_SORT] = ID3v24FieldKey.DJMIXER_SORT
		tagFieldToId3[FieldKey.MOOD_ELECTRONIC] = ID3v24FieldKey.MOOD_ELECTRONIC
		tagFieldToId3[FieldKey.ENCODER] = ID3v24FieldKey.ENCODER
		tagFieldToId3[FieldKey.ENGINEER] = ID3v24FieldKey.ENGINEER
		tagFieldToId3[FieldKey.ENGINEER_SORT] = ID3v24FieldKey.ENGINEER_SORT
		tagFieldToId3[FieldKey.ENSEMBLE] = ID3v24FieldKey.ENSEMBLE
		tagFieldToId3[FieldKey.ENSEMBLE_SORT] = ID3v24FieldKey.ENSEMBLE_SORT
		tagFieldToId3[FieldKey.FBPM] = ID3v24FieldKey.FBPM
		tagFieldToId3[FieldKey.GENRE] = ID3v24FieldKey.GENRE
		tagFieldToId3[FieldKey.GROUP] = ID3v24FieldKey.GROUP
		tagFieldToId3[FieldKey.GROUPING] = ID3v24FieldKey.GROUPING
		tagFieldToId3[FieldKey.INSTRUMENT] = ID3v24FieldKey.INSTRUMENT
		tagFieldToId3[FieldKey.INVOLVEDPEOPLE] = ID3v24FieldKey.INVOLVED_PEOPLE
		tagFieldToId3[FieldKey.IPI] = ID3v24FieldKey.IPI
		tagFieldToId3[FieldKey.ISRC] = ID3v24FieldKey.ISRC
		tagFieldToId3[FieldKey.ISWC] = ID3v24FieldKey.ISWC
		tagFieldToId3[FieldKey.IS_CLASSICAL] = ID3v24FieldKey.IS_CLASSICAL
		tagFieldToId3[FieldKey.IS_COMPILATION] = ID3v24FieldKey.IS_COMPILATION
		tagFieldToId3[FieldKey.IS_SOUNDTRACK] = ID3v24FieldKey.IS_SOUNDTRACK
		tagFieldToId3[FieldKey.IS_GREATEST_HITS] = ID3v24FieldKey.IS_GREATEST_HITS
		tagFieldToId3[FieldKey.IS_HD] = ID3v24FieldKey.IS_HD
		tagFieldToId3[FieldKey.IS_LIVE] = ID3v24FieldKey.IS_LIVE
		tagFieldToId3[FieldKey.ITUNES_GROUPING] = ID3v24FieldKey.ITUNES_GROUPING
		tagFieldToId3[FieldKey.JAIKOZ_ID] = ID3v24FieldKey.JAIKOZ_ID
		tagFieldToId3[FieldKey.KEY] = ID3v24FieldKey.KEY
		tagFieldToId3[FieldKey.LANGUAGE] = ID3v24FieldKey.LANGUAGE
		tagFieldToId3[FieldKey.LYRICIST] = ID3v24FieldKey.LYRICIST
		tagFieldToId3[FieldKey.LYRICIST_SORT] = ID3v24FieldKey.LYRICIST_SORT
		tagFieldToId3[FieldKey.LYRICS] = ID3v24FieldKey.LYRICS
		tagFieldToId3[FieldKey.MEDIA] = ID3v24FieldKey.MEDIA
		tagFieldToId3[FieldKey.MIXER] = ID3v24FieldKey.MIXER
		tagFieldToId3[FieldKey.MIXER_SORT] = ID3v24FieldKey.MIXER_SORT
		tagFieldToId3[FieldKey.MOOD] = ID3v24FieldKey.MOOD
		tagFieldToId3[FieldKey.MOOD_ACOUSTIC] = ID3v24FieldKey.MOOD_ACOUSTIC
		tagFieldToId3[FieldKey.MOOD_AGGRESSIVE] = ID3v24FieldKey.MOOD_AGGRESSIVE
		tagFieldToId3[FieldKey.MOOD_AROUSAL] = ID3v24FieldKey.MOOD_AROUSAL
		tagFieldToId3[FieldKey.MOOD_DANCEABILITY] = ID3v24FieldKey.MOOD_DANCEABILITY
		tagFieldToId3[FieldKey.MOOD_HAPPY] = ID3v24FieldKey.MOOD_HAPPY
		tagFieldToId3[FieldKey.MOOD_INSTRUMENTAL] = ID3v24FieldKey.MOOD_INSTRUMENTAL
		tagFieldToId3[FieldKey.MOOD_PARTY] = ID3v24FieldKey.MOOD_PARTY
		tagFieldToId3[FieldKey.MOOD_RELAXED] = ID3v24FieldKey.MOOD_RELAXED
		tagFieldToId3[FieldKey.MOOD_SAD] = ID3v24FieldKey.MOOD_SAD
		tagFieldToId3[FieldKey.MOOD_VALENCE] = ID3v24FieldKey.MOOD_VALENCE
		tagFieldToId3[FieldKey.MOVEMENT] = ID3v24FieldKey.MOVEMENT
		tagFieldToId3[FieldKey.MOVEMENT_NO] = ID3v24FieldKey.MOVEMENT_NO
		tagFieldToId3[FieldKey.MOVEMENT_TOTAL] = ID3v24FieldKey.MOVEMENT_TOTAL
		tagFieldToId3[FieldKey.MUSICBRAINZ_ARTISTID] = ID3v24FieldKey.MUSICBRAINZ_ARTISTID
		tagFieldToId3[FieldKey.MUSICBRAINZ_DISC_ID] = ID3v24FieldKey.MUSICBRAINZ_DISC_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_ORIGINAL_RELEASE_ID] =
			ID3v24FieldKey.MUSICBRAINZ_ORIGINAL_RELEASEID
		tagFieldToId3[FieldKey.MUSICBRAINZ_RELEASEARTISTID] = ID3v24FieldKey.MUSICBRAINZ_RELEASEARTISTID
		tagFieldToId3[FieldKey.MUSICBRAINZ_RELEASEID] = ID3v24FieldKey.MUSICBRAINZ_RELEASEID
		tagFieldToId3[FieldKey.MUSICBRAINZ_RELEASE_COUNTRY] = ID3v24FieldKey.MUSICBRAINZ_RELEASE_COUNTRY
		tagFieldToId3[FieldKey.MUSICBRAINZ_RELEASE_GROUP_ID] =
			ID3v24FieldKey.MUSICBRAINZ_RELEASE_GROUP_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_RELEASE_STATUS] = ID3v24FieldKey.MUSICBRAINZ_RELEASE_STATUS
		tagFieldToId3[FieldKey.MUSICBRAINZ_RELEASE_TRACK_ID] =
			ID3v24FieldKey.MUSICBRAINZ_RELEASE_TRACK_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_RELEASE_TYPE] = ID3v24FieldKey.MUSICBRAINZ_RELEASE_TYPE
		tagFieldToId3[FieldKey.MUSICBRAINZ_TRACK_ID] = ID3v24FieldKey.MUSICBRAINZ_TRACK_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK] = ID3v24FieldKey.MUSICBRAINZ_WORK
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_ID] = ID3v24FieldKey.MUSICBRAINZ_WORK_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_RECORDING_WORK] = ID3v24FieldKey.MUSICBRAINZ_RECORDING_WORK
		tagFieldToId3[FieldKey.MUSICBRAINZ_RECORDING_WORK_ID] =
			ID3v24FieldKey.MUSICBRAINZ_RECORDING_WORK_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL1_ID] =
			ID3v24FieldKey.MUSICBRAINZ_WORK_PART_LEVEL1_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL2_ID] =
			ID3v24FieldKey.MUSICBRAINZ_WORK_PART_LEVEL2_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL3_ID] =
			ID3v24FieldKey.MUSICBRAINZ_WORK_PART_LEVEL3_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL4_ID] =
			ID3v24FieldKey.MUSICBRAINZ_WORK_PART_LEVEL4_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL5_ID] =
			ID3v24FieldKey.MUSICBRAINZ_WORK_PART_LEVEL5_ID
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL6_ID] =
			ID3v24FieldKey.MUSICBRAINZ_WORK_PART_LEVEL6_ID
		tagFieldToId3[FieldKey.MUSICIP_ID] = ID3v24FieldKey.MUSICIP_ID
		tagFieldToId3[FieldKey.OCCASION] = ID3v24FieldKey.OCCASION
		tagFieldToId3[FieldKey.OPUS] = ID3v24FieldKey.OPUS
		tagFieldToId3[FieldKey.ORCHESTRA] = ID3v24FieldKey.ORCHESTRA
		tagFieldToId3[FieldKey.ORCHESTRA_SORT] = ID3v24FieldKey.ORCHESTRA_SORT
		tagFieldToId3[FieldKey.ORIGINAL_ALBUM] = ID3v24FieldKey.ORIGINAL_ALBUM
		tagFieldToId3[FieldKey.ORIGINALRELEASEDATE] = ID3v24FieldKey.ORIGINALRELEASEDATE
		tagFieldToId3[FieldKey.ORIGINAL_ARTIST] = ID3v24FieldKey.ORIGINAL_ARTIST
		tagFieldToId3[FieldKey.ORIGINAL_LYRICIST] = ID3v24FieldKey.ORIGINAL_LYRICIST
		tagFieldToId3[FieldKey.ORIGINAL_YEAR] = ID3v24FieldKey.ORIGINAL_YEAR
		tagFieldToId3[FieldKey.OVERALL_WORK] = ID3v24FieldKey.OVERALL_WORK
		tagFieldToId3[FieldKey.PART] = ID3v24FieldKey.PART
		tagFieldToId3[FieldKey.PART_NUMBER] = ID3v24FieldKey.PART_NUMBER
		tagFieldToId3[FieldKey.PART_TYPE] = ID3v24FieldKey.PART_TYPE
		tagFieldToId3[FieldKey.PERFORMER] = ID3v24FieldKey.PERFORMER
		tagFieldToId3[FieldKey.PERFORMER_NAME] = ID3v24FieldKey.PERFORMER_NAME
		tagFieldToId3[FieldKey.PERFORMER_NAME_SORT] = ID3v24FieldKey.PERFORMER_NAME_SORT
		tagFieldToId3[FieldKey.PERIOD] = ID3v24FieldKey.PERIOD
		tagFieldToId3[FieldKey.PRODUCER] = ID3v24FieldKey.PRODUCER
		tagFieldToId3[FieldKey.PRODUCER_SORT] = ID3v24FieldKey.PRODUCER_SORT
		tagFieldToId3[FieldKey.QUALITY] = ID3v24FieldKey.QUALITY
		tagFieldToId3[FieldKey.RANKING] = ID3v24FieldKey.RANKING
		tagFieldToId3[FieldKey.RATING] = ID3v24FieldKey.RATING
		tagFieldToId3[FieldKey.RECORD_LABEL] = ID3v24FieldKey.RECORD_LABEL
		tagFieldToId3[FieldKey.RECORDINGDATE] = ID3v24FieldKey.RECORDINGDATE
		tagFieldToId3[FieldKey.RECORDINGSTARTDATE] = ID3v24FieldKey.RECORDINGSTARTDATE
		tagFieldToId3[FieldKey.RECORDINGENDDATE] = ID3v24FieldKey.RECORDINGENDDATE
		tagFieldToId3[FieldKey.RECORDINGLOCATION] = ID3v24FieldKey.RECORDINGLOCATION
		tagFieldToId3[FieldKey.REMIXER] = ID3v24FieldKey.REMIXER
		tagFieldToId3[FieldKey.ROONALBUMTAG] = ID3v24FieldKey.ROONALBUMTAG
		tagFieldToId3[FieldKey.ROONTRACKTAG] = ID3v24FieldKey.ROONTRACKTAG
		tagFieldToId3[FieldKey.SCRIPT] = ID3v24FieldKey.SCRIPT
		tagFieldToId3[FieldKey.SECTION] = ID3v24FieldKey.SECTION
		tagFieldToId3[FieldKey.SINGLE_DISC_TRACK_NO] = ID3v24FieldKey.SINGLE_DISC_TRACK_NO
		tagFieldToId3[FieldKey.SONGKONG_ID] = ID3v24FieldKey.SONGKONG_ID
		tagFieldToId3[FieldKey.SUBTITLE] = ID3v24FieldKey.SUBTITLE
		tagFieldToId3[FieldKey.TAGS] = ID3v24FieldKey.TAGS
		tagFieldToId3[FieldKey.TEMPO] = ID3v24FieldKey.TEMPO
		tagFieldToId3[FieldKey.TIMBRE] = ID3v24FieldKey.TIMBRE
		tagFieldToId3[FieldKey.TITLE] = ID3v24FieldKey.TITLE
		tagFieldToId3[FieldKey.TITLE_MOVEMENT] = ID3v24FieldKey.TITLE_MOVEMENT
		tagFieldToId3[FieldKey.TITLE_SORT] = ID3v24FieldKey.TITLE_SORT
		tagFieldToId3[FieldKey.TONALITY] = ID3v24FieldKey.TONALITY
		tagFieldToId3[FieldKey.TRACK] = ID3v24FieldKey.TRACK
		tagFieldToId3[FieldKey.TRACK_TOTAL] = ID3v24FieldKey.TRACK_TOTAL
		tagFieldToId3[FieldKey.URL_DISCOGS_ARTIST_SITE] = ID3v24FieldKey.URL_DISCOGS_ARTIST_SITE
		tagFieldToId3[FieldKey.URL_DISCOGS_RELEASE_SITE] = ID3v24FieldKey.URL_DISCOGS_RELEASE_SITE
		tagFieldToId3[FieldKey.URL_LYRICS_SITE] = ID3v24FieldKey.URL_LYRICS_SITE
		tagFieldToId3[FieldKey.URL_OFFICIAL_ARTIST_SITE] = ID3v24FieldKey.URL_OFFICIAL_ARTIST_SITE
		tagFieldToId3[FieldKey.URL_OFFICIAL_RELEASE_SITE] = ID3v24FieldKey.URL_OFFICIAL_RELEASE_SITE
		tagFieldToId3[FieldKey.URL_WIKIPEDIA_ARTIST_SITE] = ID3v24FieldKey.URL_WIKIPEDIA_ARTIST_SITE
		tagFieldToId3[FieldKey.URL_WIKIPEDIA_RELEASE_SITE] = ID3v24FieldKey.URL_WIKIPEDIA_RELEASE_SITE
		tagFieldToId3[FieldKey.WORK] = ID3v24FieldKey.WORK
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL1] = ID3v24FieldKey.WORK_PART_LEVEL1
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL1_TYPE] =
			ID3v24FieldKey.WORK_PART_LEVEL1_TYPE
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL2] = ID3v24FieldKey.WORK_PART_LEVEL2
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL2_TYPE] =
			ID3v24FieldKey.WORK_PART_LEVEL2_TYPE
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL3] = ID3v24FieldKey.WORK_PART_LEVEL3
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL3_TYPE] =
			ID3v24FieldKey.WORK_PARTOF_LEVEL3_TYPE
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL4] = ID3v24FieldKey.WORK_PART_LEVEL4
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL4_TYPE] =
			ID3v24FieldKey.WORK_PART_LEVEL4_TYPE
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL5] = ID3v24FieldKey.WORK_PART_LEVEL5
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL5_TYPE] =
			ID3v24FieldKey.WORK_PART_LEVEL5_TYPE
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL6] = ID3v24FieldKey.WORK_PART_LEVEL6
		tagFieldToId3[FieldKey.MUSICBRAINZ_WORK_PART_LEVEL6_TYPE] =
			ID3v24FieldKey.WORK_PART_LEVEL6_TYPE
		tagFieldToId3[FieldKey.VERSION] = ID3v24FieldKey.VERSION
		tagFieldToId3[FieldKey.WORK_TYPE] = ID3v24FieldKey.WORK_TYPE
		tagFieldToId3[FieldKey.YEAR] = ID3v24FieldKey.YEAR
		populateId3ToTagField()
	}

	private fun populateId3ToTagField() {
		for ((key: FieldKey, value: ID3v24FieldKey) in tagFieldToId3) {
			id3ToTagField[value] = key
		}
	}

	override fun setITunes12_6WorkGroupingMode(id3v2ITunes12_6Mode: Boolean) {
		if (id3v2ITunes12_6Mode) {
			tagFieldToId3[FieldKey.WORK] = ID3v24FieldKey.GROUPING
			tagFieldToId3[FieldKey.GROUPING] = ID3v24FieldKey.ITUNES_GROUPING
		} else {
			tagFieldToId3[FieldKey.WORK] = ID3v24FieldKey.WORK
			tagFieldToId3[FieldKey.GROUPING] = ID3v24FieldKey.GROUPING
		}
		populateId3ToTagField()
	}

	/**
	 * @param genericKey
	 * @return id3 key for generic key
	 */
	fun getId3KeyFromGenericKey(genericKey: FieldKey): ID3v24FieldKey? {
		return tagFieldToId3[genericKey]
	}

	/**
	 * Get generic key for ID3 field key
	 * @param fieldKey
	 * @return
	 */
	fun getGenericKeyFromId3(fieldKey: ID3v24FieldKey): FieldKey? {
		return id3ToTagField[fieldKey]
	}

	companion object {
		/**
		 * Frame IDs beginning with T are text frames, and with W are url frames
		 */
		const val FRAME_ID_ACCOMPANIMENT = "TPE2"
		const val FRAME_ID_ALBUM = "TALB"
		const val FRAME_ID_ALBUM_SORT_ORDER = "TSOA"
		const val FRAME_ID_ARTIST = "TPE1"
		const val FRAME_ID_ATTACHED_PICTURE = "APIC"
		const val FRAME_ID_AUDIO_ENCRYPTION = "AENC"
		const val FRAME_ID_AUDIO_SEEK_POINT_INDEX = "ASPI"
		const val FRAME_ID_BPM = "TBPM"
		const val FRAME_ID_CHAPTER = ID3v2ChapterFrames.FRAME_ID_CHAPTER
		const val FRAME_ID_CHAPTER_TOC = ID3v2ChapterFrames.FRAME_ID_TABLE_OF_CONTENT
		const val FRAME_ID_COMMENT = "COMM"
		const val FRAME_ID_COMMERCIAL_FRAME = "COMR"
		const val FRAME_ID_COMPOSER = "TCOM"
		const val FRAME_ID_CONDUCTOR = "TPE3"
		const val FRAME_ID_CONTENT_GROUP_DESC = "TIT1"
		const val FRAME_ID_COPYRIGHTINFO = "TCOP"
		const val FRAME_ID_ENCODEDBY = "TENC"
		const val FRAME_ID_ENCODING_TIME = "TDEN"
		const val FRAME_ID_ENCRYPTION = "ENCR"
		const val FRAME_ID_EQUALISATION2 = "EQU2"
		const val FRAME_ID_EVENT_TIMING_CODES = "ETCO"
		const val FRAME_ID_FILE_OWNER = "TOWN"
		const val FRAME_ID_FILE_TYPE = "TFLT"
		const val FRAME_ID_GENERAL_ENCAPS_OBJECT = "GEOB"
		const val FRAME_ID_GENRE = "TCON"
		const val FRAME_ID_GROUP_ID_REG = "GRID"
		const val FRAME_ID_HW_SW_SETTINGS = "TSSE"
		const val FRAME_ID_INITIAL_KEY = "TKEY"
		const val FRAME_ID_INVOLVED_PEOPLE = "TIPL"
		const val FRAME_ID_ISRC = "TSRC"
		const val FRAME_ID_ITUNES_GROUPING = "GRP1"
		const val FRAME_ID_LANGUAGE = "TLAN"
		const val FRAME_ID_LENGTH = "TLEN"
		const val FRAME_ID_LINKED_INFO = "LINK"
		const val FRAME_ID_LYRICIST = "TEXT"
		const val FRAME_ID_MEDIA_TYPE = "TMED"
		const val FRAME_ID_MOOD = "TMOO"
		const val FRAME_ID_MOVEMENT = "MVNM"
		const val FRAME_ID_MOVEMENT_NO = "MVIN"
		const val FRAME_ID_MPEG_LOCATION_LOOKUP_TABLE = "MLLT"
		const val FRAME_ID_MUSICIAN_CREDITS = "TMCL"
		const val FRAME_ID_MUSIC_CD_ID = "MCDI"
		const val FRAME_ID_ORIGARTIST = "TOPE"
		const val FRAME_ID_ORIGINAL_RELEASE_TIME = "TDOR"
		const val FRAME_ID_ORIG_FILENAME = "TOFN"
		const val FRAME_ID_ORIG_LYRICIST = "TOLY"
		const val FRAME_ID_ORIG_TITLE = "TOAL"
		const val FRAME_ID_OWNERSHIP = "OWNE"
		const val FRAME_ID_ARTIST_SORT_ORDER = "TSOP"
		const val FRAME_ID_PLAYLIST_DELAY = "TDLY"
		const val FRAME_ID_PLAY_COUNTER = "PCNT"
		const val FRAME_ID_POPULARIMETER = "POPM"
		const val FRAME_ID_POSITION_SYNC = "POSS"
		const val FRAME_ID_PRIVATE = "PRIV"
		const val FRAME_ID_PRODUCED_NOTICE = "TPRO"
		const val FRAME_ID_PUBLISHER = "TPUB"
		const val FRAME_ID_RADIO_NAME = "TRSN"
		const val FRAME_ID_RADIO_OWNER = "TRSO"
		const val FRAME_ID_RECOMMENDED_BUFFER_SIZE = "RBUF"
		const val FRAME_ID_RELATIVE_VOLUME_ADJUSTMENT2 = "RVA2"
		const val FRAME_ID_RELEASE_TIME = "TDRL"
		const val FRAME_ID_REMIXED = "TPE4"
		const val FRAME_ID_REVERB = "RVRB"
		const val FRAME_ID_SEEK = "SEEK"
		const val FRAME_ID_SET = "TPOS"
		const val FRAME_ID_SET_SUBTITLE = "TSST"
		const val FRAME_ID_SIGNATURE = "SIGN"
		const val FRAME_ID_SYNC_LYRIC = "SYLT"
		const val FRAME_ID_SYNC_TEMPO = "SYTC"
		const val FRAME_ID_TAGGING_TIME = "TDTG"
		const val FRAME_ID_TERMS_OF_USE = "USER"
		const val FRAME_ID_TITLE = "TIT2"
		const val FRAME_ID_TITLE_REFINEMENT = "TIT3"
		const val FRAME_ID_TITLE_SORT_ORDER = "TSOT"
		const val FRAME_ID_TRACK = "TRCK"
		const val FRAME_ID_UNIQUE_FILE_ID = "UFID"
		const val FRAME_ID_UNSYNC_LYRICS = "USLT"
		const val FRAME_ID_URL_ARTIST_WEB = "WOAR"
		const val FRAME_ID_URL_COMMERCIAL = "WCOM"
		const val FRAME_ID_URL_COPYRIGHT = "WCOP"
		const val FRAME_ID_URL_FILE_WEB = "WOAF"
		const val FRAME_ID_URL_OFFICIAL_RADIO = "WORS"
		const val FRAME_ID_URL_PAYMENT = "WPAY"
		const val FRAME_ID_URL_PUBLISHERS = "WPUB"
		const val FRAME_ID_URL_SOURCE_WEB = "WOAS"
		const val FRAME_ID_USER_DEFINED_INFO = "TXXX"
		const val FRAME_ID_USER_DEFINED_URL = "WXXX"
		const val FRAME_ID_YEAR = "TDRC"
		const val FRAME_ID_ALBUM_ARTIST_SORT_ORDER_ITUNES = "TSO2"
		const val FRAME_ID_COMPOSER_SORT_ORDER_ITUNES = "TSOC"
		const val FRAME_ID_IS_COMPILATION = "TCMP"

		//TODO this is temporary to provide backwards compatability
		const val FRAME_ID_PERFORMER_SORT_OWNER = FRAME_ID_ARTIST_SORT_ORDER
		const val FRAME_ID_TITLE_SORT_OWNER = FRAME_ID_TITLE_SORT_ORDER
		private var id3v24Frames: ID3v24Frames? = null
		val instanceOf: ID3v24Frames
			get() {
				if (id3v24Frames == null) {
					id3v24Frames = ID3v24Frames()
				}
				return id3v24Frames!!
			}
	}
}
