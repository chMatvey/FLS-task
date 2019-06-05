package ru.chudakov.data

import java.sql.Date

data class Action(
        val id: Int,
        val dateCreated: Date,
        val actionType: ActionType,
        val profile: Profile?
)
