package com.ayaansaksham.spend_aware.data.repository

import com.ayaansaksham.spend_aware.data.local.dao.UserDao
import com.ayaansaksham.spend_aware.data.local.entity.User

class UserRepository(private val userDao: UserDao) {
    suspend fun insertUser(user: User): Long {
        return userDao.insertUser(user)
    }

    suspend fun getUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)
    }
}