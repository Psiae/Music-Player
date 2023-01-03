package com.flammky.musicplayer.base.coroutine.hilt

import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BaseCoroutineModule {

	@Provides
	@Singleton
	fun provideAndroidDispatchers(): AndroidCoroutineDispatchers {
		return AndroidCoroutineDispatchers.DEFAULT
	}
}
