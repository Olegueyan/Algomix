package fr.olegueyan.algomix.application.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AppErrorTest {
    @Test
    fun validationErrorHasStableTypeAndDetailMessage() {
        val error = AppError.Validation("Invalid sequence")

        assertEquals(AppErrorType.VALIDATION, error.type)
        assertEquals("Invalid sequence", error.message)
    }

    @Test
    fun networkErrorUsesDefaultMessageWhenDetailIsMissing() {
        val error = AppError.Network()

        assertEquals(AppErrorType.NETWORK, error.type)
        assertEquals(AppErrorType.NETWORK.defaultMessage, error.message)
    }

    @Test
    fun unknownErrorKeepsCause() {
        val cause = IllegalStateException("boom")
        val error = AppError.Unknown(cause = cause)

        assertEquals(AppErrorType.UNKNOWN, error.type)
        assertSame(cause, error.cause)
    }
}
