package com.kylentt.musicplayer.domain.musiclib.interactor

import com.kylentt.mediaplayer.domain.musiclib.service.ServiceConnector
import com.kylentt.musicplayer.domain.musiclib.MusicLibrary
import com.kylentt.musicplayer.domain.musiclib.dependency.Injector


class Agent private constructor() {
	constructor(lib: MusicLibrary) : this()

	val mask = AgentMask(this)
	val injector = Injector()

	val session = object : Session {

		override val controller: ServiceConnector.ControllerInteractor
			get() = serviceConnector.interactor.controller

		override val info: ServiceConnector.Info
			get() = serviceConnector.interactor.info
	}

	private val serviceConnector: ServiceConnector = ServiceConnector(this)

	interface Session {
		val controller: ServiceConnector.ControllerInteractor // TODO
		val info: ServiceConnector.Info // TODO
	}
}
