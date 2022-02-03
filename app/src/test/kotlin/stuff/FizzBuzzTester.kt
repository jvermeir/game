package stuff

import kotlin.test.Test
import kotlin.test.assertEquals

class FizzBuzzTester {
    val max = 100_000_000

    private fun fizzBuzzTester(i: Int): String = when {
        i % 15 == 0 -> "FizzBuzz"
        i % 3 == 0 -> "Fizz"
        i % 5 == 0 -> "Buzz"
        else -> "$i"
    }

    fun <T> runIt(testMethodName: String, method: () -> T): T {
        val start = System.currentTimeMillis()
        return method().also {
            println("$testMethodName: ${System.currentTimeMillis() - start} ms")
        }
    }

    @Test
    fun `fizz Buzz Using A Sequence`() {
        runIt("sequence") {
            (1..max).asSequence().forEach { fizzBuzzTester(it) }
        }
    }

    @Test
    fun `fizz Buzz Using Tail Recursion`() {
        tailrec fun next(count: Int) {
            if (count <= max) {
                fizzBuzzTester(count)
                next(count + 1)
            }
        }
        runIt("recursive") { next(0) }
    }

    @Test
    fun `fizz Buzz Mapping Over A Range`() {
        runIt("map over range") {
            (0..max).forEach {
                fizzBuzzTester(it)
            }
        }
    }

    @Test
    fun `fizz Buzz Using A Boring Loop`() {
        runIt("boring for loop") {
            for (i in 0..max) {
                fizzBuzzTester(i)
            }
        }
    }

    @Test
    fun `fizz Buzz Sanity Check`() {
        val expectedResults = arrayOf(
            "justIgnoreThisOK?", "1", "2", "Fizz", "4", "Buzz", "Fizz", "7", "8", "Fizz", "Buzz", "11", "Fizz", "13", "14",
            "FizzBuzz", "16", "17", "Fizz", "19", "Buzz", "Fizz", "22", "23", "Fizz", "Buzz", "26",
            "Fizz", "28", "29", "FizzBuzz"
        )
        for (i in 1..30) {
            assertEquals(expectedResults[i], fizzBuzzTester(i))
        }
    }
}