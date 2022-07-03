package com.kylentt.mediaplayer.domain.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.annotation.MainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkArgument
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.ui.activity.CollectionExtension.forEachClear

@MainThread
class ContextBroadcastManager(
	context: Context
) {
	private val broadcastReceivers = mutableListOf<BroadcastReceiver>()
	private var localContext: Context? = context

	var released = false
		private set (value) {
			checkArgument(value)
			field = value
		}


	fun registerBroadcastReceiver(receiver: BroadcastReceiver, intentFilter: IntentFilter): Boolean {
		checkMainThread()
		if (released) return false
		val found = broadcastReceivers.find { it === receiver } != null
		return if (found) false else run {
			localContext!!.registerReceiver(receiver, intentFilter)
			broadcastReceivers.add(receiver)
		}
	}

	fun removeBroadcastReceiver(receiver: BroadcastReceiver): Boolean {
		checkMainThread()
		return if (released) false else run {
			localContext!!.unregisterReceiver(receiver)
			broadcastReceivers.removeIf { it === receiver }
		}
	}

	fun release() {
		checkMainThread()
		if (released) return
		doRelease()
	}

	private fun doRelease() {
		checkState(!released)
		val getLocalContext = localContext
		if (getLocalContext == null) {
			checkState(broadcastReceivers.isEmpty())
			released = true
			return
		}
		broadcastReceivers.forEachClear { getLocalContext.unregisterReceiver(it)}
		localContext = null
		doRelease()
	}
}
