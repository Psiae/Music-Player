
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.AudioFileIO
import java.io.File

object M {

	@JvmStatic
	fun main(args: Array<String>) {
		testWav()
	}
	private fun testOgg() {
		val opusFile = File("C:/Users/Kyle/Desktop/Dev/Media/Audio/OGG/OGG-sample-valid.ogg")
		val af = AudioFileIO.readMagic(opusFile)
		println("artDataSize=${af.tag?.firstArtwork?.binaryData?.size}")
	}

	private fun testOggOPUS() {
		val opusFile = File("C:/Users/Kyle/Desktop/Dev/Media/Audio/OGG/OGG.OPUS-sample-valid.opus")
		val af = AudioFileIO.readMagic(opusFile)
		println("artDataSize=${af.tag?.firstArtwork?.binaryData?.size}")
	}

	private fun testOggOPUSOggExtension() {
		val opusFile = File("C:/Users/Kyle/Desktop/Dev/Media/Audio/OGG/OGG.OPUS.ogg-sample-valid.ogg")
		val af = AudioFileIO.readMagic(opusFile)
		println("artDataSize=${af.tag?.firstArtwork?.binaryData?.size}")
	}

	private fun testM4A() {
		val testFile = File("C:/Users/Kyle/Desktop/Dev/Media/Audio/M4A/M4A-AAC-sample-convert.m4a")
		val af = AudioFileIO.readMagic(testFile)
		println("artDataSize=${af.tag?.firstArtwork?.binaryData?.size}")
	}

	private fun testWav() {
		val testFile = File("C:/Users/Kyle/Desktop/Dev/Media/Audio/WAV/WAV-sample-valid.wav")
		val af = AudioFileIO.readMagic(testFile)
		println("artDataSize=${af.tag?.firstArtwork?.binaryData?.size}")
	}
}
