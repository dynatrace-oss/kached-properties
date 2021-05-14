package com.dynatrace.kachedproperties

import java.time.Duration
import java.time.Instant

interface TimeSource {
    fun markNow(): Instant
    fun elapsedTime(): Duration
}

class SystemTimeSource : TimeSource {
    private var time: Instant = Instant.now()

    override fun markNow(): Instant {
        time = Instant.now()
        return time
    }

    override fun elapsedTime(): Duration {
        val currentTime = Instant.now()
        return Duration.between(time, currentTime)
    }
}

class SimulatedTimeSource : TimeSource {
    private var time: Instant = Instant.now()
    private var currentTime: Instant = Instant.now()

    override fun markNow(): Instant {
        time = currentTime
        return time
    }

    fun setTime(instant: Instant) {
        time = instant
    }

    fun setCurrentTime(instant: Instant) {
        currentTime = instant
    }

    fun advanceBy(duration: Duration) {
        currentTime = currentTime.plus(duration)
    }

    override fun elapsedTime(): Duration {
        return Duration.between(time, currentTime)
    }
}
