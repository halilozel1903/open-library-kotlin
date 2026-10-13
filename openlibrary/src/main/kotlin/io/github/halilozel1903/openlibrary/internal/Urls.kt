package io.github.halilozel1903.openlibrary.internal

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Percent-encodes a query parameter value; spaces become `%20`. */
internal fun encodeQueryValue(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")

/** Builds `base?k=v&k=v`, skipping null values. */
internal fun buildUrl(base: String, params: List<Pair<String, String?>>): String {
    val query = params
        .filter { it.second != null }
        .joinToString("&") { (key, value) -> "$key=${encodeQueryValue(value!!)}" }
    return if (query.isEmpty()) base else "$base?$query"
}

/** Percent-encodes one path segment. */
internal fun encodePathSegment(value: String): String = encodeQueryValue(value)
