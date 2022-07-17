package com.kylentt.mediaplayer.domain.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.annotation.MainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkArgument
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.ui.activity.CollectionExtension.forEachClear

@MainThread
class ContextBroadcastManager(context: Context) {
	private val broadcastReceivers = mutableListOf<BroadcastReceiver>()

	private var localContext: Context? = context
		set(value) {
			field = if (!released) value else null
		}

	var released = false
		private set (value) {
			checkArgument(value)
			field = value
		}

	fun registerBroadcastReceiver(receiver: BroadcastReceiver, intentFilter: IntentFilter): Boolean {
		return when {
			!checkMainThread() || released || broadcastReceivers.find { it === receiver } != null -> false
			else -> {
				localContext
					?.let { context ->
						context.registerReceiver(receiver, intentFilter)
						broadcastReceivers.add(receiver)
					}
					?: false
			}
		}
	}

	fun removeBroadcastReceiver(receiver: BroadcastReceiver): Boolean {
		return when {
			!checkMainThread() || released || broadcastReceivers.find { it === receiver } == null -> false
			else -> {
				localContext
					?.let { context ->
						context.unregisterReceiver(receiver)
						broadcastReceivers.removeIf { it === receiver }
					}
					?: false
			}
		}
	}

	fun release() {
		checkMainThread()
		doRelease()
	}

	private fun doRelease() {
		var releaseAttempt = 0

		while (!released || localContext != null || broadcastReceivers.isNotEmpty()) {
			if (releaseAttempt > 3) throw IllegalStateException()

			localContext
				?.let { context ->
					broadcastReceivers.forEachClear { context.unregisterReceiver(it) }
					localContext = null
				}
				?: run {
					checkState(broadcastReceivers.isEmpty())
					released = true
				}

			releaseAttempt++
		}
	}
}
