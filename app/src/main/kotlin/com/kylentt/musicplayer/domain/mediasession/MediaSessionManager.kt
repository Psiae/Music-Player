package com.kylentt.musicplayer.domain.mediasession

import android.content.Context
import com.kylentt.musicplayer.domain.mediasession.service.MediaServiceConnector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

class MediaSessionManager private constructor(
    private val context: Context
) {
    private val mediaServiceConnector = MediaServiceConnector(this, context)
    val serviceState = mediaServiceConnector.serviceState

    fun connectService() {
        mediaServiceConnector.connectService()
    }

    companion object {
        private lateinit var instance: MediaSessionManager
        fun getInstance(context: Context) = run {
            if (::instance.isInitialized) return@run instance
            MediaSessionManager(context.applicationContext)
        }
    }

}

@Module
@InstallIn(SingletonComponent::class)
object MediaSessionModule {

    @Singleton
    @Provides
    fun provideSessionManager(@ApplicationContext context: Context) = MediaSessionManager.getInstance(context)

}