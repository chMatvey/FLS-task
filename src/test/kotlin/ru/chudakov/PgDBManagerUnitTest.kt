package ru.chudakov

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class PgDBManagerUnitTest {
    private val pgDBManager = PgDBManager(DBConfig.url, DBConfig.userName, DBConfig.password)

    @Test
    fun createProfile() {
        assertTrue(!pgDBManager.createProfile("profile", "password"))
    }

    @Test
    fun getAuthorizationResult() {
        assertTrue(pgDBManager.getAuthorizationResult("profile", "password"))
    }

    @Test
    fun getProfileActionByUsername() {
        assertTrue(pgDBManager.getProfileActionByUsername("profile").isNotEmpty())
    }

    @Test
    fun changePassword() {
        assertTrue(pgDBManager.changePassword("profile", "password", "password"))
    }
}
