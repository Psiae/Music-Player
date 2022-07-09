package com.kylentt.mediaplayer.domain.musiclib

import com.kylentt.mediaplayer.core.annotation.Singleton
import com.kylentt.mediaplayer.domain.musiclib.service.ServiceConnector
import com.kylentt.mediaplayer.helper.Preconditions.checkState

@Singleton
class MusicLibrary private constructor() {
	private lateinit var bundle: DependencyBundle

	private var manager = MusicLibraryManager(this)

	private fun initialize(dependency: DependencyBundle) {
		checkState(!this::bundle.isInitialized)
		bundle = dependency.copy()
		manager.start(bundle)
	}

	companion object {
		private val musicLibrary = MusicLibrary()
		fun initialize(dependencies: DependencyBundle) = musicLibrary.initialize(dependencies)

		val serviceInteractor
			get() = musicLibrary.manager.serviceConnector.interactor

		val serviceInfo
			get() = musicLibrary.manager.serviceConnector.info
	}
}

class MusicLibraryManager(private val musicLibrary: MusicLibrary) {

	val serviceConnector = ServiceConnector()

	fun start(bundle: DependencyBundle) {
		serviceConnector.connect(bundle)
	}
}
