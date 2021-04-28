package com.dynatrace.kachedproperties

import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

@ExperimentalTime
fun <T : Any> cachedWithLazyRefreshFor(
    updateDuration: Duration,
    provideValue: () -> T,
    executor: Executor = Executors.newCachedThreadPool(),
    timeSource: TimeSource = TimeSource.Monotonic
) =
    TimeBoundCacheDelegatedProperty(updateDuration, provideValue, true, executor, timeSource)

@ExperimentalTime
fun <T : Any> cachedFor(
    updateDuration: Duration,
    provideValue: () -> T,
    timeSource: TimeSource = TimeSource.Monotonic
) =
    TimeBoundCacheDelegatedProperty(updateDuration, provideValue, false, null, timeSource)

@ExperimentalTime
class TimeBoundCacheDelegatedProperty<T : Any>(
    private val updateDuration: Duration,
    private val provideValue: () -> T,
    private val lazyRefresh: Boolean,
    private val executor: Executor?,
    private val timeSource: TimeSource,
) {
    private var lastFetchedTimeMark = timeSource.markNow()
    private var notFirstInit = false
    private var beingUpdated = false
    private lateinit var lastValue: T

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        synchronized(notFirstInit) {
            if (!notFirstInit) {
                updateValue()
                notFirstInit = true
            }

            if (lastFetchedTimeMark.elapsedNow() >= updateDuration) {
                return queueUpdateValue()
            }
        }
        return lastValue
    }

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
        lastFetchedTimeMark = timeSource.markNow()
    }
}
