// Copyright 2021 Dynatrace LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
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

class TimeBoundCacheDelegatedProperty<T : Any>(val cache: TimeBoundCache<T>) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = cache.getValue()
}
