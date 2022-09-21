/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Rapha�l Slinckx <raphael@slinckx.net>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.wav

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.GenericTag
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkSummary
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav.WavOptions
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.Hex
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.images.Artwork
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.ID3V2Version
import java.nio.charset.Charset
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Represent wav metadata found in a Wav file
 *
 *
 * This can come from LIST INFO chunk or ID3 tag, LIST INFO can only contain a subset of what can be held in an ID3v2 tag,
 *
 * The default is that ID3 takes precedence if it exists
 */
class WavTag(private val wavOptions: WavOptions) : Tag, Id3SupportingTag {
	private val chunkSummaryList: MutableList<ChunkSummary> = ArrayList()
	private val metadataChunkList: MutableList<ChunkSummary> = ArrayList()
	fun addChunkSummary(cs: ChunkSummary) {
		chunkSummaryList.add(cs)
	}

	fun getChunkSummaryList(): List<ChunkSummary> {
		return chunkSummaryList
	}

	fun addMetadataChunkSummary(cs: ChunkSummary) {
		metadataChunkList.add(cs)
	}

	val metadataChunkSummaryList: List<ChunkSummary>
		get() = metadataChunkList

	//(Read audio okay) but was unable to read all chunks because of bad data chunks
	var isBadChunkData = false

	//Found null bytes not part of any chunk
	var isNonStandardPadding = false

	//Metadata tag is incorrectly aligned
	var isIncorrectlyAlignedTag = false

	/**
	 * @return true if the file that this tag was written from already contains an ID3 chunk
	 */
	var isExistingId3Tag = false

	/**
	 *
	 * @return true if the file that this tag read from already contains a LISTINFO chunk
	 */
	var isExistingInfoTag = false

	/**
	 * @return the Info tag
	 */
	var infoTag: WavInfoTag? = null
	/**
	 * Returns the ID3 tag
	 */
	/**
	 * Sets the ID3 tag
	 */
	override var iD3Tag: ID3v2TagBase? = null

	/**
	 * Does the info tag exist, note it is created by default if one does not exist in file it was read from
	 *
	 * @return
	 */
	fun isInfoTag(): Boolean {
		return infoTag != null
	}

	/**
	 * Does an ID3 tag exist, note it is created by default if one does not exist in file it was read from
	 *
	 * @return
	 */
	fun isID3Tag(): Boolean {
		return iD3Tag != null
	}

	override fun toString(): String {
		val sb = StringBuilder()
		sb.append("Chunk Summary:\n")
		for (cs in chunkSummaryList) {
			sb.append(
				"""	$cs
"""
			)
		}
		sb.append("\n")
		if (iD3Tag != null) {
			sb.append("Wav ID3 Tag:\n")
			if (isExistingId3Tag) {
				sb.append(
					"\tstartLocation:" + Hex.asDecAndHex(
						startLocationInFileOfId3Chunk
					) + "\n"
				)
				sb.append(
					"\tendLocation:" + Hex.asDecAndHex(
						endLocationInFileOfId3Chunk
					) + "\n"
				)
			}
			//Wav ID3 tags needs trailing null for each value (not usually required) to work in Windows
			sb.append(
				"""
    ${iD3Tag.toString().replace("\u0000", "")}

    """.trimIndent()
			)
		}
		if (infoTag != null) {
			sb.append(
				"""
    ${infoTag.toString()}

    """.trimIndent()
			)
		}
		return sb.toString()
	}

	val activeTag: Tag?
		get() = when (wavOptions) {
			WavOptions.READ_ID3_ONLY, WavOptions.READ_ID3_ONLY_AND_SYNC -> iD3Tag
			WavOptions.READ_INFO_ONLY, WavOptions.READ_INFO_ONLY_AND_SYNC -> infoTag
			WavOptions.READ_ID3_UNLESS_ONLY_INFO, WavOptions.READ_ID3_UNLESS_ONLY_INFO_AND_SYNC -> if (isExistingId3Tag || !isExistingInfoTag) {
				iD3Tag
			} else {
				infoTag
			}
			WavOptions.READ_INFO_UNLESS_ONLY_ID3, WavOptions.READ_INFO_UNLESS_ONLY_ID3_AND_SYNC -> if (isExistingInfoTag || !isExistingId3Tag) {
				infoTag
			} else {
				iD3Tag
			}
			else -> iD3Tag
		}

	override fun equals(obj: Any?): Boolean {
		return activeTag == obj
	}

	@Throws(FieldDataInvalidException::class)
	override fun addField(field: TagField?) {
		activeTag!!.addField(field)
	}

	override fun getFields(id: String?): List<TagField?> {
		return activeTag!!.getFields(id)
	}

