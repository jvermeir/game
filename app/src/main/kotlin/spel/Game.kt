package spel

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int

/*
TODO:
 - refactor tests to fix duplication
 - allow stealing tiles from other players
 - try out different strategy (steal if possible, try for >30 tile if player owns no tiles yet)
 - allow human players to participate
 */

fun main(args: Array<String>) = Simulator().main(args)

class Simulator : CliktCommand() {
    private val numberOfPlayers: Int by option(help = "Number of players").int().default(3)
    private val numberOfRuns: Int by option(help = "Number of runs").int().default(1)

    override fun run() {
        val results: MutableList<Result> = mutableListOf()
        for (run in 1..numberOfRuns) {
            val players =
                (1..numberOfPlayers).map { i -> Player("$i", mutableListOf(), StopAfterFirstTileStrategy()) }
                    .toTypedArray()
            val board = Board("myBoard")
            val game = Game(board, players)
            game.play()
            game.printGameResult()
            println("--")
            results.add(Result(game.players))
        }
        val sortedResults = results.sortedByDescending { it.totalValue(it.winner.tilesWon) }.take(10)
        sortedResults.forEach { result -> println("name: ${result.winner.name}, tiles:${result.winner.tilesWon}")}
    }
}

data class Result(val players: Array<Player>) {
   val winner = players.maxByOrNull { totalValue(it.tilesWon) }!!

    fun totalValue(tiles: List<Tile>):Int {
        return tiles.sumBy { tile -> tile.score }
    }
}

data class Game(val board: Board = Board("spel"), val players: Array<Player>) {
    private var currentPlayer = 0
    fun getCurrentPlayer() = players[currentPlayer]

    fun play() {
        while (!board.empty()) {
            currentPlayerMakesMove()
        }
    }

    fun currentPlayerMakesMove() {
        getCurrentPlayer().doTurn(board)
        currentPlayer = (currentPlayer + 1) % players.size
    }

    fun printGameResult() {
        players.iterator().forEach { player -> println(player) }
    }
}

data class Tile(val value: Int) {
    val score: Int =
        if (value == 0) 0
        else if (value < 27) 1
        else if (value < 32) 2
        else 3
}

val NullTile = Tile(0)

data class PlayResult(val moves: List<Move>, val board: Board)

data class Player(val name: String, val tilesWon: MutableList<Tile> = mutableListOf(), val strategy: Strategy) {
    val turns = mutableListOf<Turn>()
    fun doTurn(board: Board): Player {
        Logger.log(2, "doTurn (start): $name playing, tilesWon: $tilesWon")

        val turn = Turn(strategy, board)
        turns.add(turn)
        val (moves, _) = turn.play()
        if (moves.last() is StopTurnMove) {
            tilesWon.add(turn.tileSelected)
        } else if (moves.last() is PlayFailedMove) {
            if (tilesWon.isNotEmpty()) {
                Logger.log(2, "removing $tilesWon.last()")
                tilesWon.removeLast()
            }
        }
        Logger.log(2, "doTurn (end): $name playing, tilesWon: $tilesWon")
        return Player(name, tilesWon, strategy)
    }
}

data class Turn(val strategy: Strategy, val board: Board) {
    var moves: List<Move> = listOf()
    var numberOfDiceLeft = 8
    var facesUsed: List<Dice> = listOf()
    var tileSelected: Tile = NullTile

    fun play(): PlayResult {
        while (movesAreStillPossible() && !hasStopped()) {
            makeMove()
        }
        if (hasStopped()) {
            return PlayResult(moves, board)
        }
        moves = moves + PlayFailedMove(0)
        return PlayResult(moves, board)
    }

    fun movesAreStillPossible(): Boolean {
        if (numberOfDiceLeft == 0) return false
        if (facesUsed.size == 6) return false
        if (board.tiles.isEmpty()) return false
        return true
    }

    private fun hasStopped(): Boolean {
        return moves.isNotEmpty() && moves.last().stopped()
    }

    private fun makeMove(): Move {
        Logger.log(2, "MakeMove with moves = $moves, numberOfDiceLeft: $numberOfDiceLeft, facesUses: $facesUsed")
        val nextMove = strategy.makeMove(this)
        val selected = strategy.selectDiceFromThrow(nextMove.resultOfThrow, this)
        Logger.log(2, "nextMove: $nextMove, throwing: ${nextMove.resultOfThrow}, selected: $selected")
        if (selected.isEmpty()) {
            moves = moves + PlayFailedMove(0)
            return PlayFailedMove(0)
        }

        nextMove.diceSelected = selected
        facesUsed = facesUsed + selected.first()
        numberOfDiceLeft -= selected.size
        moves = moves + nextMove
        Logger.log(2, "moves: $moves")
        val total = moves.sumBy { move -> move.diceSelected.sumBy { dice -> dice.numericValue } }
        Logger.log(2, "total: $total")

        if (strategy.shouldIContinue(moves, board)) {
            Logger.log(2, "continue")
            return nextMove
        }
        Logger.log(2, "stop")
        val takeTileMove = TakeTileMove(0)
        takeTileMove.takeBestTile(board, this)
        tileSelected = takeTileMove.bestTile
        moves = moves + takeTileMove + StopTurnMove(0)
        Logger.log(2, "tileSelected: $tileSelected, moves: $moves")
        return StopTurnMove(0)
    }
}

