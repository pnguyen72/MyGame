package mygame.multiplayer.client;

import java.nio.file.Path;
import java.util.UUID;

import mygame.game.TicTacToe;
import mygame.multiplayer.Protocol;
import mygame.multiplayer.Service;
import mygame.multiplayer.services.Connection;
import mygame.multiplayer.services.Monitor;
import mygame.multiplayer.services.PublisherService;
import mygame.multiplayer.services.Scheduler;

/**
 * A client in the game. Handles connecting to the server, then each turn
 * listening to the server's direction, updating internal game state, and
 * sending move to the server.
 * <p>
 * Requires an implementation of the {@link #decideMove()} method,
 * which returns the move to be played each turn.
 * For example, a CPU-controlled client should implement an algorithm
 * that returns to move to play; a human-controlled client should
 * prompt the user to enter the move.
 * <p>
 * As a {@link PublisherService}, the client publishes the game result when it ends,
 * or {@code null} if the game never started.
 *
 * @author Felix Nguyen
 * @version 1
 * @see Player
 * @see CPU
 */
public abstract class Client extends PublisherService<TicTacToe.Status>
{
    private final String  clientID;
    private final Service mainServerConnection;
    private final Service requestResultMonitor;

    private Service          clientConnection;
    private Service          gameServerConnection;
    private TicTacToe.Status gameStatus;
    private TicTacToe        game;
    private Path             gameMove;
    private Service          moveService;

    /**
     * Instantiates a {@code Client} object and starts the service.
     * <p>
     * It will attempt to connect to the main server
     * and make a request to join the game.
     * When the request is approved (see {@link #handleJoinInvitation}),
     * it will join the game, then listen to the game server
     * for what to do each turn  (see {@link #handleTurnSignal}).
     * <p>
     * If at any point the main server connection fails,
     * the client service will stop.
     */
    public Client()
    {
        log("Waiting for an opponent...");

        final Path server;
        final Path requestClient;
        final Path requestServer;

        clientID      = UUID.randomUUID().toString();
        server        = Protocol.getServer();
        requestClient = Protocol.getRequestClient(clientID);
        requestServer = Protocol.getRequestServer(clientID);

        clientConnection     = new Connection(requestClient);
        mainServerConnection = Monitor.When.connectionLost(server)
                                           .then(this::serverFailed)
                                           .then(this::stop);
        requestResultMonitor = Monitor.When.fileChange(requestServer)
                                           .then(this::handleJoinInvitation);
    }

    /**
     * Gets the move to be played.
     *
     * @return an a number representing the position to be played
     */
    abstract int decideMove();

    /**
     * Stops all game-related services.
     * <p>
     * This is called when the game is over. If force-called,
     * if the client is not yet in a game, it will drop the join request;
     * if it's in a game, it will abandon the game.
     */
    @Override
    public void stop()
    {
        publish(gameStatus);
        mainServerConnection.stop();
        requestResultMonitor.stop();
        if(gameServerConnection != null)
        {
            gameServerConnection.stop();
        }
        if(moveService != null)
        {
            moveService.stop();
        }
        clientConnection.stop();
    }

    /**
     * Creates a game to keep track of the game state.
     * <p>
     * Opponent's ID is required to determine who plays first,
     * which is by alphabetical order of the IDs.
     *
     * @param opponentID the opponent's ID
     * @return a new {@link TicTacToe} instance
     */
    TicTacToe createGame(final String opponentID)
    {
        return new TicTacToe(clientID, opponentID);
    }

    /**
     * Logs a message.
     * <p>
     * Child classes should override this to provide their own logging.
     * By default this prints the message to the console.
     *
     * @param str the message to log
     */
    void log(final String str)
    {
        System.out.println(str);
    }

    /**
     * Gets a random available slot on the board. Returns null if none is available.
     *
     * @return a number representing its position
     */
    final Integer getAvailableMove()
    {
        return game.getAvailableMove();
    }

