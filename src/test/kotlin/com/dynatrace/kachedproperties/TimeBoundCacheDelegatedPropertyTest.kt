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

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.Thread.sleep
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class TimeBoundCacheDelegatedPropertyTest {
    @Test
    fun `should return the same value when cache didn't expire`() {
        val mockTimeSource = SimulatedTimeSource()
        val fetchValueFromDependency = mockk<() -> String>()
        every { fetchValueFromDependency() } returnsMany listOf("Call 1", "Call 2")
        val cachedValue by cachedLazyFor(
            Duration.ofSeconds(5),
            { fetchValueFromDependency() },
            timeSource = mockTimeSource,
        )

        assertEquals("Call 1", cachedValue)
        mockTimeSource.advanceBy(Duration.ofSeconds(2))
        assertEquals("Call 1", cachedValue)

        verify(exactly = 1) { fetchValueFromDependency() }
    }

    @Test
    fun `should update the value after the cache expires`() {
        val mockTimeSource = SimulatedTimeSource()
        val fetchValueFromDependency = mockk<() -> String>()
        every { fetchValueFromDependency() } returnsMany listOf("Call 1", "Call 2")
        val cachedValue by cachedBlockingFor(Duration.ofSeconds(5), { fetchValueFromDependency() }, mockTimeSource)

        assertEquals("Call 1", cachedValue)
        mockTimeSource.advanceBy(Duration.ofSeconds(10))
        assertEquals("Call 2", cachedValue)

        verify(exactly = 2) { fetchValueFromDependency() }
    }

    @Test
    fun `should return same value when different threads call it for updating with lazy refresh `() {
        val mockTimeSource = SimulatedTimeSource()
        val fetchValueFromDependency = mockk<() -> String>()
        val dependencyCallCounter = AtomicInteger(1)
        every { fetchValueFromDependency() } answers {
            sleep(1000) // Simulating long network call.
            "Call ${dependencyCallCounter.getAndIncrement()}"
        }
        val cachedValue by cachedLazyFor(
            Duration.ofSeconds(5),
            { fetchValueFromDependency() },
            timeSource = mockTimeSource,
        )

        lateinit var cachedValueReadFromThread1: String
        lateinit var cachedValueReadFromThread2: String
        val thread1 = thread {
            cachedValueReadFromThread1 = cachedValue
        }
        val thread2 = thread {
            cachedValueReadFromThread2 = cachedValue
        }
        thread1.join()
        thread2.join()

        val retrievedValues = setOf(cachedValueReadFromThread1, cachedValueReadFromThread2)
        assertEquals(setOf("Call 1"), retrievedValues)

        verify(exactly = 1) { fetchValueFromDependency() }
    }

    @Test
    fun `should return same value when different threads call it for updating without lazy refresh`() {
        val mockTimeSource = SimulatedTimeSource()
        val fetchValueFromDependency = mockk<() -> String>()
        val dependencyCallCounter = AtomicInteger(1)
        every { fetchValueFromDependency() } answers {
            sleep(1000) // Simulating long network call.
            "Call ${dependencyCallCounter.getAndIncrement()}"
        }
        val cachedValue by cachedBlockingFor(Duration.ofSeconds(5), { fetchValueFromDependency() }, mockTimeSource)

        lateinit var cachedValueReadFromThread1: String
        lateinit var cachedValueReadFromThread2: String
        val thread1 = thread {
            cachedValueReadFromThread1 = cachedValue
        }
        val thread2 = thread {
            cachedValueReadFromThread2 = cachedValue
        }
        thread1.join()
        thread2.join()

        val retrievedValues = setOf(cachedValueReadFromThread1, cachedValueReadFromThread2)
        assertEquals(setOf("Call 1"), retrievedValues)

        verify(exactly = 1) { fetchValueFromDependency() }
    }

    @Test
    fun `should return same value with lazy refresh`() {
        val mockTimeSource = SimulatedTimeSource()
        val executor = Executors.newSingleThreadExecutor()
        val fetchValueFromDependency = mockk<() -> String>()
        val dependencyCallCounter = AtomicInteger(1)
        every { fetchValueFromDependency() } answers {
            sleep(1000) // Simulating long network call.
            "Call ${dependencyCallCounter.getAndIncrement()}"
        }
        val cachedValue by cachedLazyFor(
            Duration.ofSeconds(5),
            { fetchValueFromDependency() },
            executor,
            mockTimeSource,
        )

        val cachedValueReadFromThread1: String = cachedValue
        mockTimeSource.advanceBy(Duration.ofSeconds(10))
        val cachedValueReadFromThread2: String = cachedValue

        val retrievedValues = setOf(cachedValueReadFromThread1, cachedValueReadFromThread2)
        assertEquals(setOf("Call 1"), retrievedValues)

        executor.awaitTermination(1, TimeUnit.SECONDS)
        verify(exactly = 2) { fetchValueFromDependency() } // Should be updating in the background
    }

    @Test
    fun `should return updated value immediately without lazy refresh`() {
        val mockTimeSource = SimulatedTimeSource()
        val fetchValueFromDependency = mockk<() -> String>()
        val dependencyCallCounter = AtomicInteger(1)
        every { fetchValueFromDependency() } answers {
            sleep(1000) // Simulating long network call.
            "Call ${dependencyCallCounter.getAndIncrement()}"
        }
        val cachedValue by cachedBlockingFor(Duration.ofSeconds(5), { fetchValueFromDependency() }, mockTimeSource)

        val read1: String = cachedValue
        mockTimeSource.advanceBy(Duration.ofSeconds(10))
        val read2: String = cachedValue

        val retrievedValues = setOf(read1, read2)
        assertEquals(setOf("Call 1", "Call 2"), retrievedValues)

        verify(exactly = 2) { fetchValueFromDependency() }
    }

    @Test
    @Suppress("TooGenericExceptionThrown")
    fun `retries fetching value on next request when fetching value threw an exception - lazy version`() {
        val mockTimeSource = SimulatedTimeSource()
        val fetchValueFromDependency = mockk<() -> String>()
        val dependencyCallCounter = AtomicInteger(1)
        every { fetchValueFromDependency() } answers {
            if (dependencyCallCounter.get() == 2) {
                throw RuntimeException(
                    "A ${dependencyCallCounter.getAndIncrement()} call to the dependency failed! " +
                        "Next will be ${dependencyCallCounter.get()}"
                )
            } else {
                "Call ${dependencyCallCounter.getAndIncrement()}"
            }
        }
        val cachedValue by cachedLazyFor(
            Duration.ofSeconds(5),
            { fetchValueFromDependency() },
            timeSource = mockTimeSource,
        )

        assertEquals("Call 1", cachedValue)
        mockTimeSource.advanceBy(Duration.ofSeconds(10))
        sleep(10)
        assertEquals("Call 1", cachedValue)
        mockTimeSource.advanceBy(Duration.ofSeconds(10))
        sleep(10)
        assertEquals("Call 1", cachedValue)
        mockTimeSource.advanceBy(Duration.ofSeconds(10))
        sleep(10)
        assertEquals("Call 3", cachedValue)
        mockTimeSource.advanceBy(Duration.ofSeconds(10))
        sleep(10)
        assertEquals("Call 3", cachedValue)

        verify(exactly = 4) { fetchValueFromDependency() }
    }

    @Test
    @Suppress("TooGenericExceptionThrown")
    fun `retries fetching value on next request when fetching value threw an exception - blocking version`() {
        val mockTimeSource = SimulatedTimeSource()
        val fetchValueFromDependency = mockk<() -> String>()
        val dependencyCallCounter = AtomicInteger(1)
        every { fetchValueFromDependency() } answers {
            if (dependencyCallCounter.get() == 2) {
                throw RuntimeException(
                    "A ${dependencyCallCounter.getAndIncrement()} call to the dependency failed! " +
                        "Next will be ${dependencyCallCounter.get()}"
                )
            } else {
                "Call ${dependencyCallCounter.getAndIncrement()}"
            }
        }
        val cachedValue by cachedBlockingFor(
            Duration.ofSeconds(5),
            { fetchValueFromDependency() },
            timeSource = mockTimeSource,
        )

        assertEquals("Call 1", cachedValue)
        mockTimeSource.advanceBy(Duration.ofSeconds(10))
        sleep(10)
        assertThrows<RuntimeException> {
            cachedValue
        }
        mockTimeSource.advanceBy(Duration.ofSeconds(10))
        sleep(10)
        assertEquals("Call 3", cachedValue)
        mockTimeSource.advanceBy(Duration.ofSeconds(10))
        sleep(10)
        assertEquals("Call 4", cachedValue)
        mockTimeSource.advanceBy(Duration.ofSeconds(10))
        sleep(10)
        assertEquals("Call 5", cachedValue)

        verify(exactly = 5) { fetchValueFromDependency() }
    }
}
