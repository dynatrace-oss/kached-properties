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
            try {
                updateValue()
            } finally {
                beingUpdated = false
            }
            return lastValue
        }

        executor.execute {
            updateValue()
        }
        return last
    }

    private fun updateValue() {
        try {
            lastValue = provideValue()
            timeSource.markNow()
        } finally {
            beingUpdated = false
        }
    }
}
