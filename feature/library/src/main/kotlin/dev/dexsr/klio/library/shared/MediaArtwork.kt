package dev.dexsr.klio.library.shared

import dev.dexsr.klio.base.UNSET
import dev.dexsr.klio.base.resource.LocalImage
import dev.dexsr.klio.library.compose.ComposeImmutable

@ComposeImmutable
data class LocalMediaArtwork(
    // whether or not we are allowed to transform the provided artwork
	val allowTransform: Boolean,
	// whether or not we are allowed to put another image on top of the artwork UI
	val allowImageOverlay: Boolean,
	// whether or not we are allowed to put any kind of branding Interface on top of the artwork UI
	val allowBrandOverlay: Boolean,
	val image: LocalImage<*>
): UNSET<LocalMediaArtwork> by Companion {

    companion object: UNSET<LocalMediaArtwork> {

        override val UNSET: LocalMediaArtwork = LocalMediaArtwork(
            allowTransform = false,
			allowImageOverlay = false,
			allowBrandOverlay = false,
            image = LocalImage.None
        )
    }
}
