package fr.olegueyan.algomix.application.core

import java.time.Instant

interface ClockProvider {
    fun now(): Instant
}

object SystemClockProvider : ClockProvider {
    override fun now(): Instant = Instant.now()
}
