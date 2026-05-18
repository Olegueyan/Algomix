package fr.olegueyan.algomix.ui.timer

import android.os.SystemClock

interface TimerTimeSource {
    fun elapsedRealtimeMillis(): Long
}

object SystemTimerTimeSource : TimerTimeSource {
    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()
}
