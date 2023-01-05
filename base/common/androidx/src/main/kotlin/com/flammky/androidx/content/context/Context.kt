package com.flammky.androidx.content.context

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.ContextWrapper

fun Context.findActivity(): Activity? {
	return if (this is ContextWrapper) {
		unwrapUntil { it is Activity } as? Activity
	} else {
		null
	}
}

fun Context.findService(): Service? {
	return if (this is ContextWrapper) {
		unwrapUntil { it is Service } as? Service
	} else {
		null
	}
}

fun ContextWrapper.findBase() = unwrapUntil { it !is ContextWrapper }!!

fun ContextWrapper.unwrap() = baseContext

fun ContextWrapper.unwrapUntil(predicate: (Context) -> Boolean): Context? {
	var context: Context = this
	while (context is ContextWrapper) {
		if (predicate(context)) return context
		context = context.unwrap()
	}
	return if (predicate(context)) context else null
}
