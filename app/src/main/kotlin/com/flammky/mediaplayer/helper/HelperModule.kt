package com.flammky.mediaplayer.helper

import android.content.Context
import coil.ImageLoader
import com.flammky.mediaplayer.helper.image.CoilHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HelperModule {

  @Provides
  @Singleton
  fun provideCoilHelper(
    @ApplicationContext context: Context,
    loader: ImageLoader,
  ) = CoilHelper(context, loader)

}
