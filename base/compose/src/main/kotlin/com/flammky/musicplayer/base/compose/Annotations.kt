package com.flammky.musicplayer.base.compose

import androidx.compose.runtime.snapshots.Snapshot

/**
 * Denote that the annotated target will read a [Snapshot] target.
 */
@Target(
	AnnotationTarget.FUNCTION,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.PROPERTY_SETTER,
	AnnotationTarget.TYPE,
)
@Retention(AnnotationRetention.SOURCE)
annotation class SnapshotRead()

/**
 * Denote that any read of a [Snapshot] target within the block will trigger a Snapshot Observer.
 */
@Target(
	AnnotationTarget.FUNCTION,
	AnnotationTarget.TYPE,
	AnnotationTarget.TYPE_PARAMETER,
)
@Retention(AnnotationRetention.SOURCE)
annotation class SnapshotReader()

/**
 * Denote that execution of the block will write to a [Snapshot] Target.
 */
@Target(
	AnnotationTarget.FUNCTION,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.SOURCE)
annotation class SnapshotWrite()

/**
 * Denote that execution of the block will write to a [Snapshot] Target.
 */
@Target(
	AnnotationTarget.FUNCTION,
	AnnotationTarget.TYPE,
	AnnotationTarget.TYPE_PARAMETER,
)
@Retention(AnnotationRetention.SOURCE)
annotation class SnapshotWriter()
