package dev.dexsr.klio.player.presentation

import dev.dexsr.klio.base.UNSET
import dev.dexsr.klio.base.resource.LocalImage

data class LocalMediaArtwork(
    // whether or not we are allowed to transform the provided artwork,
    // certain provider does not legally allow this
    val allowTransform: Boolean,
    val image: LocalImage<*>
): UNSET<LocalMediaArtwork> by Companion {

    companion object: UNSET<LocalMediaArtwork> {

        override val UNSET: LocalMediaArtwork = LocalMediaArtwork(
            allowTransform = false,
            image = LocalImage.None
        )
    }
}
