package mygame.multiplayer.services;

import java.nio.file.Path;

import mygame.multiplayer.Service;
import mygame.multiplayer.services.Connection.ConnectionMonitor;

/**
 * An abstract {@link PublisherService} that periodically
 * polls something and calls the callbacks with the result if there is an update.
 * This "something" must be implemented by the {@link #poll} method.
 *
 * @param <T> the type of argument the callbacks may take
 * @author Felix Nguyen
 * @version 1
 * @see FileMonitor
 * @see DirectoryMonitor
 * @see ConnectionMonitor
 */
public abstract class Monitor<T> extends PublisherService<T>
{
    private Service timer;

    /**
     * Creates a monitor. The monitor does not start until a callback is added.
     */
    public Monitor() {}

    /**
     * This method is called periodically to check for changes.
     * It should call {@link #publish} if there is an update.
     */
    abstract void poll();

    @Override
    public void start()
    {
        if(timer == null)
        {
            timer = Scheduler.repeat(this::poll);
        }
    }

    @Override
    public void stop()
    {
        if(timer != null)
        {
            timer.stop();
        }
    }

    /**
     * Wrapper class for all monitors used in this program,
     * with the purpose to make the code more readable.
     * <p>
     * For example, instead of writing
     * <pre>
     *    new FileMonitor(file).then(this::doSomething);
     * </pre>
     * you can write
     * <pre>
     *   When.fileChange(file).then(this::doSomething);
     * </pre>
     */
    public final static class When
    {
        /**
         * Creates a file change monitor.
         * The monitor does not start until a callback is added.
         *
         * @param file path to the file
         * @return a {@link PublisherService} that publishes the new content of the file every
         * time it changes
         * @see FileMonitor
         */
        public static PublisherService<String> fileChange(final Path file)
        {
            return new FileMonitor(file);
        }

        /**
         * Creates a directory change monitor.
         * The monitor does not start until a callback is added.
         *
         * @param directory path to the directory
         * @return a {@link DirectoryMonitor} that publishes new paths when they are added to the
         * directory
         * @see DirectoryMonitor
         */
        public static PublisherService<Path> directoryChange(final Path directory)
        {
            return new DirectoryMonitor(directory);
        }

        /**
         * Creates a connection monitor.
         * The monitor does not start until a callback is added.
         *
         * @param connectionPath path to the connection file
         * @return a {@link PublisherService} that publishes the connection path when the
         * connection is lost
         * @see ConnectionMonitor
         */
        public static PublisherService<Path> connectionLost(final Path connectionPath)
        {
            return new ConnectionMonitor(connectionPath);
        }

        private When() {}
    }
}
