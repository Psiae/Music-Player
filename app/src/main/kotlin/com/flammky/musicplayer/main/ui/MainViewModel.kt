package com.flammky.musicplayer.main.ui

import android.os.Bundle
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class MainViewModel @Inject constructor(
	private val presenter: MainPresenter
) : ViewModel() {

	private val _intentRequestErrorMessageChannel = Channel<String>(capacity = Channel.UNLIMITED)
	private val _playbackErrorMessageChannel = Channel<String>(capacity = Channel.UNLIMITED)

	private val presenterDelegate = object : MainPresenter.ViewModel {

		override val coroutineContext: CoroutineContext = viewModelScope.coroutineContext

		override fun showIntentRequestErrorMessage(message: String) {
			_intentRequestErrorMessageChannel.trySend(message)
		}

		override fun showPlaybackErrorMessage(message: String) {
			_playbackErrorMessageChannel.trySend(message)
		}

		override fun loadSaver(): Bundle? {
			return null
		}
	}

	val intentRequestErrorMessageChannel: ReceiveChannel<String> = _intentRequestErrorMessageChannel
	val playbackErrorMessageChannel: ReceiveChannel<String> = _playbackErrorMessageChannel

	val splashHolders = mutableListOf<Any>()

	/**
	 * Wait for the first Guard to be initialized
	 */
	val firstEntryGuardWaiter = mutableStateListOf<() -> Unit>()

	/**
	 * Wait for the authGuard to be initialized
	 */
	val authGuardWaiter = mutableStateListOf<() -> Unit>()

	/**
	 * Wait for the permGuard to be initialized
	 */
	val permGuardWaiter = mutableStateListOf<() -> Unit>()

	/**
	 * Wait for all entryGuard to be initialized
	 */
	val intentEntryGuardWaiter = mutableStateListOf<() -> Unit>()

	init {
		presenter.initialize(presenterDelegate)
	}

	val intentHandler = presenter.intentReceiver

	override fun onCleared() {
		presenter.dispose()
	}
}
