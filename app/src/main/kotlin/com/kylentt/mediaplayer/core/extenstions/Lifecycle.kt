package com.kylentt.mediaplayer.core.extenstions

import android.app.Service
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

interface LifecycleEvent {
	fun asLifecycleEvent(): Lifecycle.Event
}

interface LifecycleService : LifecycleOwner {
	val service: Service
}
