package com.flammky.mediaplayer.ui.activity

object CollectionExtension {

  @JvmStatic
  inline fun <T> MutableList<T>.forEachClear(each: (T) -> Unit) {
    forEach { each(it) }
    clear()
  }

  @JvmStatic
  inline fun <T> MutableList<T>.forEachClearSync(lock: Any = this, each: (T) -> Unit) {
    synchronized(lock) {
      forEach { each(it) }
      clear()
    }
  }

	@JvmStatic
	@Suppress("NOTHING_TO_INLINE")
	inline fun <T> MutableList< () -> T >.forEachClearSync(lock: Any = this) {
		synchronized(lock) {
			forEach { it() }
			clear()
		}
	}
}



