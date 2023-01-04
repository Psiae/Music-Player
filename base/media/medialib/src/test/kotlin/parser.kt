
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.AudioFileIO
import java.io.File

object M {

	@JvmStatic
	fun main(args: Array<String>) {
		val testFile = File("C:/Users/Kyle/Desktop/Dev/Media/Audio/M4A/M4A-AAC-sample-convert.m4a")
		val af = AudioFileIO.readMagic(testFile)
		println("artDataSize=${af.tag?.firstArtwork?.binaryData?.size}")
	}
}
