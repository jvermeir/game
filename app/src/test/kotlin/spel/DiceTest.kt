package spel

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
    fun testIGetFiveValuesWhenThrowIsCalledWith5 () {
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
        val countByValue = values.groupBy({it}, {it})

        assertEquals(6, countByValue.size, "all dice values should be selected at least once")
    }
}