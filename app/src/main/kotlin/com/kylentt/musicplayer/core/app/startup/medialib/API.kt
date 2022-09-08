package com.kylentt.musicplayer.core.app.startup.medialib

import android.content.Context
import androidx.startup.Initializer
import com.kylentt.musicplayer.medialib.MediaLibrary
import com.kylentt.musicplayer.medialib.api.MediaLibraryAPI
import com.kylentt.musicplayer.medialib.internal.RealMediaLibrary
import com.kylentt.musicplayer.medialib.api.RealMediaLibraryAPI

class ApiInitializer : Initializer<MediaLibraryAPI> {
	override fun create(context: Context): MediaLibraryAPI = MediaLibrary.construct(context)
	override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()
}
