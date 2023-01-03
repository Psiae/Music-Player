package com.flammky.musicplayer.common.media.audio.meta_tag.logging

import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Formatter
import java.util.logging.LogRecord

/**
 * For Formatting log output
 *
 *
 * This is not required by jaudiotagger, but its advantage over the default formatter is that all the format for a log
 * entry is on one line, making it much easier to read. To use this formatter with your code edit loggin.properties
 * within your jre/lib folder and  modify as follows
 * e.g java.util.logging.ConsoleHandler.formatter = org.jaudiotagger.logging.LogFormatter
 */
class LogFormatter : Formatter() {
	// Line separator string.  This is the value of the line.separator
	// property at the moment that the SimpleFormatter was created.
	private val lineSeparator = System.lineSeparator()
	private val sfDateOut = SimpleDateFormat("dd/MM/yyyy HH.mm.ss:")
	private val date = Date()
	override fun format(record: LogRecord): String {
		val sb = StringBuffer()
		date.time = record.millis
		sb.append(sfDateOut.format(date))
		val recordName: String
		recordName = if (record.sourceClassName != null) {
			record.sourceClassName + ":" + record.sourceMethodName
		} else {
			record.loggerName + ":"
		}
		if (recordName != null) {
			sb.append(recordName)
			sb.append(":")
		}
		val message = formatMessage(record)
		sb.append(record.level.localizedName)
		sb.append(": ")
		sb.append(message)
		sb.append(lineSeparator)
		if (record.thrown != null) {
			try {
				val sw = StringWriter()
				val pw = PrintWriter(sw)
				record.thrown.printStackTrace(pw)
				pw.close()
				sb.append(sw.toString())
			} catch (ex: Exception) {
			}
		}
		return sb.toString()
	}

	companion object {
		const val ACTION_PERFORMED = "actionPerformed"
		const val IDENT = "\$Id$"
	}
}
