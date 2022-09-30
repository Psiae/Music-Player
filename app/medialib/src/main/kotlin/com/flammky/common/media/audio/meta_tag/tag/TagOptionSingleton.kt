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
 * Options that are used for every datatype and class in this library.
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav.WavOptions
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav.WavSaveOptions
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav.WavSaveOrder
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v22Frames
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v23Frames
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Frames
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.AbstractID3v2FrameBody
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.FrameBodyCOMM
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.FrameBodyTIPL
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.ID3v24FrameBody
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.lyrics3.Lyrics3v2Fields
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.options.PadNumberOption
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.GenreTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.ID3V2Version
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.Languages
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.vorbiscomment.VorbisAlbumArtistReadOptions
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.vorbiscomment.VorbisAlbumArtistSaveOptions
import java.util.*

class TagOptionSingleton private constructor() {
	var isWriteWavForTwonky = false
	var wavOptions = WavOptions.READ_ID3_ONLY
	var wavSaveOptions = WavSaveOptions.SAVE_BOTH
	var wavSaveOrder = WavSaveOrder.INFO_THEN_ID3
	var vorbisAlbumArtistSaveOptions = VorbisAlbumArtistSaveOptions.WRITE_ALBUMARTIST
	var vorbisAlbumArtisReadOptions = VorbisAlbumArtistReadOptions.READ_ALBUMARTIST_THEN_JRIVER
		private set

	fun setVorbisAlbumArtistReadOptions(vorbisAlbumArtistReadOptions: VorbisAlbumArtistReadOptions) {
		vorbisAlbumArtisReadOptions = vorbisAlbumArtistReadOptions
	}

	/**
	 *
	 */
	private var keywordMap = HashMap<Class<out ID3v24FrameBody?>, LinkedList<String>>()
	/**
	 * @return
	 */
	/**
	 * Map of lyric ID's to Boolean objects if we should or should not save the
	 * specific lyrics3 field. Defaults to true.
	 */
	var lyrics3SaveFieldMap = HashMap<String, Boolean>()
		private set

	/**
	 * parenthesis map stuff
	 */
	private var parenthesisMap = HashMap<String, String>()

	/**
	 * `HashMap` listing words to be replaced if found
	 */
	private var replaceWordMap = HashMap<String, String>()

