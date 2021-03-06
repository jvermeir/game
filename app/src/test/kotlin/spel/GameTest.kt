package spel

import kotlin.collections.ArrayDeque
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/*
TODO: refactor duplication of testThrowDiceMethod code
 */

class GameTest {
    private val player1 = Player("1", mutableListOf(), TestStrategy())
    private val player2 = Player("2", mutableListOf(), TestStrategy())
    private val twoPlayers = ArrayDeque(listOf(player1, player2))

    class TestStrategy : Strategy() {
        override fun makeMove(board: Board, turn: Turn): Move {
            return StopTurnMove(Turn(this, Game(Board("board"), ArrayDeque())))
        }

        override fun shouldIContinue(moves: List<Move>, game: Game, turn: Turn): Boolean {
            return false
        }

        override fun selectDiceFromThrow(diceInThrow: List<Dice>, turn: Turn): List<Dice> {
            return listOf(Dice(25))
        }

        override val id = "test"
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

    @Test
    fun testGameStopsIfAllTilesAreUsed2() {
        val listOfThrows = (1..16).map { Dice(6) }.stream().iterator()

        fun testThrowDiceMethod(): Dice {
            return listOfThrows.next()
        }

        Config.throwDiceMethod = ::testThrowDiceMethod

        val board = Board("myBoard")
        board.tiles = listOf(Tile(23))
        val player1 = Player("1", mutableListOf(), StopAfterFirstTileStrategy())
        val player2 = Player("2", mutableListOf(), StopAfterFirstTileStrategy())
        val game = Game(board, ArrayDeque(listOf(player1, player2)))
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
        val game = Game(board, ArrayDeque())
        val strategy = StopAfterFirstTileStrategy()
        val turn = Turn(strategy, game)

        val listOfDice1 = listOf(Dice(6), Dice(6), Dice(6), Dice(5), Dice(5), Dice(5), Dice(1), Dice(2))
        val selectDiceFromThrow = strategy.selectDiceFromThrow(listOfDice1, turn)
        assertEquals(listOf(Dice(6), Dice(6), Dice(6)), selectDiceFromThrow, "first throw should select 3 worms")

        turn.facesUsed = listOf(Dice(6))
        val listOfDice2 = listOf(Dice(6), Dice(6), Dice(6), Dice(5), Dice(5))
        val selectDiceFromSecondThrow = strategy.selectDiceFromThrow(listOfDice2, turn)
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
        val game = Game(board, ArrayDeque())

        val turn = Turn(StopAfterFirstTileStrategy(), game)
        val (moves, _) = turn.play()
        assertEquals(4, moves.size, "expecting 4 moves")
        assertTrue(
            moves.first() is ThrowDiceMove &&
                    moves[1] is ThrowDiceMove &&
                    moves[2] is TakeTileMove &&
                    moves[3] is StopTurnMove, "expecting 2 ThrowDiceMoves, 1 TakeTileMove and 1 StopTurnMove"
        )
        assertEquals(Tile(25), (moves[2] as TakeTileMove).tileSelected, "Expecting tile 25 to be selected")
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
        val game = Game(board, ArrayDeque())

        board.tiles = listOf(Tile(23))
        val turn = Turn(StopAfterFirstTileStrategy(), game)
        val (moves, _) = turn.play()
        assertEquals(4, moves.size, "expecting 4 moves")
        assertTrue(
            moves.first() is ThrowDiceMove &&
                    moves[1] is ThrowDiceMove &&
                    moves[2] is TakeTileMove &&
                    moves[3] is StopTurnMove, "expecting 2 ThrowDiceMoves, 1 TakeTileMove and 1 StopTurnMove"
        )
        assertEquals(Tile(23), (moves[2] as TakeTileMove).tileSelected, "Expecting tile 23 to be selected")
    }

