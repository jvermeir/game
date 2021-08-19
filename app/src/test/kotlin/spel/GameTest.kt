package spel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameTest {
    class TestStrategy : Strategy() {
        override fun selectDiceFromThrow(diceInThrow: List<Dice>, turn: Turn): List<Dice> {
            return listOf(Dice(25))
        }
    }

    private val player1 = Player("1", mutableListOf(), TestStrategy())
    private val player2 = Player("2", mutableListOf(), TestStrategy())
    private val twoPlayers = arrayOf(player1, player2)

    @Test
    fun test3PlayersTakeTurns() {
        val game = Game(Board("myBoard"), twoPlayers)
        assertEquals(player1, game.getCurrentPlayer())
        game.currentPlayerMakesMove()
        assertEquals(player2, game.getCurrentPlayer())
        game.currentPlayerMakesMove()
        assertEquals(player1, game.getCurrentPlayer())
    }

    @Test
    fun testGameStopsIfAllTilesAreUsed() {
        val board = Board("myBoard")
        board.tiles = listOf()
        val game = Game(board, twoPlayers)
        game.play()
        assertEquals(player1, game.getCurrentPlayer(), "expecting player1 to be current")
    }

    @Test
    fun testGameStopsIfAllTilesAreUsed2() {
        val listOfThrows = (1..16).map { Dice(6) }.stream().iterator()

        fun testThrowDiceMethod(): Dice {
            return listOfThrows.next()
        }

        Config.throwDiceMethod = ::testThrowDiceMethod

        val board = Board("myBoard")
        val player1 = Player("1", mutableListOf(), StopAfterFirstTileStrategy())
        val player2 = Player("2", mutableListOf(), StopAfterFirstTileStrategy())
        board.tiles = listOf(Tile(23))
        val game = Game(board, arrayOf(player1, player2))
        game.play()
        assertEquals(player2, game.getCurrentPlayer(), "expecting player2 to be current")
        assertEquals(Tile(23), player1.tilesWon.first(), "expecting player1 to have won Tile(23)")
        assertEquals(listOf(), player2.tilesWon, "expecting player2 to have won no tiles")
    }
}

class StopAfterFirstTileStrategyTest {
    @Test
    fun testHighestValueUnusedDiceIsSelected() {
        val board = Board("myBoard")
        val strategy = StopAfterFirstTileStrategy()
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
        val turn = Turn(StopAfterFirstTileStrategy(), board)
        val (moves, _) = turn.play()
        assertEquals(4, moves.size, "expecting 4 moves")
        assertTrue(
            moves.first() is ThrowDiceMove &&
                    moves[1] is ThrowDiceMove &&
                    moves[2] is TakeTileMove &&
                    moves[3] is StopTurnMove, "expecting 2 ThrowDiceMoves, 1 TakeTileMove and 1 StopTurnMove"
        )
        assertEquals(Tile(25), (moves[2] as TakeTileMove).bestTile, "Expecting tile 25 to be selected")
    }