	/**
	 * default language for any ID3v2 tags frames which require it. This string
	 * is in the [ISO-639-2] ISO/FDIS 639-2 definition
	 */
	private var language = "eng"
	/**
	 * @return
	 */
	/**
	 * @param filenameTagSave
	 */
	/**
	 *
	 */
	var isFilenameTagSave = false
	/**
	 * @return
	 */
	/**
	 * @param id3v1Save
	 */
	/**
	 * if we should save any fields of the ID3v1 tag or not. Defaults to true.
	 */
	var isId3v1Save = true
	/**
	 * @return
	 */
	/**
	 * @param id3v1SaveAlbum
	 */
	/**
	 * if we should save the album field of the ID3v1 tag or not. Defaults to
	 * true.
	 */
	var isId3v1SaveAlbum = true
	/**
	 * @return
	 */
	/**
	 * @param id3v1SaveArtist
	 */
	/**
	 * if we should save the artist field of the ID3v1 tag or not. Defaults to
	 * true.
	 */
	var isId3v1SaveArtist = true
	/**
	 * @return
	 */
	/**
	 * @param id3v1SaveComment
	 */
	/**
	 * if we should save the comment field of the ID3v1 tag or not. Defaults to
	 * true.
	 */
	var isId3v1SaveComment = true
	/**
	 * @return
	 */
	/**
	 * @param id3v1SaveGenre
	 */
	/**
	 * if we should save the genre field of the ID3v1 tag or not. Defaults to
	 * true.
	 */
	var isId3v1SaveGenre = true
	/**
	 * @return
	 */
	/**
	 * @param id3v1SaveTitle
	 */
	/**
	 * if we should save the title field of the ID3v1 tag or not. Defaults to
	 * true.
	 */
	var isId3v1SaveTitle = true
	/**
	 * @return
	 */
	/**
	 * @param id3v1SaveTrack
	 */
	/**
	 * if we should save the track field of the ID3v1 tag or not. Defaults to
	 * true.
	 */
	var isId3v1SaveTrack = true
	/**
	 * @return
	 */
	/**
	 * @param id3v1SaveYear
	 */
	/**
	 * if we should save the year field of the ID3v1 tag or not. Defaults to
	 * true.
	 */
	var isId3v1SaveYear = true
	/**
	 * @return
	 */
	/**
	 * @param id3v2PaddingCopyTag
	 */
	/**
	 * When adjusting the ID3v2 padding, if should we copy the current ID3v2
	 * tag to the new MP3 file. Defaults to true.
	 */
	var isId3v2PaddingCopyTag = true
	/**
	 * @return
	 */
	/**
	 * @param id3v2PaddingWillShorten
	 */
	/**
	 * When adjusting the ID3v2 padding, if we should shorten the length of the
	 * ID3v2 tag padding. Defaults to false.
	 */
	var isId3v2PaddingWillShorten = false
	/**
	 * @return
	 */
	/**
	 * @param id3v2Save
	 */
	/**
	 * if we should save any fields of the ID3v2 tag or not. Defaults to true.
	 */
	var isId3v2Save = true
	/**
	 *
	 * Special work/grouping mode for iTunes 12.6.
	 *
	 *
	 * If `true`, map
	 * [FieldKey.WORK] to [FrameBodyTIT1]
	 * and [FieldKey.GROUPING] to [FrameBodyGRP1].
	 *
	 *
	 * If `false`, map
	 * [FieldKey.WORK] to special [FrameBodyTXXX]
	 * and [FieldKey.GROUPING] to [FrameBodyTIT1].
	 *
	 *
	 * The latter used to be the default behavior before iTunes 12.6.
	 *
	 * @param id3v2ITunes12_6WorkGroupingMode `true` or `false`.
	 */
	/**
	 * Special mode for iTunes 12.6.
	 *
	 * If `true`, map
	 * [FieldKey.WORK] to [FrameBodyTIT1]
	 * and [FieldKey.GROUPING] to [FrameBodyGRP1].
	 *
	 * If `false`, map
	 * [FieldKey.WORK] to special [FrameBodyTXXX]
	 * and [FieldKey.GROUPING] to [FrameBodyTIT1].
	 *
	 * The latter used to be the default behavior before iTunes 12.6.
	 */
	var isId3v2ITunes12_6WorkGroupingMode = false
		set(id3v2ITunes12_6WorkGroupingMode) {
			val oldMode = isId3v2ITunes12_6WorkGroupingMode
			if (oldMode != id3v2ITunes12_6WorkGroupingMode) {
				ID3v22Frames.instanceOf
					.setITunes12_6WorkGroupingMode(id3v2ITunes12_6WorkGroupingMode)
				ID3v23Frames.instanceOf
					.setITunes12_6WorkGroupingMode(id3v2ITunes12_6WorkGroupingMode)
				ID3v24Frames.instanceOf
					.setITunes12_6WorkGroupingMode(id3v2ITunes12_6WorkGroupingMode)
				field = id3v2ITunes12_6WorkGroupingMode
			}
		}
	/**
	 * @return
	 */
	/**
	 * @param lyrics3KeepEmptyFieldIfRead
	 */
	/**
	 * if we should keep an empty Lyrics3 field while we're reading. This is
	 * different from a string of white space. Defaults to false.
	 */
	var isLyrics3KeepEmptyFieldIfRead = false
	/**
	 * @return
	 */
	/**
	 * @param lyrics3Save
	 */
	/**
	 * if we should save any fields of the Lyrics3 tag or not. Defaults to
	 * true.
	 */
	var isLyrics3Save = true
	/**
	 * @return
	 */
	/**
	 * @param lyrics3SaveEmptyField
	 */
	/**
	 * if we should save empty Lyrics3 field or not. Defaults to false.
	 *
	 * todo I don't think this is implemented yet.
	 */
	var isLyrics3SaveEmptyField = false
	/**
	 * @return
	 */
	/**
	 * @param originalSavedAfterAdjustingID3v2Padding
	 */
	/**
	 *
	 */
	var isOriginalSavedAfterAdjustingID3v2Padding = true

