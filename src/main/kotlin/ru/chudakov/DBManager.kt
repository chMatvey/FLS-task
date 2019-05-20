package ru.chudakov

import ru.chudakov.dao.Action

abstract class DBManager(val url: String) {

    abstract val driverClassName: String

    abstract fun createProfile(name: String, password: String = "password"): Boolean

    abstract fun getAuthorizationResult(name: String, password: String): Boolean

    abstract fun getProfileActionByUsername(name: String): List<Action>

    abstract fun changePassword(name: String, oldPassword: String, newPassword: String): Boolean
}
