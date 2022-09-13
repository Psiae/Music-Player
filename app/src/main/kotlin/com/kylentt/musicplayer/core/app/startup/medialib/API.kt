package com.kylentt.musicplayer.core.app.startup.medialib

import android.content.Context
import androidx.startup.Initializer
import com.flammky.android.medialib.temp.MediaLibrary
import com.flammky.android.medialib.temp.api.MediaLibraryAPI
import com.kylentt.musicplayer.domain.musiclib.service.MusicLibraryService

class ApiInitializer : Initializer<MediaLibraryAPI> {
	override fun create(context: Context): MediaLibraryAPI = MediaLibrary.construct(context, MusicLibraryService::class.java)
	override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()
}
