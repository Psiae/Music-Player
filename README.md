App Name Coming Soon

Simple Music Player `100% Made in Kotlin` with Jetpack Compose & AndroidX Media3.

> This app has somewhat basic playback control both in app and notification and should be able to play any format supported by [ExoPlayer](https://github.com/google/ExoPlayer) corresponds to the current [Media3](https://github.com/androidx/media/) version 

> it filters incoming Intent with scheme: "content://" ; action: "android.intent.action.VIEW" ; mimeType: "audio/*"

> I'm usually active helping mobile-devs in https://discord.gg/programming for self-learning

## Contribution
Due to full-scale rewrite on the codebase it will be quite hard to accept any pull-request, I might consider if it aligns with the current target

Feel free to open an [issue](https://github.com/flammky/Music-Player/issues) for suggestion

## Features
> Audio Playback + Playback Control in App and Notification

> Resolve Audio scan-able by [MediaStore](https://developer.android.com/reference/android/provider/MediaStore)
>
> (if insufficient a Full scanner will be implemented to for manual insert to MediaStore with User permission)

> Audio Metadata Extraction with [JAudioTagger](https://www.jthink.net/jaudiotagger/) 
>
> artwork currently only read to [MP3, FLAC, MP4-AAC, OGG-Vorbis, OGG-Opus, WAV], write is not tested.

> Artwork color generated Playback background on PlaybackController UI with [Palette API](https://developer.android.com/develop/ui/views/graphics/palette-colors) 
>
> (considering to also mix it with the theme accent)

> Material You (Android 12++)

## Planned Features
> Playback Persistence (still thinking whether to implement user remote persistence via Firebase and GDrive)

> Playlist Builder

> Dynamic Queue Builder

> Lyrics

> Metadata writer
>
> will probably shift metadata operation to rust via NDK JNI

> Online Artwork Search

> Integrate `Youtube Music` and `Spotify` API's

> UI theming and ColorScheme generator for API <= 31

> Android Auto variant Support

> Google Cast Support

> `etc.`

## Previews
> Image Preview below will be updated accordingly on any changes made

> Currently only [Library] and [User] screen are in consideration

| Library Screen |
| -------------- |
| <img src="https://user-images.githubusercontent.com/94031495/212595987-07a46e4f-535a-4e89-996a-ee4fcf8155cf.png" width="195"> <img src="https://user-images.githubusercontent.com/94031495/212595992-5a24d0c0-c250-4d64-b595-6445022c5d3d.png" width="195"> <img src="https://user-images.githubusercontent.com/94031495/212595996-2f4ebb8a-ae55-4cb6-a096-776032662325.png" width="195"> <img src="https://user-images.githubusercontent.com/94031495/212596000-9d2cf692-61a1-4a7b-a1df-280867107b3b.png" width="195"> |

| Playback Control on Dark Mode |
| -------------- |
| <img src="https://user-images.githubusercontent.com/94031495/199474607-5a383a6b-9872-4829-9000-8427f622704b.png" width="260"> <img src="https://user-images.githubusercontent.com/94031495/212596001-4453726c-367f-4d0f-99ac-6be0f51ac235.png" width="260"> |

| Permission Pager + Light Mode|
| -------------- |
| <img src="https://user-images.githubusercontent.com/94031495/181867588-5a80bdc8-4be6-47d2-8be9-86098257e395.png" width="195"> <img src="https://user-images.githubusercontent.com/94031495/181867589-6746d1a7-3415-41f4-a22d-7f08060c541c.png" width="195"> <img src="https://user-images.githubusercontent.com/94031495/181867590-2e418c87-d6cd-4303-b8d9-3cbb45224ce8.png" width="195"> <img src="https://user-images.githubusercontent.com/94031495/181867591-c7d20bc7-5b14-43b4-a4d0-b446244653a5.png" width="195"> |

| Permission Pager + Dark Mode|
| -------------- |
| <img src="https://user-images.githubusercontent.com/94031495/181867586-5d661af4-03f2-4911-9fb1-22141d7e69fc.png" width="195"> <img src="https://user-images.githubusercontent.com/94031495/181867587-bcf46f93-9aaa-4ae6-a531-5582efbe073d.png" width="195"> <img src="https://user-images.githubusercontent.com/94031495/181867593-dc71a6cf-9609-4786-bc66-ac8ba9d51b83.png" width="195"> <img src="https://user-images.githubusercontent.com/94031495/181867595-bc00fdf8-d0e7-46cf-a8ab-151b877a8128.png" width="195"> |

credits: https://icons8.com/

## Libraries

* Collections

  > [Kotlinx.Collections.Immutable](https://github.com/Kotlin/kotlinx.collections.immutable)

* Coroutines & Concurrency

  > // For proper concurrency integration with Java-based `androidx.Media3`
  >
  > [Guava](https://github.com/google/guava)
  >
  > [Kotlinx.Coroutines.Guava](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-guava/)

* DI
  
  > // Consider migrating to [Koin](https://github.com/InsertKoinIO/koin) for KSP support
  >
  > [Dagger-Hilt](https://dagger.dev/hilt/)

* Media

  > [Androidx.Media](https://github.com/androidx/media)

  > [ExoPlayer](https://github.com/google/ExoPlayer)


* Persistence

  > [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
 
  > [Room](https://developer.android.com/jetpack/androidx/releases/room)

* Reflection & Serialization

  > [Kotlin.Reflect](https://kotlinlang.org/docs/reflection.html)

  > [Kotlinx.Serialization](https://github.com/Kotlin/kotlinx.serialization)

* SAF
  > [Startup](https://developer.android.com/topic/libraries/app-startup)

* Secure
  > [Androidx.Security](https://developer.android.com/jetpack/androidx/releases/security)

* Tests & Analytics

  > [Leak-Canary](https://square.github.io/leakcanary/)

  > [Timber](https://github.com/JakeWharton/timber)

* UI & Navigation

  > [Accompanist](https://github.com/google/accompanist)

  > [Androidx Navigation](https://developer.android.com/jetpack/androidx/releases/navigation)

  > [Compose-Material](https://developer.android.com/jetpack/androidx/releases/compose-material)

  > [Compose-Material3](https://developer.android.com/jetpack/androidx/releases/compose-material3)

  > [Coil](https://coil-kt.github.io/coil/)

  > [Jetpack Compose](https://developer.android.com/jetpack/compose)

  > [Lottie](https://github.com/airbnb/lottie-android)

  > [Palette API](https://developer.android.com/develop/ui/views/graphics/palette-colors)

  > [Transformers-Coil](https://github.com/wasabeef/transformers)

* `etc.`
