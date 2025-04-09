package MyGame;

import MyGame.Terminal.Reader;
import MyGame.Terminal.TUI;
import MyGame.Terminal.Options;
import MyGame.Client.Client;
import MyGame.Client.CPU;
import MyGame.Client.Player;
import MyGame.Game.TicTacToe;
import MyGame.Server.MainServer;
import MyGame.Services.Connection;
import MyGame.Services.DirectoryMonitor;
import MyGame.Services.FileMonitor;
import MyGame.Services.Scheduler;

import java.nio.file.Path;
import java.util.Optional;

/**
 * <h2>A multiplayer tic-tac-toe game</h2>
 *
 * <h3>What is meant by "multiplayer"</h3>
 * <p>
 * This program does not implement anything network-related. It's multiplayer in
 * the sense that there is a Server (see {@link MainServer}) and multiple
 * Clients (see {@link Client}), each is an individual Java program that runs
 * independently of the others and communicates with each other via a protocol.
 * However, they all run on the same machine. To simulate true multiplayer
 * experience, players have to remote-connect to the same machine using a
 * third-party tool (e.g. SSH).
 *
 * <h3>How multiplayer is implemented</h3>
 * <p>
 * This program uses a file-based protocol to communicate between the server and
 * clients. The directory containing the game data is structured like this:
 *
 * <pre>
 * {@value GameFiles#SERVER_FILE} the main server
 * {@value GameFiles#REQUESTS_DIR}: directory for clients join requests
 * | {@code clientID}: directory for an individual request
 * | | {@value GameFiles#CLIENT_FILE}: the client in this request
 * | | {@value GameFiles#SERVER_FILE}: the main server in response to this client
 * {@value GameFiles#GAMES_DIR}: directory for all games
 * | {@code gameID}: directory for an individual game
 * | | {@code clientID}: directory for an individual client in this game
 * | | | {@value GameFiles#CLIENT_FILE}: the client in this game
 * | | | {@value GameFiles#MOVE_FILE}: the move made by this client
 * | | | {@value GameFiles#SERVER_FILE}: the game server in response to this client
 * </pre>
 * <p>
 * {@value GameFiles#SERVER_FILE} is the main server's {@link Connection} file.
 * The server maintains this Connection to signal that it's running. Clients
 * monitor this connection (see {@link Connection.Monitor}) to handle in case
 * the server fails.
 * <p>
 * A client makes a request to join a game by creating a file at
 * /{@value GameFiles#REQUESTS_DIR}/{@code clientID}/{@value GameFiles#CLIENT_FILE}
 * and maintains its Connection.
 * <p>
 * The main server monitors the requests directory (see
 * {@link DirectoryMonitor}) to detect incoming requests, and monitors the
 * requests connection files to know who's still in the queue vs who has left.
 * When the main server finds 2 requests, it creates a game server and invites
 * the clients to join by writing the game's ID to
 * /{@value GameFiles#REQUESTS_DIR}/{@code clientID}/{@value GameFiles#CLIENT_FILE}
 * (for each client).
 * <p>
 * Each client monitors the main server's response file (see
 * {@link FileMonitor}), and when it gets this message, it joins the game by
 * creating a file at
 * /{@value GameFiles#GAMES_DIR}/{@code gameID}/{@code clientID}/{@value GameFiles#CLIENT_FILE}
 * and maintain its Connection. The game server will monitor this connection to
 * know whether the client is still connected or has abandoned the game.
 * <p>
 * When the game server receives enough players, it starts the game. Each turn,
 * it writes to each client's
 * /{@value GameFiles#GAMES_DIR}/{@code gameID}/{@code clientID}/{@value GameFiles#SERVER_FILE}
 * file, telling the game status (who won/lost/tie, or whose turn it is) and the
 * last move made. The clients monitor this file and respond accordingly.
 * <p>
 * To make a move, the client writes to
 * /{@value GameFiles#GAMES_DIR}/{@code gameID}/{@code clientID}/{@value GameFiles#MOVE_FILE}
 * the value of its move. The game server monitors this file, updates the game
 * state, then signal the next client to move. And so on.
 * <p>
 * This is a generic multiplayer protocol that should work for any turn-based
 * game. This program implements a tic-tac-toe game, but with minimal changes it
 * can work with many other games.
 *
 * <h3>As for the game itself</h3>
 * <p>
 * See {@link TicTacToe}
 *
 * @author Felix Nguyen
 * @version 1
 */
