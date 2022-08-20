package com.kylentt.musicplayer.domain.musiclib.annotation

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER)
annotation class StorageDataUnitValue(val unit: DataStorageUnit)

enum class DataStorageUnit {
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
