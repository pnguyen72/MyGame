package MyGame.Services;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import MyGame.GameFiles;
import MyGame.Interfaces.Service;
import MyGame.Interfaces.Subscriber;

/**
 * A Connection is the way the server and clients
 * signal to each other that they are still active.
 * <p>
 * This is so that, for example, if the server crashes, the clients should disconnect;
 * or when a player abandons the game, the other player should win.
 * <p>
 * To maintain a Connection, the connector repeatedly (see {@link Scheduler#repeat})
 * writes to a designated file, constantly changing its content.
 * Other parties monitor this file (see {@link Connection.Monitor});
 * if it stops changing, it means that party has disconnected.
 *
 * @author Felix Nguyen
 * @version 1
 */
public final class Connection implements Service
{
    private static final int BUFFER_MILLIS  = 30;
    private static final int TIMEOUT_MILLIS = Scheduler.CLOCK_PERIOD_MILLIS +
                                              BUFFER_MILLIS;

    private static final Map<Path, Connection> CONNECTIONS = new HashMap<>();
    private static final int                   MAX_COUNTER = 100000;

    private final Path        path;
    private final Service     updater;
    private final FileMonitor whenInterrupted;

    private int counter = 0;

    /**
     * Starts a connection.
     *
     * @param connectionPath path to the connection file
     */
    public Connection(final Path connectionPath)
    {
        if(CONNECTIONS.containsKey(connectionPath))
        {
            throw new IllegalStateException("Connection already exists at this file");
        }
        CONNECTIONS.put(connectionPath, this);

        final Path interruptSignal;
        interruptSignal = GameFiles.getInterruptSignal(connectionPath);
        GameFiles.removeRecursive(interruptSignal);

        this.path            = connectionPath;
        this.whenInterrupted = new FileMonitor(interruptSignal);
        this.updater         = Scheduler.repeat(this::maintainConnection);
    }

    /**
     * Writes to the connection file.
     */
    private void maintainConnection()
    {
        counter = (counter + 1) % MAX_COUNTER;
        GameFiles.write(path, counter);
    }

    /**
     * Allows the connection to be interrupted by an external process.
     * <p>
     * This watches for the creation of an interrupt file
     * in the parent directory of the connection file;
     * when it's created, stops the connection.
     *
     * @return a {@link PublisherService} that triggers when the connection is interrupted
     */
    public PublisherService<String> allowInterrupt()
    {
        whenInterrupted.then(this::stop);

        return new PublisherService<>()
        {
            @Override
            public void stop()
            {
                Connection.this.stop();
            }

            @Override
            public PublisherService<String> then(final Subscriber<String> callback)
            {
                whenInterrupted.then(callback);
                return this;
            }
        };
    }

    /**
     * A {@link PublisherService} that monitors a connection.
     * When the connection is lost, publishes the connection path and stops itself.
     * <p>
     * It does so by having a {@link FileMonitor} that monitors the connection file,
     * with a callback that updates the internal clock;
     * and a timer that periodically checks how long it's been since the last update;
     * if it's been too long, it means the connection is lost.
     */
    public static final class Monitor extends MyGame.Services.Monitor<Path>
    {
        private final Path        filePath;
        private final FileMonitor monitor;

        private long lastUpdateTime;

        /**
         * Creates a connection monitor. Does not start until a callback is added.
         *
         * @param connectionPath path to the connection file
         */
        public Monitor(final Path connectionPath)
        {
            filePath = connectionPath;
            monitor  = new FileMonitor(connectionPath);
        }

        /**
         * Checks whether the connection is still active.
         * <p>
         * If {@code current time - last update time} is larger
         * than the scheduler's clock period plus a bit of buffer time,
         * it means the connection is lost. In that case
         * calls the callbacks with the connection path and stops the service.
         */
        @Override
        void poll()
        {
            if(System.currentTimeMillis() - lastUpdateTime > TIMEOUT_MILLIS)
            {
                publish(filePath);
                stop();
            }
        }

        @Override
        public void start()
        {
            updateTime();
            monitor.then(this::updateTime);
            super.start();
        }

        @Override
        public void stop()
        {
            monitor.stop();
            super.stop();
        }

        private void updateTime()
        {
            lastUpdateTime = System.currentTimeMillis();
        }
    }

    /**
     * Check whether a connection is active at the moment.
     * Blocks the current thread to do the checking.
     * <p>
     * It does so by reading the connection file,
     * wait for the scheduler's clock period plus a bit of buffer time,
     * and then read the file again.
     * The connection is active if and only if the file content has changed.
     *
     * @param filePath the connection file
     * @return whether it's active
     */
    public static boolean isActive(final Path filePath)
    {
        final String contentBefore;
        final String contentAfter;

        contentBefore = GameFiles.read(filePath);
        Scheduler.wait(TIMEOUT_MILLIS);
        contentAfter = GameFiles.read(filePath);

        return contentAfter != null && !contentAfter.equals(contentBefore);
    }

    @Override
    public void stop()
    {
        updater.stop();
        if(whenInterrupted != null)
        {
            whenInterrupted.stop();
        }
        CONNECTIONS.remove(path);
    }
}