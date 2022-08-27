package com.kylentt.musicplayer.domain.musiclib.annotation

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER)
annotation class StorageDataValue(val unit: StorageDataUnit)
enum class StorageDataUnit {
	BIT,
	BYTE,
	KB,
	KiB,
	MB,
	MiB,
	GB,
	GiB,
	TB,
	TiB,
	PB,
	PiB,
	EB,
	EiB
}
