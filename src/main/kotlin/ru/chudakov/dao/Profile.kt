package ru.chudakov.dao

import java.sql.Date

data class Profile(
        val id: Int,
        val name: String,
        val password: String,
        val lastConnectDate: Date
)
