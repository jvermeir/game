package spel

import spel.Throw.buildTree
import spel.Throw.findCombinationsOfLengthX
import spel.Throw.maxDepth
import spel.Throw.theTree
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class DiceTest {
    @Test
    fun testValuesArePrintedCorrect() {
        assertEquals("1", Dice(1).toString())
        assertEquals("worm", Dice(6).toString())
    }

    @Test
    fun testNumericalValueOfDice() {
        assertEquals(5, Dice(6).numericValue)
        assertEquals(1, Dice(1).numericValue)
        assertEquals(5, Dice(5).numericValue)
    }
}

class ThrowTest {
    @Test
    fun testIGetFiveValuesWhenThrowIsCalledWith5() {
        Config.throwDiceMethod = Config::regularThrowDiceMethod

        val numberOfThrows = 5
        val values = Throw.throwDice(numberOfThrows)

        assertEquals(numberOfThrows, values.size, "size should be $numberOfThrows")
    }

    @Test
    fun testIfEachValueOccursAtLeastOnce() {
        Config.throwDiceMethod = Config::regularThrowDiceMethod

        val numberOfThrows = 5000
        val values = Throw.throwDice(numberOfThrows)
        val countByValue = values.groupBy({ it }, { it })

        assertEquals(6, countByValue.size, "all dice values should be selected at least once")
    }

    @Test
    fun testTreeYieldsCorrectNumberOfCombinations() {
        Logger.logLevel = 3
        var counter = 0
        fun collector(dice: Array<Int>) {
            Logger.log(4, Arrays.toString(dice))
            counter++
        }

        Logger.log(3, Date().toString())
        maxDepth = 5
        theTree = buildTree(maxDepth)
        Logger.log(3, Date().toString())
        findCombinationsOfLengthX(theTree, 3, ::collector)
        Logger.log(3, Date().toString())
        Logger.log(3, "counter: ${counter}")
        assertEquals(216, counter, "expecting 6*6*6 nodes to be visited if depth = 3")
    }
}

fun main(args: Array<String>) {
    val i = 123
    val iAsString = i.toString()
    var digit = 0
    val result = iAsString.map { Pair(digit++, it) }
    println(result)
    // turn into dice
    // check if valid combination
}