package com.flammky.musicplayer.common.media.audio.uri

import java.io.File

interface AndroidFileBuilder {
	fun build(): File
	fun buildToDir(dir: File): File
	fun release()
}
