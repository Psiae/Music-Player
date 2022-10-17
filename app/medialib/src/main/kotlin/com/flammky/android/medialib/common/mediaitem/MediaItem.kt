package com.flammky.android.medialib.common.mediaitem

import android.net.Uri
import android.os.Bundle
import com.flammky.android.medialib.common.mediaitem.MediaItem.Companion
import com.flammky.android.medialib.context.LibraryContext
import com.flammky.android.medialib.context.internal.InternalLibraryContext

/**
 * MediaItem Container, Implementations are thread-safe.
 *
 * @see [Companion.build] to build instances
 */
sealed class MediaItem(

	/**
	 * The Id
	 */
	@JvmField
	val mediaId: String,

	/**
	 * The Uri
	 */
	@JvmField
	val mediaUri: Uri,

	/**
	 * Metadata Information
	 *
	 * @see [MediaMetadata]
	 */
	@JvmField
	val metadata: MediaMetadata,

	/**
	 * Extra Configuration
	 */
	@JvmField
	val extra: Extra
) {

	/**
	 * Class for containing extra configuration
	 */
	class Extra @JvmOverloads constructor(val bundle: Bundle = Bundle()) {
		internal val internalBundle: Bundle = Bundle()

		companion object {
			@JvmStatic
			fun Bundle.toMediaItemExtra(): Extra = Extra(this)

			@JvmStatic
			val UNSET = Extra(Bundle.EMPTY)
		}
	}

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

	object UNSET : MediaItem(
		mediaId = "UNSET",
		mediaUri = Uri.EMPTY,
		metadata = MediaMetadata.UNSET,
		extra = Extra(Bundle.EMPTY)
	)

	companion object {

		/**
		 * Build MediaItem using given [context]
		 *
		 * @return instance of [MediaItem]
		 */
		@JvmStatic
		fun build(context: LibraryContext, apply: Builder.() -> Unit): MediaItem {
			val internal = context as InternalLibraryContext
			return RealMediaItem.build(internal.mediaItemBuilder, apply)
		}

		/**
		 * Build MediaItem using given [LibraryContext]
		 *
		 * @return instance of [MediaItem]
		 */
		@JvmStatic
		fun LibraryContext.buildMediaItem(apply: Builder.() -> Unit): MediaItem = build(this, apply)
	}
}

internal class RealMediaItem private constructor(
	mediaId: String,
	mediaUri: Uri,
	metadata: MediaMetadata,
	extra: Extra,
	private val internalBuilder: InternalBuilder
) : MediaItem(mediaId, mediaUri, metadata, extra) {

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
