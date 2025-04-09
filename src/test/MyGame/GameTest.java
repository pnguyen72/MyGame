package MyGame;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

import MyGame.Game.TicTacToe;

/**
 * Tests for the game logic
 */
public class GameTest
{
    private static TicTacToe game;
    private static String    firstPlayerID;
    private static String    secondPlayerID;

    // Must update the tests if these numbers change
    @BeforeAll
    static void testConstants()
    {
        assertEquals(TicTacToe.BOARD_SIZE, 15);
        assertEquals(TicTacToe.WIN_TARGET, 5);
    }

    @BeforeEach
    void setupGame()
    {
        game          = new TicTacToe("player 1", "player 2");
        firstPlayerID = game.getNextPlayerID();
        game.update(0);
        secondPlayerID = game.getNextPlayerID();
    }

    /**
     * Tests that the game correctly keeps track of whose turn it is for each turn.
     */
    @Test
    void testPlayerTakingTurns()
    {
        assertNotEquals(firstPlayerID, secondPlayerID);

        game.update(1);
        assertEquals(firstPlayerID, game.getNextPlayerID());

        game.update(2);
        assertEquals(secondPlayerID, game.getNextPlayerID());

        game.update(3);
        assertEquals(firstPlayerID, game.getNextPlayerID());
    }

    /**
     * Tests that invalid moves will throw an appropriate exception.
     */
    @Test
    void testInvalidMove()
    {
        final String expectedMessage;
        expectedMessage = "Invalid move";

        final Exception e1;
        e1 = assertThrows(IllegalArgumentException.class, () -> game.update(-1));
        assertEquals(expectedMessage, e1.getMessage());

        final Exception e2;
        e2 = assertThrows(IllegalArgumentException.class,
                          () -> game.update(TicTacToe.BOARD_SIZE * TicTacToe.BOARD_SIZE));
        assertEquals(expectedMessage, e2.getMessage());

        game.update(1);

        final Exception e3;
        e3 = assertThrows(IllegalArgumentException.class, () -> game.update(300));
        assertEquals(expectedMessage, e3.getMessage());
    }

    /**
     * Tests the win condition when the winning path is horizontal, like
     *      O O O O O
     */
    @Test
    void testWinHorizontal()
    {
        game.update(15);  // player 2
        game.update(1);   // player 1
        assertNull(game.getWinnerID());

        game.update(30);  // player 2
        game.update(2);   // player 1
        assertNull(game.getWinnerID());

        game.update(45);  // player 2
        game.update(3);   // player 1
        assertNull(game.getWinnerID());

        game.update(60);  // player 2
        game.update(17);  // player 1
        assertNull(game.getWinnerID());

        game.update(90);  // player 2
        game.update(18);  // player 1
        assertNull(game.getWinnerID());

        game.update(105); // player 2
        game.update(19);  // player 1
        assertNull(game.getWinnerID());

        game.update(120); // player 2
        game.update(4);   // player 1
        assertEquals(game.getWinnerID(), firstPlayerID);
        assertNull(game.getNextPlayerID());
    }

    /**
     * Tests the win condition when the winning path is vertical, like
     *      O
     *      O
     *      O
     *      O
     *      O
     */
    @Test
    void testWinVertical()
    {
        game.update(1);   // player 2
        game.update(15);  // player 1
        assertNull(game.getWinnerID());

        game.update(2);   // player 2
        game.update(30);  // player 1
        assertNull(game.getWinnerID());

        game.update(3);   // player 2
        game.update(45);  // player 1
        assertNull(game.getWinnerID());

        game.update(5);   // player 2
        game.update(31);  // player 1
        assertNull(game.getWinnerID());

        game.update(6);  // player 2
        game.update(46); // player 1
        assertNull(game.getWinnerID());

        game.update(7);  // player 2
        game.update(61); // player 1
        assertNull(game.getWinnerID());

        game.update(8);  // player 2
        game.update(60);   // player 1
        assertEquals(game.getWinnerID(), firstPlayerID);
        assertNull(game.getNextPlayerID());
    }

    /**
     * Tests the win condition when the winning path is diagonal, like
     *      O
     *        O
     *          O
     *            O
     *              O
     */
    @Test
    void testWinDiagonal()
    {
        game.update(1);   // player 2
        game.update(16);  // player 1
        assertNull(game.getWinnerID());

        game.update(2);   // player 2
        game.update(32);  // player 1
        assertNull(game.getWinnerID());

        game.update(3);   // player 2
        game.update(48);  // player 1
        assertNull(game.getWinnerID());

        game.update(5);   // player 2
        game.update(33);  // player 1
        assertNull(game.getWinnerID());

        game.update(6);  // player 2
        game.update(34); // player 1
        assertNull(game.getWinnerID());

        game.update(7);  // player 2
        game.update(49); // player 1
        assertNull(game.getWinnerID());

        game.update(8);  // player 2
        game.update(64); // player 1
        assertEquals(game.getWinnerID(), firstPlayerID);
        assertNull(game.getNextPlayerID());
    }

    /**
     * Tests the win condition when the winning path is anti-diagonal, like
     *              O
     *            O
     *          O
     *        O
     *      O
     */
    @Test
    void testWinAntiDiagonal()
    {
        game.update(1);  // player 2
        game.update(14); // player 1
        assertNull(game.getWinnerID());

        game.update(2);  // player 2
        game.update(28); // player 1
        assertNull(game.getWinnerID());

        game.update(3);  // player 2
        game.update(45); // player 1
        assertNull(game.getWinnerID());

        game.update(4);  // player 2
        game.update(42); // player 1
        assertNull(game.getWinnerID());

        game.update(6);  // player 2
        game.update(56); // player 1
        assertNull(game.getWinnerID());

        game.update(7);  // player 2
        game.update(55); // player 1
        assertNull(game.getWinnerID());

        game.update(8);  // player 2
        game.update(71); // player 1

        game.update(9);  // player 2
        game.update(70); // player 1
        assertEquals(game.getWinnerID(), firstPlayerID);
        assertNull(game.getNextPlayerID());
    }
}
