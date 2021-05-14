package com.dynatrace.kachedproperties

import java.time.Duration
import java.util.concurrent.Executor

class TimeBoundCache<T : Any>(
    private val updateDuration: Duration,
    private val provideValue: () -> T,
    private val lazyRefresh: Boolean,
    private val executor: Executor?,
    private val timeSource: TimeSource,
) {
    init {
        timeSource.markNow()
    }

    private var notFirstInit = false
    private var beingUpdated = false
    private lateinit var lastValue: T

    fun getValue(): T {
        synchronized(notFirstInit) {
            if (!notFirstInit) {
                updateValue()
                notFirstInit = true
            }

            if (timeSource.elapsedTime() >= updateDuration) {
                return queueUpdateValue()
            }
        }
        return lastValue
    }

    @Suppress("ReturnCount")
    private fun queueUpdateValue(): T {
        val last = lastValue
        if (beingUpdated) {
            return last
        }

        beingUpdated = true
        if (!lazyRefresh || executor == null) {
            updateValue()
            beingUpdated = false
            return lastValue
        }

        executor.execute {
            updateValue()
            beingUpdated = false
        }
        return last
    }

    private fun updateValue() {
        lastValue = provideValue()
        timeSource.markNow()
    }
}
