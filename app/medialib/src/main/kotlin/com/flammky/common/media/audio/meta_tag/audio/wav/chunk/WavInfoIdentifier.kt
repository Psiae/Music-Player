package com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav.chunk

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.FieldKey

/**
 * Known Identifiers used in an INFO Chunk together with their mapping to a generic FieldKey (if known)
 *
 * TODO There are multiple INFO fields that shoud be ampped to the same FieldKey (see QOOBUZ fields) but
 * we dont currently support that
 */
enum class WavInfoIdentifier(
	val code: String,
	val fieldKey: FieldKey?,
	val preferredWriteOrder: Int
) {
	ARTIST("IART", FieldKey.ARTIST, 1), ALBUM("IPRD", FieldKey.ALBUM, 2), TITLE(
		"INAM",
		FieldKey.TITLE,
		3
	),
	TRACKNO("ITRK", FieldKey.TRACK, 4), YEAR("ICRD", FieldKey.YEAR, 5), GENRE(
		"IGNR",
		FieldKey.GENRE,
		6
	),  //Custom MediaMonkey field, there appears to be no official AlbumArtist field, nothing ever displayed for this field or IAAR in Windows Explorer
	ALBUM_ARTIST("iaar", FieldKey.ALBUM_ARTIST, 7), COMMENTS("ICMT", FieldKey.COMMENT, 8), COMPOSER(
		"IMUS",
		FieldKey.COMPOSER,
		9
	),
	CONDUCTOR("ITCH", FieldKey.CONDUCTOR, 10), LYRICIST(
		"IWRI",
		FieldKey.LYRICIST,
		11
	),
	ENCODER("ISFT", FieldKey.ENCODER, 12), RATING("IRTD", FieldKey.RATING, 13), ISRC(
		"ISRC",
		FieldKey.ISRC,
		14
	),
	LABEL("ICMS", FieldKey.RECORD_LABEL, 15), COPYRIGHT(
		"ICOP",
		FieldKey.COPYRIGHT,
		16
	),
	QOBUZ_TRACKNO("IPRT", null, 17), QOBUZ_TRACK_TOTAL("IFRM", null, 18), QOBUZ_ALBUMARTIST(
		"ISTR",
		null,
		19
	),
	TRACK_GAIN("ITGL", null, 20),  //Currently No mapping to a FieldKey for this
	ALBUM_GAIN("IAGL", null, 21),  //Currently No mapping to a FieldKey for this
	TWONKY_TRACKNO("itrk", null, 1);

	companion object {
		//Uses nonstandard field
		private val CODE_TYPE_MAP: MutableMap<String?, WavInfoIdentifier> = HashMap()
		private val FIELDKEY_TYPE_MAP: MutableMap<FieldKey?, WavInfoIdentifier> = HashMap()

		/**
		 * Get [WavInfoIdentifier] for code (e.g. "SSND").
		 *
		 * @param code chunk id
		 * @return chunk type or `null` if not registered
		 */
		@Synchronized
		fun getByCode(code: String?): WavInfoIdentifier? {
			if (CODE_TYPE_MAP.isEmpty()) {
				for (type in values()) {
					CODE_TYPE_MAP[type.code] = type
				}
			}
			return CODE_TYPE_MAP[code]
		}

		/**
		 * Get [WavInfoIdentifier] for code (e.g. "SSND").
		 *
		 * @param fieldKey
		 * @return chunk type or `null` if not registered
		 */
		@JvmStatic
		@Synchronized
		fun getByFieldKey(fieldKey: FieldKey?): WavInfoIdentifier? {
			if (FIELDKEY_TYPE_MAP.isEmpty()) {
				for (type in values()) {
					if (type.fieldKey != null) {
						FIELDKEY_TYPE_MAP[type.fieldKey] = type
					}
				}
			}
			return FIELDKEY_TYPE_MAP[fieldKey]
		}
	}
}
