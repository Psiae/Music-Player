package com.flammky.musicplayer.common.media.audio.meta_tag.logging

/**
 * For parsing the exact cause of a file exception, because variations not handled well by Java
 */
enum class FileSystemMessage(  // message from a *nix OS
	var msg: String
) {
	ACCESS_IS_DENIED("Access is denied"), PERMISSION_DENIED("Permission denied");

}
