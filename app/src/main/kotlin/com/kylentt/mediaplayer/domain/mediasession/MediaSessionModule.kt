package com.kylentt.mediaplayer.domain.mediasession

import android.content.Context
import com.kylentt.mediaplayer.app.coroutines.AppDispatchers
import com.kylentt.mediaplayer.app.coroutines.AppScope
import com.kylentt.mediaplayer.data.repository.MediaRepository
import com.kylentt.mediaplayer.data.repository.ProtoRepository
import com.kylentt.mediaplayer.helper.image.CoilHelper
import com.kylentt.mediaplayer.helper.media.MediaItemHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module Providing MediaSession Component
 * @author Kylentt
 * @since 2022/04/03
 * @see [MediaSessionManager]
 */

@Module
@InstallIn(SingletonComponent::class)
object MediaSessionModule {

  @Provides
  @Singleton
  fun provideMediaSessionManager(
    @ApplicationContext context: Context,
    coroutineScope: AppScope,
    coilHelper: CoilHelper,
    dispatchers: AppDispatchers,
    itemHelper: MediaItemHelper,
    mediaRepo: MediaRepository,
    protoRepo: ProtoRepository
  ): MediaSessionManager {
    return MediaSessionManager(
      appScope = coroutineScope,
      baseContext = context,
      coilHelper = coilHelper,
      dispatchers = dispatchers,
      itemHelper = itemHelper,
      mediaRepo = mediaRepo,
      protoRepo = protoRepo
    )
  }

}
