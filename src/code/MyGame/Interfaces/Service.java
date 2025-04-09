package MyGame.Interfaces;

import MyGame.Services.Connection;
import MyGame.Services.PublisherService;

/**
 * Something that runs in a thread.
 * <p>
 * A {@link #stop} method is required, but {@link #start} is optional
 * because depending on the implementation,
 * the service may start immediately when instantiated.
 *
 * @author Felix Nguyen
 * @version 1
 * @see Connection
 * @see PublisherService
 */
@FunctionalInterface
public interface Service
{
    /**
     * Starts the service.
     */
    default void start() {}

    /**
     * Stops the service.
     */
    void stop();
}
