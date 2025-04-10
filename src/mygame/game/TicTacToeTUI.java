package mygame.game;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import mygame.terminal.TUI;
import mygame.terminal.TUI.Color;
import mygame.terminal.TUI.Color.Background;
import mygame.terminal.Reader;
import mygame.terminal.Reader.Key;

/**
 * A TicTacToe game with a TUI.
 * <p>
 * Extends {@link TicTacToe} to provide methods
 * for prompting the user for their move and and printing the game board.
 *
 * @author Felix Nguyen
 * @version 1
 */
public final class TicTacToeTUI extends TicTacToe
{
    private static final Reader keyReader = Reader.getInstance();

    private static final String             EMPTY_SLOT  = "   ";
    private static final String             COL_DIVIDER = "|";
    private static final String             ROW_DIVIDER = "+ - ".repeat(BOARD_SIZE) + "+";
    private static final List<String>       SYMBOLS     = Arrays.asList(TUI.bold(" O "),
                                                                        TUI.bold(" X "));
    private static final Background[]       BACKGROUNDS = {Color.Background.GREEN,
                                                           Color.Background.RED};
    private static final Color.Foreground[] FOREGROUNDS = {Color.Foreground.GREEN,
                                                           Color.Foreground.RED};

    private final String clientID;
    private final String clientSymbol;

    /**
     * Creates a TicTacToe game with a TUI interface.
     * <p>
     * The symbols (O and X) are assigned randomly each game,
     * but our user will always be green and the opponent red.
     *
     * @param playerIDs the IDs of the players, the first of which must be that of the user
     */
    public TicTacToeTUI(final String... playerIDs)
    {
        super(playerIDs);
        clientID = playerIDs[0];
        Collections.shuffle(SYMBOLS);
        clientSymbol = SYMBOLS.getFirst();
    }

    /**
     * Prompts the user to make their move by using arrow keys to navigate and Enter to select.
     * <p>
     * This blocks the current thread until the user pressed Enter.*
     *
     * @return the ordinal of the chosen slot
     */
    public int prompt()
    {
        Coordinate currentPosition;
        Key        keyPressed;

        currentPosition = new Coordinate(getAvailableMove());
        do
        {
            printBoard(currentPosition);
            keyPressed = keyReader.readKey();
            if(keyPressed == null)
            {
                continue;
            }
            currentPosition = switch(keyPressed)
            {
                case Key.LEFT -> nextColumn(currentPosition, Direction.BACKWARD);
                case Key.RIGHT -> nextColumn(currentPosition, Direction.FORWARD);
                case Key.UP -> nextRow(currentPosition, Direction.BACKWARD);
                case Key.DOWN -> nextRow(currentPosition, Direction.FORWARD);
                default -> currentPosition;
            };
        }
        while(!(keyPressed == Key.ENTER &&
                isAvailable(currentPosition)));

        return currentPosition.ordinal();
    }

    @Override
    public void update(final Integer move)
    {
        super.update(move);
        printBoard();
    }

    /**
     * Gets the next available slot in the given direction.
     *
     * @param from      the current position
     * @param direction the direction to move (forward or backward)
     * @return the next available slot
     */
    private Coordinate nextColumn(final Coordinate from, final int direction)
    {
        int ordinal;
        ordinal = from.ordinal();

        Coordinate to;
        do
        {
            ordinal = (ordinal + direction + SLOTS_COUNT) % SLOTS_COUNT;
            to      = new Coordinate(ordinal);
        }
        while(!isAvailable(to));
        return to;
    }

    /**
     * Gets the next available slot in the given direction.
     *
     * @param from      the current position
     * @param direction the direction to move (forward or backward)
     * @return the next available slot
     */
    private Coordinate nextRow(final Coordinate from, final int direction)
    {
        int ordinal;
        ordinal = from.flip().ordinal();

        Coordinate to;
        do
        {
            ordinal = (ordinal + direction + SLOTS_COUNT) % SLOTS_COUNT;
            to      = new Coordinate(ordinal).flip();
        }
        while(!isAvailable(to));

        return to;
    }

    /**
     * Prints the game board and highlights the chosen slot.
     * If there is a winner, highlights the winning slots
     * with the winner's color (green for us, red for the opponent).
     *
     * @param chosenSlot the slot to highlight,
     *                   or {@code null} if no slot is chosen
     */
    private void printBoard(final Coordinate chosenSlot)
    {
        getWinnerID();
        TUI.clearScreen();
        System.out.println("Connect " + WIN_TARGET + " dots in a row to win.");

        System.out.println(ROW_DIVIDER);
        for(int row = 0; row < BOARD_SIZE; row++)
        {
            System.out.print(COL_DIVIDER);
            for(int col = 0; col < BOARD_SIZE; col++)
            {
                String slot;
                if(new Coordinate(row, col).equals(chosenSlot) && isOurTurn())
                {
                    slot = TUI.colorize(clientSymbol,
                                        Color.Background.YELLOW,
                                        Color.Foreground.BLACK);
                } else
                {
                    final Integer player;
                    player = getMoveAt(row, col);

                    slot = getSymbol(player);
                    if(isWinningSlot(row, col))
                    {
                        slot = TUI.colorize(slot,
                                            BACKGROUNDS[player],
                                            Color.Foreground.BLACK);
                    } else
                    {
                        slot = TUI.colorize(slot, player == null
                                                 ? Color.Foreground.DEFAULT
                                                 : FOREGROUNDS[player]);
                    }
                }
                System.out.print(slot);
                System.out.print(COL_DIVIDER);
            }
            System.out.println();
            System.out.println(ROW_DIVIDER);
        }
        System.out.println();
        if(isOurTurn())
        {
            System.out.println("Use arrow keys to navigate and Enter to select");
        }
    }

    /**
     * Prints the game board. If there is a winner, highlights the winning slots
     * with the winner's color (green for us, red for the opponent).
     */
    private void printBoard()
    {
        printBoard(null);
    }

    /**
     * Checks if our user is the next player.
     *
     * @return true if it's our turn, false otherwise
     */
    private boolean isOurTurn()
    {
        return clientID.equals(getNextPlayerID());
    }

    /**
     * Gets the symbol for the given player.
     *
     * @param player the ordinal of the player
     * @return the symbol for the player
     */
    private String getSymbol(final Integer player)
    {
        if(player == null)
        {
            return EMPTY_SLOT;
        }
        return SYMBOLS.get(player);
    }
}
