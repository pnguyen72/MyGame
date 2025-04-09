package MyGame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Utility class for game-related file operations.
 * <p>
 * See {@link MyGame} for how the files are structured and used.
 */
public final class GameFiles
{
    static final String SERVER_FILE      = "server.txt";
    static final String CLIENT_FILE      = "client.txt";
    static final String INTERRUPT_SIGNAL = "interrupt";
    static final String REQUESTS_DIR     = "requests";
    static final String GAMES_DIR        = "games";
    static final String MOVE_FILE        = "move.txt";

    /**
     * Gets the path to the system server's file.
     *
     * @return the path to the system server's file
     */
    public static Path getServer()
    {
        return MyGame.PATH.resolve(SERVER_FILE);
    }

    /**
     * Gets the path to the game server's file.
     *
     * @param gameID   the game's ID
     * @param clientID the client's ID
     * @return the path to the game server's file
     */
    public static Path getGameServer(final String gameID,
                                     final String clientID)
    {
        return getGame(gameID).resolve(clientID).resolve(SERVER_FILE);
    }

    /**
     * Gets the path to the interrupt signal for a connection.
     *
     * @param connection the path to the connection
     * @return the path to the interrupt signal for the connection
     */
    public static Path getInterruptSignal(final Path connection)
    {
        return connection.getParent().resolve(INTERRUPT_SIGNAL);
    }

    /**
     * Gets the path to requests directory.
     *
     * @return the path to requests directory
     */
    public static Path getRequests()
    {
        return MyGame.PATH.resolve(REQUESTS_DIR);
    }

    /**
     * Gets the path to an individual request's directory,
     * which contains a client file and a server file.
     *
     * @param clientID the client's ID
     * @return the path to the request
     */
    public static Path getRequest(final String clientID)
    {
        return getRequests().resolve(clientID);
    }

    /**
     * Gets the path to the client file of a request.
     *
     * @param clientID the client's ID
     * @return the path to the client file of a request
     */
    public static Path getRequestClient(final String clientID)
    {
        return getRequest(clientID).resolve(CLIENT_FILE);
    }

    /**
     * Gets the path to the server file of a request.
     *
     * @param clientID the client's ID
     * @return the path to the server file of a request
     */
    public static Path getRequestServer(final String clientID)
    {
        return getRequest(clientID).resolve(SERVER_FILE);
    }

    /**
     * Gets the path to the directory containing all games.
     *
     * @return the path to the games directory
     */
    public static Path getGames()
    {
        return MyGame.PATH.resolve(GAMES_DIR);
    }

    /**
     * Gets the path to an individual game's directory.
     *
     * @param gameID the game's ID
     * @return the path to the game
     */
    public static Path getGame(final String gameID)
    {
        return getGames().resolve(gameID);
    }

    /**
     * Gets the path to a client's directory in a game.
     *
     * @param gameID   the game's ID
     * @param clientID the client's ID
     * @return the path to the client
     */
    public static Path getClient(final String gameID,
                                 final String clientID)
    {
        return getGame(gameID).resolve(clientID).resolve(CLIENT_FILE);
    }

    /**
     * Gets the path to a client's move file.
     *
     * @param gameID   the game's ID
     * @param clientID the client's ID
     * @return the path to the client's move file
     */
    public static Path getMove(final String gameID,
                               final String clientID)
    {
        return getGame(gameID).resolve(clientID).resolve(MOVE_FILE);
    }

    /**
     * Writes to a file.
     * Creates the file if it doesn't exist.
     * Creates the parent directories if they don't exist.
     *
     * @param filePath the path to the file
     * @param content  the content to write to the file
     * @throws RuntimeException if the file can't be written
     */
    public static void write(final Path filePath,
                             final Object content)
    {
        try
        {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath,
                              content.toString(),
                              StandardOpenOption.CREATE,
                              StandardOpenOption.TRUNCATE_EXISTING);
        } catch(final IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a file.
     * Creates the parent directories if they don't exist.
     *
     * @param filePath the path to the file
     */
    public static void create(final Path filePath)
    {
        write(filePath, "");
    }

    /**
     * Reads a file. Returns {@code null} if for any reason the file can't be read.
     *
     * @param filePath the path to the file
     * @return the content of the file
     */
    public static String read(final Path filePath)
    {
        try
        {
            return Files.readString(filePath);
        } catch(final IOException e)
        {
            return null;
        }
    }

    /**
     * {@code rm -rf} a path. Silently ignores all errors.
     *
     * @param filePath the path to {@code rm -rf}
     */
    public static void removeRecursive(final Path filePath)
    {
        try
        {
            if(Files.isDirectory(filePath))
            {
                emptyDirRecursive(filePath);
                Files.deleteIfExists(filePath);
            } else
            {
                Files.deleteIfExists(filePath);
            }
        } catch(final Exception ignored)
        {
            /*
             * Deleting stuff is not critical to this program's operation,
             * it's just for cleaning up. So we should ignore all exception.
             */
        }
    }

    /**
     * Recursively removes all files in a directory, but not the directory itself.
     * Silently ignores all errors.
     *
     * @param dirPath the path to the directory
     */
    public static void emptyDirRecursive(final Path dirPath)
    {
        try(final Stream<Path> filesStream = Files.list(dirPath))
        {
            filesStream.forEach(GameFiles::removeRecursive);

        } catch(final Exception ignored)
        {
            /*
             * Deleting stuff is not critical to this program's operation,
             * it's just for cleaning up. So we should ignore all exception.
             */
        }
    }

    /**
     * Lists all files in a directory.
     * Returns an empty list if for any reason the operation fails.
     *
     * @param dirPath the path to the directory
     * @return a list of all files in the directory
     */
    public static List<Path> listDir(final Path dirPath)
    {
        if(!Files.exists(dirPath))
        {
            return new ArrayList<>();
        }

        final List<Path> result;
        try(final Stream<Path> stream = Files.list(dirPath))
        {
            result = new ArrayList<>(stream.toList());
        } catch(IOException e)
        {
            return new ArrayList<>();
        }
        return result;
    }
}
