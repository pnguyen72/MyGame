package mygame.multiplayer;

import mygame.multiplayer.services.PublisherService;

/**
 * The Observer in the observer design pattern, but with a cooler name.
 *
 * @param <T> the type of argument the subscriber accepts
 * @author Felix Nguyen
 * @version 1
 * @see PublisherService
 */
@FunctionalInterface
public interface Subscriber<T>
{
    /**
     * Receives a message from a {@link Publisher}
     *
     * @param message the message
     */
    void update(final T message);
}
