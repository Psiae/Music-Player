package com.flammky.android.content.intent

import android.content.Intent

fun Intent.isActionMain(): Boolean = this.action == Intent.ACTION_MAIN
fun Intent.isActionView(): Boolean = action == Intent.ACTION_VIEW
fun Intent.putExtraAsString(name: String, value: Any?): Intent = putExtra(name, value.toString())