	/**
	 * Maps the generic key to the specific key and return the list of values for this field as strings
	 *
	 * @param genericKey
	 * @return
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	override fun getAll(genericKey: FieldKey?): List<String?>? {
		return activeTag!!.getAll(genericKey)
	}

	override fun hasCommonFields(): Boolean {
		return activeTag!!.hasCommonFields()
	}

	/**
	 * Determines whether the tag has no fields specified.<br></br>
	 *
	 *
	 *
	 * If there are no images we return empty if either there is no VorbisTag or if there is a
	 * VorbisTag but it is empty
	 *
	 * @return `true` if tag contains no field.
	 */
	override val isEmpty: Boolean
		get() = activeTag == null || activeTag!!.isEmpty

	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun setField(genericKey: FieldKey?, vararg value: String?) {
		val tagfield = createField(genericKey, *value)
		setField(tagfield)
	}

	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun addField(genericKey: FieldKey?, vararg value: String?) {
		val tagfield = createField(genericKey, *value)
		addField(tagfield)
	}

	/**
	 * @param field
	 * @throws FieldDataInvalidException
	 */
	@Throws(FieldDataInvalidException::class)
	override fun setField(field: TagField?) {
		activeTag!!.setField(field)
	}

	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun createField(genericKey: FieldKey?, vararg value: String?): TagField? {
		return activeTag!!.createField(genericKey, *value)
	}

	override fun getFirst(id: String?): String? {
		return activeTag!!.getFirst(id)
	}

	@Throws(KeyNotFoundException::class)
	override fun getValue(id: FieldKey?, n: Int): String? {
		return activeTag!!.getValue(id, n)
	}

	@Throws(KeyNotFoundException::class)
	override fun getFirst(id: FieldKey?): String? {
		return getValue(id, 0)
	}

	override fun getFirstField(id: String?): TagField? {
		return activeTag!!.getFirstField(id)
	}

	@Throws(KeyNotFoundException::class)
	override fun getFirstField(genericKey: FieldKey?): TagField? {
		return if (genericKey == null) {
			throw KeyNotFoundException()
		} else {
			activeTag!!.getFirstField(genericKey)
		}
	}

	/**
	 * Delete any instance of tag fields with this key
	 *
	 * @param fieldKey
	 */
	@Throws(KeyNotFoundException::class)
	override fun deleteField(fieldKey: FieldKey?) {
		activeTag!!.deleteField(fieldKey)
	}

	@Throws(KeyNotFoundException::class)
	override fun deleteField(id: String?) {
		activeTag!!.deleteField(id)
	}

	override val fields: Iterator<TagField?>
		get() = activeTag!!.fields
	override val fieldCount: Int
		get() = activeTag!!.fieldCount
	override val fieldCountIncludingSubValues: Int
		get() = fieldCount

	@Throws(FieldDataInvalidException::class)
	override fun setEncoding(enc: Charset?): Boolean {
		return activeTag!!.setEncoding(enc)
	}

	/**
	 * Create artwork field. Not currently supported.
	 */
	@Throws(FieldDataInvalidException::class)
	override fun createField(artwork: Artwork?): TagField? {
		return activeTag!!.createField(artwork)
	}

	@Throws(KeyNotFoundException::class)
	override fun getFields(id: FieldKey?): List<TagField?>? {
		return activeTag!!.getFields(id)
	}

	override val firstArtwork: Artwork?
		get() = activeTag!!.firstArtwork

	/**
	 * Delete all instance of artwork Field
	 *
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	override fun deleteArtworkField() {
		activeTag!!.deleteArtworkField()
	}

	/**
	 * @param genericKey
	 * @return
	 */
	override fun hasField(genericKey: FieldKey?): Boolean {
		return activeTag!!.hasField(genericKey)
	}

	override fun hasField(id: String?): Boolean {
		return activeTag!!.hasField(id)
	}

	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun createCompilationField(value: Boolean): TagField? {
		return createField(FieldKey.IS_COMPILATION, value.toString())
	}

	override val artworkList: List<Artwork>?
		get() = activeTag!!.artworkList

	/**
	 * Create field and then set within tag itself
	 *
	 * @param artwork
	 * @throws FieldDataInvalidException
	 */
	@Throws(FieldDataInvalidException::class)
	override fun setField(artwork: Artwork?) {
		this.setField(createField(artwork))
	}

	@Throws(FieldDataInvalidException::class)
	override fun addField(artwork: Artwork?) {
		this.addField(createField(artwork))
	}

	/**
	 *
	 * @return size of the vanilla ID3Tag exclusing surrounding chunk
	 */
	val sizeOfID3TagOnly: Long
		get() = if (!isExistingId3Tag) {
			0
		} else iD3Tag!!.endLocationInFile!! - iD3Tag!!.startLocationInFile!!

	/**
	 *
	 * @return size of the ID3 Chunk including header
	 */
	val sizeOfID3TagIncludingChunkHeader: Long
		get() = if (!isExistingId3Tag) {
			0
		} else sizeOfID3TagOnly + ChunkHeader.CHUNK_HEADER_SIZE

	/**
	 * Offset into file of start ID3Chunk including header
	 * @return
	 */
	val startLocationInFileOfId3Chunk: Long
		get() = if (!isExistingId3Tag) {
			0
		} else iD3Tag!!.startLocationInFile!! - ChunkHeader.CHUNK_HEADER_SIZE
	val endLocationInFileOfId3Chunk: Long
		get() = if (!isExistingId3Tag) {
			0
		} else iD3Tag!!.endLocationInFile!!

