package com.kylentt.mediaplayer.ui

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.kylentt.mediaplayer.domain.presenter.ControllerViewModel
import com.kylentt.mediaplayer.domain.service.MusicService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // List
    private var mediaItems = listOf<MediaItem>()

    val controller: ControllerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (checkPermission()) {
            controller
            // TODO: Permission Screen
        }

        setContent {

        }
    }

    private fun checkPermission(): Boolean {
        return true
    }

    @Composable
    fun ImageCard() {

    }
}

