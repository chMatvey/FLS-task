package ru.chudakov

import org.apache.commons.dbcp2.BasicDataSource
import ru.chudakov.data.Action
import ru.chudakov.data.ActionType
import ru.chudakov.data.Profile
import java.sql.Connection
import java.sql.SQLException
import java.sql.Timestamp


class PgDBManager(url: String, username: String, password: String) {
    private val driverClassName = "org.postgresql.Driver"

    private val connectionPool = BasicDataSource()

    private val findProfileByNameQuery = "select * from profiles where name = ?"

    private val createProfileQuery = "insert into profiles(name, password, lastConnectDate) values(?,?,?)"
    private val addCreateProfileActionQuery = "insert into actions(dateCreated, actionType, profile_id) values (?, 'PROFILE_CREATED', ?)"

    private val loginQuery = "update profiles set lastConnectDate = ? where name = ? and password = ?"
    private val addLoginActionQuery = "insert into actions(dateCreated, actionType, profile_id) values(?, 'LOGIN', ?)"

    private val getProfileActionsQuery = "select * from actions inner join profiles on actions.profile_id = profiles.id where profiles.name = ?"

    private val changePasswordQuery = "update profiles set password = ? where name = ? and password = ?"
    private val addChangePasswordActionQuery = "insert into actions(dateCreated, actionType, profile_id) values(?, 'CHANGE_PASSWORD', ?)"

    init {
        connectionPool.driverClassName = driverClassName
        connectionPool.url = url
        connectionPool.username = username
        connectionPool.password = password

        getConnection().createStatement().use {

            it.execute("create table if not exists profiles(" +
                    "id SERIAL primary key, " +
                    "name varchar(50) UNIQUE, " +
                    "password varchar(25), " +
                    "lastConnectDate timestamp" + ");"
            )

            it.execute("DO \$\$\n" +
                    "BEGIN\n" +
                    "    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'action_type') THEN\n" +
                    "        CREATE TYPE action_type AS ENUM('PROFILE_CREATED', 'LOGIN', 'CHANGE_PASSWORD');\n" +
                    "    END IF;\n" +
                    "END\$\$;")

            it.execute("create table if not exists actions(" +
                    "id SERIAL primary key, " +
                    "dateCreated timestamp, " +
                    "actionType action_type, " +
                    "profile_id integer references profiles (id) ON DELETE CASCADE ON UPDATE CASCADE" + ");"
            )
        }

        createProfile("profile", "password")
    }

    private fun getConnection(): Connection = connectionPool.connection

    private fun getCurrentDate(): Timestamp = Timestamp(java.util.Date().time)

    private fun findProfileByName(connection: Connection, name: String): Profile? {
        var result: Profile? = null

        connection.prepareStatement(findProfileByNameQuery).use { statement ->
            statement.setString(1, name)
            val rs = statement.executeQuery()
            if (rs.next()) {
                result = Profile(rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("password"),
                        rs.getDate("lastConnectDate")
                )
            }
        }
        return result
    }

    fun createProfile(name: String, password: String): Boolean {
        val connection = getConnection()
        connection.autoCommit = false

        try {
            connection.prepareStatement(createProfileQuery).use {
                it.setString(1, name)
                it.setString(2, password)
                it.setTimestamp(3, getCurrentDate())
                it.addBatch()
                it.executeBatch()
            }

            val profile = findProfileByName(connection, name)

            connection.prepareStatement(addCreateProfileActionQuery).use {
                it.setTimestamp(1, getCurrentDate())
                it.setInt(2, profile!!.id)
                it.addBatch()
                it.executeBatch()
            }

            connection.commit()
            return true
        } catch (e: SQLException) {
            connection.rollback()
            return false
        } finally {
            connection.close()
        }
    }

    fun getAuthorizationResult(name: String, password: String): Boolean {
        val connection = getConnection()
        connection.autoCommit = false

        try {
            connection.prepareStatement(loginQuery).use {
                it.setTimestamp(1, getCurrentDate())
                it.setString(2, name)
                it.setString(3, password)
                if (it.executeUpdate() == 0) return false
            }

            val profile = findProfileByName(connection, name)

            connection.prepareStatement(addLoginActionQuery).use {
                it.setTimestamp(1, getCurrentDate())
                it.setInt(2, profile!!.id)
                it.addBatch()
                it.executeBatch()
            }

            connection.commit()
            return true
        } catch (e: SQLException) {
            connection.rollback()
            return false
        } finally {
            connection.close()
        }
    }

    fun getProfileActionByUsername(name: String): List<Action> {
        val result = mutableListOf<Action>()

        getConnection().use { connection ->
            connection.autoCommit = true

            connection.prepareStatement(getProfileActionsQuery).use {
                it.setString(1, name)
                val rs = it.executeQuery()
                while (rs.next()) {
                    result.add(Action(
                            rs.getInt("id"),
                            rs.getDate("dateCreated"),
                            ActionType.valueOf(rs.getString("actionType")),
                            null
                    ))
                }
            }
        }
        return result
    }

    fun changePassword(name: String, oldPassword: String, newPassword: String): Boolean {
        val connection = getConnection()
        connection.autoCommit = false

        try {
            connection.prepareStatement(changePasswordQuery).use {
                it.setString(1, newPassword)
                it.setString(2, name)
                it.setString(3, oldPassword)
                if (it.executeUpdate() == 0) return false
            }

            val profile = findProfileByName(connection, name)

            connection.prepareStatement(addChangePasswordActionQuery).use {
                it.setTimestamp(1, getCurrentDate())
                it.setInt(2, profile!!.id)
                it.addBatch()
                it.executeBatch()
            }

            connection.commit()
            return true
        } catch (e: SQLException) {
            connection.rollback()
            return false
        } finally {
            connection.close()
        }
    }
}
