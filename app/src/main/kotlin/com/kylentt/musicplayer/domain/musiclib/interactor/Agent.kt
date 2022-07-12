package com.kylentt.musicplayer.domain.musiclib.interactor

import com.kylentt.mediaplayer.core.extenstions.sync
import com.kylentt.mediaplayer.domain.musiclib.service.ServiceConnector
import com.kylentt.musicplayer.domain.musiclib.MusicLibrary
import com.kylentt.musicplayer.domain.musiclib.dependency.Injector


class Agent private constructor() {

	constructor(delegate: Agent.() -> MusicLibrary.AgentDelegate) : this() {
		this.libraryDelegate = delegate()
	}

	private lateinit var libraryDelegate: MusicLibrary.AgentDelegate

	val injector = Injector()

	val initializer = object : Initializer {
		private var mInitialized = false

		override val initialized
			get() = mInitialized

		override val agent: Agent = this@Agent
		override fun initialize(): Initializer = sync {
			if (!initialized) {
				libraryDelegate.serviceConnector.initialize(agent.injector)
				libraryDelegate.serviceConnector.connect()
				mInitialized = true
			}
			this
		}
	}

	val session = object : Session {
		override val agent: Agent = this@Agent

		override val controller: ServiceConnector.ControllerInteractor
			get() = libraryDelegate.serviceConnector.interactor.controller

		override val info: ServiceConnector.Info
			get() = libraryDelegate.serviceConnector.interactor.info
	}


	interface Initializer {
		val agent: Agent
		val initialized: Boolean

		fun initialize(): Initializer
	}

	interface Loader {
		val agent: Agent
		fun loadDependency(vararg obj: Any): Loader
	}

	interface Session {
		val agent: Agent
		val controller: ServiceConnector.ControllerInteractor // TODO
		val info: ServiceConnector.Info // TODO
	}
}
