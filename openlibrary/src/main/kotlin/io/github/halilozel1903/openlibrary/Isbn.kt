package io.github.halilozel1903.openlibrary

/** ISBN-10 and ISBN-13 helpers: cleaning, check digits and conversion. */
public object Isbn {

    /** [text] without spaces and hyphens, upper case (the ISBN-10 check digit can be `X`). */
    public fun clean(text: String): String = text.filterNot { it == '-' || it.isWhitespace() }.uppercase()

    /** Whether [text] is a valid ISBN-10 or ISBN-13, check digit included. Hyphens and spaces are ignored. */
    public fun isValid(text: String): Boolean {
        val isbn = clean(text)
        return when (isbn.length) {
            10 -> isValid10(isbn)
            13 -> isValid13(isbn)
            else -> false
        }
    }

    /** The ISBN-13 of [text] (an ISBN-10 gets the `978` prefix and a new check digit), or `null` when it is not valid. */
    public fun toIsbn13(text: String): String? {
        val isbn = clean(text)
        return when {
            isbn.length == 13 && isValid13(isbn) -> isbn
            isbn.length == 10 && isValid10(isbn) -> {
                val body = "978" + isbn.substring(0, 9)
                body + check13(body)
            }
            else -> null
        }
    }

    /** The ISBN-10 of [text], or `null` when it is not valid or is a `979` ISBN-13, which has no ISBN-10. */
    public fun toIsbn10(text: String): String? {
        val isbn = clean(text)
        return when {
            isbn.length == 10 && isValid10(isbn) -> isbn
            isbn.length == 13 && isValid13(isbn) && isbn.startsWith("978") -> {
                val body = isbn.substring(3, 12)
                body + check10(body)
            }
            else -> null
        }
    }

    private fun isValid10(isbn: String): Boolean {
        if (!isbn.substring(0, 9).all { it.isDigit() }) return false
        val last = isbn[9]
        if (!last.isDigit() && last != 'X') return false
        return check10(isbn.substring(0, 9)) == last
    }

    private fun isValid13(isbn: String): Boolean {
        if (!isbn.all { it.isDigit() }) return false
        return check13(isbn.substring(0, 12)) == isbn[12]
    }

    private fun check10(body: String): Char {
        val sum = body.mapIndexed { i, c -> (10 - i) * (c - '0') }.sum()
        val check = (11 - sum % 11) % 11
        return if (check == 10) 'X' else '0' + check
    }

    private fun check13(body: String): Char {
        val sum = body.mapIndexed { i, c -> (c - '0') * if (i % 2 == 0) 1 else 3 }.sum()
        return '0' + (10 - sum % 10) % 10
    }
}
