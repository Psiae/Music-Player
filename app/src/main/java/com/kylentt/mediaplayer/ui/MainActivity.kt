package com.kylentt.mediaplayer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.media3.common.MediaItem
import com.kylentt.mediaplayer.domain.presenter.ControllerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        val TAG: String = this::class.java.simpleName
        var isActive = false
    }

    // List
    private var mediaItems = listOf<MediaItem>()
    private val controller: ControllerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isActive = true

        if (checkPermission()) {
            controller // simple call so viewModel init is called
            // TODO: Permission Screen
        }

        setContent {

        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun checkPermission(): Boolean {
        return true
    }

    override fun onDestroy() {
        isActive = false
        super.onDestroy()
    }
}

