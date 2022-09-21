package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.GUID
import java.util.*

/**
 * Structure to tell the differences occurred by altering a chunk.
 *
 * @author Christian Laireiter
 */
class ModificationResult {
	/**
	 * Returns the difference of bytes.
	 *
	 * @return the byte difference
	 */
	/**
	 * Stores the difference of bytes.<br></br>
	 */
	val byteDifference: Long
	/**
	 * Returns the difference of the amount of chunks.
	 *
	 * @return the chunk count difference
	 */
	/**
	 * Stores the difference of the amount of chunks.<br></br>
	 * &quot;-1&quot; if the chunk disappeared upon modification.<br></br>
	 * &quot;0&quot; if the chunk was just modified.<br></br>
	 * &quot;1&quot; if a chunk has been created.<br></br>
	 */
	val chunkCountDifference: Int

	/**
	 * Stores all GUIDs, which have been read.<br></br>
	 */
	private val occuredGUIDs: MutableSet<GUID?> = HashSet()

	/**
	 * Creates an instance.<br></br>
	 *
	 * @param chunkCountDiff amount of chunks appeared, disappeared
	 * @param bytesDiffer    amount of bytes added or removed.
	 * @param occurred       all GUIDs which have been occurred, during processing
	 */
	constructor(chunkCountDiff: Int, bytesDiffer: Long, vararg occurred: GUID?) {
		assert(occurred != null && occurred.size > 0)
		chunkCountDifference = chunkCountDiff
		byteDifference = bytesDiffer
		occuredGUIDs.addAll(Arrays.asList(*occurred))
	}

	/**
	 * Creates an instance.<br></br>
	 *
	 * @param chunkCountDiff amount of chunks appeared, disappeared
	 * @param bytesDiffer    amount of bytes added or removed.
	 * @param occurred       all GUIDs which have been occurred, during processing
	 */
	constructor(chunkCountDiff: Int, bytesDiffer: Long, occurred: Set<GUID?>?) {
		chunkCountDifference = chunkCountDiff
		byteDifference = bytesDiffer
		occuredGUIDs.addAll(occurred!!)
	}

	/**
	 * Returns all GUIDs which have been occurred during processing.
	 *
	 * @return see description.s
	 */
	fun getOccuredGUIDs(): Set<GUID?> {
		return HashSet(occuredGUIDs)
	}
}
