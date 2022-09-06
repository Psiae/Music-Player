package com.kylentt.musicplayer.medialib.api.session.component.internal

import com.kylentt.musicplayer.medialib.api.session.component.SessionComponent
import com.kylentt.musicplayer.medialib.internal.MediaLibraryContext

internal class InternalSessionComponent(
	override val builder: InternalSessionBuilder,
	override val manager: InternalSessionManager,
	private val libraryContext: MediaLibraryContext,
) : SessionComponent
