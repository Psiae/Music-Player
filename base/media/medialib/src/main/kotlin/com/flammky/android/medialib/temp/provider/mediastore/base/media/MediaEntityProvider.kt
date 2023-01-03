package com.flammky.android.medialib.temp.provider.mediastore.base.media

import com.flammky.android.common.io.exception.PermissionException
import com.flammky.android.medialib.temp.api.provider.mediastore.MediaStoreProvider

interface MediaEntityProvider<E : MediaStoreEntity, F : MediaStoreFile, M : MediaStoreMetadata, Q : MediaStoreQuery> {

	val mediaItemFactory: MediaItemFactory<E, F, M, Q>

	/**
	 * Query the Entity
	 *
	 * @param cacheAllowed Whether the provider is allowed to return cached list from its previous query.
	 *
	 * &nbsp
	 *
	 * Cached list will not be returned when:
	 * + [cacheAllowed] is false
	 * + the current cache needs to be updated
	 */
	@Throws(PermissionException::class)
	suspend fun queryEntity(cacheAllowed: Boolean): List<E>

	fun registerOnContentChangedListener(listener: MediaStoreProvider.OnContentChangedListener)

	fun unregisterOnContentChangedListener(listener: MediaStoreProvider.OnContentChangedListener)
}
