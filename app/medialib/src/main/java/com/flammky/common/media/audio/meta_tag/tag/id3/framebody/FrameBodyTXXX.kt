/**
 * @author : Paul Taylor
 * @author : Eric Farng
 *
 * Version @version:$Id$
 *
 * MusicTag Copyright (C)2003,2004
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
 * you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * Description:
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.DataTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.NumberHashMap
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.TextEncodedStringNullTerminated
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.TextEncodedStringSizeTerminated
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3TextEncodingConversion.getTextEncoding
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3TextEncodingConversion.getUnicodeTextEncoding
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Frames
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * User defined text information frame
 *
 * This frame is intended for one-string text information concerning the
 * audio file in a similar way to the other "T"-frames. The frame body
 * consists of a description of the string, represented as a terminated
 * string, followed by the actual string. There may be more than one
 * "TXXX" frame in each tag, but only one with the same description.
 *
 * <Header for></Header> 'User defined text information frame', ID: "TXXX">
 * Text encoding     $xx
 * Description       <text string according to encoding> $00 (00)
 * Value             <text string according to encoding>
</text></text> */
class FrameBodyTXXX : AbstractFrameBodyTextInfo, ID3v24FrameBody, ID3v23FrameBody {

	override val identifier: String?
		get() = ID3v24Frames.FRAME_ID_USER_DEFINED_INFO

