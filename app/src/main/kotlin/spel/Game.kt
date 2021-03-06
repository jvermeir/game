package spel

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kotlin.math.pow
import kotlin.system.exitProcess

/*
TODO:
 - try out different strategy (
    - choose lower value dice if early in round and only a single worm or 5 dice is thrown
    - be more conservative if top tile on your stack has value bigger than 1
 - allow human players to participate
 */

fun main(args: Array<String>) = Simulator().main(args)

class Simulator : CliktCommand() {

    private val numberOfPlayers: Int by option(help = "Number of players").int().default(4)
    private val numberOfRuns: Int by option(help = "Number of runs").int().default(10)
    private val printResults: Boolean by option(
        "--print",
        help = "Print outcome of each simulated game"
    ).flag(default = false)

    override fun run() {
        val strategies = listOf(
            StopAfterFirstTileStrategy(),
            ContinueIfOddsAreHighEnoughStrategy(),
            StopEarlyFavorHighSumStrategy(),
            ContinueIfOddsAreHighEnoughFavorHighestSumStrategy(),
            TakeMoreRiskIfPlayerDoesntOwnTilesStrategy()
        )
        if (numberOfPlayers.mod(strategies.size) != 0) {
            println("number-of-players should be a multiple of the number of strategies (${strategies.size})")
            exitProcess(-1)
        }

        val numberOfPlayersPerStrategy = numberOfPlayers.div(strategies.size)

        val results = (1..numberOfRuns).map { run ->
            doARun(numberOfPlayersPerStrategy, strategies)
        }
        val sortedResults = results.sortedByDescending { it.totalValue(it.winner.tilesWon) }

        println("top 10")
        sortedResults.take(10)
            .forEach { result ->
                println(result.toString())
            }

        println("bottom 10")
        sortedResults.takeLast(10)
            .forEach { result ->
                println(result.toString())
            }

        println("wins by strategy")
        results.groupBy { r -> r.winner.strategy.id }
            .entries.sortedByDescending { it.value.size }.associateBy({ it.key }, { it.value })
            .forEach { entry -> println("${entry.key}: ${entry.value.size}") }
    }

    private fun doARun(
        numberOfPlayersPerStrategy: Int,
        strategies: List<Strategy>,
    ): Result {
        val players: ArrayDeque<Player> = ArrayDeque()
        (1..numberOfPlayersPerStrategy).forEach {
            strategies.forEach { strategy ->
                players.add(
                    Player(
                        "${strategy.javaClass.name} - $it",
                        mutableListOf(),
                        strategy
                    )
                )
            }
        }

        val board = Board("myBoard")
        val game = Game(board, players)
        game.play()
        if (printResults) game.printGameResult()
        return Result(game.players, game.numberOfTurns / game.players.size)
    }

    data class Result(val players: ArrayDeque<Player>, val numberOfTurns: Int) {
        private val playersSortedByValue = players
            .map { player -> Pair(player, totalValue(player.tilesWon)) }
            .sortedByDescending { pair -> pair.second }

        val winner = players.maxByOrNull { totalValue(it.tilesWon) }!!

        fun totalValue(tiles: List<Tile>): Int {
            return tiles.sumOf { tile -> tile.score }
        }

        private val tilesOwnedByAPlayer = players
            .map { player -> player.tilesWon }
            .flatten()
            .sorted()

        private val tilesNotOwned =
            (Board.firstTile.value..Board.lastTile.value)
                .map { value -> Tile(value) }
                .minus(tilesOwnedByAPlayer)
                .sorted()

        override fun toString(): String {
            return "name: ${winner.name}, " +
                    "value: ${playersSortedByValue.first().second}, " +
                    "numberOfTilesLost: ${playersSortedByValue.first().first.tilesLost.size}, " +
                    "numberOfTurns: ${numberOfTurns}, " +
                    "tilesOwnedByAPlayer:${tilesOwnedByAPlayer.sortedBy { t -> t.value }}, " +
                    "tilesNotOwned:${tilesNotOwned}"
        }
    }
}

data class Game(val board: Board = Board("spel"), val players: ArrayDeque<Player>) {
    var numberOfTurns: Int = 0
    fun getCurrentPlayer(): Player = players.first()

    fun play() {
        while (!board.empty()) {
            numberOfTurns++
            currentPlayerMakesMove()
        }
    }

    fun currentPlayerMakesMove() {
        getCurrentPlayer().doTurn(this)
        nextPlayer()
    }

    fun nextPlayer() {
        players.add(players.removeFirst())
    }

    fun printGameResult() {
        players.iterator().forEach { player -> println(player) }
        println("--")
    }
}

