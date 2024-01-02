package dev.dexsr.klio.media.db.realm

import io.realm.kotlin.Realm
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class IncrementalRealmPrimaryKey(
) : RealmObject {

    @PrimaryKey
    var _id: String = ""
    var incrementValue: String = ""

    companion object {
        const val PRIMARY_KEY_VALUE = "klio:media:IncrementalRealmPrimaryKey"
        const val VALUE_SEGMENT_SEPARATOR = ':'
        const val VALUE_PREFIX = "primaryKey${VALUE_SEGMENT_SEPARATOR}incremental"
        const val VALUE_INITIAL = "initial"
    }
}

fun Realm.latestPrimaryKey() = query(
    IncrementalRealmPrimaryKey::class,
    "_id == $0",
    IncrementalRealmPrimaryKey.PRIMARY_KEY_VALUE
)

fun IncrementalRealmPrimaryKey.advance() = apply {
    // fixme: check if we need to copy

    if (incrementValue == IncrementalRealmPrimaryKey.VALUE_INITIAL) {
        incrementValue = 0.toString()
        return@apply
    }
    incrementValue = incrementValue.split('-').last().toLong().inc().toString()
}

fun IncrementalRealmPrimaryKey.toPrimaryKey() = with(IncrementalRealmPrimaryKey) {
    buildString {
        append(VALUE_PREFIX)
        append(VALUE_SEGMENT_SEPARATOR)
        append(incrementValue)
    }
}

fun IncrementalRealmPrimaryKey.toInitial() = apply {
    incrementValue = IncrementalRealmPrimaryKey.VALUE_INITIAL
}

fun IncrementalRealmPrimaryKey.Companion.of(name: String) = IncrementalRealmPrimaryKey()
    .apply {
        _id = "$PRIMARY_KEY_VALUE${VALUE_SEGMENT_SEPARATOR}$name"
    }

fun IncrementalRealmPrimaryKey.Companion.default() = IncrementalRealmPrimaryKey()
    .apply {
        _id = PRIMARY_KEY_VALUE
    }