	/**
	 * default time stamp format for any ID3v2 tag frames which require it.
	 */
	private var timeStampFormat: Byte = 2
	/**
	 * Returns the number of MP3 frames to sync when trying to find the start
	 * of the MP3 frame data. The start of the MP3 frame data is the start of
	 * the music and is different from the ID3v2 frame data. WinAmp 2.8 seems
	 * to sync 3 frames. Default is 5.
	 *
	 * @return number of MP3 frames to sync
	 */
	/**
	 * Sets the number of MP3 frames to sync when trying to find the start of
	 * the MP3 frame data. The start of the MP3 frame data is the start of the
	 * music and is different from the ID3v2 frame data. WinAmp 2.8 seems to
	 * sync 3 frames. Default is 5.
	 *
	 * @param numberMP3SyncFrame number of MP3 frames to sync
	 */
	/**
	 * number of frames to sync when trying to find the start of the MP3 frame
	 * data. The start of the MP3 frame data is the start of the music and is
	 * different from the ID3v2 frame data.
	 */
	var numberMP3SyncFrame = 3
	/**
	 * @return are tags unsynchronized when written if contain bit pattern that could be mistaken for audio marker
	 */
	/**
	 * Unsync tag where necessary, currently only applies to IDv23
	 *
	 * @param unsyncTags set whether tags are  unsynchronized when written if contain bit pattern that could
	 * be mistaken for audio marker
	 */
	/**
	 * Unsynchronize tags/frames this is rarely required these days and can cause more
	 * problems than it solves
	 */
	var isUnsyncTags = false
	/**
	 * Do we remove unnecessary trailing null characters on write
	 *
	 * @return true if we remove unnecessary trailing null characters on write
	 */
	/**
	 * Remove unnecessary trailing null characters on write
	 *
	 * @param removeTrailingTerminatorOnWrite
	 */
	/**
	 * iTunes needlessly writes null terminators at the end for TextEncodedStringSizeTerminated values,
	 * if this option is enabled these characters are removed
	 */
	var isRemoveTrailingTerminatorOnWrite = true

	/**
	 * This is the default text encoding to use for new v23 frames, when unicode is required
	 * UTF16 will always be used because that is the only valid option for v23.
	 */
	private var id3v23DefaultTextEncoding = TextEncoding.ISO_8859_1

	/**
	 * This is the default text encoding to use for new v24 frames, it defaults to simple ISO8859
	 * but by changing this value you could always used UTF8 for example whether you needed to or not
	 */
	private var id3v24DefaultTextEncoding = TextEncoding.ISO_8859_1

	/**
	 * This is text encoding to use for new v24 frames when unicode is required, it defaults to UTF16 just
	 * because this encoding is understand by all ID3 versions
	 */
	private var id3v24UnicodeTextEncoding = TextEncoding.UTF_16
	/**
	 * When writing frames if this is set to true then the frame will be written
	 * using the defaults disregarding the text encoding originally used to create
	 * the frame.
	 *
	 * @return
	 */
	/**
	 * When writing frames if this is set to true then the frame will be written
	 * using the defaults disregarding the text encoding originally used to create
	 * the frame.
	 *
	 * @param resetTextEncodingForExistingFrames
	 */
	/**
	 * When writing frames if this is set to true then the frame will be written
	 * using the defaults disregarding the text encoding originally used to create
	 * the frame.
	 */
	var isResetTextEncodingForExistingFrames = false
	/**
	 *
	 * @return truncate without errors
	 */
	/**
	 * Set truncate without errors
	 *
	 * @param truncateTextWithoutErrors
	 */
	/**
	 * Some formats impose maxmimum lengths for fields , if the text provided is longer
	 * than the formats allows it will truncate and write a warning, if this is not set
	 * it will throw an exception
	 */
	var isTruncateTextWithoutErrors = false

	/**
	 * Frames such as TRCK and TPOS sometimes pad single digit numbers to aid sorting
	 *
	 * Currently only applies to ID3 files
	 */
	var isPadNumbers = false
	/**
	 * Total length of number, i.e if set to 2 the value 1 would be stored as 01, if set to 3 would bs stored as 001
	 */
	/**
	 * Number of padding zeroes digits 1- 9, numbers larger than nine will be padded accordingly based on the value.
	 * Only has any effect if padNumbers is set to true
	 *
	 * Currently only applies to ID3 files
	 */
	var padNumberTotalLength = PadNumberOption.PAD_ONE_ZERO

	/**
	 * There are a couple of problems with the Java implementation on Google Android, enabling this value
	 * switches on Google workarounds
	 */
	var isAndroid = false