    @Test
    fun testFindTileThatCanBeStolen() {
        val board = Board("board at start of game")

        val player1 = Player("1", mutableListOf(Tile(23)), GameTest.TestStrategy())
        val player2 = Player("2", mutableListOf(), GameTest.TestStrategy())
        val players: ArrayDeque<Player> = ArrayDeque(listOf(player1, player2))

        val game = Game(board, players)
        game.nextPlayer()

        val tile = findTileThatCanBeStolen(findPlayerWithTileThatCanBeStolen(23, game))
        assertEquals(Tile(23), tile, "expecting player2 can steal Tile(23) from player1")

        game.nextPlayer()
        val tile2 = findTileThatCanBeStolen(findPlayerWithTileThatCanBeStolen(23, game))
        assertEquals(NullTile, tile2, "expecting player1 cannot steal a tile from player2")
    }

    @Test
    fun testShouldIContinueIsFalseIfTileCanBeStolen() {
        val board = Board("board at start of game")
        board.tiles = listOf(Tile(23))

        val player1 = Player("1", mutableListOf(Tile(25)), GameTest.TestStrategy())
        val player2 = Player("2", mutableListOf(), GameTest.TestStrategy())
        val players = ArrayDeque(listOf(player1, player2))

        val game = Game(board, players)
        game.nextPlayer()

        val turn = Turn(StopAfterFirstTileStrategy(), game)
        val takeTileMove = TakeTileMove(turn, game)
        takeTileMove.diceSelected = listOf(Dice(6), Dice(6), Dice(6), Dice(6), Dice(6))
        turn.moves = listOf(takeTileMove)

        assertFalse(
            StopAfterFirstTileStrategy().shouldIContinue(turn.moves, game, turn),
            "expecting turn to stop if a tile can be taken from another player"
        )
    }
}

class StopEarlyFavorHighSumStrategyTest {
    @Test
    fun testHighestTotalValueIsSelectedFromDice() {
        val board = Board("board at start of game")
        board.tiles = listOf(Tile(23), Tile(25), Tile(30))

        val player1 = Player("1", mutableListOf(), ContinueIfOddsAreHighEnoughStrategy())
        val player2 = Player("2", mutableListOf(), ContinueIfOddsAreHighEnoughStrategy())
        val players = ArrayDeque(listOf(player1, player2))

        val game = Game(board, players)
        val turn = Turn(StopEarlyFavorHighSumStrategy(), game)

        val diceSelected = selectDiceFromThrowUsingHighestTotalValue(listOf(Dice(6),Dice(5),Dice(5)), turn)
        assertEquals(listOf(Dice(5),Dice(5)), diceSelected, "expecting 2 x Dice(5) to be selected")
    }
}

class ContinueIfOddsAreHighEnoughStrategyTest {
    @Test
    fun testPlayerContinuesIfOddsAreBetterThan50Percent() {
        val listOfThrows = listOf(
            Dice(6), Dice(6), Dice(6), Dice(6), Dice(6), Dice(5), Dice(1), Dice(2)
        ).stream().iterator()

        fun testThrowDiceMethod(): Dice {
            return listOfThrows.next()
        }

        Config.throwDiceMethod = ::testThrowDiceMethod

        val board = Board("board at start of game")
        board.tiles = listOf(Tile(23), Tile(25), Tile(30))

        val player1 = Player("1", mutableListOf(), ContinueIfOddsAreHighEnoughStrategy())
        val player2 = Player("2", mutableListOf(), ContinueIfOddsAreHighEnoughStrategy())
        val players = ArrayDeque(listOf(player1, player2))

        val game = Game(board, players)
        val turn = Turn(ContinueIfOddsAreHighEnoughStrategy(), game)
        turn.numberOfDiceLeft = 3
        turn.facesUsed = listOf(Dice(6))
        val throwDiceMove = ThrowDiceMove(board, turn)
        throwDiceMove.diceSelected = listOf(Dice(6), Dice(6), Dice(6), Dice(6), Dice(6))
        val moves = listOf(throwDiceMove)
        // 3 dice left to find a value of 30 or higher (216 possible values, 115 are better) so we should continue
        assertTrue(ContinueIfOddsAreHighEnoughStrategy().shouldIContinue(moves, game, turn), "expecting turn to continue")

        turn.numberOfDiceLeft = 1
        // 1 dice left to find a value of 30 or higher (216 possible values, 1 better:5 only). so we should stop
        assertFalse(ContinueIfOddsAreHighEnoughStrategy().shouldIContinue(moves, game, turn), "expecting turn to stop")

    }
}

class TakeTileMoveTest {
    @Test
    fun testMoveSelectsTileFromOtherPlayerIfHigher() {
        val listOfThrows = listOf(
            Dice(6), Dice(6), Dice(6), Dice(6), Dice(6), Dice(5), Dice(1), Dice(2)
        ).stream().iterator()

        fun testThrowDiceMethod(): Dice {
            return listOfThrows.next()
        }

        Config.throwDiceMethod = ::testThrowDiceMethod

        val board = Board("board at start of game")
        board.tiles = listOf()
        val player1 = Player("1", mutableListOf(), StopAfterFirstTileStrategy())
        val player2 = Player("2", mutableListOf(), StopAfterFirstTileStrategy())
        val players = ArrayDeque(listOf(player1, player2))

        val game = Game(board, players)

        val player1Move = Move()
        player1Move.tileSelected = Tile(25)
        player1.tilesWon.add(Tile(25))

        game.nextPlayer()

        player2.doTurn(game)

        assertTrue(player1.tilesWon.isEmpty(), "expecting player1 to have no tiles left")
        assertEquals(Tile(25), player2.tilesWon.first(), "expecting player2 to have won Tile(25)")
    }
}

class TurnTest {
    @Test
    fun testMovesArePossibleConditions() {
        val board = Board("board at start of game")
        val game = Game(board, ArrayDeque())
        val turn = Turn(StopAfterFirstTileStrategy(), game)
        assertTrue(turn.movesAreStillPossible(), "moves should still be possible if there are no previous moves")

        turn.numberOfDiceLeft = 2
        turn.facesUsed = listOf(Dice(6))
        assertTrue(turn.movesAreStillPossible(), "moves should still be possible if only Dice(1) is used")
    }

