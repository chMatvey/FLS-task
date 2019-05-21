package ru.chudakov

import org.apache.commons.dbcp2.BasicDataSource
import ru.chudakov.dao.Action
import ru.chudakov.dao.ActionType
import ru.chudakov.dao.Profile
import java.sql.Connection
import java.sql.Date
import java.sql.SQLException


class PgDBManager(url: String, username: String, password: String) {
    private val driverClassName = "org.postgresql.Driver"

    private val connectionPool = BasicDataSource()

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
                    "profile_id integer references profiles (id)" + ");"
            )
        }
    }

    private fun getConnection(): Connection = connectionPool.connection

    private fun getCurrentDate(): Date = Date(java.util.Date().time)

    private fun findProfileByName(connection: Connection, name: String): Profile? {
        val findProfileByNameQuery = "select * from profiles where name = ?"

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
        val createProfileQuery = "insert into profiles(name, password, lastConnectDate) values(?,?,?)"
        val addActionQuery = "insert into actions(dateCreated, actionType, profile_id) values (?, 'PROFILE_CREATED', ?)"

        val connection = getConnection()
        connection.autoCommit = false

        try {
            if (findProfileByName(connection, name) != null) {
                return false
            }

            connection.prepareStatement(createProfileQuery).use {
                it.setString(1, name)
                it.setString(2, password)
                it.setDate(3, getCurrentDate())
                it.addBatch()
                it.executeBatch()
            }

            val profile = findProfileByName(connection, name) ?: return false

            connection.prepareStatement(addActionQuery).use {
                it.setDate(1, getCurrentDate())
                it.setInt(2, profile.id)
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
        val findUserQuery = "select * from profiles where name = ? and password = ?"
        val updateLastConnect = "update profiles set lastConnectDate = ? where name = ?"
        val addActionQuery = "insert into actions(dateCreated, actionType, profile_id) values(?, 'LOGIN', ?)"

        val connection = getConnection()
        connection.autoCommit = false

        try {
            val profile = connection.prepareStatement(findUserQuery).use {
                it.setString(1, name)
                it.setString(2, password)
                val rs = it.executeQuery()
                if (rs.next()) {
                    Profile(rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("password"),
                            rs.getDate("lastConnectDate")
                    )
                } else return false
            }

            connection.prepareStatement(updateLastConnect).use {
                it.setDate(1, getCurrentDate())
                it.setString(2, name)
                it.executeUpdate()
            }

            connection.prepareStatement(addActionQuery).use {
                it.setDate(1, getCurrentDate())
                it.setInt(2, profile.id)
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
        val query = "select * from actions where profile_id = ?"

        val result = mutableListOf<Action>()

        getConnection().use { connection ->
            connection.autoCommit = true

            connection.prepareStatement(query).use {
                val profile = findProfileByName(connection, name) ?: return emptyList()

                it.setInt(1, profile.id)
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
        val changePasswordQuery = "update profiles set password = ? where name = ? and password = ?"
        val addActionQuery = "insert into actions(dateCreated, actionType, profile_id) values(?, 'CHANGE_PASSWORD', ?)"

        val connection = getConnection()
        connection.autoCommit = false

        try {
            connection.prepareStatement(changePasswordQuery).use {
                it.setString(1, newPassword)
                it.setString(2, name)
                it.setString(3, oldPassword)
                if (it.executeUpdate() == 0) return false
            }

            val profile = findProfileByName(connection, name) ?: return false

            connection.prepareStatement(addActionQuery).use {
                it.setDate(1, getCurrentDate())
                it.setInt(2, profile.id)
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
