package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io.WriteableChunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import java.io.IOException
import java.io.OutputStream
import java.math.BigInteger
import java.util.*

/**
 * This structure represents the &quot;Metadata Object&quot;,&quot;Metadata
 * Library Object&quot; and &quot;Extended Content Description&quot;.<br></br>
 *
 * @author Christian Laireiter
 */
open class MetadataContainer(
	val containerType:
	ContainerType,
	pos: Long = 0,
	size: BigInteger = BigInteger.ZERO
) : Chunk(containerType.containerGUID, pos, size), WriteableChunk {
	/**
	 * This class is used to uniquely identify an enclosed descriptor by its
	 * name, language index and stream number.<br></br>
	 * The type of the descriptor is ignored, since it just specifies the data
	 * content.
	 *
	 * @author Christian Laireiter
	 */
	private class DescriptorPointer(descriptor: MetadataDescriptor?) {
		/**
		 * The represented descriptor.
		 */
		private var desc: MetadataDescriptor? = null

		/**
		 * Creates an instance.
		 *
		 * @param descriptor the metadata descriptor to identify.
		 */
		init {
			setDescriptor(descriptor)
		}

		/**
		 * {@inheritDoc}
		 */
		override fun equals(obj: Any?): Boolean {
			var result = obj === this
			if (obj is DescriptorPointer && !result) {
				val other = obj.desc
				result = desc!!.name == other!!.name
				result = result and (desc!!.languageIndex == other.languageIndex)
				result = result and (desc!!.streamNumber == other.streamNumber)
			}
			return result
		}

		/**
		 * {@inheritDoc}
		 */
		override fun hashCode(): Int {
			var hashCode: Int = desc!!.name.hashCode()
			hashCode = hashCode * 31 + desc!!.languageIndex
			hashCode = hashCode * 31 + desc!!.languageIndex
			return hashCode
		}

		/**
		 * Sets the descriptor to identify.
		 *
		 * @param descriptor the descriptor to identify.
		 * @return this instance.
		 */
		fun setDescriptor(descriptor: MetadataDescriptor?): DescriptorPointer {
			assert(descriptor != null)
			desc = descriptor
			return this
		}
	}
	/**
	 * Returns the type of container this instance represents.<br></br>
	 *
	 * @return represented container type.
	 */

	/**
	 * Stores the descriptors.
	 */
	private val descriptors: MutableMap<DescriptorPointer, MutableList<MetadataDescriptor>> =
		Hashtable()

	/**
	 * for performance reasons this instance is used to look up existing
	 * descriptors in [.descriptors].<br></br>
	 */
	private val perfPoint = DescriptorPointer(MetadataDescriptor(""))
	/**
	 * Creates an instance.
	 *
	 * @param containerType determines the type of the container
	 * @param pos  location in the ASF file
	 * @param size size of the chunk.
	 */
	/**
	 * Creates an instance.
	 *
	 * @param containerGUID the containers GUID
	 * @param pos           location in the ASF file
	 * @param size          size of the chunk.
	 */
	constructor(containerGUID: GUID, pos: Long, size: BigInteger)
		: this(determineType(containerGUID)!!, pos, size)

	/**
	 * Adds a metadata descriptor.
	 *
	 * @param toAdd the descriptor to add.
	 * @throws IllegalArgumentException if descriptor does not meet container requirements, or
	 * already exist.
	 */
	@Throws(IllegalArgumentException::class)
	fun addDescriptor(toAdd: MetadataDescriptor) {
		// check with throwing exceptions
		containerType.assertConstraints(
			toAdd.name,
			toAdd.rawData,
			toAdd.type,
			toAdd.streamNumber,
			toAdd.languageIndex
		)
		// validate containers capabilities
		require(isAddSupported(toAdd)) { "Descriptor cannot be added, see isAddSupported(...)" }
		/*
		 * Check for containers types capabilities.
		 */
		// Search for descriptor list by name, language and stream.
		var list: MutableList<MetadataDescriptor>?
		synchronized(perfPoint) { list = descriptors[perfPoint.setDescriptor(toAdd)] }
		if (list == null) {
			list = ArrayList()
			descriptors[DescriptorPointer(toAdd)] = list!!
		} else {
			require(!(!list!!.isEmpty() && !containerType.isMultiValued)) { "Container does not allow multiple values of descriptors with same name, language index and stream number" }
		}
		list!!.add(toAdd)
	}
	/**
	 * This method asserts that this container has a descriptor with the
	 * specified key, means returns an existing or creates a new descriptor.
	 *
	 * @param key  the descriptor name to look up (or create)
	 * @param containerType if the descriptor is created, this data type is applied.
	 * @return the/a descriptor with the specified name.
	 */
	/**
	 * This method asserts that this container has a descriptor with the
	 * specified key, means returns an existing or creates a new descriptor.
	 *
	 * @param key the descriptor name to look up (or create)
	 * @return the/a descriptor with the specified name (and initial type of
	 * [MetadataDescriptor.TYPE_STRING].
	 */
	protected fun assertDescriptor(
		key: String,
		type: Int = MetadataDescriptor.Companion.TYPE_STRING
	): MetadataDescriptor {
		val desc: MetadataDescriptor
		val descriptorsByName = getDescriptorsByName(key)
		if (descriptorsByName == null || descriptorsByName.isEmpty()) {
			desc = MetadataDescriptor(
				containerType, key, type
			)
			addDescriptor(desc)
		} else {
			desc = descriptorsByName[0]
		}
		return desc
	}

	/**
	 * Checks whether a descriptor already exists.<br></br>
	 * Name, stream number and language index are compared. Data and data type
	 * are ignored.
	 *
	 * @param lookup descriptor to look up.
	 * @return `true` if such a descriptor already exists.
	 */
	fun containsDescriptor(lookup: MetadataDescriptor?): Boolean {
		assert(lookup != null)
		return descriptors.containsKey(perfPoint.setDescriptor(lookup))
	}

	override val currentAsfChunkSize: Long
		get() {
			/**
			 * 16 bytes GUID, 8 bytes chunk size, 2 bytes descriptor count
			 */
			var result: Long = 26
			for (curr in getDescriptors()) {
				result += curr.getCurrentAsfSize(containerType).toLong()
			}
			return result
		}

	/**
	 * Returns the number of contained descriptors.
	 *
	 * @return number of descriptors.
	 */
	fun getDescriptorCount(): Int {
		return getDescriptors().size
	}

	/**
	 * Returns all stored descriptors.
	 *
	 * @return stored descriptors.
	 */
	fun getDescriptors(): List<MetadataDescriptor> {
		val result: MutableList<MetadataDescriptor> = ArrayList()
		for (curr in descriptors.values) {
			result.addAll(curr)
		}
		return result
	}

	/**
	 * Returns a list of descriptors with the given
	 * [name][MetadataDescriptor.getName].<br></br>
	 *
	 * @param name name of the descriptors to return
	 * @return list of descriptors with given name.
	 */
	fun getDescriptorsByName(name: String): List<MetadataDescriptor> {
		val result: MutableList<MetadataDescriptor> = ArrayList()
		val values: Collection<List<MetadataDescriptor>> = descriptors.values
		for (currList in values) {
			if (currList.isNotEmpty() && currList[0].name == name) {
				result.addAll(currList)
			}
		}
		return result
	}

	/**
	 * This method looks up a descriptor with given name and returns its value
	 * as string.<br></br>
	 *
	 * @param name the name of the descriptor to look up.
	 * @return the string representation of a found descriptors value. Even an
	 * empty string if no descriptor has been found.
	 */
	protected fun getValueFor(name: String): String? {
		var result: String? = ""
		val descs = getDescriptorsByName(name)
		if (descs != null) {
			assert(descs.size <= 1)
			if (!descs.isEmpty()) {
				result = descs[0].getString()
			}
		}
		return result
	}

	/**
	 * Determines if this container contains a descriptor with given
	 * [name][MetadataDescriptor.getName].<br></br>
	 *
	 * @param name Name of the descriptor to look for.
	 * @return `true` if descriptor has been found.
	 */
	fun hasDescriptor(name: String): Boolean {
		return getDescriptorsByName(name).isNotEmpty()
	}

	/**
	 * Determines/checks if the given descriptor may be added to the container.<br></br>
	 * This implies a check for the capabilities of the container specified by
	 * its [container type][.getContainerType].<br></br>
	 *
	 * @param descriptor the descriptor to test.
	 * @return `true` if [.addDescriptor]
	 * can be called with given descriptor.
	 */
	open fun isAddSupported(descriptor: MetadataDescriptor): Boolean {
		var result = containerType.checkConstraints(
			descriptor.name,
			descriptor.rawData,
			descriptor.type,
			descriptor.streamNumber,
			descriptor.languageIndex
		) == null
		// Now check if there is already a value contained.
		if (result && !containerType.isMultiValued) {
			synchronized(perfPoint) {
				val list: List<MetadataDescriptor>? =
					descriptors[perfPoint.setDescriptor(descriptor)]
				if (list != null) {
					result = list.isEmpty()
				}
			}
		}
		return result
	}

	override val isEmpty: Boolean
		get() {
			var result = true
			if (getDescriptorCount() != 0) {
				val iterator = getDescriptors().iterator()
				while (result && iterator.hasNext()) {
					result = result and iterator.next().isEmpty()
				}
			}
			return result
		}

	/**
	 * {@inheritDoc}
	 */
	override fun prettyPrint(prefix: String): String {
		val result = StringBuilder(super.prettyPrint(prefix))
		for (curr in getDescriptors()) {
			result.append(prefix).append("  |-> ")
			result.append(curr)
			result.append(Utils.LINE_SEPARATOR)
		}
		return result.toString()
	}

	/**
	 * Removes all stored descriptors with the given
	 * [name][MetadataDescriptor.getName].<br></br>
	 *
	 * @param name the name to remove.
	 */
	fun removeDescriptorsByName(name: String?) {
		assert(name != null)
		val iterator: MutableIterator<List<MetadataDescriptor>> = descriptors.values.iterator()
		while (iterator.hasNext()) {
			val curr = iterator.next()
			if (!curr.isEmpty() && curr[0].name == name) {
				iterator.remove()
			}
		}
	}

	/**
	 * [asserts][.assertDescriptor] the existence of a
	 * descriptor with given `name` and
	 * [assings][MetadataDescriptor.setStringValue] the string
	 * value.
	 *
	 * @param name  the name of the descriptor to set the value for.
	 * @param value the string value.
	 */
	protected fun setStringValue(name: String, value: String?) {
		assertDescriptor(name).setStringValue(value)
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun writeInto(out: OutputStream): Long {
		val chunkSize = currentAsfChunkSize
		val descriptorList = getDescriptors()
		out.write(guid.bytes)
		Utils.writeUINT64(chunkSize, out)
		Utils.writeUINT16(descriptorList.size, out)
		for (curr in descriptorList) {
			curr.writeInto(out, containerType)
		}
		return chunkSize
	}

	companion object {
		/**
		 * Looks up all [guids][ContainerType.getContainerGUID] and
		 * returns the matching type.
		 *
		 * @param guid GUID to look up
		 * @return matching container type.
		 * @throws IllegalArgumentException if no container type matches
		 */
		@Throws(IllegalArgumentException::class)
		private fun determineType(guid: GUID): ContainerType? {
			var result: ContainerType? = null
			for (curr in ContainerType.values()) {
				if (curr.containerGUID == guid) {
					result = curr
					break
				}
			}
			return result
		}
	}
}
