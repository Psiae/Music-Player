package com.kylentt.musicplayer.ui.main

import androidx.lifecycle.Lifecycle
import timber.log.Timber
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class DelegatedState : ReadOnlyProperty<Any?, DelegatedState.State> {

	sealed class State {
		object Nothing : State()
		object Initialized : State()
		object Created : State()
		object Started : State()
		object Resumed : State()
		object Destroyed : State()

		fun wasLaunched() = this != Nothing
		fun isAlive() = wasLaunched() && !isDestroyed()
		fun isCreated() = this == Created || isVisible()
		fun isVisible() = this == Started || isReady()
		fun isReady() = this == Resumed
		fun isDestroyed() = this == Destroyed
	}


	private var mState: State = State.Nothing
	private var hashCode: Int? = null

	fun updateState(activity: MainActivity, state: State) {

		when(state) {
			State.Nothing -> throw IllegalArgumentException()
			State.Initialized -> hashCode = activity.hashCode()
			State.Destroyed -> if (!hashEqual(activity)) return
			else -> Unit
		}

		require(hashEqual(activity))
		mState = state

		Timber.d("MainActivity StateDelegate, updated to $mState")
	}

	private fun hashEqual(activity: MainActivity) = hashCode == activity.hashCode()

	fun getValue(): State = mState
	override fun getValue(thisRef: Any?, property: KProperty<*>): State = getValue()
}
