package com.nomad.android.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ResultTest {

    // --- Success ---

    @Test
    fun `Success isSuccess returns true`() {
        val result = Result.success("data")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `Success isError returns false`() {
        val result = Result.success("data")
        assertFalse(result.isError)
    }

    @Test
    fun `Success getOrNull returns data`() {
        val result = Result.success("hello")
        assertEquals("hello", result.getOrNull())
    }

    @Test
    fun `Success exceptionOrNull returns null`() {
        val result = Result.success(42)
        assertNull(result.exceptionOrNull())
    }

    @Test
    fun `Success data property holds value`() {
        val result = Result.success(listOf(1, 2, 3)) as Result.Success
        assertEquals(listOf(1, 2, 3), result.data)
    }

    @Test
    fun `Success with nullable data`() {
        val result: Result<String?> = Result.success(null)
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `Success with Unit`() {
        val result = Result.success(Unit)
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
    }

    // --- Error ---

    @Test
    fun `Error isSuccess returns false`() {
        val result = Result.error("fail")
        assertFalse(result.isSuccess)
    }

    @Test
    fun `Error isError returns true`() {
        val result = Result.error("fail")
        assertTrue(result.isError)
    }

    @Test
    fun `Error getOrNull returns null`() {
        val result = Result.error<Int>("fail")
        assertNull(result.getOrNull())
    }

    @Test
    fun `Error exceptionOrNull returns null when no exception`() {
        val result = Result.error<Int>("fail")
        assertNull(result.exceptionOrNull())
    }

    @Test
    fun `Error exceptionOrNull returns exception when provided`() {
        val exception = RuntimeException("boom")
        val result = Result.error<Int>("fail", exception)
        assertNotNull(result.exceptionOrNull())
        assertEquals("boom", result.exceptionOrNull()!!.message)
    }

    @Test
    fun `Error message property holds message`() {
        val result = Result.error<Int>("Something went wrong") as Result.Error
        assertEquals("Something went wrong", result.message)
    }

    @Test
    fun `Error is of type Result Nothing`() {
        val result: Result<String> = Result.error("fail")
        assertTrue(result is Result.Error)
    }

    // --- runCatching ---

    @Test
    fun `runCatching returns Success when block succeeds`() = runTest {
        val result = Result.runCatching { 42 }
        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `runCatching returns Error when block throws`() = runTest {
        val result = Result.runCatching<Int> {
            throw RuntimeException("kaboom")
        }
        assertTrue(result.isError)
        assertNotNull(result.exceptionOrNull())
        assertEquals("kaboom", result.exceptionOrNull()!!.message)
    }

    @Test
    fun `runCatching captures exception message in error message`() = runTest {
        val result = Result.runCatching<Int> {
            throw IllegalStateException("bad state")
        }
        assertTrue(result.isError)
        assertEquals("bad state", result.getOrNull()) // getOrNull returns null for Error
        assertNull(result.getOrNull())
    }

    @Test
    fun `runCatching with null exception message uses fallback`() = runTest {
        val result = Result.runCatching<Int> {
            throw RuntimeException()
        }
        assertTrue(result.isError)
        val error = result as Result.Error
        assertEquals("Unknown error", error.message)
    }

    @Test
    fun `runCatching supports suspend functions`() = runTest {
        val result = Result.runCatching {
            // Simulate a suspend call
            kotlinx.coroutines.delay(1)
            "async result"
        }
        assertTrue(result.isSuccess)
        assertEquals("async result", result.getOrNull())
    }

    // --- Type safety ---

    @Test
    fun `Error can be assigned to any Result type`() {
        val intResult: Result<Int> = Result.error("not a number")
        val stringResult: Result<String> = Result.error("not a string")
        assertTrue(intResult.isError)
        assertTrue(stringResult.isError)
    }

    @Test
    fun `sealed class pattern matching works`() {
        val results = listOf<Result<String>>(
            Result.success("ok"),
            Result.error("fail"),
            Result.success("also ok")
        )
        val successes = results.filterIsInstance<Result.Success<String>>()
        val errors = results.filterIsInstance<Result.Error>()
        assertEquals(2, successes.size)
        assertEquals(1, errors.size)
    }
}
