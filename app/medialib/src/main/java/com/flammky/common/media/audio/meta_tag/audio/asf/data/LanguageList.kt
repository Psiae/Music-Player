package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import java.math.BigInteger

/**
 * This structure represents the data of the ASF language object.<br></br>
 * The language list is simply a listing of language codes which should comply
 * to RFC-1766.<br></br>
 * **Consider:** the index of a language is used by other entries in the ASF
 * metadata.
 *
 * @author Christian Laireiter
 */
class LanguageList : Chunk {
	/**
	 * List of language codes, complying RFC-1766
	 */
	private val languages: MutableList<String> = ArrayList()

	/**
	 * Creates a new instance.<br></br>
	 */
	constructor() : super(GUID.GUID_LANGUAGE_LIST, 0, BigInteger.ZERO)

	/**
	 * Creates an instance.
	 *
	 * @param pos  position within the ASF file.
	 * @param size size of the chunk
	 */
	constructor(pos: Long, size: BigInteger?) : super(GUID.GUID_LANGUAGE_LIST, pos, size)

	/**
	 * This method adds a language.<br></br>
	 *
	 * @param language language code
	 */
	fun addLanguage(language: String) {
		if (language.length < MetadataDescriptor.Companion.MAX_LANG_INDEX) {
			if (!languages.contains(language)) {
				languages.add(language)
			}
		} else {
			throw IllegalArgumentException(
				ErrorMessage.WMA_LENGTH_OF_LANGUAGE_IS_TOO_LARGE.getMsg(
					language.length * 2 + 2
				)
			)
		}
	}

	/**
	 * Returns the language code at the specified index.
	 *
	 * @param index the index of the language code to get.
	 * @return the language code at given index.
	 */
	fun getLanguage(index: Int): String {
		return languages[index]
	}

	/**
	 * Returns the amount of stored language codes.
	 *
	 * @return number of stored language codes.
	 */
	val languageCount: Int
		get() = languages.size

	/**
	 * Returns all language codes in list.
	 *
	 * @return list of language codes.
	 */
	fun getLanguages(): List<String> {
		return ArrayList(languages)
	}

	/**
	 * {@inheritDoc}
	 */
	override fun prettyPrint(prefix: String): String {
		val result = StringBuilder(super.prettyPrint(prefix))
		for (i in 0 until languageCount) {
			result.append(prefix)
			result.append("  |-> ")
			result.append(i)
			result.append(" : ")
			result.append(getLanguage(i))
			result.append(Utils.LINE_SEPARATOR)
		}
		return result.toString()
	}

	/**
	 * Removes the language entry at specified index.
	 *
	 * @param index index of language to remove.
	 */
	fun removeLanguage(index: Int) {
		languages.removeAt(index)
	}
}
