package spel

import java.util.*
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

    private var maxDepth = 4
    var theTree: Node = buildTree(maxDepth)

    private fun buildTree(newMaxDepth: Int = 4): Node {
        Logger.log(2, "building tree with depth $maxDepth, ${Date()}")
        maxDepth = newMaxDepth
        val root = Node(Dice(0), 0)
        Logger.log(2, "building tree (done): ${Date()}")
        return root
    }

    fun initLeaves(depth: Int): Array<Node?> {
        if (depth < maxDepth)
            return Array(6) { Node(Dice(it + 1), depth + 1) }
        else
            return Array(6) { null }
    }

    class Node(
        val key: Dice,
        private val depth: Int,
        val leaves: Array<Node?> = initLeaves(depth)
    )

    fun traverseCombinationsOfLengthX(tree: Node, depth: Int, collector: (Array<Dice>) -> Unit) {
        fun traverseCombinationsRecursive(tree: Node, depth: Int, acc: Array<Dice>) {
            if (depth > 0) {
                tree.leaves.forEach { node ->
                    acc[depth - 1] = node!!.key
                    traverseCombinationsRecursive(node, depth - 1, acc)
                }
            } else
                collector(acc)
        }
        traverseCombinationsRecursive(tree, depth, Array(depth) { Dice(0) })
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
