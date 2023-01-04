package com.flammky.musicplayer.common.media.audio.meta_tag.utils

import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

object FileTypeUtil {
	private const val BUFFER_SIZE_LIMIT = 4096
	private const val MAX_SIGNATURE_SIZE = 12

	private val mp3v2Sig = arrayOf<Int?>(
		'I'.code, // 0x49 | 73
		'D'.code, // 0x44 | 68
		'3'.code  // 0x33 | 51
	)

	// older MP3, maybe obsolete
	private val mp3v1Sig_1 = arrayOf<Int?>(0xFF, 0xF3)
	// can't really find this anywhere,
	// I guess it follows FA and FB which is just reversed version of each other
	private val mp3v1Sig_2 = arrayOf<Int?>(0xFF, 0xFA)
	private val mp3v1Sig_3 = arrayOf<Int?>(0xFF, 0xF2)
	private val mp3v1Sig_4 = arrayOf<Int?>(0xFF, 0xFB)

	private val mp4Sign = arrayOf<Int?>(
		0x00,
		0x00,
		0x00,
		null,
		'f'.code, // 0x66 | 102
		't'.code, // 0x74 | 116
		'y'.code, // 0x79 | 121
		'p'.code  // 0x70 | 112
	)

	private val oggSign = arrayOf<Int?>(
		'O'.code, // 0x4F |
		'g'.code, // 0x67
		'g'.code, // 0x67
		'S'.code  // 0x53
	)

	private val flacSign = arrayOf<Int?>(
		'f'.code, // 66
		'L'.code, // 4C
		'a'.code, // 61
		'C'.code  // 43
	)

	private val wavSign = arrayOf<Int?>(
		'R'.code, // 52
		'I'.code, // 49
		'F'.code, // 46
		'F'.code, // 46
		null, null, null, null,
		'W'.code, // 57
		'A'.code, // 41
		'V'.code, // 56
		'E'.code 	// 45
	)

	private var signatureMap: MutableMap<String, Array<Int?>>? = null
	private var extensionMap: MutableMap<String, String>? = null


	init {
		signatureMap = LinkedHashMap()
		val signatureMap = signatureMap!!
		signatureMap["MP3IDv2"] = mp3v2Sig
		signatureMap["MP3IDv1_1"] = mp3v1Sig_1
		signatureMap["MP3IDv1_2"] = mp3v1Sig_2
		signatureMap["MP3IDv1_3"] = mp3v1Sig_3
		signatureMap["MP3IDv1_4"] = mp3v1Sig_4
		signatureMap["MP4"] = mp4Sign
		signatureMap["OGG"] = oggSign
		signatureMap["FLAC"] = flacSign
		signatureMap["WAV"] = wavSign
		extensionMap = LinkedHashMap()
		val extensionMap = extensionMap!!
		extensionMap["MP3IDv2"] = "mp3"
		extensionMap["MP3IDv1_1"] = "mp3"
		extensionMap["MP3IDv1_2"] = "mp3"
		extensionMap["MP3IDv1_3"] = "mp3"
		extensionMap["MP3IDv1_4"] = "mp3"
		extensionMap["MP4"] = "m4a"
		extensionMap["OGG"] = "ogg"
		extensionMap["FLAC"] = "flac"
		extensionMap["WAV"] = "wav"
		extensionMap["UNKNOWN"] = ""
	}

	@JvmStatic
	@Throws(IOException::class)
	fun getMagicFileType(f: File): String {
		val buffer = ByteArray(MAX_SIGNATURE_SIZE)
		val length: Int
		return FileInputStream(f).use { iStream ->
			length = iStream.read(buffer, 0, buffer.size)
			var fileType = "UNKNOWN"
			signatureMap!!.forEach { entry ->
				if (matchesSignature(
						signature = entry.value,
						buffer,
						length
				)) {
					fileType = entry.key
					return@forEach
				}
			}
			fileType
		}
	}

	@JvmStatic
	@Throws(IOException::class)
	fun getMagicFileType(fd: FileDescriptor): String {
		val buffer = ByteArray(MAX_SIGNATURE_SIZE)
		val length: Int
		return FileInputStream(fd).use { iStream ->
			length = iStream.read(buffer, 0, buffer.size)
			var fileType = "UNKNOWN"
			signatureMap!!.forEach { entry ->
				if (matchesSignature(
						signature = entry.value,
						buffer,
						length
				)) {
					fileType = entry.key
					return@forEach
				}
			}
			fileType
		}
	}

	@JvmStatic
	@Throws(IOException::class)
	fun getMagicFileType(fc: FileChannel): String {
		fc.position(0)
		val buffer = ByteBuffer.allocate(MAX_SIGNATURE_SIZE)
		val length: Int = fc.read(buffer)
		var fileType = "UNKNOWN"
		signatureMap!!.forEach { entry ->
			if (matchesSignature(
					signature = entry.value,
					buffer.array(),
					length
			)) {
				fileType = entry.key
				return@forEach
			}
		}
		return fileType
	}

	@JvmStatic
	fun getMagicExt(fileType: String): String? {
		return extensionMap!![fileType]
	}

	private fun matchesSignature(signature: Array<Int?>, buffer: ByteArray, size: Int): Boolean {
		if (size < signature.size) {
			return false
		}
		var b = true
		for (i in signature.indices) {
			println("matchesSignature: $i ${signature[i]} ${buffer[i].toInt()}" )
			if (signature[i] != null) {
				if (signature[i] != 0x00ff and buffer[i].toInt()) {
					b = false
					break
				}
			}
		}
		return b
	}

	@Throws(IOException::class)
	@JvmStatic
	fun main(args: Array<String>) {
		// if (args.length < 1) {
		// System.out.println("Usage: java TestExcelPDF <filename>");
		// System.exit(1);
		// }
		val testFileLoc =
			"C:/Users/keerthi/Dropbox/Works/Java/github/GaanaExtractor/workspace/jaudiotagger/testm4a"
		// FileTypeUtil t = new FileTypeUtil();
		val fileType = getMagicFileType(File(testFileLoc))
		println("File type: $fileType")
		println("File Extension: " + getMagicExt(fileType))
	}
}