	/**
	 * Creates a new FrameBodyTXXX datatype.
	 */
	constructor() {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, TextEncoding.ISO_8859_1)
		setObjectValue(DataTypes.OBJ_DESCRIPTION, "")
		setObjectValue(DataTypes.OBJ_TEXT, "")
	}

	/**
	 * Convert from V4 TMOO Frame to V3 Frame
	 * @param body
	 */
	constructor(body: FrameBodyTMOO) {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, body.textEncoding)
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, TextEncoding.ISO_8859_1)
		setObjectValue(DataTypes.OBJ_DESCRIPTION, MOOD)
		setObjectValue(DataTypes.OBJ_TEXT, body.text)
	}

	constructor(body: FrameBodyTXXX) : super(body)

	/**
	 * Creates a new FrameBodyTXXX datatype.
	 *
	 * @param textEncoding
	 * @param description
	 * @param text
	 */
	constructor(textEncoding: Byte, description: String?, text: String?) {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, textEncoding)
		setObjectValue(DataTypes.OBJ_DESCRIPTION, description)
		setObjectValue(DataTypes.OBJ_TEXT, text)
	}

	/**
	 * Creates a new FrameBodyTXXX datatype.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)
	/**
	 * @return the description field
	 */
	/**
	 * Set the description field
	 *
	 * @param description
	 */
	var description: String?
		get() = getObjectValue(DataTypes.OBJ_DESCRIPTION) as String
		set(description) {
			setObjectValue(DataTypes.OBJ_DESCRIPTION, description)
		}

	/**
	 * Because TXXX frames also have a text encoded description we need to check this as well.     *
	 */
	override fun write(tagBuffer: ByteArrayOutputStream) {
		//Ensure valid for type
		textEncoding = getTextEncoding(header, textEncoding)

		//Ensure valid for description
		if (!(getObject(DataTypes.OBJ_DESCRIPTION) as TextEncodedStringNullTerminated?)!!.canBeEncoded()) {
			textEncoding = getUnicodeTextEncoding(header)
		}
		super.write(tagBuffer)
	}

	/**
	 * This is different to other text Frames
	 */
	override fun setupObjectList() {
		objectList.add(
			NumberHashMap(
				DataTypes.OBJ_TEXT_ENCODING,
				this,
				TextEncoding.TEXT_ENCODING_FIELD_SIZE
			)
		)
		objectList.add(TextEncodedStringNullTerminated(DataTypes.OBJ_DESCRIPTION, this))
		objectList.add(TextEncodedStringSizeTerminated(DataTypes.OBJ_TEXT, this))
	}

	companion object {
		//Used by Picard and Jaikoz
		const val ACOUSTID_FINGERPRINT = "Acoustid Fingerprint"
		const val ACOUSTID_ID = "Acoustid Id"
		const val AMAZON_ASIN = "ASIN"
		const val ARRANGER = "ARRANGER"
		const val ARRANGER_SORT = "ARRANGER_SORT"
		const val ARTISTS = "ARTISTS"
		const val ARTISTS_SORT = "ARTISTS_SORT"
		const val ALBUM_ARTISTS = "ALBUM_ARTISTS"
		const val ALBUM_ARTISTS_SORT = "ALBUM_ARTISTS_SORT"
		const val ALBUM_YEAR = "ALBUM_YEAR"
		const val BARCODE = "BARCODE"
		const val CATALOG_NO = "CATALOGNUMBER"
		const val CHOIR = "CHOIR"
		const val CHOIR_SORT = "CHOIR_SORT"
		const val CLASSICAL_CATALOG = "CLASSICAL_CATALOG"
		const val CLASSICAL_NICKNAME = "CLASSICAL_NICKNAME"
		const val CONDUCTOR_SORT = "CONDUCTOR_SORT"
		const val COUNTRY = "Country"
		const val DJMIXER = "DJMIXER"
		const val DJMIXER_SORT = "DJMIXER_SORT"
		const val ENGINEER = "ENGINEER"
		const val ENGINEER_SORT = "ENGINEER_SORT"
		const val ENSEMBLE = "ENSEMBLE"
		const val ENSEMBLE_SORT = "ENSEMBLE_SORT"
		const val FBPM = "FBPM"
		const val GROUP = "GROUP"
		const val IPI = "IPI"
		const val INSTRUMENT = "INSTRUMENT"
		const val IS_CLASSICAL = "IS_CLASSICAL"
		const val IS_GREATEST_HITS = "IS_GREATEST_HITS"
		const val IS_HD = "IS_HD"
		const val IS_SOUNDTRACK = "IS_SOUNDTRACK"
		const val ISWC = "ISWC"
		const val JAIKOZ_ID = "JAIKOZ_ID"
		const val LIVE = "LIVE"
		const val LYRICIST_SORT = "LYRICIST_SORT"
		const val MIXER = "MIXER"
		const val MIXER_SORT = "MIXER_SORT"
		const val MOOD = "MOOD" //ID3 v23 only
		const val MOOD_ACOUSTIC = "MOOD_ACOUSTIC"
		const val MOOD_AGGRESSIVE = "MOOD_AGGRESSIVE"
		const val MOOD_AROUSAL = "MOOD_AROUSAL"
		const val MOOD_DANCEABILITY = "MOOD_DANCEABILITY"
		const val MOOD_ELECTRONIC = "MOOD_ELECTRONIC"
		const val MOOD_HAPPY = "MOOD_HAPPY"
		const val MOOD_INSTRUMENTAL = "MOOD_INSTRUMENTAL"
		const val MOOD_PARTY = "MOOD_PARTY"
		const val MOOD_RELAXED = "MOOD_RELAXED"
		const val MOOD_SAD = "MOOD_SAD"
		const val MOOD_VALENCE = "MOOD_VALENCE"
		const val MUSICBRAINZ_ALBUMID = "MusicBrainz Album Id"
		const val MUSICBRAINZ_ALBUM_ARTISTID = "MusicBrainz Album Artist Id"
		const val MUSICBRAINZ_ALBUM_COUNTRY = "MusicBrainz Album Release Country"
		const val MUSICBRAINZ_ALBUM_STATUS = "MusicBrainz Album Status"
		const val MUSICBRAINZ_ALBUM_TYPE = "MusicBrainz Album Type"
		const val MUSICBRAINZ_ARTISTID = "MusicBrainz Artist Id"
		const val MUSICBRAINZ_DISCID = "MusicBrainz Disc Id"
		const val MUSICBRAINZ_ORIGINAL_ALBUMID = "MusicBrainz Original Album Id"
		const val MUSICBRAINZ_RELEASE_GROUPID = "MusicBrainz Release Group Id"
		const val MUSICBRAINZ_RELEASE_TRACKID = "MusicBrainz Release Track Id"
		const val MUSICBRAINZ_RECORDING_WORK = "MUSICBRAINZ_RECORDING_WORK"
		const val MUSICBRAINZ_RECORDING_WORK_ID = "MUSICBRAINZ_RECORDING_WORK_ID"
		const val MUSICBRAINZ_WORKID = "MusicBrainz Work Id"
		const val MUSICBRAINZ_WORK = "MUSICBRAINZ_WORK"
		const val MUSICBRAINZ_WORK_PART_LEVEL1 = "MUSICBRAINZ_WORK_PART_LEVEL1"
		const val MUSICBRAINZ_WORK_PART_LEVEL1_ID = "MUSICBRAINZ_WORK_PART_LEVEL1_ID"
		const val MUSICBRAINZ_WORK_PART_LEVEL1_TYPE = "MUSICBRAINZ_WORK_PART_LEVEL1_TYPE"
		const val MUSICBRAINZ_WORK_PART_LEVEL2 = "MUSICBRAINZ_WORK_PART_LEVEL2"
		const val MUSICBRAINZ_WORK_PART_LEVEL2_ID = "MUSICBRAINZ_WORK_PART_LEVEL2_ID"
		const val MUSICBRAINZ_WORK_PART_LEVEL2_TYPE = "MUSICBRAINZ_WORK_PART_LEVEL2_TYPE"
		const val MUSICBRAINZ_WORK_PART_LEVEL3 = "MUSICBRAINZ_WORK_PART_LEVEL3"
		const val MUSICBRAINZ_WORK_PART_LEVEL3_ID = "MUSICBRAINZ_WORK_PART_LEVEL3_ID"
		const val MUSICBRAINZ_WORK_PART_LEVEL3_TYPE = "MUSICBRAINZ_WORK_PART_LEVEL3_TYPE"
		const val MUSICBRAINZ_WORK_PART_LEVEL4 = "MUSICBRAINZ_WORK_PART_LEVEL4"
		const val MUSICBRAINZ_WORK_PART_LEVEL4_ID = "MUSICBRAINZ_WORK_PART_LEVEL4_ID"
		const val MUSICBRAINZ_WORK_PART_LEVEL4_TYPE = "MUSICBRAINZ_WORK_PART_LEVEL4_TYPE"
		const val MUSICBRAINZ_WORK_PART_LEVEL5 = "MUSICBRAINZ_WORK_PART_LEVEL5"
		const val MUSICBRAINZ_WORK_PART_LEVEL5_ID = "MUSICBRAINZ_WORK_PART_LEVEL5_ID"
		const val MUSICBRAINZ_WORK_PART_LEVEL5_TYPE = "MUSICBRAINZ_WORK_PART_LEVEL5_TYPE"
		const val MUSICBRAINZ_WORK_PART_LEVEL6 = "MUSICBRAINZ_WORK_PART_LEVEL6"
		const val MUSICBRAINZ_WORK_PART_LEVEL6_ID = "MUSICBRAINZ_WORK_PART_LEVEL6_ID"
		const val MUSICBRAINZ_WORK_PART_LEVEL6_TYPE = "MUSICBRAINZ_WORK_PART_LEVEL6_TYPE"
		const val MUSICIP_ID = "MusicIP PUID"
		const val OPUS = "OPUS"
		const val ORCHESTRA = "ORCHESTRA"
		const val ORCHESTRA_SORT = "ORCHESTRA_SORT"
		const val ORIGINALRELEASEDATE = "ORIGINALRELEASEDATE"
		const val OVERALL_WORK = "OVERALL_WORK"
		const val PART = "PART"
		const val PART_NUMBER = "PARTNUMBER"
		const val PART_TYPE = "PART_TYPE"
		const val PERFORMER = "PERFORMER"
		const val PERFORMER_NAME = "PERFORMER_NAME"
		const val PERFORMER_NAME_SORT = "PERFORMER_NAME_SORT"
		const val PERIOD = "PERIOD"
		const val PRODUCER = "PRODUCER"
		const val PRODUCER_SORT = "PRODUCER_SORT"
		const val RANKING = "RANKING"
		const val RECORDINGDATE = "RECORDINGDATE"
		const val RECORDINGSTARTDATE = "RECORDINGSTARTDATE"
		const val RECORDINGENDDATE = "RECORDINGENDDATE"
		const val RECORDINGLOCATION = "RECORDINGLOCATION"
		const val REMIXER = "REMIXER"
		const val ROONALBUMTAG = "ROONALBUMTAG"
		const val ROONTRACKTAG = "ROONTRACKTAG"
		const val SCRIPT = "SCRIPT"
		const val SECTION = "SECTION"
		const val SINGLE_DISC_TRACK_NO = "SINGLE_DISC_TRACK_NO"
		const val SONGKONG_ID = "SONGKONG_ID"
		const val TAGS = "TAGS"
		const val TIMBRE = "TIMBRE_BRIGHTNESS"
		const val TITLE_MOVEMENT = "TITLE_MOVEMENT"
		const val TONALITY = "TONALITY"
		const val VERSION = "VERSION"
		const val WORK = "WORK"
		const val WORK_TYPE = "WORK_TYPE"

		//used by Foobar 20000
		const val ALBUM_ARTIST = "ALBUM ARTIST"
	}
}