    /**
     * Plays a move.
     * This updates the internal game state and sends the move to the server.
     *
     * @param move a number representing the position to be played
     */
    private void playMove(final int move)
    {
        game.update(move);
        Protocol.write(gameMove, move);
    }

    /**
     * Handles the server's invitation to join a game.
     * <p>
     * The message should have 2 lines.
     * The first line is the game's ID and the second is the opponent's ID.
     *
     * @param message the server's message
     */
    private void handleJoinInvitation(final String message)
    {
        if(message == null || message.isEmpty())
        {
            return;
        }

        clientConnection.stop();
        requestResultMonitor.stop();

        final String[] lines;
        final String   gameID;
        final String   opponentID;
        final Path     clientFile;
        final Path     serverFile;

        lines                = message.split(System.lineSeparator());
        gameID               = lines[0];
        opponentID           = lines[1];
        game                 = createGame(opponentID);
        gameMove             = Protocol.getMove(gameID, clientID);
        clientFile           = Protocol.getClient(gameID, clientID);
        serverFile           = Protocol.getGameServer(gameID, clientID);
        clientConnection     = new Connection(clientFile);
        gameServerConnection = Monitor.When.fileChange(serverFile)
                                           .then(this::handleTurnSignal);
    }

    /**
     * Handles the server's turn signal.
     * <p>
     * Each turn the server sends a signal of what to do for this turn.
     * The data should have one line containing the game's status,
     * and another line containing what move was previously played.
     * <p>
     * Use the 2nd line to update the internal game state,
     * and handle the status signal as follows:
     *
     * <ul>
     *     <li>
     *         If it's a game-ending signal ({@code WON/LOST/TIE}), {@link #stop()}
     *     </li>
     *     <li>
     *         If it's a {@code WAIT} signal, do nothing.
     *     </li>
     *     <li>
     *         If it's a {@code YOUR_TURN} signal,
     *         call {@link #decideMove()}, then {@link #playMove(int)}.
     *         Because {@code decideMove} is a blocking call,
     *         it will be run in another thread so that
     *         the client can still react to events such as server failure.
     *     </li>
     * </ul>
     *
     * @param message the server's message
     */
    private void handleTurnSignal(final String message)
    {
        if(message == null)
        {
            serverFailed();
            return;
        }
        if(message.isEmpty())
        {
            return;
        }

        final String[] tokens;
        tokens = message.split(System.lineSeparator());
        if(tokens.length == 2)
        {
            final String previousMove;
            previousMove = tokens[1];
            game.update(Integer.parseInt(previousMove));
        } else
        {
            game.update(null);
        }

        this.gameStatus = TicTacToe.Status.valueOf(tokens[0]);
        switch(gameStatus)
        {
            case TIE:
                log("It is tie!");
                stop();
                break;
            case WON:
                if(getAvailableMove() != null)
                {
                    log("Opponent disconnected.");
                } else
                {
                    log("You won!");
                }
                stop();
                break;
            case LOST:
                if(getAvailableMove() != null)
                {
                    clientFailed();
                } else
                {
                    log("You lost!");
                }
                stop();
                break;
            case YOUR_TURN:
                moveService = Scheduler.await(this::decideMove)
                                       .then(this::playMove);
                break;
            case WAIT:
                log("Opponent's turn...");
                break;
            case ERROR:
                stop();
                break;
        }
    }

    /**
     * Handles when the server fails.
     */
    private void serverFailed()
    {
        gameStatus = TicTacToe.Status.ERROR;
        log("Server connection failed.");
    }

    /**
     * Handles when the client fails mid-game.
     */
    private void clientFailed()
    {
        gameStatus = TicTacToe.Status.ERROR;
        log("Client connection failed.");
    }

    /**
     * Gets the client's ID.
     *
     * @return the client's ID
     */
    public final String getClientID()
    {
        return clientID;
    }
}
