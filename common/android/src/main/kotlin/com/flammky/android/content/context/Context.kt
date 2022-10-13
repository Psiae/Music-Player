package com.flammky.android.content.context

import android.content.Context
import android.content.ContextWrapper

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
