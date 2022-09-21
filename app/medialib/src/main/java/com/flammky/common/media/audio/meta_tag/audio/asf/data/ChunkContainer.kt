package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.ChunkPositionComparator
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import java.math.BigInteger
import java.util.*

/**
 * Stores multiple ASF objects (chunks) in form of [Chunk] objects, and is
 * itself an ASF object (chunk).<br></br>
 * <br></br>
 * Because current implementation is solely used for ASF metadata, all chunks
 * (except for [StreamChunk]) may only be [ inserted][.addChunk] once.
 *
 * @author Christian Laireiter
 */
open class ChunkContainer(chunkGUID: GUID?, pos: Long, length: BigInteger?) :
	Chunk(chunkGUID, pos, length) {
	/**
	 * Stores the [Chunk] objects to their [GUID].
	 */
	private val chunkTable: MutableMap<GUID?, MutableList<Chunk>>

	/**
	 * Creates an instance.
	 *
	 * @param chunkGUID the GUID which identifies the chunk.
	 * @param pos       the position of the chunk within the stream.
	 * @param length    the length of the chunk.
	 */
	init {
		chunkTable = Hashtable()
	}

	/**
	 * Adds a chunk to the container.<br></br>
	 *
	 * @param toAdd The chunk which is to be added.
	 * @throws IllegalArgumentException If a chunk of same type is already added, except for
	 * [StreamChunk].
	 */
	fun addChunk(toAdd: Chunk) {
		val list = assertChunkList(toAdd.guid)
		require(
			!(!list.isEmpty() && !MULTI_CHUNKS!!.contains(
				toAdd.guid
			))
		) {
			"The GUID of the given chunk indicates, that there is no more instance allowed." //$NON-NLS-1$
		}
		list.add(toAdd)
		assert(chunkstartsUnique(this)) {
			"Chunk has equal start position like an already inserted one." //$NON-NLS-1$
		}
	}

	/**
	 * This method asserts that a [List] exists for the given [GUID]
	 * , in [.chunkTable].<br></br>
	 *
	 * @param lookFor The GUID to get list for.
	 * @return an already existing, or newly created list.
	 */
	protected fun assertChunkList(lookFor: GUID?): MutableList<Chunk> {
		var result = chunkTable[lookFor]
		if (result == null) {
			result = ArrayList()
			chunkTable[lookFor] = result
		}
		return result
	}

	/**
	 * Returns a collection of all contained chunks.<br></br>
	 *
	 * @return all contained chunks
	 */
	val chunks: Collection<Chunk>
		get() {
			val result: MutableList<Chunk> = ArrayList()
			for (curr in chunkTable.values) {
				result.addAll(curr)
			}
			return result
		}

	/**
	 * Looks for the first stored chunk which has the given GUID.
	 *
	 * @param lookFor    GUID to look up.
	 * @param instanceOf The class which must additionally be matched.
	 * @return `null` if no chunk was found, or the stored instance
	 * doesn't match.
	 */
	fun getFirst(lookFor: GUID, instanceOf: Class<out Chunk>): Chunk? {
		var result: Chunk? = null
		val list: List<Chunk>? = chunkTable[lookFor]
		if (list != null && list.isNotEmpty()) {
			val chunk = list[0]
			if (instanceOf.isAssignableFrom(chunk.javaClass)) {
				result = chunk
			}
		}
		return result
	}

	/**
	 * This method checks if a chunk has been [ added][.addChunk] with specified [GUID][Chunk.getGuid].<br></br>
	 *
	 * @param lookFor GUID to look up.
	 * @return `true` if chunk with specified GUID has been added.
	 */
	fun hasChunkByGUID(lookFor: GUID?): Boolean {
		return chunkTable.containsKey(lookFor)
	}

	/**
	 * {@inheritDoc}
	 */
	override fun prettyPrint(prefix: String): String {
		return prettyPrint(prefix, "")
	}

	/**
	 * Nearly the same as [.prettyPrint] however, additional
	 * information can be injected below the [Chunk.prettyPrint]
	 * output and the listing of the contained chunks.<br></br>
	 *
	 * @param prefix        The prefix to prepend.
	 * @param containerInfo Information to inject.
	 * @return Information of current Chunk Object.
	 */
	fun prettyPrint(prefix: String, containerInfo: String?): String {
		val result = StringBuilder(super.prettyPrint(prefix))
		result.append(containerInfo)
		result.append(prefix).append("  |").append(Utils.LINE_SEPARATOR)
		val list = ArrayList(
			chunks
		)
		Collections.sort(list, ChunkPositionComparator())
		for (curr in list) {
			result.append(curr.prettyPrint("$prefix  |"))
			result.append(prefix).append("  |").append(Utils.LINE_SEPARATOR)
		}
		return result.toString()
	}

	companion object {
		/**
		 * Stores the [GUID] instances, which are allowed multiple times
		 * within an ASF header.
		 */
		private var MULTI_CHUNKS: MutableSet<GUID?>? = null

		init {
			MULTI_CHUNKS = HashSet()
			MULTI_CHUNKS!!.add(GUID.GUID_STREAM)
		}

		/**
		 * Tests whether all stored chunks have a unique starting position among
		 * their brothers.
		 *
		 * @param container the container to test.
		 * @return `true` if all chunks are located at an unique
		 * position. However, no intersection is tested.
		 */
		protected fun chunkstartsUnique(container: ChunkContainer): Boolean {
			var result = true
			val chunkStarts: MutableSet<Long> = HashSet()
			val chunks = container.chunks
			for (curr in chunks) {
				result = result and chunkStarts.add(curr.position)
			}
			return result
		}
	}
}
