package com.kylentt.musicplayer.medialib.api.service.internal

import android.content.ComponentName
import android.content.Intent
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import com.kylentt.musicplayer.domain.musiclib.service.MusicLibraryService
import com.kylentt.musicplayer.medialib.api.service.ServiceComponent
import com.kylentt.musicplayer.medialib.internal.MediaLibraryContext

internal class InternalServiceComponent(private val context: MediaLibraryContext) : ServiceComponent {

	@Deprecated("")
	private val sessionToken = SessionToken(context.android, ComponentName(context.android, MusicLibraryService::class.java))

	override fun startService() {
		val intent = Intent(MediaSessionService.SERVICE_INTERFACE)
		intent.setClassName(sessionToken.getPackageName(), sessionToken.getServiceName())
		context.android.startService(intent)
	}
}
