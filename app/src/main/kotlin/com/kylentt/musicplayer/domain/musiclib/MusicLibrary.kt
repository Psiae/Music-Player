package com.kylentt.musicplayer.domain.musiclib

import com.kylentt.mediaplayer.core.annotation.Singleton
import com.kylentt.musicplayer.domain.musiclib.dependency.DependencyProvider
import com.kylentt.musicplayer.domain.musiclib.interactor.Agent
import com.kylentt.mediaplayer.domain.musiclib.service.ServiceConnector

@Singleton
class MusicLibrary private constructor() {

	private val serviceConnector = ServiceConnector(ServiceDelegate())

	inner class ServiceDelegate
	inner class AgentDelegate private constructor() {
		private lateinit var mAgent: Agent

		constructor(agent: Agent) : this() {
			mAgent = agent
		}

		val serviceConnector
			get() = this@MusicLibrary.serviceConnector
	}

	companion object {
		private val musicLibrary = MusicLibrary()

		// Singleton Interactor
		val localAgent: Agent = Agent {
			musicLibrary.AgentDelegate(this)
		}
	}
}