    @Test
    fun testEndConditions() {
        val board = Board("board at start of game")
        val game = Game(board, ArrayDeque())
        val turn = Turn(StopAfterFirstTileStrategy(), game)
        turn.numberOfDiceLeft = 0
        assertFalse(turn.movesAreStillPossible(), "moves should be impossible if all dice were used")

        turn.numberOfDiceLeft = 2
        turn.facesUsed = listOf(Dice(6), Dice(1), Dice(2), Dice(3), Dice(4), Dice(5))
        assertFalse(turn.movesAreStillPossible(), "moves should be impossible if all faces were used")
    }

    @Test
    fun testTurnFailsIfNoNewDiceCanBeSelected() {
        val listOfThrows = (1..16).map { Dice(1) }.stream().iterator()

        fun testThrowDiceMethod(): Dice {
            return listOfThrows.next()
        }

        Config.throwDiceMethod = ::testThrowDiceMethod

        val board = Board("board at start of game")
        val game = Game(board, ArrayDeque())
        val turn = Turn(StopAfterFirstTileStrategy(), game)
        val (moves, _) = turn.play()
        assertEquals(2, moves.size, "expecting 2 moves")
        assertTrue(
            moves.first() is ThrowDiceMove &&
                    moves[1] is PlayFailedMove, "expecting 1 ThrowDiceMove and 1 PlayFailedMove"
        )
    }

    @Test
    fun testTurnContinuesUntilAtLeastOneWormIsThrown() {
        val listOfThrows = listOf(
            Dice(5), Dice(5), Dice(5), Dice(5), Dice(5), Dice(1),
            Dice(6), Dice(6), Dice(6), Dice(6), Dice(6), Dice(6)
        ).stream().iterator()

        fun testThrowDiceMethod(): Dice {
            return listOfThrows.next()
        }

        Config.throwDiceMethod = ::testThrowDiceMethod

        val board = Board("board at start of game")
        val game = Game(board, ArrayDeque())
        val turn = Turn(StopAfterFirstTileStrategy(), game)
        turn.numberOfDiceLeft = 6
        val (moves, _) = turn.play()
        assertEquals(4, moves.size, "expecting 4 moves")
        assertTrue(
            moves[0] is ThrowDiceMove &&
                    moves[1] is ThrowDiceMove &&
                    moves[2] is TakeTileMove &&
                    moves[3] is StopTurnMove, "expecting 2 ThrowDiceMoves, 1 TakeTileMove and 1 StopTurnMove"
        )
    }

