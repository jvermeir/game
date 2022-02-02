package stuff

import kotlin.test.Test
import kotlin.test.assertEquals

class FizzBuzzTester {
    val max = 100_000_000

    private fun fizzBuzzTester(i: Int): String {
        if (i % 15 == 0) return "FizzBuzz"
        if (i % 3 == 0) return "Fizz"
        if (i % 5 == 0) return "Buzz"
        return "" + i
    }

    fun runIt(method: () -> Unit, testMethodName: String) {
        val start = System.currentTimeMillis()
        method()
        val runTime = System.currentTimeMillis() - start

        println("$testMethodName: $runTime ms")
    }

    @Test
    fun fizzBuzzUsingASequence() {
        runIt({
            var count = 0
            val sequence = generateSequence {
                (count++).takeIf { it <= max }
            }
            sequence.iterator().forEach { fizzBuzzTester(it) }
        }, "sequence")
    }

    @Test
    fun fizzBuzzUsingTailRecursion() {

        tailrec fun next(count: Int) {
            if (count <= max) {
                fizzBuzzTester(count)
                next(count + 1)
            }
        }

        runIt({ next(0) }, "recursive")
    }

    @Test
    fun fizzBuzzMappingOverARange() {
        runIt({
            (0..max).map {
                fizzBuzzTester(it)
            }
        }, "map over range")
    }

    @Test
    fun fizzBuzzUsingABoringLoop() {
        runIt({
            for (i in 0..max) {
                fizzBuzzTester(i)
            }
        }, "boring for loop")
    }

    @Test
    fun fizzBuzzSanityCheck() {
        val expectedResults = arrayOf(
            "x", "1", "2", "Fizz", "4", "Buzz", "Fizz", "7", "8", "Fizz", "Buzz", "11", "Fizz", "13", "14",
            "FizzBuzz", "16", "17", "Fizz", "19", "Buzz", "Fizz", "22", "23", "Fizz", "Buzz", "26",
            "Fizz", "28", "29", "FizzBuzz"
        )
        for (i in 1..30) {
            assertEquals(expectedResults[i], fizzBuzzTester(i))
        }
    }
}
