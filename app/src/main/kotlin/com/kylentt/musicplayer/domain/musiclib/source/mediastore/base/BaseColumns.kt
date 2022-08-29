package com.kylentt.musicplayer.domain.musiclib.source.mediastore.base

/**
 * [GoogleSource](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-7.0.0_r36/core/java/android/provider/BaseColumns.java)
 */
internal abstract class BaseColumns {
	/**
	 * The unique ID for a row.
	 *
	 * "_id"
	 * type: Long
	 */
	val _ID: String = android.provider.BaseColumns._ID

	/**
	 * The count of rows in a directory.
	 *
	 * "_count"
	 * type: Int
	 */
	val _Count: String = android.provider.BaseColumns._COUNT

	companion object : BaseColumns() {
		// convenient reference
	}
}
