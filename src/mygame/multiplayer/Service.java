package mygame.multiplayer;

import mygame.multiplayer.services.PublisherService;
import mygame.multiplayer.services.Connection;

/**
 * Something that runs in another thread.
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
