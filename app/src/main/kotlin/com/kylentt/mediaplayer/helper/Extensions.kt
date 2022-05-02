package com.kylentt.mediaplayer.helper

object StringExtension {

  @JvmStatic fun String.addPrefix(prefix: String): String {
    return prefix + this
  }

  @JvmStatic fun String.setPrefix(prefix: String): String {
    if (!this.startsWith(prefix)) {
      return this.addPrefix(prefix)
    }
    return this
  }
}
