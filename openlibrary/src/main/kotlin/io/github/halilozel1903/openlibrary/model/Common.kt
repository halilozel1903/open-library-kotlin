package io.github.halilozel1903.openlibrary.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Open Library text fields come in two shapes: a plain string (`"description": "A novel..."`) or a typed value
 * (`"description": {"type": "/type/text", "value": "A novel..."}`). Dates do the same with `/type/datetime`.
 * This serializer reads both as a [String] and writes a plain string.
 */
public object OpenLibraryTextSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("io.github.halilozel1903.openlibrary.Text", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val json = decoder as? JsonDecoder ?: return decoder.decodeString()
        return textOf(json.decodeJsonElement()) ?: ""
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }

    /** The text of [element]: a string, the `value` of a typed object, or `null` for anything else. */
    public fun textOf(element: JsonElement): String? = when (element) {
        is JsonNull -> null
        is JsonPrimitive -> element.contentOrNull
        is JsonObject -> (element["value"] as? JsonPrimitive)?.contentOrNull
        is JsonArray -> null
    }
}

/**
 * A link to another record, `{"key": "/authors/OL26320A"}`. Older records sometimes store only the key string;
 * both decode to a [KeyRef].
 */
@Serializable(with = KeyRefSerializer::class)
public data class KeyRef(
    /** The record key, such as `/authors/OL26320A` or `/languages/eng`. */
    public val key: String,
) {
    /** The last part of [key]: an OLID (`OL26320A`) or a code (`eng`). */
    public val id: String get() = key.substringAfterLast('/')
}

/** Reads `{"key": "..."}` or a bare `"..."` as a [KeyRef]. */
public object KeyRefSerializer : KSerializer<KeyRef> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("io.github.halilozel1903.openlibrary.KeyRef", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): KeyRef {
        val json = decoder as? JsonDecoder ?: return KeyRef(decoder.decodeString())
        return when (val element = json.decodeJsonElement()) {
            is JsonPrimitive -> KeyRef(element.content)
            is JsonObject -> KeyRef(element["key"]?.jsonPrimitive?.contentOrNull ?: "")
            else -> KeyRef("")
        }
    }

    override fun serialize(encoder: Encoder, value: KeyRef) {
        encoder.encodeString(value.key)
    }
}

/** An external link of a work or author. */
@Serializable
public data class Link(
    public val title: String? = null,
    public val url: String = "",
)

/**
 * Decodes a JSON array element by element and leaves out elements that do not decode, so one odd record in a page
 * of editions or search results does not fail the whole page. Open Library's data is edited by volunteers and a
 * few records have unexpected shapes.
 */
public open class LenientListSerializer<T>(private val element: KSerializer<T>) : KSerializer<List<T>> {
    private val strict = ListSerializer(element)
    override val descriptor: SerialDescriptor = strict.descriptor

    override fun deserialize(decoder: Decoder): List<T> {
        val json = decoder as? JsonDecoder ?: return strict.deserialize(decoder)
        val array = json.decodeJsonElement() as? JsonArray ?: return emptyList()
        return array.mapNotNull { item ->
            try {
                json.json.decodeFromJsonElement(element, item)
            } catch (e: IllegalArgumentException) {
                // SerializationException is an IllegalArgumentException.
                null
            }
        }
    }

    override fun serialize(encoder: Encoder, value: List<T>) {
        strict.serialize(encoder, value)
    }
}

/** `links` of a paged list such as `/works/{id}/editions.json`. */
@Serializable
public data class PageLinks(
    public val self: String? = null,
    public val work: String? = null,
    public val author: String? = null,
    public val next: String? = null,
    @SerialName("prev") public val previous: String? = null,
)

/** A positive id from [ids], skipping the `-1` placeholders Open Library leaves after removed covers. */
internal fun firstPositive(ids: List<Long>): Long? = ids.firstOrNull { it > 0 }

/** The first four digit year in [date], such as 1965 in `"June 1965"` or `"1965-08-01"`. */
internal fun yearIn(date: String?): Int? =
    date?.let { Regex("""(?<!\d)(\d{4})(?!\d)""").find(it)?.groupValues?.get(1)?.toIntOrNull() }
