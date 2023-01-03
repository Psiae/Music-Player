package com.flammky.musicplayer.base.auth

import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule

@kotlinx.serialization.Serializable
class AuthData internal constructor(
	private val bundles: Map<AuthSerializable, AuthSerializable>
) {

	operator fun <K> get(key: K): AuthSerializable? where K: AuthSerializable = bundles[key]

	@kotlinx.serialization.Serializable(with = AuthDataMapSerializer::class)
	class Map<K: AuthSerializable, out V: AuthSerializable>(keyValues: ImmutableMap<K, V>)
		: ImmutableMap<K, V> by keyValues

	@OptIn(ExperimentalSerializationApi::class)
	private class AuthDataMapSerializer
		: KSerializer<AuthData.Map<AuthSerializable, AuthSerializable>> {
		override fun deserialize(decoder: Decoder): AuthData.Map<AuthSerializable, AuthSerializable> {
			return MapSerializer(
				keySerializer = AuthSerializable.serializer(),
				valueSerializer = AuthSerializable.serializer()
			).deserialize(decoder).toPersistentMap().let { s -> Map(s) }
		}

		override val descriptor: SerialDescriptor = AuthDataMapDescriptor()

		override fun serialize(
			encoder: Encoder,
			value: AuthData.Map<AuthSerializable, AuthSerializable>
		) {
			return MapSerializer(
				keySerializer = AuthSerializable.serializer(),
				valueSerializer = AuthSerializable.serializer()
			).serialize(encoder, value)
		}
	}

	@ExperimentalSerializationApi
	private class AuthDataMapDescriptor() : SerialDescriptor {
		override val annotations: List<Annotation> = super.annotations
		override val elementsCount: Int = 2
		override val isInline: Boolean = super.isInline
		override val isNullable: Boolean = super.isNullable
		override val kind: SerialKind = StructureKind.MAP
		override val serialName: String = "AuthData.PersistentAuthMap"

		@ExperimentalSerializationApi
		override fun getElementAnnotations(index: Int): List<Annotation> {
			require(index >= 0) {
				"getElementAnnotations: Illegal index=$index, serialName=$serialName"
			}
			return emptyList()
		}

		@ExperimentalSerializationApi
		override fun getElementDescriptor(index: Int): SerialDescriptor {
			require(index >= 0) {
				"getElementDescriptor: Illegal index=$index, serialName=$serialName"
			}
			return AuthSerializable.serializer().descriptor
		}

		@ExperimentalSerializationApi
		override fun getElementIndex(name: String): Int = requireNotNull(name.toIntOrNull()) {
			"Invalid Map Index=$name"
		}

		@ExperimentalSerializationApi
		override fun getElementName(index: Int): String {
			require(index >= 0) { "getElementName: Illegal index=$index" }
			return index.toString()
		}

		@ExperimentalSerializationApi
		override fun isElementOptional(index: Int): Boolean {
			require(index >= 0) { "isElementOptional: Illegal index=$index" }
			return false
		}
	}

	companion object {
		val UNSET = AuthData(AuthData.Map(persistentMapOf()))
	}
}

@OptIn(ExperimentalSerializationApi::class)
@kotlinx.serialization.Serializable(with = AuthSerializable.PolymorphicSerializer::class)
abstract class AuthSerializable private constructor() {
	protected constructor(serializedName: String) : this() {}

	object PolymorphicSerializer : KSerializer<AuthSerializable> {

		override val descriptor: SerialDescriptor =
			PolymorphicSerializer(AuthSerializable::class).descriptor

		override fun serialize(encoder: Encoder, value: AuthSerializable) =
			PolymorphicSerializer(AuthSerializable::class).serialize(encoder, value)

		override fun deserialize(decoder: Decoder): AuthSerializable =
			PolymorphicSerializer(AuthSerializable::class).deserialize(decoder)
	}

	companion object {
		val module = SerializersModule {
			InternalAuthModule
				.getAuthObjectSerializerModule()
				.forEach { it.dumpTo(this) }
		}
	}
}
