package com.flammky.android.medialib.common.mediaitem

import android.net.Uri
import android.os.Bundle
import com.flammky.android.medialib.common.mediaitem.MediaItem.Companion
import com.flammky.android.medialib.context.LibraryContext

/**
 * MediaItem Container, internal Implementations are thread-safe.
 *
 * @see [Companion.build] to build instances
 */
abstract class MediaItem internal constructor() {

	/**
	 * The Id
	 */
	abstract val mediaId: String

	/**
	 * The Uri
	 */
	abstract val mediaUri: Uri

	/**
	 * Metadata Information
	 *
	 * @see [MediaMetadata]
	 */
	abstract val metadata: MediaMetadata

	/**
	 * Extra Configuration
	 */
	abstract val extra: Extra

	class Extra(val bundle: Bundle = Bundle())

	/**
	 * Builder Interface for MediaItem
	 */
	interface Builder {
		val mediaId: String
		val mediaUri: Uri
		val metadata: MediaMetadata
		val extra: Extra

		fun setMediaId(id: String): Builder
		fun setMediaUri(uri: Uri): Builder
		fun setMetadata(metadata: MediaMetadata): Builder
		fun setExtra(extra: Extra): Builder
		fun build(): MediaItem
	}

	object UNSET : MediaItem() {
		override val mediaId: String = "UNSET"
		override val mediaUri: Uri = Uri.EMPTY
		override val metadata: MediaMetadata = MediaMetadata.UNSET
		override val extra: Extra = Extra(Bundle.EMPTY)
	}

	companion object {

		/**
		 * Build MediaItem using given [context]
		 *
		 * @return instance of [MediaItem]
		 */
		@JvmStatic
		fun build(context: LibraryContext, apply: Builder.() -> Unit): MediaItem {
			return RealMediaItem.build(context.internal.mediaItemBuilder, apply)
		}

		@JvmStatic
		fun LibraryContext.buildMediaItem(apply: Builder.() -> Unit): MediaItem {
			return build(this, apply)
		}
	}
}

internal data class RealMediaItem @Suppress("DataClassPrivateConstructor") private constructor(
	override val mediaId: String,
	override val mediaUri: Uri,
	override val metadata: MediaMetadata,
	override val extra: Extra,
	private val internalBuilder: InternalBuilder
) : MediaItem() {

	// calling copy will recreate this instance
	internal val internalItem: InternalMediaItem = internalBuilder.build(this)

	class Builder internal constructor(
		private val internalBuilder: InternalMediaItem.Builder
	): MediaItem.Builder {

		override var mediaId: String = ""
			private set

		override var mediaUri: Uri = Uri.EMPTY
			private set

		override var metadata: MediaMetadata = MediaMetadata.UNSET
			private set

		override var extra: Extra = Extra()
			private set

		override fun setMediaId(id: String) = apply {
			this.mediaId = id
		}
		override fun setMediaUri(uri: Uri) = apply {
			this.mediaUri = uri
		}
		override fun setMetadata(metadata: MediaMetadata) = apply {
			this.metadata = metadata
		}
		override fun setExtra(extra: Extra): MediaItem.Builder = apply {
			this.extra = extra
		}

		override fun build(): RealMediaItem = RealMediaItem(mediaId, mediaUri, metadata, extra) {
			internalBuilder.build(extra, mediaId, mediaUri, metadata)
		}
	}

	/**
	 * Builder Interface to build our internal entity for the given [RealMediaItem],
	 */
	internal fun interface InternalBuilder {
		fun build(item: RealMediaItem): InternalMediaItem
	}

	companion object {
		@JvmStatic
		fun build(internalBuilder: InternalMediaItem.Builder, apply: Builder.() -> Unit): RealMediaItem {
			return Builder(internalBuilder).apply(apply).build()
		}
	}
}
