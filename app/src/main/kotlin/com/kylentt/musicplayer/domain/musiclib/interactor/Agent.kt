package com.kylentt.musicplayer.domain.musiclib.interactor

import com.kylentt.mediaplayer.core.extenstions.sync
import com.kylentt.mediaplayer.domain.musiclib.service.ServiceConnector
import com.kylentt.musicplayer.domain.musiclib.MusicLibrary
import com.kylentt.musicplayer.domain.musiclib.dependency.DependencyProvider


class Agent private constructor() {
	private lateinit var delegate: MusicLibrary.AgentDelegate
	private val registry = Registry()

	constructor(delegate: Agent.() -> MusicLibrary.AgentDelegate) : this() {
		this.delegate = delegate()
	}

	val dependency
		get() = registry.dependency

	val initializer = object : Initializer {
		private var mInitialized = false

		override val initialized
			get() = mInitialized

		override val agent: Agent = this@Agent
		override fun initialize(): Initializer = sync {
			if (!initialized) {
				delegate.serviceConnector.initialize(agent)
				delegate.serviceConnector.connect()
				mInitialized = true
			}
			this
		}
	}

	val loader = object : Loader {
		override val agent = this@Agent
		override fun loadDependency(vararg obj: Any): Loader = sync {
			registry.loadDependency(*obj)
			this
		}
	}

	val session = object : Session {
		override val agent: Agent = this@Agent

		override val controller: ServiceConnector.ControllerInteractor
			get() = delegate.serviceConnector.interactor.controller

		override val info: ServiceConnector.Info
			get() = delegate.serviceConnector.interactor.info
	}

	private inner class Registry {
		private val mDependency = DependencyProvider.Mutable()

		val dependency
			get() = mDependency.toImmutable

		fun loadDependency(vararg obj: Any) = sync { mDependency.add(*obj) }
	}

	interface Session {
		val agent: Agent
		val controller: ServiceConnector.ControllerInteractor // TODO
		val info: ServiceConnector.Info // TODO
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
}
