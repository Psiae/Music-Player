package com.flammky.musicplayer.base.startup

import android.app.Application
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.base.auth.r.RealAuthService
import com.flammky.musicplayer.base.auth.r.local.LocalAuthProvider

object BaseInitializer {
	fun run(app: Application) {
		val realAuth = RealAuthService.provides(app)
		RealAuthService.registerProvider(LocalAuthProvider(app))
		AuthService.provides(realAuth)
	}
}
