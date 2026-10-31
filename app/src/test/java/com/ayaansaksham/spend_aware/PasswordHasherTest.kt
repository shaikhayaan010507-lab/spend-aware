package com.ayaansaksham.spend_aware

import com.ayaansaksham.spend_aware.domain.logic.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class PasswordHasherTest {

    @Test
    fun correct_password_verifies_successfully() = runBlocking {
        val password = "SecurePassword123"
        val saltBytes = PasswordHasher.generateSalt()
        val saltBase64 = Base64.getEncoder().encodeToString(saltBytes)
        val hashBase64 = PasswordHasher.hashPassword(password, saltBytes)

        val isValid = PasswordHasher.verifyPassword(password, saltBase64, hashBase64)
        assertTrue(isValid)
    }

    @Test
    fun wrong_password_fails_verification() = runBlocking {
        val password = "SecurePassword123"
        val wrongPassword = "WrongPassword456"
        val saltBytes = PasswordHasher.generateSalt()
        val saltBase64 = Base64.getEncoder().encodeToString(saltBytes)
        val hashBase64 = PasswordHasher.hashPassword(password, saltBytes)

        val isValid = PasswordHasher.verifyPassword(wrongPassword, saltBase64, hashBase64)
        assertFalse(isValid)
    }

    @Test
    fun same_password_with_different_salts_produces_different_hashes() = runBlocking {
        val password = "MySecretPassword"
        val salt1 = PasswordHasher.generateSalt()
        val salt2 = PasswordHasher.generateSalt()

        val hash1 = PasswordHasher.hashPassword(password, salt1)
        val hash2 = PasswordHasher.hashPassword(password, salt2)

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun known_test_vector_verifies_correctly() = runBlocking {
        // PBKDF2WithHmacSHA256, 100,000 iterations, 256 bits
        val password = "password"
        val saltBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        val saltBase64 = Base64.getEncoder().encodeToString(saltBytes)

        val expectedHash = PasswordHasher.hashPassword(password, saltBytes)

        assertTrue(PasswordHasher.verifyPassword("password", saltBase64, expectedHash))
        assertEquals(expectedHash, PasswordHasher.hashPassword("password", saltBytes))
    }
}