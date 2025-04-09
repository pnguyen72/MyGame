package MyGame.Game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A extended Tic Tac Toe game, with a {@value #BOARD_SIZE}x{@value #BOARD_SIZE} board
 * and you connect {@value #WIN_TARGET} dots in a row to win.
 * <p>
 * Work is in progress to allow more than {@value #PLAYERS_PER_GAME} players. Soon, hopefully.
 *
 * @author Felix Nguyen
 * @version 1
 */
public class TicTacToe
{
    /**
     * The number of players in a game.
     */
    /*
     * There is an attempt to keep the code generic,
     * but the game for the most part is still hardcoded for 2 players.
     */
    public static final int PLAYERS_PER_GAME = 2;

    /**
     * Status of the game for each client.
     */
    public enum Status
    {
        WON, LOST, TIE, // game ended
        WAIT,           // opponent's turn
        YOUR_TURN;
    }

    /**
     * The width and height of the board.
     */
    public static final int BOARD_SIZE = 15;

    /**
     * How many dots to connect in a row to win.
     */
    public static final int WIN_TARGET = 5;

    /**
     * Represents a coordinate on the game board.
     *
     * @param row row number
     * @param col column number
     */
    record Coordinate(int row, int col)
    {
        Coordinate(int position)
        {
            this(position / BOARD_SIZE,
                 position % BOARD_SIZE);
        }

        /**
         * Converts the coordinate to a number in range [0, {@value #SLOTS_COUNT}),
         * representing the index of the position on a 1D array.
         *
         * @return the number
         */
        int ordinal()
        {
            return BOARD_SIZE * row + col;
        }

        /**
         * Flips the coordinate.
         *
         * @return a new Coordinate with the row and column swapped
         */
        Coordinate flip()
        {
            return new Coordinate(col, row);
        }

        /**
         * Checks if the coordinate is within the bounds of the board.
         *
         * @return true if it's inbound, false otherwise
         */
        private boolean isValid()
        {
            return (row >= 0 && row < BOARD_SIZE &&
                    col >= 0 && col < BOARD_SIZE);
        }


        /**
         * Calculates the "distance" to another coordinate.
         * <p>
         * This does not use the Euclidean distance, but a special kind of distance that
         * works better for a grid.
         * <p>
         * <a href=
         * "https://math.stackexchange.com/questions/1401180/general-equation-for-distance-over-grid-with-diagonals">
         * Source for the formula</a>.
         * <p>
         * See {@link #getAvailableMove()} for usage.
         *
         * @param other another Coordinate
         * @return the "distance"
         */
        private double distance(final Coordinate other)
        {
            final double diagonalFactor;
            diagonalFactor = Math.sqrt(2);

            final int colDistance;
            final int rowDistance;
            colDistance = Math.abs(col - other.col);
            rowDistance = Math.abs(row - other.row);

            return diagonalFactor * Math.min(colDistance, rowDistance)
                   + Math.abs(colDistance - rowDistance);
        }
    }

    /**
     * The number of slots on the board.
     */
    static final int SLOTS_COUNT = BOARD_SIZE * BOARD_SIZE;

    /**
     * The center of the board.
     * <p>
     * This is used to calculate the distance to the center of the board
     * when choosing a random slot.
     */
    static final Coordinate CENTER = new Coordinate(BOARD_SIZE / 2,
                                                    BOARD_SIZE / 2);

    /**
     * Represents which direction to move starting from a grid on the board.
     */
    static class Direction
    {
        /**
         * Towards the direction of larger coordinate value.
         */
        static final int FORWARD = 1;

        /**
         * Towards the direction of smaller coordinate value
         */
        static final int BACKWARD = -1;
    }

    private static final int[] DIRECTIONS = {Direction.FORWARD,
                                             Direction.BACKWARD};

    /**
     * Ways multiple dots in arrow can be formed, used for checking win condition.
     * Each orientation is represented by a {@link Coordinate} of unit vector.
     */
    private static final Coordinate[] ORIENTATIONS = {
            new Coordinate(1, 0), // vertical
            new Coordinate(0, 1), // horizontal
            new Coordinate(1, 1), // diagonal
            new Coordinate(1, -1) // anti-diagonal
    };

    private static final Random RANDOM = new Random();

    private final Integer[][]      moves;
    private final String[]         playerIDs;
    private final List<Coordinate> emptySlots;
    private final List<Coordinate> winningSlots;

    private Coordinate previousMove;
    private int        previousPlayer;

    /**
     * Creates a new Tic Tac Toe game.
     * <p>
     * The IDs of the players are required to determine who to play first.
     * Essentially it should be random, but we cannot use Random
     * because the server and clients must agree on who plays first.
     * One thing they do agree is the IDs of all parties, 
     * and they are assigned randomly.
     * 
     * @param playerIDs the IDs of the players
     */
    public TicTacToe(final String... playerIDs)
    {
        if(playerIDs.length != PLAYERS_PER_GAME)
        {
            throw new IllegalArgumentException("Invalid number of players");
        }

        emptySlots = new ArrayList<>();
        for(int i = 0; i < BOARD_SIZE; i++)
        {
            for(int j = 0; j < BOARD_SIZE; j++)
            {
                emptySlots.add(new Coordinate(i, j));
            }
        }
        winningSlots = new ArrayList<>();

        this.playerIDs = playerIDs;
        this.moves     = new Integer[BOARD_SIZE][BOARD_SIZE];
        this.previousPlayer = playerIDs[0].compareTo(playerIDs[1]) > 0
                              ? 0
                              : 1;
    }

    /**
     * Updates the game state with a new move.
     *
     * @param move the move to make, represented as a number in range [0, {@value #SLOTS_COUNT})
     */
    public void update(final Integer move)
    {
        if(move == null)
        {
            return;
        }
        if(!isAvailable(move))
        {
            throw new IllegalArgumentException("Invalid move");
        }
        previousPlayer                            = getNextPlayer();
        previousMove                              = new Coordinate(move);
        moves[previousMove.row][previousMove.col] = previousPlayer;
        emptySlots.remove(previousMove);
    }

    /**
     * Checks if a move is available.
     *
     * @param coord the coordinate of the move
     * @return whether it's available to play
     */
    boolean isAvailable(final Coordinate coord)
    {
        return coord.isValid() && getMoveAt(coord) == null;
    }

    /**
     * Checks if a move is available.
     *
     * @param move the move represented as a number in range [0, {@value #SLOTS_COUNT})
     * @return whether it's available to play
     */
    boolean isAvailable(final int move)
    {
        return isAvailable(new Coordinate(move));
    }

    /**
     * Gets the ID of the player whose turn it is.
     *
     * @return the ID of the next player
     */
    public String getNextPlayerID()
    {
        Integer nextPlayer;
        nextPlayer = getNextPlayer();
        if(nextPlayer == null)
        {
            return null;
        }
        return playerIDs[nextPlayer];
    }

    /**
     * Gets the ID of the player who won.
     * <p>
     * If the game is not over, this will return null.
     *
     * @return the ID of the winning player, or null if the game is not over
     */
    public String getWinnerID()
    {
        if(previousMove == null)
        {
            return null;
        }

        if(winningSlots.size() >= WIN_TARGET)
        {
            return playerIDs[previousPlayer];
        }

        for(final Coordinate orientation : ORIENTATIONS)
        {
            if(checkWinCondition(orientation))
            {
                return playerIDs[previousPlayer];
            }
        }

        return null;
    }

    /**
     * Gets an empty slot on the board.
     * <p>
     * This is to serve as a "good enough" starting point for the player
     * of where to play next. It's not meant to be a good move,
     * just better than a random slot, or say, the upper left corner.
     * <p>
     * The slot is the closest available one to the last played move
     * (using a special {@link Coordinate#distance} metric),
     * with a bias towards the center of the board,
     * plus a bit of randomness.
     *
     * @return the ordinal of an empty slot, {@code null} if the game is over
     */
    public Integer getAvailableMove()
    {
        if(getWinnerID() != null)
        {
            return null;
        }

        if(previousMove == null)
        {
            return CENTER.ordinal();
        }

        final Coordinate nearestAvailable;
        nearestAvailable = emptySlots.stream()
                                     .min(this::compareSlots)
                                     .orElse(null);
        if(nearestAvailable == null)
        {
            return null;
        }
        return nearestAvailable.ordinal();
    }

    /**
     * Get the ordinal of the player who played at a position.
     * <p>
     * Since the game has two players, the ordinal is either 0 or 1.
     *
     * @param coord the coordinate to check
     * @return the ordinal of the player who played at the position,
     * or {@code null} if the position is empty
     */
    Integer getMoveAt(final Coordinate coord)
    {
        return getMoveAt(coord.row, coord.col);
    }

    /**
     * Get the ordinal of the player who played at a position.
     * <p>
     * Since the game has two players, the ordinal is either 0 or 1.
     *
     * @param row the row of the position
     * @param col the column of the position
     * @return the ordinal of the player who played at the position,
     * or null if the position is empty
     */
    Integer getMoveAt(final int row, final int col)
    {
        return moves[row][col];
    }

    /**
     * Checks if a slot is part of the {@value #WIN_TARGET} connected slots
     * that won the game.
     *
     * @param row the row of the slot
     * @param col the column of the slot
     * @return true if the slot is winning, false otherwise
     */
    boolean isWinningSlot(final int row, final int col)
    {
        return winningSlots.contains(new Coordinate(row, col));
    }

    /**
     * Gets the ordinal of the player to move next.
     * <p>
     * Since the game has two players, the ordinal is either 0 or 1.
     *
     * @return the ordinal of the player to move next,
     * or null if the game is over.
     */
    Integer getNextPlayer()
    {
        if(getWinnerID() != null || emptySlots.isEmpty())
        {
            return null;
        }
        return (previousPlayer + 1) % PLAYERS_PER_GAME;
    }

    /**
     * Compares two slots to see which one is "better".
     * Used by {@link #getAvailableMove()}.
     *
     * @param c1 the first slot
     * @param c2 the second slot
     * @return a negative number if c1 is better,
     * a positive number if c2 is better,
     * or 0 if they are equal
     */
    private int compareSlots(Coordinate c1, Coordinate c2)
    {
        final double centricBias;
        final double randomness;
        centricBias = 0.5;
        randomness  = 2;

        return (int) (c1.distance(previousMove) + centricBias * c1.distance(CENTER) -
                      (c2.distance(previousMove) + centricBias * c2.distance(CENTER)) +
                      RANDOM.nextDouble(randomness) - randomness / 2);
    }

    /**
     * Checks if the last move made by the player makes them win.
     *
     * @param orientation the orientation to check (vertical, horizontal, diagonal, anti-diagonal)
     * @return whether the player won
     */
    private boolean checkWinCondition(final Coordinate orientation)
    {
        winningSlots.add(previousMove);
        for(final int direction : DIRECTIONS)
        {
            int        row;
            int        col;
            Coordinate coord;

            row   = previousMove.row + direction * orientation.row;
            col   = previousMove.col + direction * orientation.col;
            coord = new Coordinate(row, col);

            while(coord.isValid() &&
                  Integer.valueOf(previousPlayer).equals(moves[row][col]))
            {
                winningSlots.add(coord);
                row += direction * orientation.row;
                col += direction * orientation.col;
                coord = new Coordinate(row, col);
            }
        }
        if(winningSlots.size() < WIN_TARGET)
        {
            winningSlots.clear();
            return false;
        }
        return true;
    }
}
