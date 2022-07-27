package com.kylentt.musicplayer.ui.main

import timber.log.Timber
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

object MainActivityDelegate {
	private var mState: MainActivity.State = MainActivity.State.Nothing
	private var hashCode: Int? = null

	val state
		get() = mState

	val stateDelegate = ReadOnlyProperty<Any?, MainActivity.State> { _, _ -> state }

	fun updateState(activity: MainActivity, state: MainActivity.State) {

		when (state) {
			MainActivity.State.Nothing -> throw IllegalArgumentException()
			MainActivity.State.Initialized -> hashCode = activity.hashCode()
			MainActivity.State.Destroyed -> if (!hashEqual(activity)) return
			else -> Unit
		}

		require(hashEqual(activity))
		mState = state

		Timber.d("MainActivity StateDelegate, updated to $mState")
	}

	private fun hashEqual(activity: MainActivity) = hashCode == activity.hashCode()

	fun MainActivity.State.wasLaunched() = this != MainActivity.State.Nothing
	fun MainActivity.State.isAlive() = wasLaunched() && !isDestroyed()
	fun MainActivity.State.isCreated() = this == MainActivity.State.Created || isVisible()
	fun MainActivity.State.isVisible() = this == MainActivity.State.Started || isReady()
	fun MainActivity.State.isReady() = this == MainActivity.State.Resumed
	fun MainActivity.State.isDestroyed() = this == MainActivity.State.Destroyed
}
