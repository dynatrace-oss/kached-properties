package com.dynatrace.kachedproperties

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.time.ExperimentalTime
import kotlin.time.TestTimeSource
import kotlin.time.seconds

@ExperimentalTime
class TimeBoundCacheDelegatedPropertyTest {
    @Test
    fun `should return the same value when cache didn't expire`() {
        val mockTimeSource = TestTimeSource()
        val fetchValueFromDependency = mockk<() -> String>()
        every { fetchValueFromDependency() } returnsMany listOf("Call 1", "Call 2")
        val cachedValue by cachedWithLazyRefreshFor(5.seconds, { fetchValueFromDependency() }, timeSource = mockTimeSource)

        assertEquals("Call 1", cachedValue)
        mockTimeSource += 2.seconds
        assertEquals("Call 1", cachedValue)

        verify(exactly = 1) { fetchValueFromDependency() }
    }

    @Test
    fun `should update the value after the cache expires`() {
        val mockTimeSource = TestTimeSource()
        val fetchValueFromDependency = mockk<() -> String>()
        every { fetchValueFromDependency() } returnsMany listOf("Call 1", "Call 2")
        val cachedValue by cachedFor(5.seconds, { fetchValueFromDependency() }, mockTimeSource)

        assertEquals("Call 1", cachedValue)
        mockTimeSource += 10.seconds
        assertEquals("Call 2", cachedValue)

        verify(exactly = 2) { fetchValueFromDependency() }
    }

    @Test
    fun `should return same value when different threads call it for updating with lazy refresh `() {
        val mockTimeSource = TestTimeSource()
        val fetchValueFromDependency = mockk<() -> String>()
        val dependencyCallCounter = AtomicInteger(1)
        every { fetchValueFromDependency() } answers {
            sleep(1.seconds.toLongMilliseconds()) // Simulating long network call.
            "Call ${dependencyCallCounter.getAndIncrement()}"
        }
        val cachedValue by cachedWithLazyRefreshFor(5.seconds, { fetchValueFromDependency() }, timeSource = mockTimeSource)

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
        val mockTimeSource = TestTimeSource()
        val fetchValueFromDependency = mockk<() -> String>()
        val dependencyCallCounter = AtomicInteger(1)
        every { fetchValueFromDependency() } answers {
            sleep(1.seconds.toLongMilliseconds()) // Simulating long network call.
            "Call ${dependencyCallCounter.getAndIncrement()}"
        }
        val cachedValue by cachedFor(5.seconds, { fetchValueFromDependency() }, mockTimeSource)

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
        val mockTimeSource = TestTimeSource()
        val executor = Executors.newSingleThreadExecutor()
        val fetchValueFromDependency = mockk<() -> String>()
        val dependencyCallCounter = AtomicInteger(1)
        every { fetchValueFromDependency() } answers {
            sleep(1.seconds.toLongMilliseconds()) // Simulating long network call.
            "Call ${dependencyCallCounter.getAndIncrement()}"
        }
        val cachedValue by cachedWithLazyRefreshFor(5.seconds, { fetchValueFromDependency() }, executor, mockTimeSource)

        val cachedValueReadFromThread1: String = cachedValue
        mockTimeSource += 10.seconds
        val cachedValueReadFromThread2: String = cachedValue

        val retrievedValues = setOf(cachedValueReadFromThread1, cachedValueReadFromThread2)
        assertEquals(setOf("Call 1"), retrievedValues)

        executor.awaitTermination(1, TimeUnit.SECONDS)
        verify(exactly = 2) { fetchValueFromDependency() } // Should be updating in the background
    }

    @Test
    fun `should return updated value immediately without lazy refresh`() {
        val mockTimeSource = TestTimeSource()
        val fetchValueFromDependency = mockk<() -> String>()
        val dependencyCallCounter = AtomicInteger(1)
        every { fetchValueFromDependency() } answers {
            sleep(1.seconds.toLongMilliseconds()) // Simulating long network call.
            "Call ${dependencyCallCounter.getAndIncrement()}"
        }
        val cachedValue by cachedFor(5.seconds, { fetchValueFromDependency() }, mockTimeSource)

        val read1: String = cachedValue
        mockTimeSource += 10.seconds
        val read2: String = cachedValue

        val retrievedValues = setOf(read1, read2)
        assertEquals(setOf("Call 1", "Call 2"), retrievedValues)

        verify(exactly = 2) { fetchValueFromDependency() }
    }
}
