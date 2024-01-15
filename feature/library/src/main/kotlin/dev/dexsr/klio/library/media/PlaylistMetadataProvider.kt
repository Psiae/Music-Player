package dev.dexsr.klio.library.media

import android.net.Uri
import com.flammky.android.medialib.common.mediaitem.AudioFileMetadata
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.providers.metadata.VirtualFileMetadata
import com.flammky.musicplayer.library.dump.localmedia.data.LocalSongModel
import com.flammky.musicplayer.library.dump.localmedia.ui.LocalSongViewModel
import dev.dexsr.klio.android.base.resource.AndroidLocalImage
import dev.dexsr.klio.base.resource.LocalImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration

interface PlaylistMetadataProvider {

	fun requestMetadataAsync(trackId: String): Deferred<Result<PlaylistTrackMetadata>>
	fun requestArtworkAsync(trackId: String): Deferred<Result<PlaylistTrackArtwork>>

	fun observeTrackMetadata(trackId: String): Flow<PlaylistTrackMetadata>
	fun observeTrackArtwork(trackId: String): Flow<PlaylistTrackArtwork>

	fun cachedMetadata(trackId: String): PlaylistTrackMetadata?
	fun cachedArtwork(trackId: String): PlaylistTrackArtwork?

	fun dispose()
}

// fixme: currently metadata resolver has no interface to check whether any tag signature is present
@Deprecated("dependency implementation is not ideal")
internal class RealOldPlaylistMetadataProvider(
	private val vm: LocalSongViewModel,
	private val lifetime: Job
): PlaylistMetadataProvider {

	private val coroutineScope = CoroutineScope(SupervisorJob(lifetime))

	override fun requestMetadataAsync(trackId: String): Deferred<Result<PlaylistTrackMetadata>> {
		return coroutineScope.async(Dispatchers.IO) {
			runCatching {
				val data = vm
					.requestMetadata(trackId).getOrThrow()
				data
					?.let {
						PlaylistTrackMetadata(
							data.findTitle() ?: "TITLE_NONE",
							data.findSubtitle() ?: "SUBTITLE_NONE",
							duration = data.duration ?: Duration.ZERO,
							isNone = false
						)
					}
					?: PlaylistTrackMetadata(
						"TITLE_NONE",
						"TITLE_NONE",
						duration = Duration.ZERO,
						isNone = false
					)
			}
		}
	}

	override fun requestArtworkAsync(trackId: String): Deferred<Result<PlaylistTrackArtwork>> {
		return coroutineScope.async(Dispatchers.IO) {
			runCatching {
				val data = vm
					.requestArtwork(trackId).getOrThrow()
				PlaylistTrackArtwork(
					localImage = data?.let { AndroidLocalImage.Bitmap(data) } ?: LocalImage.None,
					isNone = false
				)
			}
		}
	}

	override fun observeTrackMetadata(trackId: String): Flow<PlaylistTrackMetadata> {
		return flow {
			val channel = Channel<PlaylistTrackMetadata>(Channel.CONFLATED)

			val task = coroutineScope.launch(Dispatchers.Main.immediate) {
				vm
					.collectMetadata(LocalSongModel(trackId, null, Uri.EMPTY, MediaMetadata.UNSET))
					.map { data ->
						data
							?.let {
								PlaylistTrackMetadata(
									data.findTitle() ?: "TITLE_NONE",
									data.findSubtitle() ?: "SUBTITLE_NONE",
									duration = data.duration ?: Duration.ZERO,
									isNone = false
								)
							}
							?: PlaylistTrackMetadata(
								"TITLE_NONE",
								"SUBTITLE_NONE",
								duration = Duration.ZERO,
								isNone = false
							)
					}
					.collect(channel::send)
				channel.close()
			}
			try {
				for (element in channel) {
					emit(element)
				}
			} finally {
			    task.cancel()
			}
		}
	}

	override fun observeTrackArtwork(trackId: String): Flow<PlaylistTrackArtwork> {
		return flow {
			val channel = Channel<PlaylistTrackArtwork>(Channel.CONFLATED)

			val task = coroutineScope.launch(Dispatchers.Main.immediate) {
				vm
					.observeArtwork(LocalSongModel(trackId, null, Uri.EMPTY, MediaMetadata.UNSET))
					.map { data ->
						data
							?.let {
								PlaylistTrackArtwork(AndroidLocalImage.Bitmap(data), false)
							}
							?: PlaylistTrackArtwork(LocalImage.None, false)
					}
					.collect(channel::send)
				channel.close()
			}
			try {
				for (element in channel) {
					emit(element)
				}
			} finally {
				task.cancel()
			}
		}
	}

	override fun cachedMetadata(trackId: String): PlaylistTrackMetadata? {
		if (!coroutineScope.isActive) return null
		val data = vm.cachedMetadata(trackId)
		return data
			?.let {
				PlaylistTrackMetadata(
					data.findTitle() ?: "TITLE_NONE",
					data.findSubtitle() ?: "SUBTITLE_NONE",
					duration = data.duration ?: Duration.ZERO,
					isNone = false
				)
			}
	}

	override fun cachedArtwork(trackId: String): PlaylistTrackArtwork? {
		if (!coroutineScope.isActive) return null
		val data = vm.cachedArtwork(trackId)
		return data
			?.let {
				PlaylistTrackArtwork(AndroidLocalImage.Bitmap(data), false)
			}
	}

	override fun dispose() {
		coroutineScope.cancel("disposed by dispose call")
	}

	private fun MediaMetadata.findTitle(): String? = title?.ifBlank { null }
	private fun MediaMetadata.findSubtitle(): String? = (this as? AudioMetadata)
		?.let {
			it.albumArtistName ?: it.artistName
		}
		?: (this as? AudioFileMetadata)?.file
			?.let { fileMetadata ->
				fileMetadata.fileName?.ifBlank { null }
					?: (fileMetadata as? VirtualFileMetadata)?.uri?.toString()
			}
			?.ifBlank { null }
}
