package spel

import kotlin.random.Random

fun rand(start: Int, end: Int): Int {
    require(start <= end) { "Illegal Argument" }
    val rand = Random(System.nanoTime())
    return (start..end).random(rand)
}

data class Dice(val value: Int) {
    val numericValue = if (value == 6) 5 else value

    override fun toString(): String {
        return if (value == 6) "worm" else "$value"
    }
}
object Throw {
    fun throwDice(numberOfDice: Int = 8): List<Dice> {
        return (Array(numberOfDice) { Config.throwDice() }).asList()
    }
}

object Config {
    fun regularThrowDiceMethod(): Dice {
        return Dice(rand(1, 6))
    }

    var throwDiceMethod: () -> Dice = ::regularThrowDiceMethod

    fun throwDice(): Dice {
        return throwDiceMethod()
    }
}
