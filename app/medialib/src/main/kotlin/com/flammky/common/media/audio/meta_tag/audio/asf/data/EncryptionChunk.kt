package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import java.math.BigInteger

/**
 * @author eric
 */
class EncryptionChunk(chunkLen: BigInteger?) : Chunk(GUID.GUID_CONTENT_ENCRYPTION, chunkLen) {
	/**
	 * This method gets the keyID.
	 *
	 * @return
	 */
	/**
	 * This method appends a String.
	 *
	 * @param toAdd String to add.
	 */
	var keyID: String
	/**
	 * This method gets the license URL.
	 *
	 * @return
	 */
	/**
	 * This method appends a String.
	 *
	 * @param toAdd String to add.
	 */
	var licenseURL: String
	/**
	 * This method gets the secret data.
	 *
	 * @return
	 */
	/**
	 * This method appends a String.
	 *
	 * @param toAdd String to add.
	 */
	var protectionType: String
	/**
	 * This method gets the secret data.
	 *
	 * @return
	 */
	/**
	 * This method adds the secret data.
	 *
	 * @param toAdd String to add.
	 */
	var secretData: String

	/**
	 * The read strings.
	 */
	private val strings: ArrayList<String>

	/**
	 * Creates an instance.
	 *
	 * @param chunkLen Length of current chunk.
	 */
	init {
		strings = ArrayList()
		secretData = ""
		protectionType = ""
		keyID = ""
		licenseURL = ""
	}

	/**
	 * This method appends a String.
	 *
	 * @param toAdd String to add.
	 */
	fun addString(toAdd: String) {
		strings.add(toAdd)
	}

	/**
	 * This method returns a collection of all [String]s which were addid
	 * due [.addString].
	 *
	 * @return Inserted Strings.
	 */
	fun getStrings(): Collection<String> {
		return ArrayList(strings)
	}

	/**
	 * {@inheritDoc}
	 */
	override fun prettyPrint(prefix: String): String {
		val result = StringBuilder(super.prettyPrint(prefix))
		result.insert(0, Utils.LINE_SEPARATOR + prefix + " Encryption:" + Utils.LINE_SEPARATOR)
		result.append(prefix).append("	|->keyID ").append(keyID).append(Utils.LINE_SEPARATOR)
		result.append(prefix).append("	|->secretData ").append(secretData)
			.append(Utils.LINE_SEPARATOR)
		result.append(prefix).append("	|->protectionType ").append(protectionType)
			.append(Utils.LINE_SEPARATOR)
		result.append(prefix).append("	|->licenseURL ").append(licenseURL)
			.append(Utils.LINE_SEPARATOR)
		strings.iterator()
		for (string in strings) {
			result.append(prefix).append("   |->").append(string).append(Utils.LINE_SEPARATOR)
		}
		return result.toString()
	}
}
