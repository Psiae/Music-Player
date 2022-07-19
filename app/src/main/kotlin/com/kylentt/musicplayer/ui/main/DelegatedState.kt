package com.kylentt.musicplayer.ui.main

import androidx.lifecycle.Lifecycle
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

	fun updateState(activity: MainActivity) {
		when(activity.lifecycle.currentState) {
			Lifecycle.State.INITIALIZED -> hashCode = activity.hashCode()
			Lifecycle.State.DESTROYED -> if (hashEqual(activity)) return
			else -> {
				require(hashEqual(activity))
				mState = getState(activity.lifecycle)
			}
		}
	}

	private fun hashEqual(activity: MainActivity) = hashCode == activity.hashCode()
	private fun getState(lifecycle: Lifecycle): State = when(lifecycle.currentState) {
		Lifecycle.State.INITIALIZED -> State.Initialized
		Lifecycle.State.CREATED -> State.Created
		Lifecycle.State.STARTED -> State.Started
		Lifecycle.State.RESUMED -> State.Resumed
		Lifecycle.State.DESTROYED -> State.Destroyed
	}

	fun getValue(): State = mState
	override fun getValue(thisRef: Any?, property: KProperty<*>): State = getValue()
}
