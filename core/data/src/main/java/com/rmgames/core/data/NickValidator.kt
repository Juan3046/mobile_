package com.rmgames.core.data

/** Nick rules: 3–12 chars, letters/numbers/underscore. Uniqueness is checked in Firestore. */
object NickValidator {

    private val NICK = Regex("^[A-Za-z0-9_]{3,12}$")

    fun isValid(nick: String): Boolean = NICK.matches(nick)
}
