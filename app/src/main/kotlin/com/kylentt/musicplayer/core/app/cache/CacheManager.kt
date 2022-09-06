package com.kylentt.musicplayer.core.app.cache

import android.app.Application
import android.graphics.Bitmap
import com.kylentt.musicplayer.BuildConfig
import com.kylentt.musicplayer.common.lazy.LazyConstructor
import java.io.File
import java.io.FileOutputStream
import kotlin.reflect.KClass

/**
 * Singleton class that manage Application caches,
 */
class CacheManager private constructor(private val application: Application) {

	/**
	 *	Cache Directory that is cleaned on every App Startup (when this singleton is instantiated).
	 *	Allow us to ensure that certain temporary files will always be cleaned up
	 *	TODO: Maybe schedule WorkManager
	 */
	private val mStartupCacheDir = File(internalCacheDir, startupDir)

	/**
	 *
	 */
	private val mImageCacheDir: File = kotlin.run {
		val cacheDir = if (BuildConfig.DEBUG) externalCacheDir else internalCacheDir
		File(cacheDir, imageCacheDir)
	}

	private val mBitmapCacheDir
		get() = File(mImageCacheDir, bitmapCacheDir)

	val startupCacheDir: File
		get() {
			if (!mStartupCacheDir.exists()) mStartupCacheDir.mkdir()
			return mStartupCacheDir
		}

	init {
		if (mStartupCacheDir.exists()) cleanStartupCacheDir()
	}

	val internalCacheDir: File
		get() = application.cacheDir

	val externalCacheDir: File?
		get() = application.externalCacheDir


	private fun cleanStartupCacheDir() {
		startupCacheDir.listFiles()?.forEach(File::deleteRecursively)
	}

	fun registerImageToCache(bitmap: Bitmap, fileName: String, dirName: String): File {
		val bitmapCacheDir = mBitmapCacheDir.also {
			if (!it.exists()) it.mkdirs()
		}

		val reqDir = File(bitmapCacheDir, dirName).also {
			if (!it.exists()) it.mkdir()
		}


		val reqFile = File(reqDir, fileName).also {
			if (it.exists()) it.delete()

			it.createNewFile()
		}

		FileOutputStream(reqFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
		return reqFile
	}

	fun retrieveImageCacheFile(fileName: String, dirName: String): File? {
		val bitmapCacheDir = mBitmapCacheDir.also {
			if (!it.exists()) return null
		}

		val reqDir = File(bitmapCacheDir, dirName).also {
			if (!it.exists()) return null
		}

		return File(reqDir, fileName).run { if (exists()) this else null }
	}

	fun retrieveAllImageCacheFile(imageType: KClass<out Any>, dirName: String): List<File> {
		val list = mutableListOf<File>()

		val dir = when(imageType) {
			Bitmap::class -> mBitmapCacheDir
			else -> {
				if (BuildConfig.DEBUG) throw UnsupportedOperationException() else return list
			}
		}

		File(dir, dirName).run {
			if (exists()) listFiles()?.forEach { if (it != null) list.add(it) }
		}

		return list
	}

	companion object {
		private val instance = LazyConstructor<CacheManager>()

		fun get(application: Application) = instance.construct { CacheManager(application) }

		private const val startupDir = "startupDir"
		private const val imageCacheDir = "imageDir"
		private const val bitmapCacheDir = "bitmap"
	}

	sealed class ManagedDir(val dirName: String, internal: Boolean) {
		object BitmapCacheDir : ManagedDir(bitmapCacheDir, true)
	}
}
