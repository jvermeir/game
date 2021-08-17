package spel

/*
TODO:
 - lose if there's no worm
 - replace top tile if player looses round
 - allow stealing tiles from other players
 - try out different strategy
 - allow human players to participate
 */

var logLevel = 1

fun main() {
    val board = Board("myBoard")
    val player1 = Player("1", mutableListOf(), ThrowToFirstTileStrategy())
    val player2 = Player("2", mutableListOf(), ThrowToFirstTileStrategy())
    val game = Game(board, arrayOf(player1, player2))
    game.play()
    game.printGameResult()
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

data class Tile(val value: Int) {}

val NullTile = Tile(0)

data class PlayResult(val moves: List<Move>, val board: Board)

data class Player(val name: String, val tilesWon: MutableList<Tile> = mutableListOf(), val strategy: Strategy) {
    fun doTurn(board: Board): Player {
        log(2, "doTurn (start): $name playing, tilesWon: $tilesWon")

        val turn = Turn(strategy, board)
        val (moves, _) = turn.play()
        if (moves.last() is StopTurnMove) {
            tilesWon.add(turn.tileSelected)
        }
        log(2,"doTurn (end): $name playing, tilesWon: $tilesWon")
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
        log(2,"MakeMove with moves = $moves, numberOfDiceLeft: $numberOfDiceLeft, facesUses: $facesUsed")
// TODO: test for this case (no dice can be selected after a throw)
        val nextMove = strategy.makeMove(this)
        val selected = strategy.selectDiceFromThrow(nextMove.resultOfThrow, this)
        log(2,"nextMove: $nextMove, throwing: ${nextMove.resultOfThrow}, selected: $selected")
        if ( selected.isEmpty()) {
            moves = moves + PlayFailedMove(0)
            return PlayFailedMove(0)
        }

        nextMove.diceSelected = selected
        facesUsed = facesUsed + selected.first()
        numberOfDiceLeft -= selected.size
        moves = moves + nextMove
        log(2,"moves: $moves")
        val total = moves.sumBy{move -> move.diceSelected.sumBy { dice -> dice.numericValue }}
        log(2,"total: $total")

        if (strategy.shouldIContinue(moves, board)) {
            log(2,"continue")
            return nextMove
        }
        log(2,"stop")
        val takeTileMove = TakeTileMove(0)
        takeTileMove.takeBestTile(board, this)
        tileSelected = takeTileMove.bestTile
        moves = moves + takeTileMove + StopTurnMove(0)
        log(2,"tileSelected: $tileSelected, moves: $moves")
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
        log(2,"moves in takeTileMove: $board.moves")
        val totalValue = totalValueOfDice(turn.moves)
        bestTile = highestTileWithValueNotBiggerThanX(totalValue, board.tiles)
        log(2,"bestTile: $bestTile, totalValue: $totalValue")
        if (bestTile.value==0)
            log(1,"Error: Tile(0) selected")
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
        log(2,"this shouldn't happen")
        return false
    }

    open fun selectDiceFromThrow(diceInThrow: List<Dice>, turn: Turn): List<Dice> {
        return listOf()
    }
}

class ThrowToFirstTileStrategy() : Strategy() {
    override fun shouldIContinue(moves: List<Move>, board:Board): Boolean {
        log(2,"shouldIContinue, board: $board")
        val totalValue = totalValueOfDice(moves)
        log(2,"shouldIContinue, totalValue: $totalValue")
        if (totalValue == 0) return true
        val x = highestTileWithValueNotBiggerThanX(totalValue, board.tiles).value
        log(2,"x: $x")
        if (x == 0) return true
        if (x <= totalValue) return false
        return true
    }

    override fun makeMove(turn: Turn): Move {
        return ThrowDiceMove(turn.numberOfDiceLeft)
    }

    override fun selectDiceFromThrow(diceInThrow: List<Dice>, turn: Turn): List<Dice> {
        val diceAllowed = diceInThrow.minus(turn.facesUsed)
        if (diceAllowed.isEmpty()) return listOf()
        val highestValueInThrow = diceAllowed.sortedBy { dice -> dice.value }.last()
        return diceAllowed.filter { dice -> dice.value == highestValueInThrow.value }
    }
}

fun totalValueOfDice(moves: List<Move>) =
    moves.sumBy { move -> move.diceSelected.sumBy { dice -> dice.numericValue } }

fun highestTileWithValueNotBiggerThanX(value: Int, tiles: List<Tile>): Tile {
    val possibleSolutions = tiles.filter { tile -> tile.value <= value }
    log(2,"highestTileWithValueNotBiggerThanX, possibleSolutions: $possibleSolutions")
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
        return tiles.isEmpty()
    }
}

fun log(level: Int, message: String) {
    if (level <= logLevel) println(message)
}