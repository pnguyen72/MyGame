package MyGame.Server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import MyGame.GameFiles;
import MyGame.Game.TicTacToe;
import MyGame.Interfaces.Service;
import MyGame.Services.Monitor;

/**
 * An individual game's server. Monitors the clients' moves,
 * decides whose turn to move, and announces the game result.
 *
 * @author Felix Nguyen
 * @version 1
 */
final class GameServer
{
    private final String            gameID;
    private final Path              gameDirectory;
    private final Service           newClientsNotifier;
    private final Map<Path, String> clientIDs;

    private TicTacToe game;
    private Service   moveNotifier;
    private Integer   previousMove;

    /**
     * Starts a server.
     *
     * @param ID the game ID
     */
    GameServer(final String ID)
    {
        gameID    = ID;
        clientIDs = new HashMap<>();

        gameDirectory      = GameFiles.getGame(gameID);
        newClientsNotifier = Monitor.When.directoryChange(gameDirectory)
                                         .then(this::addClient);
    }

    /* Receive connection from a client. */
    private void addClient(final Path path)
    {
        if(!Files.isDirectory(path))
        {
            return;
        }

        // when the game has had enough players
        if(clientIDs.size() >= TicTacToe.PLAYERS_PER_GAME)
        {
            GameFiles.removeRecursive(path);
            return;
        }

        final String clientID;
        final Path   client;

        clientID = path.getFileName().toString();
        client   = GameFiles.getClient(gameID, clientID);
        clientIDs.put(client, clientID);

        /*
         * No need to keep a reference to this service to stop it.
         * The game server only stops whens all clients have disconnected,
         * and this service stops itself when triggered.
         */
        Monitor.When.connectionLost(client)
                    .then(this::clientDisconnected);

        if(clientIDs.size() == TicTacToe.PLAYERS_PER_GAME)
        {
            startGame();
        }
    }

    /**
     * Handles when a client is disconnected.
     * <p>
     * If they were the last client, stops the server.
     * Else, if the game is not over, declares the other client the winner.
     */
    private void clientDisconnected(final Path client)
    {
        final String disconnectedID;
        disconnectedID = clientIDs.remove(client);

        if(clientIDs.isEmpty())
        {
            disconnect();
        } else if(game.getAvailableMove() != null)
        {
            signal(disconnectedID, TicTacToe.Status.LOST);
            clientIDs.values()
                     .forEach(id -> signal(id, TicTacToe.Status.WON));
        }
    }

    /**
     * Starts the game.
     * <p>
     * Sets up the services needed to maintain the game,
     * then signals the first client to move.
     */
    private void startGame()
    {
        final String[]      clientIDArray;
        final List<Service> moveNotifiers;

        newClientsNotifier.stop();
        clientIDArray = clientIDs.values().toArray(String[]::new);
        moveNotifiers = clientIDs.values()
                                 .stream()
                                 .map(this::getMoveNotifier).toList();

        this.moveNotifier = () -> moveNotifiers.forEach(Service::stop);
        this.game         = new TicTacToe(clientIDArray);

        signalNextTurn();
    }

    /**
     * Gets a {@link MyGame.Services.Monitor} that monitors a client's moves.
     *
     * @param clientID ID of the client to monitor
     * @return a {@link MyGame.Interfaces.Service}
     */
    private Service getMoveNotifier(final String clientID)
    {
        final Path clientMove;
        clientMove = GameFiles.getMove(gameID, clientID);
        return Monitor.When.fileChange(clientMove)
                           .then(move -> play(clientID, move));
    }

    /**
     * Signals clients to play the next turn.
     * <p>
     * Sends to this turn's client a YOUR_TURN signal and what their opponent played,
     * and to the other client a WAIT signal.
     */
    private void signalNextTurn()
    {
        final String clientID;
        final String winnerID;

        clientID = game.getNextPlayerID();
        winnerID = game.getWinnerID();

        if(winnerID != null)
        {
            endGame(winnerID);
            return;
        }

        signal(clientID, TicTacToe.Status.YOUR_TURN, previousMove);

        clientIDs.values()
                 .stream()
                 .filter(Predicate.not(clientID::equals))
                 .forEach(otherID -> signal(otherID, TicTacToe.Status.WAIT));
    }

    /**
     * Handles a client's move.
     * <p>
     * Updates internal game state and signals the next turn.
     */
    private void play(final String clientID,
                      final String move)
    {
        if(!game.getNextPlayerID().equals(clientID))
        {
            return;
        }
        if(move == null)
        {
            return;
        }
        previousMove = Integer.parseInt(move);
        game.update(previousMove);
        signalNextTurn();
    }

    /**
     * Ends the game, announces the winner.
     *
     * @param winnerID ID of the winner. It's a tie, it will be null.
     */
    private void endGame(final String winnerID)
    {
        if(winnerID == null)
        {
            clientIDs.values()
                     .forEach((id) -> signal(id,
                                             TicTacToe.Status.TIE,
                                             previousMove));
            return;
        }
        signal(winnerID, TicTacToe.Status.WON);
        clientIDs.values()
                 .stream()
                 .filter(Predicate.not(winnerID::equals))
                 .forEach(loserID -> signal(loserID,
                                            TicTacToe.Status.LOST,
                                            previousMove));
    }

    /**
     * Send a signal to a client.
     *
     * @param clientID     the client's ID
     * @param status       the game's status (YOUR_TURN, WAIT, WON, LOST, TIE)
     * @param previousMove the game's previous move
     */
    private void signal(final String clientID,
                        final TicTacToe.Status status,
                        final Integer previousMove)
    {
        final Path serverFile;
        serverFile = GameFiles.getGameServer(gameID, clientID);

        if(previousMove == null)
        {
            GameFiles.write(serverFile, status);
        } else
        {
            GameFiles.write(serverFile, status +
                                        System.lineSeparator() +
                                        previousMove);
        }
    }

    /**
     * Send a signal to a client.
     *
     * @param clientID the client's ID
     * @param status   the game's status (YOUR_TURN, WAIT, WON, LOST, TIE)
     */
    private void signal(final String clientID,
                        final TicTacToe.Status status)
    {
        signal(clientID, status, null);
    }

    /**
     * Stops all services. Called when all clients are disconnected.
     */
    private void disconnect()
    {
        newClientsNotifier.stop();
        if(moveNotifier != null)
        {
            moveNotifier.stop();
        }
        GameFiles.removeRecursive(gameDirectory);
    }
}
