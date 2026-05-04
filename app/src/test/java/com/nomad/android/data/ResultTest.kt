package com.nomad.android.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ResultTest {

    @Test
    fun `Success holds data`() {
        val result = Result.Success("hello")
        assertEquals("hello", result.data)
    }

    @Test
    fun `Error holds message and exception`() {
        val exception = RuntimeException("boom")
        val result = Result.Error("failed", exception)
        assertEquals("failed", result.message)
        assertEquals(exception, result.exception)
    }

    @Test
    fun `Error without exception has null exception`() {
        val result = Result.Error("just a message")
        assertEquals("just a message", result.message)
        assertNull(result.exception)
    }

    @Test
    fun `isSuccess returns true for Success`() {
        val result: Result<String> = Result.Success("data")
        assertTrue(result.isSuccess)
        assertFalse(result.isError)
    }

    @Test
    fun `isError returns true for Error`() {
        val result: Result<String> = Result.Error("fail")
        assertTrue(result.isError)
        assertFalse(result.isSuccess)
    }

    @Test
    fun `getOrNull returns data for Success`() {
        val result: Result<String> = Result.Success("data")
        assertEquals("data", result.getOrNull())
    }

    @Test
    fun `getOrNull returns null for Error`() {
        val result: Result<String> = Result.Error("fail")
        assertNull(result.getOrNull())
    }

    @Test
    fun `exceptionOrNull returns null for Success`() {
        val result: Result<String> = Result.Success("data")
        assertNull(result.exceptionOrNull())
    }

    @Test
    fun `exceptionOrNull returns exception for Error`() {
        val exception = IllegalArgumentException("bad")
        val result: Result<String> = Result.Error("fail", exception)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `companion success creates Success`() {
        val result = Result.success(42)
        assertTrue(result is Result.Success)
        assertEquals(42, result.data)
    }

    @Test
    fun `companion error creates Error`() {
        val result = Result.error("something went wrong")
        assertTrue(result is Result.Error)
        assertEquals("something went wrong", result.message)
    }

    @Test
    fun `runCatching wraps successful block in Success`() = runTest {
        val result = Result.runCatching { 42 }
        assertTrue(result is Result.Success)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `runCatching wraps exception in Error`() = runTest {
        val result = Result.runCatching<String> {
            throw IllegalStateException("nope")
        }
        assertTrue(result is Result.Error)
        assertEquals("nope", result.message)
        assertTrue(result.exception is IllegalStateException)
    }

    @Test
    fun `runCatching handles exception without message`() = runTest {
        val result = Result.runCatching<String> {
            throw RuntimeException()
        }
        assertTrue(result is Result.Error)
        assertEquals("Unknown error", result.message)
    }

    @Test
    fun `runCatching works with suspend functions`() = runTest {
        val result = Result.runCatching {
            // Simulate a suspend call
            kotlinx.coroutines.delay(1)
            "async result"
        }
        assertTrue(result is Result.Success)
        assertEquals("async result", result.getOrNull())
    }
}
