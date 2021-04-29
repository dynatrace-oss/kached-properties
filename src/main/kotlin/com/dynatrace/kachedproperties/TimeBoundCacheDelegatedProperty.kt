package com.dynatrace.kachedproperties

import java.time.Duration
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.reflect.KProperty

fun <T : Any> cachedLazyFor(
    updateDuration: Duration,
    provideValue: () -> T,
    executor: Executor = Executors.newCachedThreadPool(),
    timeSource: TimeSource = SystemTimeSource()
) =
    TimeBoundCacheDelegatedProperty(TimeBoundCache(updateDuration, provideValue, true, executor, timeSource))

fun <T : Any> cachedBlockingFor(
    updateDuration: Duration,
    provideValue: () -> T,
    timeSource: TimeSource = SystemTimeSource()
) =
    TimeBoundCacheDelegatedProperty(TimeBoundCache(updateDuration, provideValue, false, null, timeSource))

class TimeBoundCacheDelegatedProperty<T: Any>(val cache: TimeBoundCache<T>) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = cache.getValue()
}