data class Tile(val value: Int) : Comparable<Tile> {
    val score: Int =
        if (value < 21) 0
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
    val tilesLost = mutableListOf<Tile>()

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
            val newTiles: MutableList<Tile> = mutableListOf(tileToBeReturned)
            newTiles.addAll(game.board.tiles)
            newTiles.sort()
            game.board.tiles = newTiles
            tilesLost.add(tileToBeReturned)
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
        val total = moves.sumOf { move -> move.diceSelected.sumOf { dice -> dice.numericValue } }
        Logger.log(2, "total: $total")

        if (strategy.shouldIContinue(moves, game, this)) {
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
        tileSelected =
            if (tileThatCanBeStolen > tileSelectedUsingDice) {
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

fun findPlayerWithTileThatCanBeStolen(totalValue: Int, game: Game): Player? {
    return game.players
        .filter { player -> player != game.getCurrentPlayer() }
        .firstOrNull { player -> player.getLastWonTile().value == totalValue }
}

fun findTileThatCanBeStolen(playerWithTileThatCanBeStolen: Player?): Tile {
    return playerWithTileThatCanBeStolen?.getLastWonTile() ?: NullTile
}

abstract class Strategy {

    abstract fun makeMove(board: Board, turn: Turn): Move

    abstract fun shouldIContinue(moves: List<Move>, game: Game, turn: Turn): Boolean

    abstract fun selectDiceFromThrow(diceInThrow: List<Dice>, turn: Turn): List<Dice>
    abstract val id: String
}

open class StopAfterFirstTileStrategy : Strategy() {
    // Stop as soon as a tile can be taken
    override fun shouldIContinue(moves: List<Move>, game: Game, turn: Turn): Boolean {
        if (moves.none { move -> move.diceSelected.contains(Dice(6)) }) return true

        val totalValue = totalValueOfDice(moves)
        val currentBestTileValue = findCurrentBestTileValue(game, totalValue)
        if (currentBestTileValue == 0) return true

        return false
    }

    override fun makeMove(board: Board, turn: Turn): Move {
        return ThrowDiceMove(board, turn)
    }

    override fun selectDiceFromThrow(diceInThrow: List<Dice>, turn: Turn): List<Dice> {
        return selectDiceFromThrowUsingHighestValueDice(diceInThrow, turn)
    }

    override val id = "StopAfterFirstTileStrategy"
}

class StopEarlyFavorHighSumStrategy : StopAfterFirstTileStrategy() {
    // Stop as soon as a tile can be taken, but favor higher totals instead of higher value dice when selecting a die
    override fun selectDiceFromThrow(diceInThrow: List<Dice>, turn: Turn): List<Dice> {
        return selectDiceFromThrowUsingHighestTotalValue(diceInThrow, turn)
    }

    override val id = "StopEarlyFavorHighSumStrategy"
}

open class ContinueIfOddsAreHighEnoughStrategy(var cutOffPercentage: Int = 50) : Strategy() {
    // if a player could take a tile but the odds of winning a higher value tile are better than X% -> continue
    override fun makeMove(board: Board, turn: Turn): Move {
        return ThrowDiceMove(board, turn)
    }

    override fun shouldIContinue(moves: List<Move>, game: Game, turn: Turn): Boolean {
        if (moves.none { move -> move.diceSelected.contains(Dice(6)) }) return true

        val totalValue = totalValueOfDice(moves)
        val currentBestTileValue = findCurrentBestTileValue(game, totalValue)
        if (currentBestTileValue == 0) return true
        if (currentBestTileValue > 29) return false

        val throwsThatAllowTakingAHigherTile =
            findNumberOfThrowsThatAllowTakingAHigherTile(totalValue, currentBestTileValue, game, turn)
        val totalCombinations = (6.0).pow(turn.numberOfDiceLeft)

        return (100 * throwsThatAllowTakingAHigherTile) / totalCombinations > cutOffPercentage
    }

    override fun selectDiceFromThrow(diceInThrow: List<Dice>, turn: Turn): List<Dice> {
        return selectDiceFromThrowUsingHighestValueDice(diceInThrow, turn)
    }

    override val id = "ContinueIfOddsAreHighEnoughStrategy"

    private fun findNumberOfThrowsThatAllowTakingAHigherTile(
        totalValue: Int,
        currentBestTileValue: Int,
        game: Game,
        turn: Turn
    ): Int {
        val currentBestTileScore = Tile(currentBestTileValue).score
        var throwsThatAllowTakingAHigherTile = 0

        fun countThrowsThatAllowTakingAHigherTile(dice: Array<Dice>) {
            val value =
                totalValue + selectDiceFromThrow(dice.toList(), turn).sumOf { d -> d.numericValue }

            val bestTile = highestTileWithValueNotBiggerThanSumOfDice(value, game.board.tiles)
            if (bestTile.score > currentBestTileScore) throwsThatAllowTakingAHigherTile++
        }

        Throw.traverseCombinationsOfLengthX(
            Throw.theTree,
            turn.numberOfDiceLeft,
            ::countThrowsThatAllowTakingAHigherTile
        )
        return throwsThatAllowTakingAHigherTile
    }
}

open class ContinueIfOddsAreHighEnoughFavorHighestSumStrategy : ContinueIfOddsAreHighEnoughStrategy() {
    // Continue if a player could take a tile but the odds of winning a higher value tile are better than X%,  but favor higher totals instead of higher value dice when selecting a die
    override fun selectDiceFromThrow(diceInThrow: List<Dice>, turn: Turn): List<Dice> {
        return selectDiceFromThrowUsingHighestTotalValue(diceInThrow, turn)
    }

    override val id = "ContinueIfOddsAreHighEnoughFavorHighestSumStrategy"
}

class TakeMoreRiskIfPlayerDoesntOwnTilesStrategy : ContinueIfOddsAreHighEnoughFavorHighestSumStrategy() {
    // If a player does not own tiles, there's no loss in trying to get a higher tile.
    // Note: a lower pct leads to lower rank. This strategy is comparable to basic ContinueIfOddsAreHighEnoughFavorHighestSumStrategy
    override fun shouldIContinue(moves: List<Move>, game: Game, turn: Turn): Boolean {
        if (game.getCurrentPlayer().tilesWon.isEmpty()) {
            super.cutOffPercentage = 40
        }
        return super.shouldIContinue(moves, game, turn)
    }

    override val id = "TakeMoreRiskIfPlayerDoesntOwnTilesStrategy"
}

fun findCurrentBestTileValue(game: Game, totalValue: Int): Int {
    val playerWithTileThatCanBeStolen = findPlayerWithTileThatCanBeStolen(totalValue, game)
    val tileThatCanBeStolen =
        findTileThatCanBeStolen(playerWithTileThatCanBeStolen)

    val highestTileThatCanBeTaken = highestTileWithValueNotBiggerThanSumOfDice(totalValue, game.board.tiles)
    return if (highestTileThatCanBeTaken.score > tileThatCanBeStolen.score)
        highestTileThatCanBeTaken.value
    else
        tileThatCanBeStolen.value
}

fun selectDiceFromThrowUsingHighestValueDice(diceInThrow: List<Dice>, turn: Turn): List<Dice> {
    val diceAllowed = diceInThrow.minus(turn.facesUsed)
    if (diceAllowed.isEmpty()) return listOf()
    val highestValueInThrow = diceAllowed.maxByOrNull { dice -> dice.value }!!
    return diceAllowed.filter { dice -> dice.value == highestValueInThrow.value }
}

fun selectDiceFromThrowUsingHighestTotalValue(diceInThrow: List<Dice>, turn: Turn): List<Dice> {
    val diceAllowed = diceInThrow
        .minus(turn.facesUsed)
    if (diceAllowed.isEmpty()) return listOf()

    val diceSortedByTotalValue = diceAllowed.groupBy { dice -> dice.value }
        .entries.sortedByDescending { entry -> entry.value.sumOf { it.value } }.associateBy({ it.key }, { it.value })
    val selected = diceSortedByTotalValue.entries.first().key

    return diceAllowed.filter { dice -> dice.value == selected }
}

fun totalValueOfDice(moves: List<Move>) =
    moves.sumOf { move -> move.diceSelected.sumOf { dice -> dice.numericValue } }

fun highestTileWithValueNotBiggerThanSumOfDice(value: Int, tiles: List<Tile>): Tile {
    val possibleSolutions = tiles.filter { tile -> tile.value <= value }
    Logger.log(3, "highestTileWithValueNotBiggerThanSumOfDice, possibleSolutions: $possibleSolutions")
    if (possibleSolutions.isEmpty()) return NullTile
    return possibleSolutions.last()
}

data class Board(val name: String) {
    companion object Board {
        val firstTile = Tile(21)
        val lastTile = Tile(36)
    }

    var tiles: List<Tile> = (firstTile.value..lastTile.value).map { value -> Tile(value) }

    constructor(listOfTiles: List<Tile>, name: String) : this(name) {
        tiles = listOfTiles
    }

    fun remove(tile: Tile): spel.Board {
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