open class Move(open val diceRemaining: Int) {
    open val resultOfThrow: List<Dice> = listOf()
    open var diceSelected: List<Dice> = listOf()
    open fun stopped(): Boolean {
        return false
    }
}

data class PlayFailedMove(override val diceRemaining: Int) : Move(diceRemaining) {
    override fun stopped(): Boolean {
        return true
    }
}

data class ThrowDiceMove(override val diceRemaining: Int) : Move(diceRemaining) {
    override val resultOfThrow: List<Dice> = doThrow()
    private fun doThrow(): List<Dice> {
        return Throw(diceRemaining).doThrow()
    }
}

class TakeTileMove(override val diceRemaining: Int) : Move(diceRemaining) {
    var bestTile: Tile = NullTile
    fun takeBestTile(board: Board, turn: Turn) {
        Logger.log(2, "moves in takeTileMove: $board.moves")
        val totalValue = totalValueOfDice(turn.moves)
        bestTile = highestTileWithValueNotBiggerThanSumOfDice(totalValue, board.tiles)
        Logger.log(2, "bestTile: $bestTile, totalValue: $totalValue")
        if (bestTile.value == 0)
            Logger.log(1, "Error: Tile(0) selected")
        board.remove(bestTile)
    }
}

data class StopTurnMove(override val diceRemaining: Int) : Move(diceRemaining) {
    override fun stopped(): Boolean {
        return true
    }
}

open class Strategy {
    open fun makeMove(turn: Turn): Move {
        return StopTurnMove(0)
    }

    open fun shouldIContinue(moves: List<Move>, board: Board): Boolean {
        Logger.log(2, "this shouldn't happen")
        return false
    }

    open fun selectDiceFromThrow(diceInThrow: List<Dice>, turn: Turn): List<Dice> {
        return listOf()
    }
}

class StopAfterFirstTileStrategy : Strategy() {
    /*
    Using this strategy, the game always ends with 1 player in possession of 1 tile, the 36 tile.
     */
    override fun shouldIContinue(moves: List<Move>, board: Board): Boolean {
        Logger.log(2, "shouldIContinue, board: $board")
        val totalValue = totalValueOfDice(moves)
        Logger.log(2, "shouldIContinue, totalValue: $totalValue")
        if (totalValue == 0) return true
        if (moves.none { move -> move.diceSelected.contains(Dice(6)) }) return true

        val highestTile = highestTileWithValueNotBiggerThanSumOfDice(totalValue, board.tiles).value
        Logger.log(2, "highestTile: $highestTile")
        if (highestTile == 0) return true
        if (highestTile <= totalValue) return false
        return true
    }

    override fun makeMove(turn: Turn): Move {
        return ThrowDiceMove(turn.numberOfDiceLeft)
    }

    override fun selectDiceFromThrow(diceInThrow: List<Dice>, turn: Turn): List<Dice> {
        val diceAllowed = diceInThrow.minus(turn.facesUsed)
        if (diceAllowed.isEmpty()) return listOf()
        val highestValueInThrow = diceAllowed.maxByOrNull { dice -> dice.value }!!
        return diceAllowed.filter { dice -> dice.value == highestValueInThrow.value }
    }
}

class ContinueIfMoreThanFiveDiceAreLeftStrategy : Strategy() {
    // TODO
}

class FavourTripleThrowsStrategy : Strategy() {
    // ie.: 3x4-dice > 2x5-dice > 1xWorm

}

class ContinueIfOddsAreHighEnoughStrategy : Strategy() {
    // if a player could take a tile but the odds of winning a higher value tile are better than 50% -> continue
}


fun totalValueOfDice(moves: List<Move>) =
    moves.sumBy { move -> move.diceSelected.sumBy { dice -> dice.numericValue } }

fun highestTileWithValueNotBiggerThanSumOfDice(value: Int, tiles: List<Tile>): Tile {
    val possibleSolutions = tiles.filter { tile -> tile.value <= value }
    Logger.log(2, "highestTileWithValueNotBiggerThanX, possibleSolutions: $possibleSolutions")
    if (possibleSolutions.isEmpty()) return NullTile
    return possibleSolutions.last()
}


data class Board(val name: String) {
    private val firstTile = Tile(21)
    private val lastTile = Tile(36)

    var tiles: List<Tile> = (firstTile.value..lastTile.value).map { value -> Tile(value) }

    constructor(listOfTiles: List<Tile>, name: String) : this(name) {
        tiles = listOfTiles
    }

    fun remove(tile: Tile): Board {
        tiles = tiles.filter { t -> t != tile }
        return this
    }

    fun empty(): Boolean {
        return tiles.isEmpty()
    }
}

object Logger {
    var logLevel = 1
    fun log(level: Int, message: String) {
        if (level <= logLevel) println(message)
    }
}
