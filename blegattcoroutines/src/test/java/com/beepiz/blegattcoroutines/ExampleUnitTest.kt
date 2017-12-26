package com.beepiz.blegattcoroutines

import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.selects.select
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.system.measureTimeMillis

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {
    @Test
    @Throws(Exception::class)
    fun addition_isCorrect() {
        assertEquals(4, (2 + 2).toLong())
    }

    @Test
    fun massiveRunExecTime() = runBlocking {
        val mutex = Mutex()
        var count = 0
        val n = 1000
        val k = 1000
        val execTime = massiveRun(n, k) {
            mutex.withLock {
                count++
            }
        }
        println("Completed ${n * k} actions in $execTime ms")
        assertEquals(n * k, count)
        val a = actor<Unit> {
            for (msg in channel) {

            }
        }
    }

    @Test
    fun testActor() = runBlocking<Unit> {
        val counter = counterActor() // create the actor
        massiveRun {
            counter.send(IncCounter)
        }
        // send a message to get a counter value from an actor
        val response = CompletableDeferred<Int>()
        counter.send(GetCounter(response))
        println("Counter = ${response.await()}")
        counter.close() // shutdown the actor
    }

    // This function launches a new counter actor
    private fun counterActor() = actor<CounterMsg> {
        var counter = 0 // actor state
        for (msg in channel) { // iterate over incoming messages
            when (msg) {
                is IncCounter -> counter++
                is GetCounter -> msg.response.complete(counter)
            }
        }
    }

    suspend fun selectAorB(a: ReceiveChannel<String>, b: ReceiveChannel<String>): String {
        return select<String> {
            a.onReceiveOrNull { value ->
                if (value == null)
                    "Channel 'a' is closed"
                else
                    "a -> '$value'"
            }
            b.onReceiveOrNull { value ->
                if (value == null)
                    "Channel 'b' is closed"
                else
                    "b -> '$value'"
            }
        }
    }

    /**
     * @param n Number of coroutines to launch
     * @param k Times an action is repeated by each coroutine
     */
    private suspend fun massiveRun(n: Int = 1000,
                                   k: Int = 1000,
                                   context: CoroutineContext = DefaultDispatcher,
                                   action: suspend () -> Unit) = measureTimeMillis {
        val jobs = List(n) {
            launch(context) {
                repeat(k) { action() }
            }
        }
        jobs.forEach { it.join() }
    }
}
