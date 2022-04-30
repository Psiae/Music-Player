package com.kylentt.mediaplayer.app.dependency

import android.content.Context
import coil.Coil
import coil.ImageLoader
import com.kylentt.mediaplayer.app.coroutines.AppDispatchers
import com.kylentt.mediaplayer.app.coroutines.AppScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

  /** Dagger Hilt Module for Application-Wide Singletons, e.g: App-level Coroutine Scope */

  @Provides
  @Singleton
  fun provideAppDispatchers(): AppDispatchers = AppDispatchers.Default

  @Provides
  @Singleton
  fun provideAppScope(dispatchers: AppDispatchers): AppScope = with(dispatchers) {
    AppScope(
      ioScope = CoroutineScope(context = io + SupervisorJob()),
      mainScope = CoroutineScope(context = main + SupervisorJob()),
      computationScope = CoroutineScope(context = computation + SupervisorJob()),
      immediateScope = CoroutineScope(context = mainImmediate + SupervisorJob())
    )
  }

  @Provides
  @Singleton
  fun provideCoilInstance(
    @ApplicationContext context: Context
  ): ImageLoader = Coil.imageLoader(context)
}
