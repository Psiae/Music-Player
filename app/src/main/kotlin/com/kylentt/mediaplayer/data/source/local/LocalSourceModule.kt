package com.kylentt.mediaplayer.data.source.local

import android.content.Context
import com.kylentt.mediaplayer.app.AppDispatchers
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalSourceModule {

  @Provides
  @Singleton
  fun provideMediaStoreSource(
    @ApplicationContext context: Context,
    coroutineDispatchers: AppDispatchers
  ) = MediaStoreSourceImpl(context, coroutineDispatchers)
}
