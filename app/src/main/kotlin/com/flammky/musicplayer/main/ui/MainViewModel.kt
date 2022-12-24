package com.flammky.musicplayer.main.ui

import android.os.Bundle
import androidx.lifecycle.ViewModel
import com.flammky.musicplayer.main.ext.IntentHandler

class MainViewModel constructor(
	private val presenter: MainPresenter
) : ViewModel(), MainPresenter.ViewModel {

	init {
		presenter.initialize(this)
	}

	val intentHandler: IntentHandler = presenter.intentHandler


	override fun loadSaver(): Bundle? = /* TODO: SavedStateHandle */ null
}
