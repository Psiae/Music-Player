package com.flammky.musicplayer.common.media.audio.meta_tag.tag.aiff

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkSummary
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.Hex
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.images.Artwork
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.ID3V2Version
import java.nio.charset.Charset

/**
 * Wraps ID3Tag for most of its metadata.
 */
class AiffTag : Tag, Id3SupportingTag {

	private val chunkSummaryList: MutableList<ChunkSummary> = ArrayList()

	fun addChunkSummary(cs: ChunkSummary) {
		chunkSummaryList.add(cs)
	}

	fun getChunkSummaryList(): List<ChunkSummary> {
		return chunkSummaryList
	}

	var fileSize: Long = 0

	/**
	 * Size returned by the FORM header, this lets us know where the enclosing FORM headeer
	 * thinks is the end of file, allowing us to identify bad data at end of file
	 */
	var formSize: Long = 0

	/**
	 * If the file is larger than the size reported by the FORM size header and the last chunk
	 * extends into this area past the size reported by FORM header then indicates the FORM header is incorrect
	 * rather than just extra non iff data at file end
	 *
	 * @return true if last chunk extends past size reported by FORM header
	 */
	var isLastChunkSizeExtendsPastFormSize = false

	/**
	 * Identifies when the ID3 tag is incorrectly aligned, one byte out either way, this alows us to fix
	 * this.
	 */
	var isIncorrectlyAlignedTag = false
	/**
	 * @return true if the file that this tag was written from already contains an ID3 chunk
	 */
	/**
	 * Is there an existing metadata tag
	 */
	var isExistingId3Tag = false
	/**
	 * Returns the ID3 tag
	 */
	/**
	 * Sets the ID3 tag
	 */
	override var iD3Tag: ID3v2TagBase? = null

	//    private String loggingFilename="";
	//    public AiffTag(String loggingFilename)
	//    {
	//        this.loggingFilename = loggingFilename;
	//    }
	constructor()
	constructor(t: ID3v2TagBase?) {
		iD3Tag = t
	}

	@Throws(FieldDataInvalidException::class)
	override fun addField(field: TagField?) {
		iD3Tag!!.addField(field)
	}

	override fun getFields(id: String?): List<TagField?> {
		return iD3Tag?.getFields(id) ?: emptyList()
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
		return iD3Tag!!.getAll(genericKey)
	}

	override fun hasCommonFields(): Boolean {
		return iD3Tag!!.hasCommonFields()
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
		get() = iD3Tag == null || iD3Tag!!.isEmpty

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
		iD3Tag!!.setField(field)
	}

	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun createField(genericKey: FieldKey?, vararg value: String?): TagField? {
		return iD3Tag!!.createField(genericKey, *value)
	}

	override fun getFirst(id: String?): String? {
		return iD3Tag!!.getFirst(id)
	}

	@Throws(KeyNotFoundException::class)
	override fun getValue(id: FieldKey?, n: Int): String? {
		return iD3Tag!!.getValue(id, n)
	}

	@Throws(KeyNotFoundException::class)
	override fun getFirst(id: FieldKey?): String? {
		return getValue(id, 0)
	}

	override fun getFirstField(id: String?): TagField? {
		return iD3Tag!!.getFirstField(id)
	}

	@Throws(KeyNotFoundException::class)
	override fun getFirstField(genericKey: FieldKey?): TagField? {
		return if (genericKey == null) {
			throw KeyNotFoundException()
		} else {
			iD3Tag!!.getFirstField(genericKey)
		}
	}

	/**
	 * Delete any instance of tag fields with this key
	 *
	 * @param fieldKey
	 */
	@Throws(KeyNotFoundException::class)
	override fun deleteField(fieldKey: FieldKey?) {
		iD3Tag!!.deleteField(fieldKey)
	}

	@Throws(KeyNotFoundException::class)
	override fun deleteField(id: String?) {
		iD3Tag!!.deleteField(id)
	}

	override val fields: Iterator<TagField?>
		get() = iD3Tag!!.fields
	override val fieldCount: Int
		get() = iD3Tag!!.fieldCount
	override val fieldCountIncludingSubValues: Int
		get() = fieldCount

	@Throws(FieldDataInvalidException::class)
	override fun setEncoding(enc: Charset?): Boolean {
		return iD3Tag!!.setEncoding(enc)
	}

