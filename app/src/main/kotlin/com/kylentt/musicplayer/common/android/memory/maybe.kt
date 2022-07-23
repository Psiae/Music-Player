package com.kylentt.musicplayer.common.android.memory

import com.kylentt.musicplayer.common.android.environtment.DeviceInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

suspend inline fun maybeWaitForMemory(
	deviceInfo: DeviceInfo,
	limitTime: Long,
	checkInterval: Long,
	onWait: () -> Unit = {}
) {
	val memInfo = deviceInfo.memoryInfo
	if (memInfo.availMem < memInfo.threshold * 2) {
		var spent = 0L
		while (deviceInfo.memoryInfo.availMem < memInfo.threshold * 2 && spent < limitTime) {
			coroutineContext.ensureActive()
			onWait()
			delay(checkInterval)
			spent += checkInterval
		}
	}
}
