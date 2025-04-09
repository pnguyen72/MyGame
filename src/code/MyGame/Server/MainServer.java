package MyGame.Server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import MyGame.GameFiles;
import MyGame.MyGame;
import MyGame.Game.TicTacToe;
import MyGame.Services.Connection;
import MyGame.Services.Monitor;

/**
 * The game system's main server. Handles client requests to join a game and
 * creates a {@link GameServer} for each game.
 *
 * @author Felix Nguyen
 * @version 1
 */
public final class MainServer
{
    private static final Path SERVER           = GameFiles.getServer();
    private static final Path INTERRUPT_SIGNAL = GameFiles.getInterruptSignal(SERVER);
    private static final Path REQUESTS         = GameFiles.getRequests();

    private final List<Path> requestsQueue;

    /**
     * Entry point to the program that starts the server.
     *
     * @param args an optional subcommand:
     *             <ul>
     *                 <li>interrupt:      stop the server</li>
     *                 <li>self-interrupt: start the server, which will stop itself when
     *                                     the source code changes</li>
     *             </ul>
     */
    public static void main(final String[] args)
    {
        if(args.length == 0)
        {
            new MainServer();
        } else if(args[0].equalsIgnoreCase("interrupt"))
        {
            MainServer.interrupt();
        } else
        {
            System.out.println("Invalid command: " + args[0]);
            System.out.println("Usage: server [interrupt]");
            System.exit(1);
        }
    }

    /**
     * Starts the server.
     *
     * @param selfInterrupt whether to watch the source code directory and
     *                      stop when there are changes (useful for debugging)
     */
    public MainServer()
    {
        if(isRunning())
        {
            throw new IllegalStateException("Server is already running");
        }

        System.out.println("Server started.");
        GameFiles.emptyDirRecursive(MyGame.PATH);

        /*
         * It's a queue but there's no need to use the queue data structure,
         * since we only have to invite the first two clients to a game,
         * when received the invitation they will join the game and
         * remove themselves from the queue.
         */
        requestsQueue = new ArrayList<>();

        new Connection(SERVER).allowInterrupt()
                              .then(this::handleInterrupt);
        Monitor.When.directoryChange(REQUESTS)
                    .then(this::handleRequest);
    }

    /**
     * Check whether the server is running.
     *
     * @return whether the server is running.
     */
    public static boolean isRunning()
    {
        return Connection.isActive(SERVER);
    }

    /**
     * Interrupts the server.
     * <p>
     * Sends a signal that tells the currently running server to stop itself.
     * <p>
     * Has no effect if the server is not running.
     */
    public static void interrupt()
    {
        GameFiles.emptyDirRecursive(MyGame.PATH);
        GameFiles.create(INTERRUPT_SIGNAL);
    }

    /**
     * Gets the requests queue
     *
     * @return the requests queue
     */
    public List<Path> getRequests()
    {
        return requestsQueue;
    }

    /* Handles interrupt signal being raised. */
    private void handleInterrupt()
    {
        System.out.println("Interrupt signal received.");
        GameFiles.emptyDirRecursive(MyGame.PATH);
        System.exit(130);
    }

    /**
     *  Handles when a new client joins the requests queue.
     */
    private void handleRequest(final Path request)
    {
        if(!Files.isDirectory(request))
        {
            return;
        }

        final String clientID;
        final Path   requestClient;
        clientID      = request.getFileName().toString();
        requestClient = GameFiles.getRequestClient(clientID);

        requestsQueue.add(request);
        Monitor.When.connectionLost(requestClient)
                    .then(() -> requestsQueue.remove(request))
                    .then(() -> GameFiles.removeRecursive(request));

        if(requestsQueue.size() >= TicTacToe.PLAYERS_PER_GAME)
        {
            createGame(requestsQueue.stream()
                                    .limit(TicTacToe.PLAYERS_PER_GAME)
                                    .toArray(Path[]::new));
        }
    }

    /** 
     * Starts a game for the specified clients.
     *
     * @param clients the clients that will play the game
     * @throws IllegalArgumentException if the number of clients is invalid 
     */
    private void createGame(final Path[] clients)
    {
        if(clients.length != TicTacToe.PLAYERS_PER_GAME)
        {
            throw new IllegalArgumentException("Invalid number of clients for a game");
        }

        final String gameID;
        final String client1ID;
        final String client2ID;

        gameID = UUID.randomUUID().toString();
        new GameServer(gameID); // no need to keep a reference to the game server,
        // it will stop itself when the clients disconnect


        client1ID = clients[0].getFileName().toString();
        client2ID = clients[1].getFileName().toString();

        GameFiles.write(GameFiles.getRequestServer(client1ID),
                        gameID + System.lineSeparator() + client2ID);
        GameFiles.write(GameFiles.getRequestServer(client2ID),
                        gameID + System.lineSeparator() + client1ID);
    }
}
