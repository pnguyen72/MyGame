package MyGame.Interfaces;

import MyGame.Services.PublisherService;

/**
 * The Subject in the observer design pattern, but with a cooler name.
 *
 * @param <T> the type of argument the subscribers take
 * @author Felix Nguyen
 * @version 1
 * @see PublisherService
 */
public interface Publisher<T>
{
    /**
     * Notifies the subscribers with a message.
     *
     * @param message the message
     */
    void publish(final T message);

    /**
     * Adds a subscriber.
     *
     * @param subscriber to be called when the publisher has an update
     */
    void attach(final Subscriber<T> subscriber);
}
