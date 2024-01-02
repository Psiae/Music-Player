package dev.dexsr.klio.library.shared

import dev.dexsr.klio.base.UNSET

data class PlaybackMediaDescription(
    val title: String?,
    val subtitle: String?
): UNSET<PlaybackMediaDescription> by Companion {

    companion object : UNSET<PlaybackMediaDescription> {

        override val UNSET: PlaybackMediaDescription = PlaybackMediaDescription(
            title = null,
            subtitle = null,
        )
    }
}
