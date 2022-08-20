package com.kylentt.musicplayer.domain.musiclib.source.mediastore.api1

internal abstract class BaseColumns {
	/**
	 * The unique ID for a row.
	 *
	 * "_id"
	 * Type: Long
	 */
	val _ID: String = android.provider.BaseColumns._ID

	/**
	 * The count of rows in a directory.
	 *
	 * "_count"
	 * Type: Int
	 */
	val _Count: String = android.provider.BaseColumns._COUNT

	companion object : BaseColumns() {
		// convenient reference
	}
}
