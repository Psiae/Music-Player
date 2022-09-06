package com.kylentt.musicplayer.medialib.player.component

import android.media.AudioDeviceInfo
import com.kylentt.musicplayer.medialib.player.playback.PlaybackController

interface AudioRerouteHandler {
	fun onAudioReroute(playbackController: PlaybackController, info: AudioRerouteInfo)
}

class AudioRerouteInfo internal constructor(private val outputDevices: Array<AudioDeviceInfo>) {

	private val currentOutputSpeaker: List<AudioDeviceInfo> by lazy {
		outputDevices.filter { device -> speakerType.any { it == device.type } }
	}

	private val currentOutputHeadset: List<AudioDeviceInfo> by lazy {
		outputDevices.filter { device -> headsetType.any { it == device.type } }
	}

	private val currentOutputBluetooth: List<AudioDeviceInfo> by lazy {
		outputDevices.filter { device -> bluetoothType.any { it == device.type } }
	}

	fun currentOutput(): AudioDeviceInfo? = currentOutputSpeaker.firstOrNull()
		?: currentOutputHeadset.firstOrNull()
		?: currentOutputBluetooth.firstOrNull()

	companion object {
		val INVALID = AudioRerouteInfo(emptyArray())

		val speakerType: Array<Int> = arrayOf(
			AudioDeviceInfo.TYPE_BLE_SPEAKER,
			AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
			AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
		)

		val headsetType: Array<Int> = arrayOf(
			AudioDeviceInfo.TYPE_BLE_HEADSET,
			AudioDeviceInfo.TYPE_USB_HEADSET,
			AudioDeviceInfo.TYPE_WIRED_HEADSET,
			AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
		)

		val bluetoothType: Array<Int> = arrayOf(
			AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
			AudioDeviceInfo.TYPE_BLUETOOTH_SCO
		)
	}
}