	/**
	 * Itunes expects APIC description to be encoded as ISO-8859-1 even if text encoding is set to 1 (UTF16)
	 */
	var isAPICDescriptionITunesCompatible = false
	/**
	 * When you specify a field should be stored as UTF16 in ID3 this means write with BOM indicating whether
	 * written as Little Endian or Big Endian, its defaults to little Endian
	 */
	/**
	 * When you specify a field should be stored as UTF16 in ID3 this means write with BOM indicating whether
	 * written as Little Endian or Big Endian, its defaults to little Endian
	 */
	var isEncodeUTF16BomAsLittleEndian = true
	/**
	 * When this is set and using the generic interface jaudiotagger will make some adjustmensts
	 * when saving field sso they work best with the specified Tagger
	 *
	 */
	/**
	 * When this is set and using the generic interface jaudiotagger will make some adjustments
	 * when saving field so they work best with the specified Tagger
	 */
	//TODO Not Actually Used yet, originally intended for dealing with ratings and genres
	var playerCompatability = -1
	/**
	 * When we have to create new audio files and shift audio data to fit in more metadata this value
	 * set the maximum amount in bytes that can be transferred in one call, this is to protect against
	 * various OutOfMemoryExceptions that cna occur, especially on networked filesystems.
	 */
	/**
	 * max size of data to copy when copying audiodata from one file to , default to 4mb
	 */
	var writeChunkSize = (4 * 1024 * 1024).toLong()

	/**
	 * If enabled we always use the ©gen atom rather than the gnre atom when writing genres to mp4s
	 * This is known to help some android apps
	 */
	var isWriteMp4GenresAsText = false

	/**
	 * If enabled we always use the ©gen atom rather than the gnre atom when writing genres to mp4s
	 * This is known to help some android apps
	 */
	var isWriteMp3GenresAsText = false
	/**
	 * @return
	 */
	/**
	 * @param id3v2Version
	 */
	var iD3V2Version = ID3V2Version.ID3_V23
	/**
	 * Whether Files.isWritable should be used to check if a file can be written. In some
	 * cases, isWritable can return false negatives.
	 */
	/**
	 * Whether Files.isWritable should be used to check if a file can be written. In some
	 * cases, isWritable can return false negatives.
	 */
	var isCheckIsWritable = false
	/**
	 *
	 *
	 * If set to `true`, when writing, make an attempt to overwrite the existing file in-place
	 * instead of first moving it out of the way and moving a temp file into its place.
	 *
	 *
	 *
	 * Preserving the file identity has the advantage of preserving the creation time
	 * as well as the Unix inode or Windows
	 * [fileIndex](https://msdn.microsoft.com/en-us/library/aa363788(v=vs.85).aspx).
	 *
	 *
	 * @return `true` or `false`. Default is `false`.
	 */
	/**
	 * If set to `true`, when writing, make an attempt to preserve the file identity.
	 *
	 * @param preserveFileIdentity `true` or `false`
	 * @see .isPreserveFileIdentity
	 */
	/**
	 * Preserve file identity if possible
	 */
	var isPreserveFileIdentity = true
	/**
	 *
	 */
	/**
	 * Creates a new TagOptions datatype. All Options are set to their default
	 * values
	 */
	init {
		setToDefault()
	}

	/**
	 * @param instanceKey
	 */
	fun setInstanceKey(instanceKey: String) {
		Companion.instanceKey = instanceKey
	}

	/**
	 * @return
	 */
	val keywordIterator: Iterator<Class<out ID3v24FrameBody?>>
		get() = keywordMap.keys.iterator()

	/**
	 * @param id3v2_4FrameBody
	 * @return
	 */
	fun getKeywordListIterator(id3v2_4FrameBody: Class<out ID3v24FrameBody?>): Iterator<String> {
		return keywordMap[id3v2_4FrameBody]!!.iterator()
	}

	/**
	 * Sets the default language for any ID3v2 tag frames which require it.
	 * While the value will already exist when reading from a file, this value
	 * will be used when a new ID3v2 Frame is created from scratch.
	 *
	 * @param lang language ID, [ISO-639-2] ISO/FDIS 639-2 definition
	 */
	fun setLanguage(lang: String) {
		if (Languages.instanceOf.getIdToValueMap().containsKey(lang)) {
			language = lang
		}
	}

	/**
	 * Returns the default language for any ID3v2 tag frames which require it.
	 *
	 * @return language ID, [ISO-639-2] ISO/FDIS 639-2 definition
	 */
	fun getLanguage(): String {
		return language
	}

