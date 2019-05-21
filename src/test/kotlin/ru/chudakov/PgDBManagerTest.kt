package ru.chudakov

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class PgDBManagerTest {
    private val pgDBManager = PgDBManager("jdbc:postgresql://localhost:5433/postgres", "postgres", "admin")

    @Test
    fun createProfile() {
        pgDBManager.createProfile("profile", "password")
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
