package com.flammky.android.common.io.file

import androidx.appcompat.app.AppCompatActivity
import java.io.File

object FileUtil {

	/*fun File.replace(replaceWith: File): Boolean {
		delete()
		if (!replaceWith.exists()) replaceWith.apply {

		}

		replaceWith.apply {
			if (!exists()) {

			}
		}


		replaceWith.createNewFile()
	}*/


	fun File.replaceIfExists(replaceWith: File): Boolean {
		return (exists() && !delete() && !replaceWith.renameTo(this))
	}

	fun File.replaceIfOtherExists(other: File): Boolean {
		return (other.exists() && delete() && other.renameTo(this))
	}


	fun <R> File.ifExists(block: () -> R) {}

	val c = AppCompatActivity().apply {


	}
}
