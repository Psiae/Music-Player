package com.flammky.musicplayer.common.android.memory

import android.app.ActivityManager.MemoryInfo
import com.flammky.musicplayer.common.android.environment.DeviceInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

suspend inline fun maybeWaitForMemory(
	nThreshold: Float = 1.5F,
	limitTime: Long,
	checkInterval: Long,
	getMemInfo: () -> MemoryInfo,
	onWait: () -> Unit = {}
) {
	if (getMemInfo().availMem < getMemInfo().threshold * nThreshold) {
		var spent = 0L
		while (getMemInfo().availMem < getMemInfo().threshold * nThreshold && spent < limitTime) {
			coroutineContext.ensureActive()
			onWait()
			delay(checkInterval)
			spent += checkInterval
		}
	}
}

suspend inline fun maybeWaitForMemory(
	nThreshold: Float = 1.5F,
	limitTime: Long,
	checkInterval: Long,
	deviceInfo: DeviceInfo,
	onWait: () -> Unit = {}
) = maybeWaitForMemory(nThreshold, limitTime, checkInterval, { deviceInfo.memoryInfo }, onWait)


