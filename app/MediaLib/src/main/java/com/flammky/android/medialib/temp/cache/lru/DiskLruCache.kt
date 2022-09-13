package com.flammky.android.medialib.temp.cache.lru

import java.io.Closeable
import java.io.InputStream

interface DiskLruCache<E: DiskLruCache.Entry> : LruCache<String, E>, Closeable {




	interface Editor {

	}

	interface Entry {
		val currentEditor: Editor?
		val key: String
		val length: Long
	}

	interface SnapShot : Closeable {
		val inputStream: InputStream
	}
}
