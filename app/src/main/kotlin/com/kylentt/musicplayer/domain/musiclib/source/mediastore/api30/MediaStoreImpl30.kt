package com.kylentt.musicplayer.domain.musiclib.source.mediastore.api30

import android.content.Context
import com.kylentt.musicplayer.common.android.context.ContextInfo
import com.kylentt.musicplayer.common.coroutines.CoroutineDispatchers
import com.kylentt.musicplayer.domain.musiclib.source.mediastore.MediaStoreImplBase

internal class MediaStoreImpl30(private val context: Context) : MediaStoreImplBase(context) {
	override val dispatchers: CoroutineDispatchers = super.dispatchers
	override val contextInfo: ContextInfo = super.contextInfo
	override val audioEntityQueryProjector = super.audioEntityQueryProjector
	override val audioEntityFileInfoProjector = super.audioEntityFileInfoProjector
	override val audioEntityMetadataInfoProjector: Array<String> = super.audioEntityMetadataInfoProjector
}
