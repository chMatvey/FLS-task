package ru.chudakov

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PgDBManagerUnitTest {
    private val pgDBManager = PgDBManager(DBConfig.url, DBConfig.userName, DBConfig.password)

    private val name = "profile"
    private val password = "password"

    @Test
    fun createProfile() {
        assertTrue(!pgDBManager.createProfile(name, password))
    }

    @Test
    fun getAuthorizationResult() {
        assertTrue(pgDBManager.getAuthorizationResult(name, password))
    }

    @Test
    fun getProfileActionByUsername() {
        assertTrue(pgDBManager.getProfileActionByUsername(name).isNotEmpty())
    }

    @Test
    fun changePassword() {
        assertTrue(pgDBManager.changePassword(name, password, "password"))
    }
}
