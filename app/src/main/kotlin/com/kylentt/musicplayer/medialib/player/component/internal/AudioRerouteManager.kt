package com.kylentt.musicplayer.medialib.player.component.internal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.kylentt.musicplayer.medialib.player.PlayerContext
import com.kylentt.musicplayer.medialib.player.component.AudioRerouteInfo

internal open class AudioRerouteManager(
	context: PlayerContext,
	private val handler: EventHandler
) {

	private val audioManager = context.android.audioManager
	private val broadcastManager = context.broadcastManager
	private val receiver = Receiver()

	private var started = false
	private var released = false


	fun start() {
		if (started || released) return
		broadcastManager.registerBroadcastReceiver(
			receiver = receiver,
			intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
		)
		started = true
	}

	fun stop() {
		if (!started || released) return
		broadcastManager.unregisterBroadcastReceiver(receiver)
		started = false
	}

	fun release() {
		if (released) return
		if (started) stop()
		broadcastManager.release()
		released = true
	}

	private fun onReceive(intent: Intent) {
		val devices: Array<AudioDeviceInfo> = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
		handler.onAudioOutputReroute(AudioRerouteInfo(devices))
	}

	private inner class Receiver() : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			if (intent != null) {
				check(intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY)
				onReceive(intent)
			}
		}
	}

	fun interface EventHandler {
		fun onAudioOutputReroute(info: AudioRerouteInfo)
	}
}
