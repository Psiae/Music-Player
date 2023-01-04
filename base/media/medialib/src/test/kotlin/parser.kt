
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.AudioFileIO
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagField
import java.io.File

object M {

	@JvmStatic
	fun main(args: Array<String>) {
		val testFile = File("C:/Users/Kyle/Desktop/Dev/Media/Audio/M4A/M4A-AAC-sample-convert.m4a")
		val tags = mutableListOf<TagField?>()
		val af = AudioFileIO.readMagic(testFile)
		val art = af.tag?.fields?.forEach {
			tags.add(it)
		}
		println("$testFile=${tags.joinToString { it?.toDescriptiveString() ?: ""}}")
		println(af.tag?.firstArtwork?.width)
	}
}
