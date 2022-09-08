package com.kylentt.musicplayer.core.app

import android.app.Application
import com.kylentt.musicplayer.medialib.MediaLibrary
import com.kylentt.musicplayer.medialib.api.MediaLibraryAPI
import com.kylentt.musicplayer.medialib.api.RealMediaLibraryAPI
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MusicPlayerApp : Application()