    @Test
    fun testShouldStopWhenTakingATileIsPossibleIfUsingFirstTileStrategyAndTotalIsBiggerThanTileValue() {
        val listOfThrows = listOf(
            Dice(6), Dice(6), Dice(6), Dice(5), Dice(5), Dice(5), Dice(1), Dice(2),
            Dice(6), Dice(6), Dice(6), Dice(5), Dice(5), Dice(5), Dice(1), Dice(2)
        ).stream().iterator()

        fun testThrowDiceMethod(): Dice {
            return listOfThrows.next()
        }

        Config.throwDiceMethod = ::testThrowDiceMethod

        val board = Board("board at start of game")
        board.tiles = listOf(Tile(23))
        val turn = Turn(StopAfterFirstTileStrategy(), board)
        val (moves, _) = turn.play()
        assertEquals(4, moves.size, "expecting 4 moves")
        assertTrue(
            moves.first() is ThrowDiceMove &&
                    moves[1] is ThrowDiceMove &&
                    moves[2] is TakeTileMove &&
                    moves[3] is StopTurnMove, "expecting 2 ThrowDiceMoves, 1 TakeTileMove and 1 StopTurnMove"
        )
        assertEquals(Tile(23), (moves[2] as TakeTileMove).bestTile, "Expecting tile 23 to be selected")
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
    fun testTurnFailsIfNoNewDiceCanBeSelected() {
        val listOfThrows = (1..16).map { Dice(1) }.stream().iterator()

        fun testThrowDiceMethod(): Dice {
            return listOfThrows.next()
        }

        Config.throwDiceMethod = ::testThrowDiceMethod

        val board = Board("board at start of game")
        val turn = Turn(StopAfterFirstTileStrategy(), board)
        val (moves, _) = turn.play()
        assertEquals(2, moves.size, "expecting 2 moves")
        assertTrue(
            moves.first() is ThrowDiceMove &&
                    moves[1] is PlayFailedMove, "expecting 1 ThrowDiceMove and 1 PlayFailedMove"
        )
    }

    @Test
    fun testTurnContinuesUntilAtLeastOneWormIsThrown() {
        val listOfThrows = listOf(Dice(5), Dice(5), Dice(5), Dice(5), Dice(5), Dice(1),
            Dice(6), Dice(6), Dice(6), Dice(6), Dice(6), Dice(6)
        ).stream().iterator()

        fun testThrowDiceMethod(): Dice {
            return listOfThrows.next()
        }

        Config.throwDiceMethod = ::testThrowDiceMethod

        val board = Board("board at start of game")
        val turn = Turn(StopAfterFirstTileStrategy(), board)
        turn.numberOfDiceLeft = 6
        val (moves, _) = turn.play()
        assertEquals(4, moves.size, "expecting 4 moves")
        assertTrue(
            moves[0] is ThrowDiceMove &&
                   moves[1] is ThrowDiceMove &&
                   moves[2] is TakeTileMove &&
                   moves[3] is StopTurnMove
            , "expecting 2 ThrowDiceMoves, 1 TakeTileMove and 1 StopTurnMove"
        )
    }

    @Test
    fun testPlayerLosesTurnIfNoWormIsThrown() {
        val listOfThrows = listOf(Dice(5), Dice(5), Dice(5), Dice(5), Dice(5), Dice(1),
            Dice(1)
        ).stream().iterator()

        fun testThrowDiceMethod(): Dice {
            return listOfThrows.next()
        }

        Config.throwDiceMethod = ::testThrowDiceMethod

        val board = Board("board at start of game")
        val turn = Turn(StopAfterFirstTileStrategy(), board)
        turn.numberOfDiceLeft = 6
        val (moves, _) = turn.play()
        assertEquals(3, moves.size, "expecting 3 moves")
        assertTrue(
            moves[0] is ThrowDiceMove &&
                    moves[1] is ThrowDiceMove &&
                    moves[2] is PlayFailedMove
            , "expecting 2 ThrowDiceMoves, 1 PlayFailedMove"
        )
    }

    @Test
    fun testPlayerLosesTopTileIfPlayFailed() {
        Logger.logLevel = 2
        val listOfThrows = listOf(
            Dice(2),Dice(2),Dice(2),Dice(2),Dice(2),Dice(2),Dice(2),Dice(1),
            Dice(2)
        ).stream().iterator()

        fun testThrowDiceMethod(): Dice {
            return listOfThrows.next()
        }

        Config.throwDiceMethod = ::testThrowDiceMethod

        val board = Board("board at start of game")
        val player = Player("player1", mutableListOf(Tile(21)), StopAfterFirstTileStrategy())
        player.doTurn(board)

        assertTrue(player.turns.last().moves.last() is PlayFailedMove, "expecting PlayFailed status")
        assertEquals(listOf(), player.tilesWon, "expecting player to lose the top tile if PlayFailed")
    }
}

class BoardTest {
    @Test
    fun testHighestValueScenarios() {
        val board = Board(listOf(Tile(21), Tile(22), Tile(23), Tile(36)), "21 22 23 36board")
        assertEquals(
            NullTile,
            highestTileWithValueNotBiggerThanSumOfDice(20, board.tiles),
            "highest tile below the minimum value should be NullTile"
        )
        assertEquals(
            Tile(36),
            highestTileWithValueNotBiggerThanSumOfDice(37, board.tiles),
            "highest tile below the minimum value should be NullTile"
        )
        assertEquals(
            Tile(22),
            highestTileWithValueNotBiggerThanSumOfDice(22, board.tiles),
            "highest tile below the minimum value should be NullTile"
        )
        assertEquals(
            Tile(23),
            highestTileWithValueNotBiggerThanSumOfDice(25, board.tiles),
            "highest tile below the minimum value should be NullTile"
        )
    }
}

class PlayerTest {
    @Test
    fun testPlayerResultsAreUpdatedAfterATurn() {
        val listOfThrows = (1..16).map { Dice(6) }.stream().iterator()

        fun testThrowDiceMethod(): Dice {
            return listOfThrows.next()
        }

        Config.throwDiceMethod = ::testThrowDiceMethod

        val board = Board("board at start of game")
        val player = Player("player1", mutableListOf(), StopAfterFirstTileStrategy())
        val playerAfterFirstRound = player.doTurn(board)
        assertEquals(listOf(Tile(36)), playerAfterFirstRound.tilesWon, "expecting Tile(36) to be won after 1st round")
        assertEquals(-1, board.tiles.lastIndexOf(Tile(36)), "expecting Tile(36) to be removed from the board")
    }
}

fun <T> areListsEqual(first: List<T>, second: List<T>): Boolean {

    if (first.size != second.size) {
        return false
    }

    return first.zip(second).all { (x, y) -> x == y }
}
