package spel

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int

/*
TODO:
 - check rules...
 - refactor tests to fix duplication
 - try out different strategy (
    - take lower value tile if early in a throw
    - try for >30 tile if player owns no tiles yet
    - calculate possible outcomes and their odds
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
        sortedResults.forEach { result -> println("name: ${result.winner.name}, tiles:${result.winner.tilesWon}") }
    }
}

data class Result(val players: Array<Player>) {
    val winner = players.maxByOrNull { totalValue(it.tilesWon) }!!

    fun totalValue(tiles: List<Tile>): Int {
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
        getCurrentPlayer().doTurn(this)
        nextPlayer()
    }

    fun nextPlayer() {
        currentPlayer = (currentPlayer + 1) % players.size
    }

    fun printGameResult() {
        players.iterator().forEach { player -> println(player) }
    }
}

data class Tile(val value: Int) : Comparable<Tile> {
    val score: Int =
        if (value == 0) 0
        else if (value < 25) 1
        else if (value < 29) 2
        else if (value < 33) 3
        else 4

    override fun compareTo(other: Tile): Int {
        return this.value.compareTo(other.value)
    }
}

val NullTile = Tile(0)

data class PlayResult(val moves: List<Move>, val board: Board)

data class Player(val name: String, val tilesWon: MutableList<Tile> = mutableListOf(), val strategy: Strategy) {
    val turns = mutableListOf<Turn>()

    fun doTurn(game: Game): Player {
        Logger.log(2, "doTurn (start): $name playing, tilesWon: $tilesWon")

        val turn = Turn(strategy, game)
        turns.add(turn)
        val (moves, _) = turn.play()
        if (moves.last() is StopTurnMove) {
            tilesWon.add(turn.tileSelected)
        } else if (moves.last() is PlayFailedMove) {
            handlePlayFailedMove(game)
        }

        Logger.log(2, "doTurn (end): $name playing, tilesWon: $tilesWon")
        return Player(name, tilesWon, strategy)
    }

    private fun handlePlayFailedMove(game: Game) {
        if (tilesWon.isNotEmpty()) {
            val tileToBeReturned: Tile = tilesWon.removeLast()
            val lastTileOnTheBoard = game.board.tiles.last()
            if (lastTileOnTheBoard > tileToBeReturned) game.board.remove(lastTileOnTheBoard)
            val newTiles: MutableList<Tile> = mutableListOf()
            newTiles.addAll(game.board.tiles)
            newTiles.add(tileToBeReturned)
            newTiles.sort()
            game.board.tiles = newTiles
        }
    }

    fun getLastWonTile(): Tile {
        return if (tilesWon.isEmpty()) NullTile
        else tilesWon.last()
    }

}

data class Turn(val strategy: Strategy, val game: Game) {
    var moves: List<Move> = listOf()
    var numberOfDiceLeft = 8
    var facesUsed: List<Dice> = listOf()
    var tileSelected: Tile = NullTile

    fun play(): PlayResult {
        while (movesAreStillPossible() && !hasStopped()) {
            makeMove()
        }
        if (hasStopped()) {
            return PlayResult(moves, game.board)
        }
        moves = moves + PlayFailedMove(game.board, this)
        return PlayResult(moves, game.board)
    }

    fun movesAreStillPossible(): Boolean {
        if (numberOfDiceLeft == 0) return false
        if (facesUsed.size == 6) return false
        return true
    }

    private fun hasStopped(): Boolean {
        return moves.isNotEmpty() && moves.last().stopped()
    }

    private fun makeMove(): Move {
        Logger.log(2, "MakeMove with moves = $moves, numberOfDiceLeft: $numberOfDiceLeft, facesUses: $facesUsed")
        val nextMove = strategy.makeMove(game.board, this)
        val diceSelected = strategy.selectDiceFromThrow(nextMove.resultOfThrow, this)
        Logger.log(2, "nextMove: $nextMove, throwing: ${nextMove.resultOfThrow}, selected: $diceSelected")
        if (diceSelected.isEmpty()) {
            moves = moves + PlayFailedMove(game.board, this)
            return moves.last()
        }

        nextMove.diceSelected = diceSelected
        facesUsed = facesUsed + diceSelected.first()
        numberOfDiceLeft -= diceSelected.size
        moves = moves + nextMove
        Logger.log(2, "moves: $moves")
        val total = moves.sumBy { move -> move.diceSelected.sumBy { dice -> dice.numericValue } }
        Logger.log(2, "total: $total")

        if (strategy.shouldIContinue(moves, game)) {
            Logger.log(2, "continue")
            return nextMove
        }
        Logger.log(2, "stop")
        val takeTileMove = TakeTileMove(this, game)
        takeTileMove.makeMove()
        tileSelected = takeTileMove.tileSelected
        moves = moves + takeTileMove + StopTurnMove(this)
        Logger.log(2, "tileSelected: $tileSelected, moves: $moves")
        return StopTurnMove(this)
    }
}

open class Move {
    open val resultOfThrow: List<Dice> = listOf()
    open var diceSelected: List<Dice> = listOf()
    open var tileSelected: Tile = NullTile
    open fun makeMove(): Any {
        return false
    }

    open fun stopped(): Boolean {
        return false
    }
}

data class PlayFailedMove(val board: Board, val turn: Turn) : Move() {
    override fun stopped(): Boolean {
        return true
    }
}

data class ThrowDiceMove(val board: Board, val turn: Turn) : Move() {
    override val resultOfThrow: List<Dice> = Throw.throwDice(turn.numberOfDiceLeft)
}

data class TakeTileMove(val turn: Turn, val game: Game) : Move() {
    override fun makeMove(): Tile {
        Logger.log(2, "moves in takeTileMove: ${turn.moves}")

        val totalValue = totalValueOfDice(turn.moves)
        val tileSelectedUsingDice = highestTileWithValueNotBiggerThanSumOfDice(totalValue, game.board.tiles)

        val playerWithTileThatCanBeStolen = findPlayerWithTileThatCanBeStolen(totalValue, game)
        val tileThatCanBeStolen = findTileThatCanBeStolen(playerWithTileThatCanBeStolen)
        tileSelected = if (tileThatCanBeStolen > tileSelectedUsingDice) {
            playerWithTileThatCanBeStolen!!.tilesWon.removeLast()
            tileThatCanBeStolen
        } else {
            tileSelectedUsingDice
        }

        if (tileSelected.value == 0)
            Logger.log(1, "Error: Tile(0) selected")

        game.board.remove(tileSelected)

        Logger.log(2, "bestTile: $tileSelected, totalValue: $totalValue")
        return tileSelected
    }
}

data class StopTurnMove(val turn: Turn) : Move() {
    override fun stopped(): Boolean {
        return true
    }
}

// TODO: abstract methods?
open class Strategy {
    open fun makeMove(board: Board, turn: Turn): Move {
        return StopTurnMove(turn)
    }

    open fun shouldIContinue(moves: List<Move>, game: Game): Boolean {
        Logger.log(2, "this shouldn't happen")
        return false
    }

    open fun selectDiceFromThrow(diceInThrow: List<Dice>, turn: Turn): List<Dice> {
        return listOf()
    }
}

fun findPlayerWithTileThatCanBeStolen(totalValue: Int, game: Game): Player? {
    return game.players.asList()
        .filter { player -> player != game.getCurrentPlayer() }
        .firstOrNull { player -> player.getLastWonTile().value == totalValue }
}

fun findTileThatCanBeStolen(playerWithTileThatCanBeStolen: Player?): Tile {
    if (playerWithTileThatCanBeStolen == null) return NullTile
    else return playerWithTileThatCanBeStolen.getLastWonTile()
}

class StopAfterFirstTileStrategy : Strategy() {
    /*
    Using this strategy, the game always ends with 1 player in possession of 1 tile (sometimes 2), most often a higher value one like 34 or 36.
     */
    override fun shouldIContinue(moves: List<Move>, game: Game): Boolean {
        Logger.log(2, "shouldIContinue, board: ${game.board}")
        val totalValue = totalValueOfDice(moves)
        Logger.log(2, "shouldIContinue, totalValue: $totalValue")
        if (totalValue == 0) return true
        if (moves.none { move -> move.diceSelected.contains(Dice(6)) }) return true

        val highestTile = highestTileWithValueNotBiggerThanSumOfDice(totalValue, game.board.tiles).value

        val playerWithTileThatCanBeStolen = findPlayerWithTileThatCanBeStolen(totalValue, game)
        val tileThatCanBeStolen =
            findTileThatCanBeStolen(playerWithTileThatCanBeStolen)

        Logger.log(2, "highestTile: $highestTile, tileThatCanBeStolen: $tileThatCanBeStolen")

        if (tileThatCanBeStolen.value == totalValue) return false
        if (highestTile != 0 && highestTile <= totalValue) return false

        return true
    }

    override fun makeMove(board: Board, turn: Turn): Move {
        return ThrowDiceMove(board, turn)
    }

    // TODO: alternative would be to select dice that add up to a stealable value or the value of a tile that is still available.
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