	/**
	 * Sets if we should save the Lyrics3 field. Defaults to true.
	 *
	 * @param id   Lyrics3 id string
	 * @param save true if you want to save this specific Lyrics3 field.
	 */
	fun setLyrics3SaveField(id: String, save: Boolean) {
		lyrics3SaveFieldMap[id] = save
	}

	/**
	 * Returns true if we should save the Lyrics3 field asked for in the
	 * argument. Defaults to true.
	 *
	 * @param id Lyrics3 id string
	 * @return true if we should save the Lyrics3 field.
	 */
	fun getLyrics3SaveField(id: String): Boolean {
		return lyrics3SaveFieldMap[id]!!
	}

	/**
	 * @param oldWord
	 * @return
	 */
	fun getNewReplaceWord(oldWord: String): String? {
		return replaceWordMap[oldWord]
	}

	/**
	 * @return
	 */
	val oldReplaceWordIterator: Iterator<String>
		get() = replaceWordMap.keys.iterator()

	/**
	 * @param open
	 * @return
	 */
	fun isOpenParenthesis(open: String): Boolean {
		return parenthesisMap.containsKey(open)
	}

	/**
	 * @return
	 */
	val openParenthesisIterator: Iterator<String>
		get() = parenthesisMap.keys.iterator()

	/**
	 * Sets the default time stamp format for ID3v2 tags which require it.
	 * While the value will already exist when reading from a file, this value
	 * will be used when a new ID3v2 Frame is created from scratch.
	 *
	 *
	 * $01  Absolute time, 32 bit sized, using MPEG frames as unit<br></br>
	 * $02  Absolute time, 32 bit sized, using milliseconds as unit<br></br>
	 *
	 *
	 * @param tsf the new default time stamp format
	 */
	fun setTimeStampFormat(tsf: Byte) {
		if (tsf.toInt() == 1 || tsf.toInt() == 2) {
			timeStampFormat = tsf
		}
	}

	/**
	 * Returns the default time stamp format for ID3v2 tags which require it.
	 *
	 *
	 * $01  Absolute time, 32 bit sized, using MPEG frames as unit<br></br>
	 * $02  Absolute time, 32 bit sized, using milliseconds as unit<br></br>
	 *
	 *
	 * @return the default time stamp format
	 */
	fun getTimeStampFormat(): Byte {
		return timeStampFormat
	}

