package com.flammky.musicplayer.common.android.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.flammky.mediaplayer.ui.activity.CollectionExtension.forEachClear
import com.flammky.musicplayer.common.android.concurrent.ConcurrencyHelper.checkMainThread

@MainThread
class ContextBroadcastManager(context: Context) {
	private val broadcastReceivers = mutableListOf<Pair<BroadcastReceiver, MutableList<IntentFilter>>>()

	private var lifecycleOwner: LifecycleOwner? = null
		set(value) {
			if (value != null) {
				check(field == null)
			} else {
				field?.lifecycle?.removeObserver(observer!!)
			}
			field = value
		}

	private var localContext: Context? = context
		set(value) {
			check(value == null)
			field = null
		}

	private val observer = lifecycleOwner?.let {
		LifecycleObserver().apply { it.lifecycle.addObserver(this) }
	}

	var released = false
		private set(value) {
			require(value)
			check(broadcastReceivers.isEmpty() && lifecycleOwner == null)
			field = value
		}

	constructor(context: Context, lifecycleOwner: LifecycleOwner) : this(context) {
		this.lifecycleOwner = lifecycleOwner
		lifecycleOwner.lifecycle.addObserver(LifecycleObserver())
	}

	fun registerBroadcastReceiver(
		receiver: BroadcastReceiver,
		intentFilter: IntentFilter,
		flags: Int? = null
	): Boolean {

		val context = localContext ?: return false

		if (flags != null) {
			context.registerReceiver(receiver, intentFilter, flags)
		} else {
			context.registerReceiver(receiver, intentFilter)
		}

		broadcastReceivers.find { it.first === receiver }?.second?.add(intentFilter)
			?: run { broadcastReceivers.add(receiver to mutableListOf(intentFilter)) }

		return true
	}

	fun unregisterBroadcastReceiver(receiver: BroadcastReceiver): Boolean {

		val context = localContext ?: return false

		val i = broadcastReceivers.indexOfFirst { it.first === receiver }

		if (i == -1) return false

		broadcastReceivers[i].first.let {
			context.unregisterReceiver(it)
			broadcastReceivers.removeAt(i)
		}

		broadcastReceivers.forEach { if (it.first === receiver) throw IllegalStateException() }

		return true
	}

	fun release() {
		checkMainThread()
		doRelease()
	}

	private fun doRelease() {
		localContext?.let { context ->
			broadcastReceivers.forEachClear { context.unregisterReceiver(it.first) }
			lifecycleOwner = null
			localContext = null
		}
	}

	private inner class LifecycleObserver() : DefaultLifecycleObserver {
		override fun onDestroy(owner: LifecycleOwner) = release()
	}
}
