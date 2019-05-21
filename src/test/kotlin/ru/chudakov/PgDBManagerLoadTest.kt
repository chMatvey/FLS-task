package ru.chudakov

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.random.Random

class PgDBManagerLoadTest {
    private val pgDBManager = PgDBManager(DBConfig.url, DBConfig.userName, DBConfig.password)

    private val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    private val coroutineCount = 1000

    private fun userAction() {
        val userName = (1..Random.nextInt(3, 10))
                .map { _ -> Random.nextInt(0, charPool.size) }
                .map(charPool::get)
                .joinToString("");

        if (pgDBManager.createProfile(userName, "password")) {
            println("Create new profile: $userName")

            pgDBManager.getAuthorizationResult(userName, "password")
            pgDBManager.changePassword(userName, "password", "password1")
        }
    }

    @Test
    fun userActionTest() = runBlocking {
        repeat(coroutineCount) {
            launch { userAction() }
        }
    }
}