    @Test
    fun testPlayerLosesTurnIfNoWormIsThrown() {
        val listOfThrows = listOf(
            Dice(5), Dice(5), Dice(5), Dice(5), Dice(5), Dice(1),
            Dice(1)
        ).stream().iterator()

        fun testThrowDiceMethod(): Dice {
            return listOfThrows.next()
        }

        Config.throwDiceMethod = ::testThrowDiceMethod

        val board = Board("board at start of game")
        val game = Game(board, ArrayDeque())
        val turn = Turn(StopAfterFirstTileStrategy(), game)
        turn.numberOfDiceLeft = 6
        val (moves, _) = turn.play()
        assertEquals(3, moves.size, "expecting 3 moves")
        assertTrue(
            moves[0] is ThrowDiceMove &&
                    moves[1] is ThrowDiceMove &&
                    moves[2] is PlayFailedMove, "expecting 2 ThrowDiceMoves, 1 PlayFailedMove"
        )
    }

    @Test
    fun testPlayerLosesTopTileIfPlayFailedAndHighestTileIsRemovedFromTheGame() {
        Logger.logLevel = 2
        val listOfThrows = listOf(
            Dice(2), Dice(2), Dice(2), Dice(2), Dice(2), Dice(2), Dice(2), Dice(1),
            Dice(2)
        ).stream().iterator()

        fun testThrowDiceMethod(): Dice {
            return listOfThrows.next()
        }

        Config.throwDiceMethod = ::testThrowDiceMethod

        val board = Board("board at start of game")
        board.tiles = listOf(Tile(22), Tile(23))
        val player = Player("player1", mutableListOf(Tile(21)), StopAfterFirstTileStrategy())
        val game = Game(board, ArrayDeque(listOf(player)))

        player.doTurn(game)

        assertTrue(player.turns.last().moves.last() is PlayFailedMove, "expecting PlayFailed status")
        assertEquals(listOf(), player.tilesWon, "expecting player to lose the top tile if PlayFailed")
        assertEquals(listOf(Tile(21), Tile(22)), board.tiles, "expecting last tile to be removed from the board")
    }

    @Test
    fun testPlayerLosesTopTileIfPlayFailedAndThisTileIsTheNewHighestTile() {
        Logger.logLevel = 2
        val listOfThrows = listOf(
            Dice(2), Dice(2), Dice(2), Dice(2), Dice(2), Dice(2), Dice(2), Dice(1),
            Dice(2)
        ).stream().iterator()

        fun testThrowDiceMethod(): Dice {
            return listOfThrows.next()
        }

        Config.throwDiceMethod = ::testThrowDiceMethod

        val board = Board("board at start of game")
        board.tiles = listOf(Tile(21), Tile(22))
        val player = Player("player1", mutableListOf(Tile(23)), StopAfterFirstTileStrategy())
        val game = Game(board,  ArrayDeque(listOf(player)))

        player.doTurn(game)

        assertTrue(player.turns.last().moves.last() is PlayFailedMove, "expecting PlayFailed status")
        assertEquals(listOf(), player.tilesWon, "expecting player to lose the top tile if PlayFailed")
        assertEquals(listOf(Tile(21), Tile(22), Tile(23)), board.tiles, "expecting last tile to be the tile the player lost")
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
        val game = Game(board,  ArrayDeque(listOf(player)))
        val playerAfterFirstRound = player.doTurn(game)
        assertEquals(listOf(Tile(36)), playerAfterFirstRound.tilesWon, "expecting Tile(36) to be won after 1st round")
        assertEquals(-1, board.tiles.lastIndexOf(Tile(36)), "expecting Tile(36) to be removed from the board")
    }
}
