package com.flammky.musicplayer.common.media.audio.meta_tag.utils

import java.io.*

object FileTypeUtil {
	private const val BUFFER_SIZE = 4096
	private const val MAX_SIGNATURE_SIZE = 8

	// PDF files starts with: %PDF
	// MS office files starts with: (D0 CF 11 E0 A1 B1 1A E1)
	// Java does not support byte literals. Use int literals instead.
	// private static final int[] pdfSig = { 0x25, 0x50, 0x44, 0x46 };
	// private static final int[] msOfficeSig = { 0xd0, 0xcf, 0x11, 0xe0, 0xa1,
	// 0xb1, 0x1a, 0xe1 };
	private val mp3v2Sig = arrayOf<Int?>(0x49, 0x44, 0x33)
	private val mp3v1Sig_1 = arrayOf<Int?>(0xFF, 0xF3)
	private val mp3v1Sig_2 = arrayOf<Int?>(0xFF, 0xFA)
	private val mp3v1Sig_3 = arrayOf<Int?>(0xFF, 0xF2)
	private val mp3v1Sig_4 = arrayOf<Int?>(0xFF, 0xFB)
	private val mp4Sig = arrayOf(0x00, 0x00, 0x00, null, 0x66, 0x74, 0x79, 0x70)
	private var signatureMap: MutableMap<String, Array<Int?>>? = null
	private var extensionMap: MutableMap<String, String>? = null

	init {
		signatureMap = HashMap()
		val signatureMap = signatureMap!!
		signatureMap["MP3IDv2"] = mp3v2Sig
		signatureMap["MP3IDv1_1"] = mp3v1Sig_1
		signatureMap["MP3IDv1_2"] = mp3v1Sig_2
		signatureMap["MP3IDv1_3"] = mp3v1Sig_3
		signatureMap["MP3IDv1_4"] = mp3v1Sig_4
		signatureMap["MP4"] = mp4Sig
		extensionMap = HashMap()
		val extensionMap = extensionMap!!
		extensionMap["MP3IDv2"] = "mp3"
		extensionMap["MP3IDv1_1"] = "mp3"
		extensionMap["MP3IDv1_2"] = "mp3"
		extensionMap["MP3IDv1_3"] = "mp3"
		extensionMap["MP3IDv1_4"] = "mp3"
		extensionMap["MP4"] = "m4a"
		extensionMap["UNKNOWN"] = ""
	}

	@JvmStatic
	@Throws(IOException::class)
	fun getMagicFileType(f: File?): String {
		val buffer = ByteArray(BUFFER_SIZE)
		val `in`: InputStream = FileInputStream(f)
		return try {
			var n = `in`.read(
				buffer,
				0,
				BUFFER_SIZE
			)
			var m = n
			while (m < MAX_SIGNATURE_SIZE && n > 0) {
				n = `in`.read(
					buffer,
					m,
					BUFFER_SIZE - m
				)
				m += n
			}
			var fileType = "UNKNOWN"
			val i: Iterator<String> =
				signatureMap!!.keys.iterator()
			while (i.hasNext()) {
				val key = i.next()
				if (matchesSignature(
						signatureMap!![key]!!,
						buffer,
						m
					)
				) {
					fileType = key
					break
				}
			}
			fileType
		} finally {
			`in`.close()
		}
	}

	@JvmStatic
	@Throws(IOException::class)
	fun getMagicFileType(fd: FileDescriptor): String {
		val buffer = ByteArray(BUFFER_SIZE)
		val `in`: InputStream = FileInputStream(fd)
		return try {
			var n = `in`.read(
				buffer,
				0,
				BUFFER_SIZE
			)
			var m = n
			while (m < MAX_SIGNATURE_SIZE && n > 0) {
				n = `in`.read(
					buffer,
					m,
					BUFFER_SIZE - m
				)
				m += n
			}
			var fileType = "UNKNOWN"
			val i: Iterator<String> =
				signatureMap!!.keys.iterator()
			while (i.hasNext()) {
				val key = i.next()
				if (matchesSignature(
						signatureMap!![key]!!,
						buffer,
						m
					)
				) {
					fileType = key
					break
				}
			}
			fileType
		} finally {
			`in`.close()
		}
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
