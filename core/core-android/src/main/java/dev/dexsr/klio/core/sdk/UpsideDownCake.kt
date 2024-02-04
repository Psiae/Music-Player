package dev.dexsr.klio.core.sdk

import com.flammky.musicplayer.core.sdk.BuildCode

// we don't target this API yet, for build tools compatibility
object UpsideDownCake : AndroidAPI() {
    const val BUILD_CODE_INT = 34
    override val buildcode: BuildCode = BuildCode(34)
}

val AndroidAPI.upsideDownCake
    get() = UpsideDownCake