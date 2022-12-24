package com.flammky.musicplayer.main.ui

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.flammky.musicplayer.main.ext.IntentHandler
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

interface MainPresenter {

	val intentHandler: IntentHandler
		/**
		 * get the IntentHandler of this presenter, must be called after `initialize` return
		 * @throws IllegalStateException if attempted to get the value before initialized
		 */
		@kotlin.jvm.Throws(IllegalStateException::class)
		get

	/**
	 * Initialize the presenter, must be called and returns before any other public property access.
	 * @param viewModel the ViewModel instance to to wire this presenter.
	 * @param coroutineContext optional [CoroutineContext]
	 * @throws IllegalStateException if called twice, this is responsibility of the caller to ensure
	 * that initialization attempt should only happen once.
	 */
	@kotlin.jvm.Throws(IllegalStateException::class)
	fun initialize(
		viewModel: ViewModel,
		coroutineContext: CoroutineContext = EmptyCoroutineContext
	)

	fun dispose()

	/**
	 * ViewModel interface specific to this presenter,
	 * useful to bridge androidx.lifecycle features such as [SavedStateHandle] or any `Features` in
	 * general that is platform SDK dependent
	 */
	interface ViewModel {
		fun loadSaver(): Bundle?
	}
}
