package mygame.multiplayer.server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mygame.multiplayer.Protocol;
import mygame.game.TicTacToe;
import mygame.multiplayer.services.Monitor;
import mygame.multiplayer.services.Connection;

/**
 * The game system's main server. Handles client requests to join a game and
 * creates a {@link GameServer} for each game.
 *
 * @author Felix Nguyen
 * @version 1
 */
public final class MainServer
{
    private static final Path SERVER           = Protocol.getServer();
    private static final Path INTERRUPT_SIGNAL = Protocol.getInterruptSignal(SERVER);
    private static final Path REQUESTS         = Protocol.getRequests();

    private final List<Path> requestsQueue;

    /**
     * Starts the server.
     */
    public MainServer()
    {
        if(isRunning())
        {
            throw new IllegalStateException("Server is already running");
        }

        System.out.println("Server started.");
        Protocol.reset();

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
        Protocol.create(INTERRUPT_SIGNAL);
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
        System.exit(130);
    }

    /**
     * Handles when a new client joins the requests queue.
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
        requestClient = Protocol.getRequestClient(clientID);

        requestsQueue.add(request);
        Monitor.When.connectionLost(requestClient)
                    .then(() -> requestsQueue.remove(request))
                    .then(() -> Protocol.removeRecursive(request));

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

        Protocol.write(Protocol.getRequestServer(client1ID),
                       gameID + System.lineSeparator() + client2ID);
        Protocol.write(Protocol.getRequestServer(client2ID),
                       gameID + System.lineSeparator() + client1ID);
    }
}
