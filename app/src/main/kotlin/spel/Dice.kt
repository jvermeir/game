package spel

import kotlin.random.Random

fun rand(start: Int, end: Int): Int {
    require(start <= end) { "Illegal Argument" }
    val rand = Random(System.nanoTime())
    return (start..end).random(rand)
}

data class Dice(val value: Int) {
    val numericValue= if (value == 6) 5 else value

    override fun toString(): String {
        return if (value == 6) "worm" else "$value"
    }
}

object Config {
    fun regularThrowDiceMethod(): Dice {
        return Dice(rand(1, 6))
    }

    var throwDiceMethod: () -> Dice = ::regularThrowDiceMethod

    fun throwDice():Dice {
        return throwDiceMethod()
    }
}

data class Throw(val numberOfDice: Int = 8) {
    fun doThrow(): List<Dice> {
        val dice = Array(numberOfDice) { Config.throwDice() }
        return dice.asList()
    }
}

