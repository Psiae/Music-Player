package com.flammky.android.medialib.common

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// Contract or Constants ?
object Contract {
	const val INDEX_UNSET: Int = -1

	val DURATION_UNSET: Duration = (-1).seconds
	val DURATION_INDEFINITE: Duration = Duration.INFINITE
}
