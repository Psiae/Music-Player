package com.kylentt.mediaplayer.app

import android.content.Context
import coil.Coil
import coil.ImageLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

  @Provides
  @Singleton
  fun provideCoilInstance(@ApplicationContext context: Context) = Coil.imageLoader(context)

  @Provides
  @Singleton
  fun provideAppDispatchers() = AppDispatchers.Default

  @Provides
  @Singleton
  fun provideAppScope(dispatchers: AppDispatchers) = AppScope.fromAppDispatchers(dispatchers)
}
