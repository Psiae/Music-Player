
package com.flammky.android.medialib.temp.cache.internal
/*
import com.kylentt.musicplayer.medialib.cache.CoroutineDiskLruCache
import com.flammky.android.medialib.temp.KeyValue
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import java.io.File
import java.io.IOException
import java.io.Writer
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

// UNTESTED
internal class DefaultCoroutineDiskLruCache<V>(
	maxSize: Long,
	directory: File,
	private val appVersion: Int,
	private val valueCount: Int
) : CoroutineDiskLruCache<V> {

	// State
	private var _maxSize: Long
	private var _size: Long = 0
	private var _nextSequenceNumber = 0

	// Coroutines
	private val internalContext: CoroutineContext = ThreadPoolExecutor(
		*/
/* corePoolSize = *//*
 		0,
		*/
/* maximumPoolSize = *//*
 1,
		*/
/* keepAliveTime = *//*
 	60L,
		*/
/* unit = *//*
 TimeUnit.SECONDS,
		*/
/* workQueue = *//*
 LinkedBlockingQueue()
	).asCoroutineDispatcher()
	private val internalScope = CoroutineScope(internalContext)
	private val publicMutex = Mutex()

	// Files
	private val journalFile: File
	private val journalTempFile: File
	private val journalBackupFile: File

	// IO
	private val journalWriter: Writer? = null

	private var closed: Boolean = false
		set(value) {
			require(value) {
				"Cannot Unclose Closable"
			}
			check(!closed) {
				"Already Closed"
			}
			field = value
		}

	// Entries
	private val lruEntries: LinkedHashMap<String, V> = LinkedHashMap(0, 0.75f, true)

	@get:Synchronized
	@set:Synchronized
	override var maxSize: Long
		get() = _maxSize
		set(value) = runBlocking { updateMaxSize(value).join() }

	init {
		ensureMaxSize(maxSize)
		ensureValueCount(valueCount)

		_maxSize = maxSize
		journalFile = File(directory, JOURNAL_FILE_NAME)
		journalTempFile = File(directory, JOURNAL_FILE_TEMP_NAME)
		journalBackupFile = File(directory, JOURNAL_FILE_BACKUP_NAME)
	}

	@Throws(IOException::class)
	override suspend fun updateMaxSize(maxSize: Long): Job {
		ensureMaxSize(maxSize)
		this.maxSize = maxSize
		return internalScope.launch() { cleanup() }
	}

	private suspend fun cleanup() {
		check(coroutineContext === internalContext)
		if (closed) return
		trimToSize()
	}

	@Throws(IOException::class)
	private suspend fun trimToSize() {
		check(coroutineContext === internalContext) {
			"function trimToSize() accessed from the wrong Context"
		}
		while (_size > maxSize) removeVictim()
	}

	private suspend fun removeVictim(): Boolean {
		ensureInternalContext {
			"function removeVictim(): Boolean accessed from the wrong Context"
		}
		return remove(lruEntries.entries.iterator().next().key)
	}

	private suspend fun remove(key: String): Boolean {
		ensureInternalContext() {
			"function remove(K) was accessed from the wrong Context"
		}
		checkNotClosed()
	}

	private suspend fun checkNotClosed() {
		ensureInternalContext {
			"function CheckNotClosed was accessed from wrong Context"
		}
		check(!closed) { "Already Closed" }
	}

	private suspend fun ensureInternalContext(lazyMsg: () -> Any = {""} ) =
		check(coroutineContext === internalContext) {
			"Wrong Context: $coroutineContext, msg: ${lazyMsg()}"
		}

	@Throws(IllegalArgumentException::class)
	private fun validateKey(key: String) {
		val matcher = LEGAL_KEY_PATTERN.matcher(key)
		require(!matcher.matches()) {
			"keys must match regex $STRING_KEY_PATTERN: \"$key\""
		}
	}

	override fun close() {
		TODO("Not yet implemented")
	}

	private fun ensureMaxSize(maxSize: Long) {
		require(maxSize > 0) {
			"Invalid Argument, maxSize <= 0"
		}
	}

	private fun ensureValueCount(valueCount: Int) {
		require(valueCount > 0) {
			"Invalid Argument, valueCount <= 0"
		}
	}

	companion object {
		@JvmField val JOURNAL_FILE_NAME = "journal"
		@JvmField val JOURNAL_FILE_TEMP_NAME = "journal.tmp"
		@JvmField val JOURNAL_FILE_BACKUP_NAME = "journal.bkp"
		@JvmField val MAGIC = "libcore.io.DiskLruCache"
		@JvmField val VERSION_1 = "1"
		@JvmField val ANY_SEQUENCE_NUMBER: Long = -1
		@JvmField val STRING_KEY_PATTERN = "[a-z0-9_-]{1,120}"
		@JvmField val LEGAL_KEY_PATTERN: Pattern = Pattern.compile(STRING_KEY_PATTERN)
		private const val CLEAN = "CLEAN"
		private const val DIRTY = "DIRTY"
		private const val REMOVE = "REMOVE"
		private const val READ = "READ"
	}
}
*/