	/**
	 *
	 */
	fun setToDefault() {
		isWriteWavForTwonky = false
		wavOptions = WavOptions.READ_ID3_UNLESS_ONLY_INFO
		wavSaveOptions = WavSaveOptions.SAVE_BOTH
		keywordMap = HashMap()
		isFilenameTagSave = false
		isId3v1Save = true
		isId3v1SaveAlbum = true
		isId3v1SaveArtist = true
		isId3v1SaveComment = true
		isId3v1SaveGenre = true
		isId3v1SaveTitle = true
		isId3v1SaveTrack = true
		isId3v1SaveYear = true
		isId3v2PaddingCopyTag = true
		isId3v2PaddingWillShorten = false
		isId3v2Save = true
		language = "eng"
		isLyrics3KeepEmptyFieldIfRead = false
		isLyrics3Save = true
		isLyrics3SaveEmptyField = false
		lyrics3SaveFieldMap = HashMap()
		numberMP3SyncFrame = 3
		parenthesisMap = HashMap()
		replaceWordMap = HashMap()
		timeStampFormat = 2
		isUnsyncTags = false
		isRemoveTrailingTerminatorOnWrite = true
		id3v23DefaultTextEncoding = TextEncoding.ISO_8859_1
		id3v24DefaultTextEncoding = TextEncoding.ISO_8859_1
		id3v24UnicodeTextEncoding = TextEncoding.UTF_16
		isResetTextEncodingForExistingFrames = false
		isTruncateTextWithoutErrors = false
		isPadNumbers = false
		isAPICDescriptionITunesCompatible = false
		isAndroid = false
		isEncodeUTF16BomAsLittleEndian = true
		writeChunkSize = 5000000
		isWriteMp4GenresAsText = false
		padNumberTotalLength = PadNumberOption.PAD_ONE_ZERO
		iD3V2Version = ID3V2Version.ID3_V23
		isCheckIsWritable = false
		isPreserveFileIdentity = true
		//default all lyrics3 fields to save. id3v1 fields are individual
		// settings. id3v2 fields are always looked at to save.
		var iterator: Iterator<String?> = Lyrics3v2Fields.instanceOf.getIdToValueMap().keys.iterator()
		var fieldId: String?
		while (iterator.hasNext()) {
			fieldId = iterator.next()
			lyrics3SaveFieldMap[fieldId!!] = true
		}
		try {
			addKeyword(FrameBodyCOMM::class.java, "ultimix")
			addKeyword(FrameBodyCOMM::class.java, "dance")
			addKeyword(FrameBodyCOMM::class.java, "mix")
			addKeyword(FrameBodyCOMM::class.java, "remix")
			addKeyword(FrameBodyCOMM::class.java, "rmx")
			addKeyword(FrameBodyCOMM::class.java, "live")
			addKeyword(FrameBodyCOMM::class.java, "cover")
			addKeyword(FrameBodyCOMM::class.java, "soundtrack")
			addKeyword(FrameBodyCOMM::class.java, "version")
			addKeyword(FrameBodyCOMM::class.java, "acoustic")
			addKeyword(FrameBodyCOMM::class.java, "original")
			addKeyword(FrameBodyCOMM::class.java, "cd")
			addKeyword(FrameBodyCOMM::class.java, "extended")
			addKeyword(FrameBodyCOMM::class.java, "vocal")
			addKeyword(FrameBodyCOMM::class.java, "unplugged")
			addKeyword(FrameBodyCOMM::class.java, "acapella")
			addKeyword(FrameBodyCOMM::class.java, "edit")
			addKeyword(FrameBodyCOMM::class.java, "radio")
			addKeyword(FrameBodyCOMM::class.java, "original")
			addKeyword(FrameBodyCOMM::class.java, "album")
			addKeyword(FrameBodyCOMM::class.java, "studio")
			addKeyword(FrameBodyCOMM::class.java, "instrumental")
			addKeyword(FrameBodyCOMM::class.java, "unedited")
			addKeyword(FrameBodyCOMM::class.java, "karoke")
			addKeyword(FrameBodyCOMM::class.java, "quality")
			addKeyword(FrameBodyCOMM::class.java, "uncensored")
			addKeyword(FrameBodyCOMM::class.java, "clean")
			addKeyword(FrameBodyCOMM::class.java, "dirty")
			addKeyword(FrameBodyTIPL::class.java, "f.")
			addKeyword(FrameBodyTIPL::class.java, "feat")
			addKeyword(FrameBodyTIPL::class.java, "feat.")
			addKeyword(FrameBodyTIPL::class.java, "featuring")
			addKeyword(FrameBodyTIPL::class.java, "ftng")
			addKeyword(FrameBodyTIPL::class.java, "ftng.")
			addKeyword(FrameBodyTIPL::class.java, "ft.")
			addKeyword(FrameBodyTIPL::class.java, "ft")
			iterator = GenreTypes.instanceOf.getValueToIdMap().keys.iterator()
			while (iterator.hasNext()) {
				addKeyword(FrameBodyCOMM::class.java, iterator.next())
			}
			/** [org.jaudiotagger.tag.TagOptionSingleton] */
		} catch (ex: TagException) {
			// this shouldn't happen, indicates coding error
			throw RuntimeException(ex)
		}
		addReplaceWord("v.", "vs.")
		addReplaceWord("vs.", "vs.")
		addReplaceWord("versus", "vs.")
		addReplaceWord("f.", "feat.")
		addReplaceWord("feat", "feat.")
		addReplaceWord("featuring", "feat.")
		addReplaceWord("ftng.", "feat.")
		addReplaceWord("ftng", "feat.")
		addReplaceWord("ft.", "feat.")
		addReplaceWord("ft", "feat.")
		iterator = getKeywordListIterator(FrameBodyTIPL::class.java)
		addParenthesis("(", ")")
		addParenthesis("[", "]")
		addParenthesis("{", "}")
		addParenthesis("<", ">")
	}

	/**
	 * @param id3v2FrameBodyClass
	 * @param keyword
	 * @throws TagException
	 */
	@Throws(TagException::class)
	fun addKeyword(id3v2FrameBodyClass: Class<out ID3v24FrameBody?>, keyword: String?) {
		if (!AbstractID3v2FrameBody::class.java.isAssignableFrom(id3v2FrameBodyClass)) {
			throw TagException("Invalid class type. Must be AbstractId3v2FrameBody $id3v2FrameBodyClass")
		}
		if (keyword != null && keyword.isNotEmpty()) {
			val keywordList: LinkedList<String>
			if (!keywordMap.containsKey(id3v2FrameBodyClass)) {
				keywordList = LinkedList()
				keywordMap[id3v2FrameBodyClass] = keywordList
			} else {
				keywordList = keywordMap[id3v2FrameBodyClass]!!
			}
			keywordList.add(keyword)
		}
	}

