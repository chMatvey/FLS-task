package ru.chudakov

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class PgDBManagerTest {
    private val pgDBManager = PgDBManager("jdbc:postgresql://localhost:5433/postgres", "postgres", "admin")

    @Test
    fun createProfile() {
        assertTrue(pgDBManager.createProfile("profile"))
    }

    @Test
    fun getAuthorizationResult() {
        assertTrue(pgDBManager.getAuthorizationResult("profile", "password"))
    }

    @Test
    fun getProfileActionByUsername() {
        assertTrue(pgDBManager.getProfileActionByUsername("profile").isNotEmpty())

        assertTrue(pgDBManager.getProfileActionByUsername("profile1").isEmpty())
    }

    @Test
    fun changePassword() {
        assertTrue(pgDBManager.changePassword("profile", "password", "password1"))
    }
}
