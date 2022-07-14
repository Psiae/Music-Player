package com.kylentt.musicplayer.domain.musiclib.interactor

import com.kylentt.mediaplayer.domain.musiclib.service.ServiceConnector
import com.kylentt.musicplayer.domain.musiclib.dependency.Injector
import com.kylentt.musicplayer.domain.musiclib.dependency.Provider

class AgentMask private constructor() {
	private lateinit var mAgent: Agent

	constructor(agent: Agent) : this() {
		mAgent = agent
	}

	val dependency = object : Dependency {
		override fun provide(injector: Injector) = mAgent.injector.fuseInjector(injector)
		override fun provide(vararg providers: Provider<Any>) = mAgent.injector.addProvider(*providers)
	}

	val session = object : Session {
		override val controller: ServiceConnector.ControllerInteractor
			get() = mAgent.session.controller
		override val info: ServiceConnector.Info
			get() = mAgent.session.info
	}

	interface Dependency {
		fun provide(injector: Injector)
		fun provide(vararg providers: Provider<Any>)
	}

	interface Session {
		val controller: ServiceConnector.ControllerInteractor
		val info: ServiceConnector.Info
	}
}
