package com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.field.Mp4FieldType
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.field.Mp4TagReverseDnsField
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.Tagger

/**
 * Starting list of known mp4 metadata fields that follow the Parent,Data or ---,issuer,name,data
 * convention. Atoms that contain metadata in other formats are not listed here because they need to be processed
 * specially.
 *
 *
 *
 * Simple metaitems use the parent atom id as their identifier whereas reverse dns (----) atoms use
 * the reversedns,issuer and name fields as their identifier. When the atom is non-0standard but follws the rules
 * we list it here with an additional Tagger field to indicate where the field was originally designed.
 *
 * From:
 * http://www.hydrogenaudio.org/forums/index.php?showtopic=29120&st=0&p=251686&#entry251686
 * http://wiki.musicbrainz.org/PicardQt/TagMapping
 * http://atomicparsley.sourceforge.net/mpeg-4files.html
 *
 *
 */
enum class Mp4FieldKey {
	ACOUSTID_FINGERPRINT(
		"com.apple.iTunes",
		"Acoustid Fingerprint",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	ACOUSTID_FINGERPRINT_OLD(
		"com.apple.iTunes",
		"AcoustId Fingerprint",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	ACOUSTID_ID("com.apple.iTunes", "Acoustid Id", Mp4FieldType.TEXT, Tagger.PICARD),
	AK_ID(
		"akID",
		Mp4TagFieldSubType.UNKNOWN,
		Mp4FieldType.INTEGER,
		1
	),
	ALBUM("©alb", Mp4TagFieldSubType.TEXT, Mp4FieldType.TEXT), ALBUM_ARTIST(
		"aART",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT
	),
	ALBUM_ARTIST_SORT("soaa", Mp4TagFieldSubType.TEXT, Mp4FieldType.TEXT),
	ALBUM_SORT(
		"soal",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT
	),
	ALBUM_YEAR("com.apple.iTunes", "ALBUM_YEAR", Mp4FieldType.TEXT, Tagger.JAIKOZ),
	AP_ID(
		"apID",
		Mp4TagFieldSubType.UNKNOWN,
		Mp4FieldType.TEXT
	),
	ARRANGER(
		"com.apple.iTunes",
		"ARRANGER",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	ARRANGER_SORT(
		"com.apple.iTunes",
		"ARRANGER_SORT",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	ARTIST("©ART", Mp4TagFieldSubType.TEXT, Mp4FieldType.TEXT),
	ARTISTS(
		"com.apple.iTunes",
		"ARTISTS",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	ARTISTS_SORT("com.apple.iTunes", "ARTISTS_SORT", Mp4FieldType.TEXT, Tagger.JAIKOZ),
	ALBUM_ARTISTS(
		"com.apple.iTunes",
		"ALBUM_ARTISTS",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	ALBUM_ARTISTS_SORT(
		"com.apple.iTunes",
		"ALBUM_ARTISTS_SORT",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	ARTIST_SORT("soar", Mp4TagFieldSubType.TEXT, Mp4FieldType.TEXT),
	ARTWORK(
		"covr",
		Mp4TagFieldSubType.ARTWORK,
		Mp4FieldType.COVERART_JPEG
	),
	ASIN("com.apple.iTunes", "ASIN", Mp4FieldType.TEXT, Tagger.PICARD),
	AT_ID(
		"atID",
		Mp4TagFieldSubType.UNKNOWN,
		Mp4FieldType.INTEGER,
		4
	),
	BARCODE("com.apple.iTunes", "BARCODE", Mp4FieldType.TEXT, Tagger.PICARD),
	BPM(
		"tmpo",
		Mp4TagFieldSubType.BYTE,
		Mp4FieldType.INTEGER,
		2
	),
	CATALOGNO("com.apple.iTunes", "CATALOGNUMBER", Mp4FieldType.TEXT, Tagger.PICARD),
	CATEGORY(
		"catg",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT
	),
	CDDB_1("com.apple.iTunes", "iTunes_CDDB_1", Mp4FieldType.TEXT),
	CDDB_IDS(
		"com.apple.iTunes",
		"iTunes_CDDB_IDs",
		Mp4FieldType.TEXT
	),
	CDDB_TRACKNUMBER("com.apple.iTunes", "iTunes_CDDB_TrackNumber", Mp4FieldType.TEXT),
	CN_ID(
		"cnID",
		Mp4TagFieldSubType.UNKNOWN,
		Mp4FieldType.INTEGER,
		4
	),
	CHOIR(
		"com.apple.iTunes",
		"CHOR",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	CHOIR_SORT("com.apple.iTunes", "CHOIR_SORT", Mp4FieldType.TEXT, Tagger.JAIKOZ),
	CLASSICAL_CATALOG(
		"com.apple.iTunes",
		"CLASSICAL_CATALOG",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	CLASSICAL_NICKNAME(
		"com.apple.iTunes",
		"CLASSICAL_NICKNAME",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	COMMENT("©cmt", Mp4TagFieldSubType.TEXT, Mp4FieldType.TEXT),
	COMPILATION(
		"cpil",
		Mp4TagFieldSubType.BYTE,
		Mp4FieldType.INTEGER,
		1
	),
	COMPOSER("©wrt", Mp4TagFieldSubType.TEXT, Mp4FieldType.TEXT),
	COMPOSER_SORT(
		"soco",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT
	),
	CONDUCTOR(
		"com.apple.iTunes",
		"CONDUCTOR",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	CONDUCTOR_MM3BETA(
		"cond",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	CONDUCTOR_SORT(
		"com.apple.iTunes",
		"CONDUCTOR_SORT",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	CONTENT_TYPE("stik", Mp4TagFieldSubType.BYTE, Mp4FieldType.INTEGER, 1),
	COPYRIGHT(
		"cprt",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT
	),
	COUNTRY("com.apple.iTunes", "Country", Mp4FieldType.TEXT, Tagger.PICARD),
	CUSTOM_1(
		"cus1",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	CUSTOM_2(
		"cus2",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	CUSTOM_3(
		"cus3",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	CUSTOM_4(
		"cus4",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	CUSTOM_5("cus5", Mp4TagFieldSubType.TEXT, Mp4FieldType.TEXT, Tagger.MEDIA_MONKEY),
	DAY(
		"©day",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT
	),
	DESCRIPTION("desc", Mp4TagFieldSubType.TEXT, Mp4FieldType.TEXT),
	DISCNUMBER(
		"disk",
		Mp4TagFieldSubType.DISC_NO,
		Mp4FieldType.IMPLICIT
	),
	DISC_SUBTITLE(
		"com.apple.iTunes",
		"DISCSUBTITLE",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	DJMIXER(
		"com.apple.iTunes",
		"DJMIXER",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	DJMIXER_SORT(
		"com.apple.iTunes",
		"DJMIXER_SORT",
		Mp4FieldType.TEXT,
		Tagger.SONGKONG
	),
	ENCODER("©too", Mp4TagFieldSubType.TEXT, Mp4FieldType.TEXT),
	ENGINEER(
		"com.apple.iTunes",
		"ENGINEER",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	ENGINEER_SORT(
		"com.apple.iTunes",
		"ENGINEER_SORT",
		Mp4FieldType.TEXT,
		Tagger.SONGKONG
	),
	ENSEMBLE(
		"com.apple.iTunes",
		"Ensemble",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	ENSEMBLE_SORT(
		"com.apple.iTunes",
		"Ensemble Sort",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	EPISODE_GLOBAL_ID(
		"egid",
		Mp4TagFieldSubType.NUMBER,
		Mp4FieldType.IMPLICIT
	),  //TODO Actually seems to store text but is marked as numeric!
	FBPM("com.apple.iTunes", "fBPM", Mp4FieldType.TEXT, Tagger.JAIKOZ),
	GENRE(
		"gnre",
		Mp4TagFieldSubType.GENRE,
		Mp4FieldType.IMPLICIT
	),
	GENRE_CUSTOM("©gen", Mp4TagFieldSubType.TEXT, Mp4FieldType.TEXT),
	GE_ID(
		"geID",
		Mp4TagFieldSubType.UNKNOWN,
		Mp4FieldType.INTEGER,
		4
	),
	GROUP("com.apple.iTunes", "GROUP", Mp4FieldType.TEXT, Tagger.JAIKOZ),
	GROUPING(
		"©grp",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT
	),
	INSTRUMENT(
		"com.apple.iTunes",
		"INSTRUMENT",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	INVOLVED_PEOPLE(
		"peop",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	INVOLVEDPEOPLE(
		"com.apple.iTunes",
		"involvedpeople",
		Mp4FieldType.TEXT,
		Tagger.ROON
	),
	IPI("com.apple.iTunes", "IPI", Mp4FieldType.TEXT, Tagger.PICARD),
	ISRC(
		"com.apple.iTunes",
		"ISRC",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	ISWC("com.apple.iTunes", "ISWC", Mp4FieldType.TEXT, Tagger.JAIKOZ),
	ISRC_MMBETA(
		"isrc",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	IS_CLASSICAL(
		"com.apple.iTunes",
		"IS_CLASSICAL",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	IS_GREATEST_HITS("com.apple.iTunes", "IS_GREATEST_HITS", Mp4FieldType.TEXT, Tagger.JAIKOZ),
	IS_HD(
		"com.apple.iTunes",
		"IS_HD",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	IS_LIVE(
		"com.apple.iTunes",
		"LIVE",
		Mp4FieldType.TEXT,
		Tagger.ROON
	),
	IS_SOUNDTRACK("com.apple.iTunes", "IS_SOUNDTRACK", Mp4FieldType.TEXT, Tagger.JAIKOZ),
	ITUNES_NORM(
		"com.apple.iTunes",
		"iTunNORM",
		Mp4FieldType.TEXT
	),
	ITUNES_SMPB("com.apple.iTunes", "iTunSMPB", Mp4FieldType.TEXT), JAIKOZ_ID(
		"com.apple.iTunes",
		"JAIKOZ_ID",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	KEY("com.apple.iTunes", "initialkey", Mp4FieldType.TEXT), KEYS(
		"keys",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT
	),
	KEYWORD("keyw", Mp4TagFieldSubType.TEXT, Mp4FieldType.TEXT), KEY_OLD(
		"com.apple.iTunes",
		"KEY",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	LABEL("com.apple.iTunes", "LABEL", Mp4FieldType.TEXT, Tagger.PICARD),
	LANGUAGE(
		"com.apple.iTunes",
		"LANGUAGE",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	LYRICIST(
		"com.apple.iTunes",
		"LYRICIST",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	LYRICIST_SORT(
		"com.apple.iTunes",
		"LYRICIST_SORT",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	LYRICIST_MM3BETA("lyrc", Mp4TagFieldSubType.TEXT, Mp4FieldType.TEXT, Tagger.MEDIA_MONKEY),
	LYRICS(
		"©lyr",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT
	),
	MEDIA("com.apple.iTunes", "MEDIA", Mp4FieldType.TEXT, Tagger.PICARD),
	MIXER(
		"com.apple.iTunes",
		"MIXER",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	MIXER_SORT(
		"com.apple.iTunes",
		"MIXER_SORT",
		Mp4FieldType.TEXT,
		Tagger.SONGKONG
	),
	MM_CUSTOM_1(
		"com.apple.iTunes",
		"CUSTOM1",
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	MM_CUSTOM_2(
		"com.apple.iTunes",
		"CUSTOM2",
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	MM_CUSTOM_3(
		"com.apple.iTunes",
		"CUSTOM3",
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	MM_CUSTOM_4(
		"com.apple.iTunes",
		"CUSTOM4",
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	MM_CUSTOM_5(
		"com.apple.iTunes",
		"CUSTOM5",
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	MM_INVOLVED_PEOPLE(
		"com.apple.iTunes",
		"INVOLVED PEOPLE",
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	MM_OCCASION(
		"com.apple.iTunes",
		"OCCASION",
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	MM_ORIGINAL_ALBUM_TITLE(
		"com.apple.iTunes",
		"ORIGINAL ALBUM",
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	MM_ORIGINAL_ARTIST(
		"com.apple.iTunes",
		"ORIGINAL ARTIST",
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	MM_ORIGINAL_LYRICIST(
		"com.apple.iTunes",
		"ORIGINAL LYRICIST",
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	MM_ORIGINAL_YEAR(
		"com.apple.iTunes",
		"ORIGINAL YEAR",
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	MM_PUBLISHER(
		"com.apple.iTunes",
		"ORGANIZATION",
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	MM_QUALITY(
		"com.apple.iTunes",
		"QUALITY",
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	MM_TEMPO(
		"com.apple.iTunes",
		"TEMPO",
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	MOOD(
		"com.apple.iTunes",
		"MOOD",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	MOOD_ACOUSTIC(
		"com.apple.iTunes",
		"MOOD_ACOUSTIC",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MOOD_AGGRESSIVE(
		"com.apple.iTunes",
		"MOOD_AGGRESSIVE",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MOOD_AROUSAL(
		"com.apple.iTunes",
		"MOOD_AROUSAL",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MOOD_DANCEABILITY(
		"com.apple.iTunes",
		"MOOD_DANCEABILITY",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MOOD_ELECTRONIC(
		"com.apple.iTunes",
		"MOOD_ELECTRONIC",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MOOD_HAPPY("com.apple.iTunes", "MOOD_HAPPY", Mp4FieldType.TEXT, Tagger.JAIKOZ),
	MOOD_INSTRUMENTAL(
		"com.apple.iTunes",
		"MOOD_INSTRUMENTAL",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MOOD_MM3BETA("mood", Mp4TagFieldSubType.TEXT, Mp4FieldType.TEXT, Tagger.MEDIA_MONKEY),
	MOOD_PARTY(
		"com.apple.iTunes",
		"MOOD_PARTY",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MOOD_RELAXED(
		"com.apple.iTunes",
		"MOOD_RELAXED",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MOOD_SAD(
		"com.apple.iTunes",
		"MOOD_SAD",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MOOD_VALENCE(
		"com.apple.iTunes",
		"MOOD_VALENCE",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MOVEMENT("©mvn", Mp4TagFieldSubType.TEXT, Mp4FieldType.TEXT), MOVEMENT_NO(
		"©mvi",
		Mp4TagFieldSubType.BYTE,
		Mp4FieldType.INTEGER,
		1
	),  //Note unlike Trackno/Total uses two different fields
	MOVEMENT_TOTAL(
		"©mvc",
		Mp4TagFieldSubType.BYTE,
		Mp4FieldType.INTEGER,
		1
	),
	MUSICBRAINZ_ALBUMARTISTID(
		"com.apple.iTunes",
		"MusicBrainz Album Artist Id",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	MUSICBRAINZ_ALBUMID(
		"com.apple.iTunes",
		"MusicBrainz Album Id",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	MUSICBRAINZ_ALBUM_STATUS(
		"com.apple.iTunes",
		"MusicBrainz Album Status",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	MUSICBRAINZ_ALBUM_TYPE(
		"com.apple.iTunes",
		"MusicBrainz Album Type",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	MUSICBRAINZ_ARTISTID(
		"com.apple.iTunes",
		"MusicBrainz Artist Id",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	MUSICBRAINZ_DISCID(
		"com.apple.iTunes",
		"MusicBrainz Disc Id",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	MUSICBRAINZ_ORIGINALALBUMID(
		"com.apple.iTunes",
		"MusicBrainz Original Album Id",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MUSICBRAINZ_RELEASE_GROUPID(
		"com.apple.iTunes",
		"MusicBrainz Release Group Id",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	MUSICBRAINZ_RELEASE_TRACKID(
		"com.apple.iTunes",
		"MusicBrainz Release Track Id",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	MUSICBRAINZ_TRACKID(
		"com.apple.iTunes",
		"MusicBrainz Track Id",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	MUSICBRAINZ_WORK(
		"com.apple.iTunes",
		"MUSICBRAINZ_WORK",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MUSICBRAINZ_WORKID(
		"com.apple.iTunes",
		"MusicBrainz Work Id",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	MUSICBRAINZ_RECORDING_WORK(
		"com.apple.iTunes",
		"MUSICBRAINZ_RECORDING_WORK",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MUSICBRAINZ_RECORDING_WORK_ID(
		"com.apple.iTunes",
		"MUSICBRAINZ_RECORDING_WORK_ID",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MUSICBRAINZ_WORK_PART_LEVEL1(
		"com.apple.iTunes",
		"MUSICBRAINZ_WORK_PART_LEVEL1",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MUSICBRAINZ_WORK_PART_LEVEL1_ID(
		"com.apple.iTunes",
		"MUSICBRAINZ_WORK_PART_LEVEL1_ID",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MUSICBRAINZ_WORK_PART_LEVEL1_TYPE(
		"com.apple.iTunes",
		"MUSICBRAINZ_WORK_PART_LEVEL1_TYPE",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MUSICBRAINZ_WORK_PART_LEVEL2(
		"com.apple.iTunes",
		"MUSICBRAINZ_WORK_PART_LEVEL2",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MUSICBRAINZ_WORK_PART_LEVEL2_ID(
		"com.apple.iTunes",
		"MUSICBRAINZ_WORK_PART_LEVEL2_ID",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MUSICBRAINZ_WORK_PART_LEVEL2_TYPE(
		"com.apple.iTunes",
		"MUSICBRAINZ_WORK_PART_LEVEL2_TYPE",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MUSICBRAINZ_WORK_PART_LEVEL3(
		"com.apple.iTunes",
		"MUSICBRAINZ_WORK_PART_LEVEL3",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MUSICBRAINZ_WORK_PART_LEVEL3_ID(
		"com.apple.iTunes",
		"MUSICBRAINZ_WORK_PART_LEVEL3_ID",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MUSICBRAINZ_WORK_PART_LEVEL3_TYPE(
		"com.apple.iTunes",
		"MUSICBRAINZ_WORK_PART_LEVEL3_TYPE",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MUSICBRAINZ_WORK_PART_LEVEL4(
		"com.apple.iTunes",
		"MUSICBRAINZ_WORK_PART_LEVEL4",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MUSICBRAINZ_WORK_PART_LEVEL4_ID(
		"com.apple.iTunes",
		"MUSICBRAINZ_WORK_PART_LEVEL4_ID",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MUSICBRAINZ_WORK_PART_LEVEL4_TYPE(
		"com.apple.iTunes",
		"MUSICBRAINZ_WORK_PART_LEVEL4_TYPE",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MUSICBRAINZ_WORK_PART_LEVEL5(
		"com.apple.iTunes",
		"MUSICBRAINZ_WORK_PART_LEVEL5",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MUSICBRAINZ_WORK_PART_LEVEL5_ID(
		"com.apple.iTunes",
		"MUSICBRAINZ_WORK_PART_LEVEL5_ID",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MUSICBRAINZ_WORK_PART_LEVEL5_TYPE(
		"com.apple.iTunes",
		"MUSICBRAINZ_WORK_PART_LEVEL5_TYPE",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MUSICBRAINZ_WORK_PART_LEVEL6(
		"com.apple.iTunes",
		"MUSICBRAINZ_WORK_PART_LEVEL6",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MUSICBRAINZ_WORK_PART_LEVEL6_ID(
		"com.apple.iTunes",
		"MUSICBRAINZ_WORK_PART_LEVEL6_ID",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MUSICBRAINZ_WORK_PART_LEVEL6_TYPE(
		"com.apple.iTunes",
		"MUSICBRAINZ_WORK_PART_LEVEL6_TYPE",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	MUSICIP_PUID(
		"com.apple.iTunes",
		"MusicIP PUID",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	OCCASION(
		"occa",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	OPUS("com.apple.iTunes", "OPUS", Mp4FieldType.TEXT, Tagger.JAIKOZ),
	ORCHESTRA(
		"com.apple.iTunes",
		"ORCHESTRA",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	ORCHESTRA_SORT(
		"com.apple.iTunes",
		"ORCHESTRA_SORT",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	ORIGINAL_ALBUM_TITLE(
		"otit",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	ORIGINALRELEASEDATE(
		"com.apple.iTunes",
		"ORIGINALRELEASEDATE",
		Mp4FieldType.TEXT,
		Tagger.ROON
	),
	ORIGINAL_ARTIST(
		"oart",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	ORIGINAL_LYRICIST(
		"olyr",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	OVERALL_WORK(
		"com.apple.iTunes",
		"OVERALL_WORK",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	PART(
		"com.apple.iTunes",
		"PART",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	PART_NUMBER(
		"com.apple.iTunes",
		"PARTNUMBER",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	PART_OF_GAPLESS_ALBUM(
		"pgap",
		Mp4TagFieldSubType.BYTE,
		Mp4FieldType.INTEGER
	),
	PART_TYPE(
		"com.apple.iTunes",
		"PART_TYPE",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	PERFORMER(
		"com.apple.iTunes",
		"Performer",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	PERFORMER_NAME(
		"com.apple.iTunes",
		"PERFORMER_NAME",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	PERFORMER_NAME_SORT(
		"com.apple.iTunes",
		"PERFORMER_NAME_SORT",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	PERIOD("com.apple.iTunes", "PERIOD", Mp4FieldType.TEXT, Tagger.JAIKOZ),
	PL_ID(
		"plID",
		Mp4TagFieldSubType.UNKNOWN,
		Mp4FieldType.INTEGER,
		8
	),
	PODCAST_KEYWORD("keyw", Mp4TagFieldSubType.TEXT, Mp4FieldType.TEXT),
	PODCAST_URL(
		"purl",
		Mp4TagFieldSubType.NUMBER,
		Mp4FieldType.IMPLICIT
	),  //TODO Actually seems to store text but is marked as numeric!
	PRODUCER(
		"com.apple.iTunes",
		"PRODUCER",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	PRODUCER_SORT(
		"com.apple.iTunes",
		"PRODUCER_SORT",
		Mp4FieldType.TEXT,
		Tagger.SONGKONG
	),
	PURCHASE_DATE("purd", Mp4TagFieldSubType.TEXT, Mp4FieldType.TEXT), QUALITY(
		"qual",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	RANKING("com.apple.iTunes", "RANKING", Mp4FieldType.TEXT, Tagger.JAIKOZ),
	RATING(
		"rtng",
		Mp4TagFieldSubType.BYTE,
		Mp4FieldType.INTEGER,
		1
	),  //AFAIK Cant be setField in itunes, but if setField to explicit itunes will show as explicit
	RECORDINGDATE(
		"com.apple.iTunes",
		"RECORDINGDATE",
		Mp4FieldType.TEXT,
		Tagger.ROON
	),
	RECORDINGSTARTDATE(
		"com.apple.iTunes",
		"RECORDINGSTARTDATE",
		Mp4FieldType.TEXT,
		Tagger.ROON
	),
	RECORDINGENDDATE(
		"com.apple.iTunes",
		"RECORDINGENDDATE",
		Mp4FieldType.TEXT,
		Tagger.ROON
	),
	RECORDINGLOCATION(
		"com.apple.iTunes",
		"RECORDINGLOCATION",
		Mp4FieldType.TEXT,
		Tagger.ROON
	),
	RELEASECOUNTRY(
		"com.apple.iTunes",
		"MusicBrainz Album Release Country",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	REMIXER(
		"com.apple.iTunes",
		"REMIXER",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	ROONALBUMTAG(
		"com.apple.iTunes",
		"ROONALBUMTAG",
		Mp4FieldType.TEXT,
		Tagger.ROON
	),
	ROONTRACKTAG("com.apple.iTunes", "ROONTRACKTAG", Mp4FieldType.TEXT, Tagger.ROON),
	SCORE(
		"rate",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),  //As in mark out of 100
	SCRIPT("com.apple.iTunes", "SCRIPT", Mp4FieldType.TEXT, Tagger.JAIKOZ),
	SF_ID(
		"sfID",
		Mp4TagFieldSubType.UNKNOWN,
		Mp4FieldType.INTEGER,
		4
	),
	SECTION("com.apple.iTunes", "SECTION", Mp4FieldType.TEXT, Tagger.ROON),
	SHOW(
		"tvsh",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT
	),  //tv show but also used just as show
	SHOW_SORT(
		"sosn",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT
	),
	SINGLE_DISC_TRACK_NO(
		"com.apple.iTunes",
		"SINGLE_DISC_TRACK_NO",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	SONGKONG_ID(
		"com.apple.iTunes",
		"SONGKONG_ID",
		Mp4FieldType.TEXT,
		Tagger.SONGKONG
	),
	SUBTITLE(
		"com.apple.iTunes",
		"SUBTITLE",
		Mp4FieldType.TEXT,
		Tagger.PICARD
	),
	TAGS("com.apple.iTunes", "TAGS", Mp4FieldType.TEXT, Tagger.JAIKOZ), TEMPO(
		"empo",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT,
		Tagger.MEDIA_MONKEY
	),
	TIMBRE("com.apple.iTunes", "TIMBRE_BRIGHTNESS", Mp4FieldType.TEXT, Tagger.JAIKOZ),
	TITLE(
		"©nam",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT
	),
	TITLE_MOVEMENT(
		"com.apple.iTunes",
		"TITLE_MOVEMENT",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	TITLE_SORT("sonm", Mp4TagFieldSubType.TEXT, Mp4FieldType.TEXT), TONALITY(
		"com.apple.iTunes",
		"TONALITY",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	TOOL("tool", Mp4TagFieldSubType.BYTE, Mp4FieldType.INTEGER, 4), TRACK(
		"trkn",
		Mp4TagFieldSubType.TRACK_NO,
		Mp4FieldType.IMPLICIT
	),
	TV_EPISODE("tves", Mp4TagFieldSubType.BYTE, Mp4FieldType.INTEGER, 1),
	TV_EPISODE_NUMBER(
		"tven",
		Mp4TagFieldSubType.TEXT,
		Mp4FieldType.TEXT
	),
	TV_NETWORK("tvnn", Mp4TagFieldSubType.TEXT, Mp4FieldType.TEXT), TV_SEASON(
		"tvsn",
		Mp4TagFieldSubType.BYTE,
		Mp4FieldType.INTEGER,
		1
	),
	URL_DISCOGS_ARTIST_SITE(
		"com.apple.iTunes",
		"URL_DISCOGS_ARTIST_SITE",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	URL_DISCOGS_RELEASE_SITE(
		"com.apple.iTunes",
		"URL_DISCOGS_RELEASE_SITE",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	URL_LYRICS_SITE(
		"com.apple.iTunes",
		"URL_LYRICS_SITE",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	URL_OFFICIAL_ARTIST_SITE(
		"com.apple.iTunes",
		"URL_OFFICIAL_ARTIST_SITE",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	URL_OFFICIAL_RELEASE_SITE(
		"com.apple.iTunes",
		"URL_OFFICIAL_RELEASE_SITE",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	URL_WIKIPEDIA_ARTIST_SITE(
		"com.apple.iTunes",
		"URL_WIKIPEDIA_ARTIST_SITE",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	URL_WIKIPEDIA_RELEASE_SITE(
		"com.apple.iTunes",
		"URL_WIKIPEDIA_RELEASE_SITE",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	),
	VERSION(
		"com.apple.iTunes",
		"VERSION",
		Mp4FieldType.TEXT,
		Tagger.ROON
	),
	WINAMP_PUBLISHER(
		"com.nullsoft.winamp",
		"publisher",
		Mp4FieldType.TEXT,
		Tagger.WINAMP
	),
	WORK("©wrk", Mp4TagFieldSubType.TEXT, Mp4FieldType.TEXT),
	WORK_TYPE(
		"com.apple.iTunes",
		"WORK_TYPE",
		Mp4FieldType.TEXT,
		Tagger.JAIKOZ
	);

	private var tagger: Tagger? = null

	/**
	 * This is the value of the fieldname that is actually used to write mp4
	 *
	 * @return
	 */
	var fieldName: String
		private set

	/**
	 * @return subclassType
	 */
	var subClassFieldType: Mp4TagFieldSubType
		private set

	/**
	 * @return issuer (Reverse Dns Fields Only)
	 */
	var issuer: String? = null
		private set

	/**
	 * @return identifier (Reverse Dns Fields Only)
	 */
	var identifier: String? = null
		private set

	/**
	 * @return fieldtype
	 */
	var fieldType: Mp4FieldType
		private set

	/**
	 * @return field length (currently only used by byte fields)
	 */
	var fieldLength = 0
		private set

	/**
	 * For usual metadata fields that use a data field
	 *
	 * @param fieldName
	 * @param fieldType of data atom
	 */
	constructor(fieldName: String, subclassType: Mp4TagFieldSubType, fieldType: Mp4FieldType) {
		this.fieldName = fieldName
		subClassFieldType = subclassType
		this.fieldType = fieldType
	}

	/**
	 * For usual metadata fields that use a data field, but not recognised as standard field
	 *
	 * @param fieldName
	 * @param fieldType of data atom
	 * @param tagger
	 */
	constructor(
		fieldName: String,
		subclassType: Mp4TagFieldSubType,
		fieldType: Mp4FieldType,
		tagger: Tagger
	) {
		this.fieldName = fieldName
		subClassFieldType = subclassType
		this.fieldType = fieldType
		this.tagger = tagger
	}

	/**
	 * For usual metadata fields that use a data field where the field length is fixed
	 * such as Byte fields
	 *
	 * @param fieldName
	 * @param fieldType
	 * @param fieldLength
	 */
	constructor(
		fieldName: String,
		subclassType: Mp4TagFieldSubType,
		fieldType: Mp4FieldType,
		fieldLength: Int
	) {
		this.fieldName = fieldName
		subClassFieldType = subclassType
		this.fieldType = fieldType
		this.fieldLength = fieldLength
	}

	/**
	 * For reverse dns fields that use an internal fieldname of '----' and have  additional issuer
	 * and identifier fields, we use all three seperated by a ':' ) to give us a unique key
	 *
	 * @param issuer
	 * @param identifier
	 * @param fieldType  of data atom
	 */
	constructor(issuer: String, identifier: String, fieldType: Mp4FieldType) {
		this.issuer = issuer
		this.identifier = identifier
		fieldName = Mp4TagReverseDnsField.IDENTIFIER + ":" + issuer + ":" + identifier
		subClassFieldType = Mp4TagFieldSubType.REVERSE_DNS
		this.fieldType = fieldType
	}

	/**
	 * For reverse dns fields that use an internal fieldname of '----' and have  additional issuer
	 * and identifier fields, we use all three seperated by a ':' ) to give us a unique key
	 * For non-standard fields
	 *
	 * @param issuer
	 * @param identifier
	 * @param fieldType  of data atom
	 * @param tagger
	 */
	constructor(issuer: String, identifier: String, fieldType: Mp4FieldType, tagger: Tagger) {
		this.issuer = issuer
		this.identifier = identifier
		fieldName = Mp4TagReverseDnsField.IDENTIFIER + ":" + issuer + ":" + identifier
		subClassFieldType = Mp4TagFieldSubType.REVERSE_DNS
		this.fieldType = fieldType
		this.tagger = tagger
	}

	/**
	 * @return true if this is a reverse dns key
	 */
	val isReverseDnsType: Boolean
		get() = identifier!!.startsWith(Mp4TagReverseDnsField.IDENTIFIER)

	fun getTagger(): Tagger = tagger ?: Tagger.ITUNES
}
