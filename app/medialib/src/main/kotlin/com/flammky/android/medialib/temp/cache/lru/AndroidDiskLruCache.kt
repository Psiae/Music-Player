
package com.flammky.android.medialib.temp.cache.lru

/*
import androidx.annotation.IntRange
import com.flammky.common.io.closeable.CloseableUtil.applyClose
import com.flammky.common.io.file.FileUtil.replaceIfOtherExists
import java.io.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class AndroidDiskLruCache(
	@IntRange(from = 1) maximumSize: Long = 0,
	directory: File,
	private val valueCount: Int,
) : DiskLruCache<AndroidDiskLruCache.Entry> {

	@Volatile
	private var _maxSize: Long = maximumSize

	@Volatile
	private var _size: Long = 0L


	private var nextSequenceNumber = 0

	private val executor = DefaultExecutor()

	private val lruEntries = LinkedHashMap<String, Entry>(0, 0.75f, true)

	// Files
	private val journalFile: File
	private val journalFileTemp: File
	private val journalFileBackup: File
	private var journalWriter: Writer? = null
	private val redundantOpCount = 0

	// IO
	private var closed: Boolean = false
		set(value) {
			require(value) {
				"Cannot Unclose Closable"
			}
			field = value
		}

	override val maxSize: Long
		get() = _maxSize

	override val size: Long
		get() = _size

	init {
		checkConstructorArgument(maximumSize, valueCount)
		journalFile = File(directory, JOURNAL_FILE_NAME)
		journalFileTemp = File(directory, JOURNAL_FILE_TEMP_NAME)
		journalFileBackup = File(directory, JOURNAL_FILE_BACKUP_NAME)
	}

	private fun checkConstructorArgument(maximumSize: Long, valueCount: Int) {
		require(maximumSize > 0) { "Invalid Argument, maximumSize <= 0" }
		require(valueCount > 0) { "Invalid Argument, maximumSize <= 0" }
	}

	private fun cleanup() {
		if (closed) return
		trimToMaximumSize()
	}

	private fun trimToMaximumSize() {
		while (_size > _maxSize) check(evictVictim()) { "Failed to evict Victim when trimming" }
	}

	private fun evictVictim(): Boolean {
		val victim = lruEntries.entries.iterator().next()
		return remove(victim.key) != null
	}

	@Throws(IOException::class)
	private fun rebuildJournalIfRequired() {
		if (!journalRebuildRequired()) return

		journalWriter?.close()

		val writer = BufferedWriter(
			OutputStreamWriter(FileOutputStream(journalFileTemp), Charsets.US_ASCII)
		)

		writer.applyClose {
			write(MAGIC);
			write("\n");
			write("1");
			write("\n");
			write("1");
			write("\n");
			write("$valueCount");
			write("\n");
			write("\n");
			lruEntries.values.forEach { entry: Entry ->
				if (entry.currentEditor != null) {
					write(DIRTY + ' ' + entry.key + '\n')
				} else {
					write(CLEAN + ' ' + entry.key + entry.length + '\n');
				}
			}
		}

		journalFileBackup.replaceIfOtherExists(journalFile)
		journalFileTemp.renameTo(journalFile)

		journalWriter = BufferedWriter(
			OutputStreamWriter(FileOutputStream(journalFile, true), Charsets.US_ASCII)
		)
		journalFileBackup.delete()
	}

	private fun journalRebuildRequired(): Boolean {
		val redundantOpCompactThreshold = 2000
		return redundantOpCount >= redundantOpCompactThreshold && redundantOpCount >= lruEntries.size
	}


	override fun resize(size: Long) {
		TODO("Not yet implemented")
	}

	class Editor : DiskLruCache.Editor {

	}

	class Entry : DiskLruCache.Entry {
		override var currentEditor: DiskLruCache.Editor? = null
		override val key: String
			get() = TODO("Not yet implemented")
	}

	class SnapShot(override val inputStream: InputStream) : DiskLruCache.SnapShot {

		override fun close() {
			TODO("Not yet implemented")
		}
	}

	companion object {
		@JvmField val JOURNAL_FILE_NAME = "journal"
		@JvmField val JOURNAL_FILE_TEMP_NAME = "journal.tmp"
		@JvmField val JOURNAL_FILE_BACKUP_NAME = "journal.bkp"
		@JvmField val STRING_KEY_PATTERN = "[a-z0-9_-]{1,120}"
		@JvmField val LEGAL_KEY_PATTERN: Pattern = Pattern.compile(STRING_KEY_PATTERN)

		@JvmField val MAGIC = "mediaLib.cache.lru.AndroidDiskLruCache"
		@JvmField val VERSION = "1"

		private const val CLEAN = "CLEAN"
		private const val DIRTY = "DIRTY"
		private const val REMOVE = "REMOVE"
		private const val READ = "READ"

		private fun DefaultExecutor()	= ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, LinkedBlockingQueue())

		private fun File.renameTo(other: File, deleteDestination: Boolean = true) {

		}
	}
}
*/
