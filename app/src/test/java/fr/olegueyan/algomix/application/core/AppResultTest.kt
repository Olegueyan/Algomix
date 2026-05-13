package fr.olegueyan.algomix.application.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppResultTest {
    @Test
    fun successExposesValueAndState() {
        val result = AppResult.success("ready")

        assertTrue(result.isSuccess)
        assertFalse(result.isFailure)
        assertEquals("ready", result.getOrNull())
        assertNull(result.errorOrNull())
    }

    @Test
    fun failureExposesErrorAndState() {
        val error = AppError.Validation("Name is required")
        val result = AppResult.failure(error)

        assertFalse(result.isSuccess)
        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
        assertEquals(error, result.errorOrNull())
    }

    @Test
    fun mapTransformsSuccessValue() {
        val result = AppResult.success(21).map { value -> value * 2 }

        assertEquals(42, result.getOrNull())
    }

    @Test
    fun mapKeepsFailureError() {
        val error = AppError.Storage("Disk is unavailable")
        val result: AppResult<Int> = AppResult.failure(error)

        val mapped = result.map { value -> value * 2 }

        assertEquals(error, mapped.errorOrNull())
    }

    @Test
    fun foldUsesMatchingBranch() {
        val success = AppResult.success(7)
        val failure: AppResult<Int> = AppResult.failure(AppError.Network())

        assertEquals("value=7", success.fold(onSuccess = { "value=$it" }, onFailure = { "error" }))
        assertEquals("error=NETWORK", failure.fold(onSuccess = { "value=$it" }, onFailure = { "error=${it.type}" }))
    }
}
