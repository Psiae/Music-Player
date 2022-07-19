package com.kylentt.musicplayer.common.intent

import android.content.Intent

fun Intent.isActionMain(): Boolean = this.action == Intent.ACTION_MAIN
fun Intent.isActionView(): Boolean = this.action == Intent.ACTION_VIEW
