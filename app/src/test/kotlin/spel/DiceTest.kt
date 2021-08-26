package spel

import spel.Throw.theTree
import spel.Throw.traverseCombinationsOfLengthX
import java.util.*
import kotlin.math.pow
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
        fun collector(dice: Array<Dice>) {
            Logger.log(4, dice.contentToString())
            counter++
        }

        Logger.log(3, "traverse to depth 8, ${Date()}")
        traverseCombinationsOfLengthX(theTree, 8, ::collector)
        Logger.log(3, "traverse to depth 8 (done), ${Date()}")
        Logger.log(3, "counter: $counter")
        assertEquals((6.0).pow(8), counter.toDouble(), "expecting 6^8 nodes to be visited if depth = 8")
    }
}
