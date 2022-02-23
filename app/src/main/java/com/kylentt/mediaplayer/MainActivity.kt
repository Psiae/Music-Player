package com.kylentt.mediaplayer

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.kylentt.mediaplayer.domain.service.MusicService


class MainActivity : ComponentActivity() {

    // Media
    private lateinit var futureMediaController: ListenableFuture<MediaController>
    private val mediaController: MediaController?
        get() = if (futureMediaController.isDone) futureMediaController.get() else null

    // List
    private var mediaItems = listOf<MediaItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (checkPermission()) {
            setupService()
        }

        setContent {

        }
    }

    private fun checkPermission(): Boolean {
        return true
    }

    private fun setupService() {
        val sessionToken = SessionToken(this, ComponentName(
            this, MusicService::class.java
        ))
        val browser = MediaBrowser.Builder(applicationContext, sessionToken).buildAsync()
        browser.addListener( {
            setupController(sessionToken)
        } , MoreExecutors.directExecutor())
    }

    private fun setupController(token: SessionToken) {
        futureMediaController = MediaController.Builder(
            this, token
        ).buildAsync()
        mediaController?.isPlaying
    }

    @Composable
    fun ImageCard() {

    }
}

