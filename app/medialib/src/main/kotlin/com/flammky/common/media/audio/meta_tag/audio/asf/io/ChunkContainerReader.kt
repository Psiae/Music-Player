package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.Chunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.ChunkContainer
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.GUID
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import java.io.IOException
import java.io.InputStream
import java.math.BigInteger
import java.util.*
import java.util.logging.Logger

/**
 * This class represents a reader implementation, which is able to read ASF
 * objects (chunks) which store other objects (chunks) within them.<br></br>
 *
 * @param <ChunkType> The [ChunkContainer] instance, the implementation will
 * create.
 * @author Christian Laireiter
</ChunkType> */
abstract class ChunkContainerReader<ChunkType : ChunkContainer> protected constructor(
	toRegister: List<Class<out ChunkReader>>,
	/**
	 * If `true` each chunk type will only be read once.<br></br>
	 */
	protected val eachChunkOnce: Boolean
) : ChunkReader {
	/**
	 * If `true` due to a [registered][.register]
	 * chunk reader, all [InputStream] objects passed to
	 * [.read] must support mark/reset.
	 */
	protected var hasFailingReaders = false

	/**
	 * Registers GUIDs to their reader classes.<br></br>
	 */
	protected val readerMap: MutableMap<GUID?, ChunkReader> = HashMap()

	/**
	 * Creates a reader instance, which only utilizes the given list of chunk
	 * readers.<br></br>
	 *
	 * @param toRegister    List of [ChunkReader] class instances, which are to be
	 * utilized by the instance.
	 * @param readChunkOnce if `true`, each chunk type (identified by chunk
	 * GUID) will handled only once, if a reader is available, other
	 * chunks will be discarded.
	 */
	init {
		for (curr in toRegister) {
			register(curr)
		}
	}

	/**
	 * Checks for the constraints of this class.
	 *
	 * @param stream stream to test.
	 * @throws IllegalArgumentException If stream does not meet the requirements.
	 */
	@Throws(IllegalArgumentException::class)
	protected fun checkStream(stream: InputStream) {
		require(!(hasFailingReaders && !stream.markSupported())) { "Stream has to support mark/reset." }
	}

	/**
	 * This method is called by [.read] in order
	 * to create the resulting object. Implementations of this class should now
	 * return a new instance of their implementation specific result **AND**
	 * all data should be read, until the list of chunks starts. (The
	 * [ChunkContainer.getChunkEnd] must return a sane result, too)<br></br>
	 *
	 * @param streamPosition position of the stream, the chunk starts.
	 * @param chunkLength    the length of the chunk (from chunk header)
	 * @param stream         to read the implementation specific information.
	 * @return instance of the implementations result.
	 * @throws IOException On I/O Errors and Invalid data.
	 */
	@Throws(IOException::class)
	protected abstract fun createContainer(
		streamPosition: Long,
		chunkLength: BigInteger,
		stream: InputStream
	): ChunkType

	/**
	 * Gets a configured [reader][ChunkReader] instance for ASF
	 * objects (chunks) with the specified `guid`.
	 *
	 * @param guid GUID which identifies the chunk to be read.
	 * @return an appropriate reader implementation, `null` if not
	 * [registered][.register].
	 */
	protected fun getReader(guid: GUID?): ChunkReader? {
		return readerMap[guid]
	}

	/**
	 * Tests whether [.getReader] won't return `null`.<br></br>
	 *
	 * @param guid GUID which identifies the chunk to be read.
	 * @return `true` if a reader is available.
	 */
	protected fun isReaderAvailable(guid: GUID?): Boolean {
		return readerMap.containsKey(guid)
	}

	/**
	 * This Method implements the reading of a chunk container.<br></br>
	 *
	 * @param guid       GUID of the currently read container.
	 * @param stream     Stream which contains the chunk container.
	 * @param chunkStart The start of the chunk container from stream start.<br></br>
	 * For direct file streams one can assume `0` here.
	 * @return `null` if no valid data found, else a Wrapper
	 * containing all supported data.
	 * @throws IOException              Read errors.
	 * @throws IllegalArgumentException If one used [ChunkReader] could
	 * [fail][ChunkReader.canFail] and the stream source
	 * doesn't support mark/reset.
	 */
	@Throws(IOException::class, IllegalArgumentException::class)
	override fun read(guid: GUID?, stream: InputStream, chunkStart: Long): ChunkType? {
		checkStream(stream)
		val cis = CountingInputStream(stream)
		require(
			Arrays.asList(
				*applyingIds
			).contains(guid)
		) { "provided GUID is not supported by this reader." }
		// For Know the file pointer pointed to an ASF header chunk.
		val chunkLen = Utils.readBig64(cis)
		/*
		 * now read implementation specific information until the chunk
		 * collection starts and create the resulting object.
		 */
		val result = createContainer(chunkStart, chunkLen, cis)
		// 16 bytes have already been for providing the GUID
		var currentPosition = chunkStart + cis.readCount + 16
		val alreadyRead = HashSet<GUID>()
		/*
		 * Now reading header of chuncks.
		 */while (currentPosition < result.chunkEnd) {
			val currentGUID = Utils.readGUID(cis)
			val skip =
				eachChunkOnce && (!isReaderAvailable(currentGUID) || !alreadyRead.add(currentGUID))
			var chunk: Chunk?
			/*
			 * If one reader tells it could fail (new method), then check the
			 * input stream for mark/reset. And use it if failed.
			 */chunk = if (!skip && isReaderAvailable(currentGUID)) {
				val reader = getReader(currentGUID)
				if (reader!!.canFail()) {
					cis.mark(READ_LIMIT)
				}
				getReader(currentGUID)!!.read(currentGUID, cis, currentPosition)
			} else {
				ChunkHeaderReader.instance
					.read(currentGUID, cis, currentPosition)
			}
			if (chunk == null) {
				/*
				 * Reader failed
				 */
				cis.reset()
			} else {
				if (!skip) {
					result.addChunk(chunk)
				}
				currentPosition = chunk.chunkEnd
				assert(cis.readCount + chunkStart + 16 == currentPosition)
			}
		}
		return result
	}

	/**
	 * Registers the given reader.<br></br>
	 *
	 * @param <T>        The actual reader implementation.
	 * @param toRegister chunk reader which is to be registered.
	</T> */
	private fun <T : ChunkReader> register(toRegister: Class<T>) {
		try {
			val reader: T = toRegister.newInstance()
			for (curr in reader.applyingIds) {
				readerMap[curr] = reader
			}
		} catch (e: InstantiationException) {
			LOGGER.severe(e.message)
		} catch (e: IllegalAccessException) {
			LOGGER.severe(e.message)
		}
	}

	companion object {
		/**
		 * Logger
		 */
		protected val LOGGER = Logger.getLogger("org.jaudiotabgger.audio") //$NON-NLS-1$

		/**
		 * Within this range, a [ChunkReader] should be aware if it fails.
		 */
		const val READ_LIMIT = 8192
	}
}
