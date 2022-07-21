package com.kylentt.musicplayer.common.extenstions

import android.content.Intent

fun Intent.putExtraAsString(name: String, value: Any?): Intent =
	this.putExtra(name, value.toString())
