package spel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameTest {
    private val a23And25Board = Board(listOf(Tile(23), Tile(25)), "a23And25Board")
    private val a23To25Board = Board(listOf(Tile(23), Tile(24), Tile(25)), "a23To25Board")
    private val player1 = Player("1", listOf<Tile>(), Strategy(), Board("myBoard"))
    private val player2 = Player("2", listOf<Tile>(), Strategy(), Board("myBoard"))
    private val twoPlayers = arrayOf(player1, player2)

    val gameWith23and25: Game = Game(a23And25Board, twoPlayers)
    val gameWith23through25: Game = Game(a23To25Board, twoPlayers)

    @Test
    fun testTileCanBeRemovedByValue() {
        val gameWithout24: Game = gameWith23through25.remove(Tile(24))
        assertEquals(gameWithout24.board.tiles, gameWith23and25.board.tiles)
    }

    @Test
    fun test3PlayersTakeTurns() {
        val game = Game(Board("myBoard"), twoPlayers)
        assertEquals(player1, game.getCurrentPlayer())
        game.currentPlayerMakesMove()
        assertEquals(player2, game.getCurrentPlayer())
        game.currentPlayerMakesMove()
        assertEquals(player1, game.getCurrentPlayer())
    }
}

class ThrowToFirstTileStrategyTest {
    @Test
    fun testHighestValueUnusedDiceIsSelected() {
        val board = Board("myBoard")
        val strategy = ThrowToFirstTileStrategy(board)
        val turn = Turn(strategy, board)

        val t1 = listOf(Dice(6), Dice(6), Dice(6), Dice(5), Dice(5), Dice(5), Dice(1), Dice(2))
        val selectDiceFromThrow = strategy.selectDiceFromThrow(t1, turn)
        assertEquals(listOf(Dice(6), Dice(6), Dice(6)), selectDiceFromThrow, "first throw should select 3 worms")

        turn.facesUsed = listOf(Dice(6))
        val t2 = listOf(Dice(6), Dice(6), Dice(6), Dice(5), Dice(5))
        val selectDiceFromSecondThrow = strategy.selectDiceFromThrow(t2, turn)
        assertEquals(
            listOf(Dice(5), Dice(5)),
            selectDiceFromSecondThrow,
            "second throw should select second highest value, ie. 2 times 5"
        )
    }

    @Test
    fun testShouldStopWhenTakingATileIsPossibleIfUsingFirstTileStrategy() {
        val listOfThrows = listOf(
            Dice(6), Dice(6), Dice(6), Dice(5), Dice(5), Dice(5), Dice(1), Dice(2),
            Dice(6), Dice(6), Dice(6), Dice(5), Dice(5), Dice(5), Dice(1), Dice(2)
        ).stream().iterator()

        fun testThrowDiceMethod(): Dice {
            return listOfThrows.next()
        }

        Config.throwDiceMethod = ::testThrowDiceMethod

        val board = Board("board at start of game")
        val turn = Turn(ThrowToFirstTileStrategy(board), board) // TODO: meuh...
        val (moves, _) = turn.play()
        assertTrue(
            moves.first() is ThrowDiceMove &&
                    moves[1] is ThrowDiceMove &&
                    moves[2] is TakeTileMove &&
                    moves[3] is StopTurnMove, "expecting 2 ThrowDiceMoves, 1 TakeTileMove and 1 StopTurnMove"
        )
        assertEquals(Tile(25), (moves[2] as TakeTileMove).bestTile, "Expecting tile 25 to be selected")
    }
}

class TurnTest {
    @Test
    fun testMovesArePossibleConditions() {
        val board = Board("board at start of game")
        val turn = Turn(Strategy(), board)
        assertTrue(turn.movesAreStillPossible(), "moves should still be possible if there are no previous moves")

        turn.numberOfDiceLeft = 2
        turn.facesUsed = listOf(Dice(6))
        assertTrue(turn.movesAreStillPossible(), "moves should still be possible if only Dice(1) is used")
    }

    @Test
    fun testEndConditions() {
        val board = Board("board at start of game")
        val turn = Turn(Strategy(), board)
        turn.numberOfDiceLeft = 0
        assertFalse(turn.movesAreStillPossible(), "moves should be impossible if all dice were used")

        turn.numberOfDiceLeft = 2
        turn.facesUsed = listOf(Dice(6), Dice(1), Dice(2), Dice(3), Dice(4), Dice(5))
        assertFalse(turn.movesAreStillPossible(), "moves should be impossible if all faces were used")

        board.tiles = listOf()
        turn.facesUsed = listOf()
        assertFalse(turn.movesAreStillPossible(), "moves should be impossible if all tiles were used")
    }

    @Test
    fun testTurnShouldFailIfNoNewDiceCanBeSelected() {
        val listOfThrows = (1..16).map { i -> Dice(1) }.stream().iterator()

        fun testThrowDiceMethod(): Dice {
            return listOfThrows.next()
        }

        Config.throwDiceMethod = ::testThrowDiceMethod

        val board = Board("board at start of game")
        val turn = Turn(ThrowToFirstTileStrategy(board), board) // TODO: meuh...
        val (moves, _) = turn.play()
        assertEquals(2, moves.size, "expecting 2 moves")
        assertTrue(
            moves.first() is ThrowDiceMove &&
                    moves[1] is PlayFailedMove, "expecting 1 ThrowDiceMove and 1 PlayFailedMove"
        )
    }
}

class BoardTest {
    @Test
    fun testHighestValueScenarios() {
        val board = Board(listOf(Tile(21), Tile(22), Tile(23), Tile(36)), "21 22 23 36board")
        assertEquals(
            NullTile,
            highestTileWithValueNotBiggerThanX(20, board.tiles),
            "highest tile below the minimum value should be NullTile"
        )
        assertEquals(
            Tile(36),
            highestTileWithValueNotBiggerThanX(37, board.tiles),
            "highest tile below the minimum value should be NullTile"
        )
        assertEquals(
            Tile(22),
            highestTileWithValueNotBiggerThanX(22, board.tiles),
            "highest tile below the minimum value should be NullTile"
        )
        assertEquals(
            Tile(23),
            highestTileWithValueNotBiggerThanX(25, board.tiles),
            "highest tile below the minimum value should be NullTile"
        )
    }
}

fun <T> areListsEqual(first: List<T>, second: List<T>): Boolean {

    if (first.size != second.size) {
        return false
    }

    return first.zip(second).all { (x, y) -> x == y }
}
