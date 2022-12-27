package com.flammky.musicplayer.base.hilt

import com.flammky.musicplayer.base.auth.AuthService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BaseModule {

	@Provides
	@Singleton
	fun provideAuthService(): AuthService = AuthService.get()
}