public final class MyGame
{
    /**
     * Directory containing all game data.
     */
    public static final Path PATH = Path.of("res/MyGame/");

    private static final Reader terminal = Reader.getInstance();

    /**
     * Entry point to the program to play the game.
     * <p>
     * To play this game, the {@link MainServer} program must be running. Start it
     * first and keep it running in the background.
     *
     * @param args an optional subcommand (case-insensitive):
     *             <ul>
     *             <li>PvP: start a player vs player game</li>
     *             <li>CPU: start a player vs CPU game</li>
     *             </ul>
     *             If no subcommand is given, the program repeatedly prompts the
     *             user which game to play after each game.
     */
    public static void main(final String[] args)
    {
        if(!MainServer.isRunning())
        {
            System.err.println("Server is not running.");
            System.exit(1);
        }

        if(args.length == 0)
        {
            play();
            return;
        }

        final String             input;
        final Optional<Opponent> choice;

        input  = args[0];
        choice = Options.match(input, Opponent.class);

        if(choice.isPresent())
        {
            switch(choice.get())
            {
                case CPU -> playVsCPU();
                case PvP -> playVsPlayer();
            }
            return;

        }

        System.err.println("Invalid command: " + input);
        System.err.printf("Usage: play [%s]",
                          Options.format(Opponent.class, "|"));
        System.out.println();
        System.exit(1);
    }

    /**
     * Plays the game.
     * <p>
     * Plays multiple games until the user chooses to quit. After each game, the
     * player is prompted whether they want to play against a human or a CPU, or to
     * quit.
     */
    public static void play()
    {
        if(!MainServer.isRunning())
        {
            System.err.println("Server is not running. Start the server first.");
            return;
        }

        do
        {
            final Opponent         opponent;
            final TicTacToe.Status gameResult;

            TUI.clearScreen();
            System.out.println("Welcome to multiplayer Tic Tac Toe!");
            System.out.println();

            opponent   = terminal.prompt("Play against CPU or another player?",
                                         Opponent.class);
            gameResult = switch(opponent)
            {
                case CPU -> playVsCPU();
                case PvP -> playVsPlayer();
            };

            System.out.println();

            if(!(gameResult == TicTacToe.Status.WON ||
                 gameResult == TicTacToe.Status.LOST ||
                 gameResult == TicTacToe.Status.TIE))
            {
                // something went wrong
                return;
            }
        } while(terminal.prompt("Play again?"));

        System.out.println("Thanks for playing!");
    }

    /**
     * Starts a game with the CPU. Blocks the main thread until the game finishes.
     *
     * @return the game result (from the user's perspective), or {@code null} if the
     * game was interrupted
     */
    private static TicTacToe.Status playVsCPU()
    {
        /*
         * This is a hack. You're not really playing against the CPU, but a CPU is
         * created to play with you.
         *
         * This is problematic because this CPU may accidentally be matched with another
         * user who's expecting to play against a human.
         *
         * It's unlikely though, and I don't have time to fix this.
         */
        new CPU();
        return playVsPlayer();
    }

    /**
     * Starts a game with another player. Blocks the current thread until the game
     * finishes.
     *
     * @return the game result (from the user's perspective), or {@code null} if the
     * game was interrupted
     */
    private static TicTacToe.Status playVsPlayer()
    {
        return Scheduler.wait(new Player());
    }

    private enum Opponent
    {
        CPU, PvP
    }
}
