package com.flammky.android.medialib.temp.api.service.internal

import android.content.ComponentName
import android.content.Intent
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import com.flammky.android.medialib.temp.api.service.ServiceComponent
import com.flammky.android.medialib.temp.internal.MediaLibraryContext

internal class InternalServiceComponent(private val context: MediaLibraryContext) :
    ServiceComponent {

	@Deprecated("")
	private val sessionToken = SessionToken(context.android, ComponentName(context.android, context.serviceClass))

	override fun startService() {
		val intent = Intent(MediaSessionService.SERVICE_INTERFACE)
		intent.setClassName(sessionToken.getPackageName(), sessionToken.getServiceName())
		context.android.startService(intent)
	}
}
