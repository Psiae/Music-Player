package com.kylentt.musicplayer.medialib.internal.provider.mediastore.base.media

import com.kylentt.musicplayer.common.io.exception.PermissionException

interface MediaEntityProvider<E : MediaStoreEntity, F : MediaStoreFile, M : MediaStoreMetadata, Q : MediaStoreQuery> {

	val mediaItemFactory: MediaItemFactory<E, F, M, Q>

	@Throws(PermissionException::class)
	suspend fun queryEntity(): List<E>
}
