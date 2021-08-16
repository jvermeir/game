package spel

/*
TODO:
 - done: stop game if all tiles are used
 - replace top tile if player looses round
 - allow stealing tiles from other players
 - play game
 - try out different strategy
 - allow human players to participate
 */

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
}

data class Tile(val value: Int) {}

val NullTile = Tile(0)

data class PlayResult(val moves: List<Move>, val board: Board)

data class Player(val name: String, val tilesWon: MutableList<Tile> = mutableListOf(), val strategy: Strategy) {
    fun doTurn(board: Board): Player {
        val turn = Turn(strategy, board)
        val (moves, _) = turn.play()
        if (moves.last() is StopTurnMove) {
            tilesWon.add(turn.tileSelected)
        }
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
        return PlayResult(moves + PlayFailedMove(0), board)
    }

    fun movesAreStillPossible(): Boolean {
        if (numberOfDiceLeft == 0) return false
        if (facesUsed.size == 6) return false
        if (board.tiles.isEmpty()) return false
        return true
    }

    fun hasStopped(): Boolean {
        return moves.isNotEmpty() && moves.last().stopped()
    }

    fun makeMove(): Move {
        val nextMove = strategy.makeMove(this)
        val selected = strategy.selectDiceFromThrow(nextMove.resultOfThrow, this)
        nextMove.diceSelected = selected
        facesUsed = facesUsed + selected.first()
        numberOfDiceLeft -= selected.size
        moves = moves + nextMove
        if (strategy.shouldIContinue(moves)) {
            return nextMove
        }
        val takeTileMove = TakeTileMove(0)
        takeTileMove.takeBestTile(board, this)
        tileSelected = takeTileMove.bestTile
        moves = moves + takeTileMove + StopTurnMove(0)
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
    fun doThrow(): List<Dice> {
        return Throw(diceRemaining).doThrow()
    }
}

class TakeTileMove(override val diceRemaining: Int) : Move(diceRemaining) {
    var bestTile: Tile = NullTile
    fun takeBestTile(board: Board, turn: Turn) {
        val totalValue = totalValueOfDice(turn.moves)
        bestTile = highestTileWithValueNotBiggerThanX(totalValue, board.tiles)
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

    open fun shouldIContinue(moves: List<Move>): Boolean {
        return false
    }

    open fun selectDiceFromThrow(diceInThrow: List<Dice>, turn: Turn): List<Dice> {
        return listOf()
    }
}

class ThrowToFirstTileStrategy(val board: Board) : Strategy() {
    override fun shouldIContinue(moves: List<Move>): Boolean {
        val totalValue = totalValueOfDice(moves)
        if (totalValue == 0) return true
        val x = highestTileWithValueNotBiggerThanX(totalValue, board.tiles).value
        if (x == 0) return true
        if (x <= totalValue) return false
        return true
    }

    override fun makeMove(turn: Turn): Move {
        return ThrowDiceMove(turn.numberOfDiceLeft)
    }

    override fun selectDiceFromThrow(diceInThrow: List<Dice>, turn: Turn): List<Dice> {
        val diceAllowed = diceInThrow.minus(turn.facesUsed)
        val highestValueInThrow = diceAllowed.sortedBy { dice -> dice.value }.last()
        return diceAllowed.filter { dice -> dice.value == highestValueInThrow.value }
    }
}

fun totalValueOfDice(moves: List<Move>) =
    moves.sumBy { move -> move.diceSelected.sumBy { dice -> dice.numericValue } }

fun highestTileWithValueNotBiggerThanX(value: Int, tiles: List<Tile>): Tile {
    val possibleSolutions = tiles.filter { tile -> tile.value <= value }
    if (possibleSolutions.isEmpty()) return NullTile
    return possibleSolutions.last()
}


data class Board(val name: String) {
    var firstTile = Tile(21)
    var lastTile = Tile(36)

    var tiles: List<Tile> = (firstTile.value..lastTile.value).map { value -> Tile(value) }

    constructor(listOfTiles: List<Tile>, name: String) : this(name) {
        tiles = listOfTiles
    }

    fun remove(tile: Tile): Board {
        tiles = tiles.filter { t -> t != tile }
        return this
    }

    fun empty(): Boolean {
        return tiles.size == 0
    }
}
