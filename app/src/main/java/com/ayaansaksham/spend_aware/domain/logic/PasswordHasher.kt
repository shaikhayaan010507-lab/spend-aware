package com.ayaansaksham.spend_aware.domain.logic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {

    suspend fun hashPassword(password: String, salt: ByteArray): String = withContext(Dispatchers.IO) {
        val spec = PBEKeySpec(password.toCharArray(), salt, 100000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        Base64.getEncoder().encodeToString(hash)
    }

    fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    suspend fun verifyPassword(password: String, saltBase64: String, hashBase64: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val salt = Base64.getDecoder().decode(saltBase64)
            val computedHash = hashPassword(password, salt)
            MessageDigest.isEqual(computedHash.toByteArray(), hashBase64.toByteArray())
        } catch (_: Exception) {
            false
        }
    }
}