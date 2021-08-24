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

    var maxDepth = 1
    var theTree: Node = buildTree(maxDepth)

    fun buildTree(newMaxDepth: Int = 8): Node {
        Logger.log(2, "building tree with depth $maxDepth")
        maxDepth = newMaxDepth
        return Node(-1, 0)
    }

    fun initCounters(depth: Int): Array<Node?> {
        if (depth < maxDepth)
            return Array(6) { Node(it, depth + 1) }
        else
            return Array(6) { null }
    }

    class Node(
        val key: Int,
        val depth: Int,
        val counters: Array<Node?> = initCounters(depth)
    )

    fun findCombinationsOfLengthX(tree: Node, depth: Int, collector: (Array<Int>) -> Unit) {
        fun findCombRecursive(tree: Node, depth: Int, acc: Array<Int>) {
            if (depth > 0) {
                tree.counters.forEach { node ->
                    acc[depth - 1] = node!!.key
                    findCombRecursive(node, depth - 1, acc)
                }
            } else
                collector(acc)
        }
        findCombRecursive(tree, depth, Array(depth) { 0 })
    }

    fun validPermutations(valuesUsed: List<Int>, numberOfDice: Int, game: Game, currentTile: Tile): Any {
        class Counter(var value: Int)

        val numberOfValuesLeft = 6 - valuesUsed.size

        val stealableTiles =
            game.players.mapNotNull { player -> player.tilesWon.lastOrNull() }

        val lowestTileLeft =
            game.board.tiles.first { tile -> tile > currentTile }

        // TDOO: buildTree and filter
        return 10
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
