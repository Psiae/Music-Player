package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data

import java.math.BigInteger

/**
 * A factory for creating appropriate [MetadataContainer] objects upon
 * specified [container types][ContainerType].<br></br>
 *
 * @author Christian Laireiter
 */
class MetadataContainerFactory
/**
 * Hidden utility class constructor.
 */
private constructor() {
	/**
	 * Convenience Method for I/O. Same as
	 * [.createContainer], but additionally assigns
	 * position and size. (since a [MetadataContainer] is actually a
	 * [Chunk]).
	 *
	 * @param type      The containers type.
	 * @param pos       the position within the stream.
	 * @param chunkSize the size of the container.
	 * @return an appropriate container implementation with assigned size and
	 * position.
	 */
	/**
	 * Creates an appropriate [container][MetadataContainer] for the given container type.
	 *
	 * @param type the type of container to get a container instance for.
	 * @return appropriate container implementation.
	 */
	@JvmOverloads
	fun createContainer(
		type: ContainerType,
		pos: Long = 0,
		chunkSize: BigInteger = BigInteger.ZERO
	): MetadataContainer {
		val result: MetadataContainer
		result =
			if (type === ContainerType.CONTENT_DESCRIPTION) {
				ContentDescription(
					pos,
					chunkSize
				)
			} else if (type === ContainerType.CONTENT_BRANDING) {
				ContentBranding(
					pos,
					chunkSize
				)
			} else {
				MetadataContainer(
					type,
					pos,
					chunkSize
				)
			}
		return result
	}

	/**
	 * Convenience method which calls [.createContainer]
	 * for each given container type.
	 *
	 * @param types types of the container which are to be created.
	 * @return appropriate container implementations.
	 */
	fun createContainers(types: Array<ContainerType>?): Array<MetadataContainer?> {
		assert(types != null)
		val result = arrayOfNulls<MetadataContainer>(
			types!!.size
		)
		for (i in result.indices) {
			result[i] = createContainer(types[i])
		}
		return result
	}

	companion object {
		/**
		 * Factory instance.
		 */
		private val INSTANCE = MetadataContainerFactory()

		/**
		 * Returns an instance.
		 *
		 * @return an instance.
		 */
		@JvmStatic
		fun getInstance(): MetadataContainerFactory {
			return INSTANCE
		}
	}
}
