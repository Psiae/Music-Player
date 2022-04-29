package com.kylentt.disposed.disposed.domain.mediaSession

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent

@Module
@InstallIn(ServiceComponent::class)
object MediaServiceModule {

  /*@ServiceScoped
  @Provides
  fun provideExoPlayer(
    @ApplicationContext context: Context
  ) = with(ExoUtil) { newExo(context, defaultAttr()) }*/

}
