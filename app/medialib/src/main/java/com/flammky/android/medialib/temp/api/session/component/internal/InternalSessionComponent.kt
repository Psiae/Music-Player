package com.flammky.android.medialib.temp.api.session.component.internal

import com.flammky.android.medialib.temp.api.session.component.SessionComponent
import com.flammky.android.medialib.temp.internal.MediaLibraryContext

internal class InternalSessionComponent(
    override val builder: InternalSessionBuilder,
    override val manager: InternalSessionManager,
    private val libraryContext: MediaLibraryContext,
) : SessionComponent
