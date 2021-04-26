package com.dynatrace.kachedproperties

import kotlin.concurrent.thread
import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

@ExperimentalTime
fun <T : Any> cachedFor(updateDuration: Duration, provideValue: () -> T) =
    TimeBoundCacheDelegatedProperty(updateDuration, provideValue, TimeSource.Monotonic)

@ExperimentalTime
// Used in tests.
internal fun <T : Any> cachedFor(updateDuration: Duration, provideValue: () -> T, timeSource: TimeSource) =
    TimeBoundCacheDelegatedProperty(updateDuration, provideValue, timeSource)

@ExperimentalTime
class TimeBoundCacheDelegatedProperty<T : Any>(
    private val updateDuration: Duration,
    private val provideValue: () -> T,
    private val timeSource: TimeSource,
) {
    // The initial value is set this way to ensure that the first request to getValue will cause fetching the value.
    private var lastFetchedTimeMark = timeSource.markNow().minus(updateDuration * 2)
    private var firstInit = false
    private var beingUpdated = false
    private lateinit var lastValue: T

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        synchronized(firstInit) {
            if (firstInit) {
                if (lastFetchedTimeMark.elapsedNow() >= updateDuration) {
                    val last = lastValue
                    if (!beingUpdated) {
                        beingUpdated = true
                        thread(start = true) {
                            updateValue()
                            beingUpdated = false
                        }
                    }
                    return last
                }
            } else {
                updateValue()
                firstInit = true
            }
        }
        return lastValue
    }

    private fun updateValue() {
        lastValue = provideValue()
        lastFetchedTimeMark = timeSource.markNow()
    }
}
