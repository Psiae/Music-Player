package com.flammky.android.medialib.temp.common.intent

import android.content.Intent

fun Intent.isActionMain(): Boolean = this.action == Intent.ACTION_MAIN
fun Intent.isActionView(): Boolean = this.action == Intent.ACTION_VIEW
fun Intent.putExtraAsString(name: String, value: Any?): Intent = putExtra(name, value.toString())