	/**
	 * @param open
	 * @param close
	 */
	fun addParenthesis(open: String, close: String) {
		parenthesisMap[open] = close
	}

	/**
	 * @param oldWord
	 * @param newWord
	 */
	fun addReplaceWord(oldWord: String, newWord: String) {
		replaceWordMap[oldWord] = newWord
	}

	/**
	 * Get the default text encoding to use for new v23 frames, when unicode is required
	 * UTF16 will always be used because that is the only valid option for v23/v22
	 *
	 * @return
	 */
	fun getId3v23DefaultTextEncoding(): Byte {
		return id3v23DefaultTextEncoding
	}

	/**
	 * Set the default text encoding to use for new v23 frames, when unicode is required
	 * UTF16 will always be used because that is the only valid option for v23/v22
	 *
	 * @param id3v23DefaultTextEncoding
	 */
	fun setId3v23DefaultTextEncoding(id3v23DefaultTextEncoding: Byte) {
		if (id3v23DefaultTextEncoding == TextEncoding.ISO_8859_1 || id3v23DefaultTextEncoding == TextEncoding.UTF_16) {
			this.id3v23DefaultTextEncoding = id3v23DefaultTextEncoding
		}
	}

	/**
	 * Get the default text encoding to use for new v24 frames, it defaults to simple ISO8859
	 * but by changing this value you could always used UTF8 for example whether you needed to or not
	 *
	 * @return
	 */
	fun getId3v24DefaultTextEncoding(): Byte {
		return id3v24DefaultTextEncoding
	}

	/**
	 * Set the default text encoding to use for new v24 frames, it defaults to simple ISO8859
	 * but by changing this value you could always used UTF8 for example whether you needed to or not
	 *
	 * @param id3v24DefaultTextEncoding
	 */
	fun setId3v24DefaultTextEncoding(id3v24DefaultTextEncoding: Byte) {
		if (id3v24DefaultTextEncoding == TextEncoding.ISO_8859_1 || id3v24DefaultTextEncoding == TextEncoding.UTF_16 || id3v24DefaultTextEncoding == TextEncoding.UTF_16BE || id3v24DefaultTextEncoding == TextEncoding.UTF_8) {
			this.id3v24DefaultTextEncoding = id3v24DefaultTextEncoding
		}
	}

	/**
	 * Get the text encoding to use for new v24 frames when unicode is required, it defaults to UTF16 just
	 * because this encoding is understand by all ID3 versions
	 *
	 * @return
	 */
	fun getId3v24UnicodeTextEncoding(): Byte {
		return id3v24UnicodeTextEncoding
	}

	/**
	 * Set the text encoding to use for new v24 frames when unicode is required, it defaults to UTF16 just
	 * because this encoding is understand by all ID3 versions
	 *
	 * @param id3v24UnicodeTextEncoding
	 */
	fun setId3v24UnicodeTextEncoding(id3v24UnicodeTextEncoding: Byte) {
		if (id3v24UnicodeTextEncoding == TextEncoding.UTF_16 || id3v24UnicodeTextEncoding == TextEncoding.UTF_16BE || id3v24UnicodeTextEncoding == TextEncoding.UTF_8) {
			this.id3v24UnicodeTextEncoding = id3v24UnicodeTextEncoding
		}
	}

	companion object {
		/**
		 *
		 */
		private val tagOptionTable = HashMap<String, TagOptionSingleton>()

		/**
		 *
		 */
		private const val DEFAULT = "default"
		/**
		 * @return
		 */
		/**
		 *
		 */
		var instanceKey = DEFAULT
			private set

		/**
		 * @return
		 */
		val instance: TagOptionSingleton
			get() = getInstance(instanceKey)

		/**
		 * @param instanceKey
		 * @return
		 */
		fun getInstance(instanceKey: String): TagOptionSingleton {
			var tagOptions = tagOptionTable[instanceKey]
			if (tagOptions == null) {
				tagOptions = TagOptionSingleton()
				tagOptionTable[instanceKey] = tagOptions
			}
			return tagOptions
		}
	}
}