	/**
	 * Create artwork field. Not currently supported.
	 */
	@Throws(FieldDataInvalidException::class)
	override fun createField(artwork: Artwork?): TagField? {
		return iD3Tag!!.createField(artwork)
	}

	@Throws(FieldDataInvalidException::class)
	override fun setField(artwork: Artwork?) {
		iD3Tag!!.setField(artwork)
	}

	@Throws(FieldDataInvalidException::class)
	override fun addField(artwork: Artwork?) {
		iD3Tag!!.addField(artwork)
	}

	override val artworkList: List<Artwork>?
		get() = iD3Tag?.artworkList

	@Throws(KeyNotFoundException::class)
	override fun getFields(id: FieldKey?): List<TagField?>? {
		return iD3Tag!!.getFields(id)
	}

	override val firstArtwork: Artwork?
		get() = iD3Tag!!.firstArtwork

	/**
	 * Delete all instance of artwork Field
	 *
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	override fun deleteArtworkField() {
		iD3Tag!!.deleteArtworkField()
	}

	override fun hasField(genericKey: FieldKey?): Boolean {
		return iD3Tag!!.hasField(genericKey)
	}

	override fun hasField(id: String?): Boolean {
		return iD3Tag!!.hasField(id)
	}

	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun createCompilationField(value: Boolean): TagField? {
		return createField(FieldKey.IS_COMPILATION, value.toString())
	}

	override fun toString(): String {
		val sb = StringBuilder()
		sb.append(
			"""
    FileSize:${Hex.asDecAndHex(fileSize)}

    """.trimIndent()
		)
		sb.append(
			"""
    FORMSize:${Hex.asDecAndHex(formSize + ChunkHeader.CHUNK_HEADER_SIZE)}

    """.trimIndent()
		)
		if (isLastChunkSizeExtendsPastFormSize) {
			sb.append("Last Chunk extends past Form stated size\n")
		} else if (fileSize > formSize + ChunkHeader.CHUNK_HEADER_SIZE) {
			sb.append(
				"""Non Iff Data at End of File:${fileSize - (formSize + ChunkHeader.CHUNK_HEADER_SIZE)} bytes
"""
			)
		}
		sb.append("Chunks:\n")
		for (cs in chunkSummaryList) {
			sb.append(
				"""	$cs
"""
			)
		}
		return if (iD3Tag != null) {
			sb.append("Aiff ID3 Tag:\n")
			if (isExistingId3Tag) {
				if (isIncorrectlyAlignedTag) {
					sb.append("\tincorrectly starts as odd byte\n")
				}
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
			sb.append(
				"""
    ${iD3Tag.toString()}

    """.trimIndent()
			)
			sb.toString()
		} else {
			"tag:empty"
		}
	}

	/**
	 * @return size of the vanilla ID3Tag excluding surrounding chunk
	 */
	val sizeOfID3TagOnly: Long
		get() = if (!isExistingId3Tag) {
			0
		} else iD3Tag!!.endLocationInFile!! - iD3Tag!!.startLocationInFile!!

	/**
	 * @return size of the ID3 Chunk including header
	 */
	val sizeOfID3TagIncludingChunkHeader: Long
		get() = if (!isExistingId3Tag) {
			0
		} else sizeOfID3TagOnly + ChunkHeader.CHUNK_HEADER_SIZE

	/**
	 * Offset into file of start ID3Chunk including header
	 *
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

	override fun equals(obj: Any?): Boolean {
		return iD3Tag!!.equals(obj)
	}

	companion object {
		/**
		 * Default based on user option
		 *
		 * @return
		 */
		@JvmStatic
		fun createDefaultID3Tag(): ID3v2TagBase {
			if (TagOptionSingleton.instance.iD3V2Version === ID3V2Version.ID3_V24) {
				return ID3v24Tag()
			} else if (TagOptionSingleton.instance.iD3V2Version === ID3V2Version.ID3_V23) {
				return ID3v23Tag()
			} else if (TagOptionSingleton.instance.iD3V2Version === ID3V2Version.ID3_V22) {
				return ID3v22Tag()
			}
			//Default in case not set somehow
			return ID3v23Tag()
		}
	}
}
