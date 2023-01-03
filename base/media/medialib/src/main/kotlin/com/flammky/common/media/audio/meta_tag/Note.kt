package com.flammky.musicplayer.common.media.audio.meta_tag

/**
 * Kotlin copy (file to file conversion) of https://www.jthink.net/jaudiotagger/
 *
 * Not familiar with the Library and it was done quickly in a day so many assertion [!!] (expect NPE)
 * is put in place instead of safe call [?]
 *
 * Many changes were needed for backward API compatibility such as certain java.nio.file usage on API < 26.
 *
 * Seems like creating a new meta_tagging library for this App or Android specifically
 * is a `better` solution rather than modifying parts of this library so this will just be a
 * Placeholder or a base to wrap upon
 *
 *
 *																	 Converted Code is untested
 */
private object Note
