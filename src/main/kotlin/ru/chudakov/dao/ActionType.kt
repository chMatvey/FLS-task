package ru.chudakov.dao

enum class ActionType {
    PROFILE_CREATED, LOGIN, CHANGE_PASSWORD;

    fun get(type: String): ActionType? {
        return when (type) {
            "PROFILE_CREATED" -> PROFILE_CREATED
            "LOGIN" -> LOGIN
            "CHANGE_PASSWORD" -> CHANGE_PASSWORD
            else -> null
        }
    }
}
