package com.flammky.musicplayer.android.base

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import com.flammky.musicplayer.android.base.activity.ActivityWatcher
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.base.auth.LocalAuth
import com.flammky.musicplayer.android.base.auth.AndroidAuthService
import com.flammky.musicplayer.base.auth.r.local.LocalAuthProvider
import dev.dexsr.klio.core.CoreDebugInitializer
import dev.dexsr.klio.core.CoreInitializer
import com.flammky.musicplayer.core.common.atomic
import dev.dexsr.klio.base.common.BuildConfig
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BaseModuleInitializer : Initializer<Unit> {


	override fun create(context: Context) {
		check(C.incrementAndGet() == 1) {
			"BaseModuleInitializer was called multiple times"
		}
		val app = context as Application
		ActivityWatcher provides app
		ActivityWatcher.get()
			.apply {
				init()
			}
		val authService = AndroidAuthService provides app
		AndroidAuthService
			.apply {
				registerProvider(LocalAuthProvider())
			}
		AuthService provides authService
		AuthService.get()
			.apply {
				@OptIn(DelicateCoroutinesApi::class)
				GlobalScope.launch {
					initialize().join()
					loginAsync(LocalAuth.ProviderID, LocalAuth.buildAuthData())
				}
			}
	}

	override fun dependencies(): MutableList<Class<out Initializer<*>>> {
		return mutableListOf<Class<out Initializer<*>>>()
			.apply {
				if (BuildConfig.DEBUG) {
					add(BaseModuleDebugInitializer::class.java)
				}
				add(CoreInitializer::class.java)
			}
	}

	companion object {
		private val C = atomic(0)
	}
}

class BaseModuleDebugInitializer : Initializer<Unit> {

	override fun create(context: Context) {
		check(BuildConfig.DEBUG) {
			"BaseModuleDebugInitializer was called on non-debug build config"
		}
		check(C.incrementAndGet() == 1) {
			"BaseModuleDebugInitializer was called multiple times"
		}
	}

	override fun dependencies(): MutableList<Class<out Initializer<*>>> =
		mutableListOf(CoreDebugInitializer::class.java)

	companion object {
		private val C = atomic(0)
	}
}