	/**
	 * If we have field in INFO tag but not ID3 tag (perhaps coz doesn't exist add them to ID3 tag)
	 */
	fun syncToId3FromInfoIfEmpty() {
		try {
			for (fieldKey in GenericTag.supportedKeys) {
				if (iD3Tag!!.getFirst(fieldKey)!!.isEmpty()) {
					val first = infoTag!!.getFirst(fieldKey)
					if (!first!!.isEmpty()) {
						iD3Tag!!.setField(fieldKey, stripNullTerminator(first))
					}
				}
			}
		} catch (deie: FieldDataInvalidException) {
			logger.log(
				Level.INFO,
				"Couldn't sync to ID3 because the data to sync was invalid",
				deie
			)
		}
	}

	/**
	 * If we have field in INFO tag but not ID3 tag (perhaps coz doesn't exist add them to ID3 tag)
	 */
	fun syncToInfoFromId3IfEmpty() {
		try {
			for (fieldKey in GenericTag.supportedKeys) {
				if (infoTag!!.getFirst(fieldKey)!!.isEmpty()) {
					if (!iD3Tag!!.getFirst(fieldKey)!!.isEmpty()) {
						infoTag!!.setField(
							fieldKey,
							addNullTerminatorIfNone(iD3Tag!!.getFirst(fieldKey))
						)
					}
				}
			}
		} catch (deie: FieldDataInvalidException) {
			logger.log(
				Level.INFO,
				"Couldn't sync to INFO because the data to sync was invalid",
				deie
			)
		}
	}

	/**
	 * If we have field in INFO tag write to ID3 tag, if not we delete form ID3
	 * (but only for tag that we can actually have in INFO tag)
	 */
	fun syncToId3FromInfoOverwrite() {
		try {
			for (fieldKey in GenericTag.supportedKeys) {
				if (!infoTag!!.getFirst(fieldKey)!!.isEmpty()) {
					iD3Tag!!.setField(fieldKey, stripNullTerminator(infoTag!!.getFirst(fieldKey)))
				} else {
					iD3Tag!!.deleteField(fieldKey)
				}
			}
		} catch (deie: FieldDataInvalidException) {
			logger.log(
				Level.INFO,
				"Couldn't sync to ID3 because the data to sync was invalid",
				deie
			)
		}
	}

	/**
	 * If we have field in ID3 tag write to INFO tag
	 */
	fun syncToInfoFromId3Overwrite() {
		try {
			for (fieldKey in GenericTag.supportedKeys) {
				if (!iD3Tag!!.getFirst(fieldKey)!!.isEmpty()) {
					infoTag!!.setField(
						fieldKey,
						addNullTerminatorIfNone(iD3Tag!!.getFirst(fieldKey))
					)
				} else {
					infoTag!!.deleteField(fieldKey)
				}
			}
		} catch (deie: FieldDataInvalidException) {
			logger.log(
				Level.INFO,
				"Couldn't sync to INFO because the data to sync was invalid",
				deie
			)
		}
	}

	private fun stripNullTerminator(value: String?): String {
		return if (value!!.endsWith(NULL)) value.substring(0, value.length - 1) else value
	}

	private fun addNullTerminatorIfNone(value: String?): String? {
		return if (value!!.endsWith(NULL)) value else value + NULL
	}

	/**
	 * Call after read to ensure your preferred tag can make use of any additional metadata
	 * held in the other tag, only used if the activetag field is empty for the fieldkey
	 */
	fun syncTagsAfterRead() {
		if (activeTag is WavInfoTag) {
			syncToInfoFromId3IfEmpty()
		} else {
			syncToId3FromInfoIfEmpty()
		}
	}

	/**
	 * Call before save if saving both tags ensure any new information is the active tag is added to the other tag
	 * overwriting any existing fields
	 */
	fun syncTagBeforeWrite() {
		if (activeTag is WavInfoTag) {
			syncToId3FromInfoOverwrite()
		} else {
			syncToInfoFromId3Overwrite()
		}
	}

	companion object {
		private val logger = Logger.getLogger(WavTag::class.java.getPackage().name)
		private const val NULL = "\u0000"

		/**
		 * Default based on user option
		 *
		 * @return
		 */
		@JvmStatic
		fun createDefaultID3Tag(): ID3v2TagBase {
			if (TagOptionSingleton.instance.iD3V2Version === ID3V2Version.ID3_V24) {
				return ID3v24Tag()
			} else if (TagOptionSingleton.instance
					.iD3V2Version === ID3V2Version.ID3_V23
			) {
				return ID3v23Tag()
			} else if (TagOptionSingleton.instance.iD3V2Version === ID3V2Version.ID3_V22
			) {
				return ID3v22Tag()
			}
			//Default in case not set somehow
			return ID3v23Tag()
		}
	}
}
