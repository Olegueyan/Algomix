package fr.olegueyan.algomix.application.di

import fr.olegueyan.algomix.application.core.AppErrorType
import fr.olegueyan.algomix.application.core.ClockProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class AppContainerTest {
    @Test
    fun exposesConfiguredClockProvider() {
        val instant = Instant.parse("2026-05-13T10:15:30Z")
        val container = AppContainer(clockProvider = FixedClockProvider(instant))

        assertEquals(instant, container.clockProvider.now())
    }

    @Test
    fun futureDependenciesAreExplicitlyNotConfiguredByDefault() {
        val error = AppContainer().libraryRepository().errorOrNull()

        assertEquals(AppErrorType.UNKNOWN, error?.type)
        assertEquals("LibraryRepository is not configured yet", error?.message)
    }

    private class FixedClockProvider(
        private val instant: Instant,
    ) : ClockProvider {
        override fun now(): Instant = instant
    }
}
