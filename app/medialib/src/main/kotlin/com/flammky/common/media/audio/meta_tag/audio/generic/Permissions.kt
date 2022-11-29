package com.flammky.common.media.audio.meta_tag.audio.generic

import android.util.Log
import com.flammky.android.core.sdk.VersionHelper
import com.flammky.common.kotlin.triple.toTriple
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.AclFileAttributeView
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermissions
import java.util.logging.Logger

/**
 * Outputs permissions to try and identify why we dont have permissions to read/write file
 */
object Permissions {
	val DEBUG_TAG = "tagger.generic.Permission"

	var logger = Logger.getLogger("org.jaudiotagger.audio.generic")

	fun displayPermissions(file: File): String {
		val sb = StringBuilder()
		sb.append("File $file permissions\n")
		try {
			val (read: Boolean, write: Boolean, execute: Boolean) = file.run {
				canRead() to canWrite() toTriple canExecute()
			}

			sb.append("\nread: $read")
			sb.append("\nwrite: $write")
			sb.append("\nexecute: $execute")

		} catch (ioe: IOException) {
			Log.e(DEBUG_TAG, "Unable to read permissions for:$file")
		}
		return sb.toString()
	}

	/**
	 * Display Permissions
	 *
	 * @param path
	 * @return
	 */
	@JvmStatic
	fun displayPermissions(path: Path): String {
		if (!VersionHelper.hasOreo()) throw UnsupportedOperationException()

		val sb = StringBuilder()
		sb.append("File $path permissions\n")
		try {
			run {
				val view = Files.getFileAttributeView(path, AclFileAttributeView::class.java)
				if (view != null) {
					sb.append(
						"""
    owner:${view.owner.name}

    """.trimIndent()
					)
					for (acl in view.acl) {
						sb.append(
							"""
    $acl

    """.trimIndent()
						)
					}
				}
			}
			run {
				val view = Files.getFileAttributeView(path, PosixFileAttributeView::class.java)
				if (view != null) {
					val pfa = view.readAttributes()
					sb.append(
						"""
    :owner:${pfa.owner().name}:group:${pfa.group().name}:${PosixFilePermissions.toString(pfa.permissions())}

    """.trimIndent()
					)
				}
			}
		} catch (ioe: IOException) {
			logger.severe(
				"Unable to read permissions for:$path"
			)
		}
		return sb.toString()
	}
}
