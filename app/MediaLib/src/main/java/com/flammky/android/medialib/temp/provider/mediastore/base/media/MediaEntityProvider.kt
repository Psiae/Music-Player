package com.flammky.android.medialib.temp.provider.mediastore.base.media

import com.flammky.android.common.io.exception.PermissionException

interface MediaEntityProvider<E : MediaStoreEntity, F : MediaStoreFile, M : MediaStoreMetadata, Q : MediaStoreQuery> {

	val mediaItemFactory: MediaItemFactory<E, F, M, Q>

	@Throws(PermissionException::class)
	suspend fun queryEntity(): List<E>
}
