package com.kylentt.disposed.musicplayer.app.util

// TODO()

object Libs {

  object Accompanist {
    const val version = "0.24.3-alpha"
    const val drawablePainter = "com.google.accompanist:accompanist-drawablepainter:$version"
    const val navigationAnimation =
      "com.google.accompanist:accompanist-navigation-animation:$version"
    const val navigationMaterial = "com.google.accompanist:accompanist-navigation-material:$version"
    const val pager = "com.google.accompanist:accompanist-pager:$version"
    const val permissions = "com.google.accompanist:accompanist-permissions:$version"
    const val placeholder = "com.google.accompanist:accompanist-placeholder:$version"
    const val systemUIController = "com.google.accompanist:accompanist-systemuicontroller:$version"
  }

  object Androidx {

    object Compose {

      object ComposeNavigation {
        const val version = "2.5.0-alpha01"
        const val composeNavigation = "androidx.navigation:navigation-compose:$version"
      }

      object ComposeUI {
        const val version = "1.1.1"
        const val foundation = "androidx.compose.foundation:foundation:$version"
        const val materialIconsCore = "androidx.compose.material:material-icons-core:$version"
        const val materialIconsExtended =
          "androidx.compose.material:material-icons-extended:$version"
      }

      object Material3 {
        const val version = "1.0.0-alpha07"
        const val composeMaterial3 = "androidx.compose.material3:material3:$version"
      }

      object SplashScreen {
        const val version = "1.0.0-beta01"
        const val coreSplashScreen = "androidx.core:core-splashscreen:$version"
      }
    }

    object Room {
      const val version = "2.4.2"
      const val runtime = "androidx.room:room-runtime:$version"
      const val ktx = "androidx.room:room-ktx:$version"
      const val compiler = "androidx.room:room-compiler:$version"
    }
  }

  object Coil {
    const val version = "1.4.0"
    const val coil = "io.coil-kt:coil:$version"
    const val coilCompose = "io.coil-kt:coil-compose:$version"

    object CoilTransformer {
      const val version = "1.0.4"
      const val coil = "jp.wasabeef.transformers:coil:$version"
      const val coilGPU = "jp.wasabeef.transformers:coil-gpu:$version"
    }
  }

  object Dagger {

    object DaggerHilt {
      const val version = "2.40.5"
      const val hilt = "com.google.dagger:hilt-android:$version"
      const val compiler = "com.google.dagger:hilt-android-compiler:$version"
    }

    object HiltLifecycle {
      const val version = "1.0.0-alpha03"
      const val viewmodel = "androidx.hilt:hilt-lifecycle-viewmodel:$version"
    }

    object HiltCompose {
      const val version = "1.0.0"
      const val navigation = "androidx.hilt:hilt-navigation-compose:$version"
    }
  }

  object Media {

    object ExoPlayer {
      const val version = "2.17.1"
      const val extMediaSession = "com.google.android.exoplayer:extension-mediasession:$version"
    }

    object Media3 {
      const val version = "1.0.0-alpha03"
      const val exoplayer = "androidx.media3:media3-exoplayer:$version"
      const val mediaSession = "androidx.media3:media3-session:$version"
      const val mediaCommon = "androidx.media3:media3-common:$version"
      const val mediaUI = "androidx.media3:media3-ui:$version"
    }
  }

  object Tagger {
    const val version = "2.3.15"
    const val jaudioTagger = "com.github.Adonai:jaudiotagger:$version"
  }

  object Timber {
    const val version = "5.0.1"
    const val timber = "com.jakewharton.timber:timber:$version"
  }

}
