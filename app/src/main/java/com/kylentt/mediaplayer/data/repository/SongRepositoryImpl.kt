package com.kylentt.mediaplayer.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.kylentt.mediaplayer.data.source.local.LocalSource
import com.kylentt.mediaplayer.data.source.local.LocalSourceImpl
import com.kylentt.mediaplayer.domain.model.Song
import com.kylentt.mediaplayer.domain.model.getDisplayTitle
import jp.wasabeef.transformers.coil.CropSquareTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream


// Local Repository for available Song

class SongRepositoryImpl(
    private val source: LocalSourceImpl,
    private val context: Context,
    private val coil: ImageLoader
) : SongRepository {

    var songList = listOf<Song>()

    override suspend fun getSongs(): Flow<List<Song>> {
        return flow {
            emit(songList)
            source.fetchSong().collect {
            songList = it
            emit(it)
        } }
    }

    fun fetchSongs(): Flow<List<Song>> {
        return flow { source.fetchSong().collect { songList = it ; emit(it)} }
    }

    suspend fun fetchMetaFromUri(uri: Uri) = withContext(Dispatchers.Default) {
        val mtr = MediaMetadataRetriever()
        mtr.setDataSource(context.applicationContext, uri)
        val artist = mtr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        val album = mtr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
        val title = mtr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        val pict = mtr.embeddedPicture
        val art = pict?.let {
            BitmapFactory.decodeByteArray(pict, 0, pict.size).squareWithCoil()
        }
        val bArr = art?.let {
            val stream = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.toByteArray()
        }
        val item = MediaItem.Builder().setUri(uri).setMediaMetadata(
            MediaMetadata.Builder()
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkData(bArr, MediaMetadata.PICTURE_TYPE_MEDIA)
                .setTitle(title)
                .setDisplayTitle(title)
                .setSubtitle(artist ?: album)
                .setMediaUri(uri)
                .build()
        ).build()
        Timber.d("SongRepository Handled Uri to MediaItem ${item.getDisplayTitle}")
        item
    }

    private suspend fun Bitmap.squareWithCoil(): Bitmap? {
        val req = ImageRequest.Builder(context.applicationContext)
            .diskCachePolicy(CachePolicy.DISABLED)
            .transformations(CropSquareTransformation())
            .size(256)
            .scale(Scale.FILL)
            .data(this)
            .build()
        return ((coil.execute(req).drawable) as BitmapDrawable?)?.bitmap
    }
}