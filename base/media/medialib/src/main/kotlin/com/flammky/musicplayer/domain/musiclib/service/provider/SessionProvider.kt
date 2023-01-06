package com.flammky.musicplayer.domain.musiclib.service.provider

import android.app.PendingIntent
import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaLibraryService.MediaLibrarySession.Callback

class SessionProvider(
	private val service: MediaLibraryService
) {

	fun getNewLibrarySession(
		context: Context,
		player: Player,
		callback: Callback
	): MediaLibrarySession {
		return buildLibrarySession(444, "FLAMM", context, service, player, callback)
	}

	private fun buildLibrarySession(
		requestCode: Int,
		sessionId: String,
		context: Context,
		service: MediaLibraryService,
		player: Player,
		callback: MediaLibrarySession.Callback,
	): MediaLibrarySession {
		val activityIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)!!
		val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		val activityPendingIntent =
			PendingIntent.getActivity(context, requestCode, activityIntent, flag)
		val builder = MediaLibrarySession.Builder(service, player, callback)
		return with(builder) {
			setId(sessionId)
			setSessionActivity(activityPendingIntent)
			build()
		}
	}
}
