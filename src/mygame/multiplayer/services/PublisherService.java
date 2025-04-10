package mygame.multiplayer.services;

import java.util.ArrayList;
import java.util.List;

import mygame.multiplayer.Publisher;
import mygame.multiplayer.Service;
import mygame.multiplayer.Subscriber;

/**
 * An abstract implementation of {@link Publisher} and {@link Service}.
 * <p>
 * Subscribers for this class are also called "callbacks".
 * The preferred way to attach a callback is via the {@link #then(Subscriber)} method,
 * which also starts the service if it's not already started and returns itself.
 * <p>
 * For convenience, callbacks that don't take any argument are also accepted.
 * When {@link #publish} is called, those will be called without the argument.
 *
 * @param <T> the type of argument the callbacks may take
 * @author Felix Nguyen
 * @version 1
 * @see Monitor
 */
public abstract class PublisherService<T> implements Publisher<T>, Service
{
    private final List<Subscriber<T>> callbacks = new ArrayList<>();

    /**
     * Creates a publisher service.
     * Does not start the service until a callback is added.
     */
    public PublisherService() {}

    /**
     * Adds a callback. This will also start the service if it's the first callback.
     *
     * @param callback to be called each time the publisher has an update
     * @return itself
     */
    public PublisherService<T> then(final Subscriber<T> callback)
    {
        attach(callback);
        if(callbacks.size() == 1)
        {
            start();
        }
        return this;
    }

    /**
     * Adds a parameterless callback. This will also start the service if it's the first callback.
     * <p>
     * The callback will be called when {@link #publish} is called,
     * but with the message discarded.
     *
     * @param callback to be called each time the publisher has an update
     * @return itself
     */
    public final PublisherService<T> then(final Runnable callback)
    {
        return then((_) -> callback.run());
    }

    @Override
    public final void publish(final T message)
    {
        callbacks.forEach(cb -> cb.update(message));
    }

    /**
     * Adds a callback.
     * <p>
     * Wrapped by {@link #then(Subscriber)} and {@link #then(Runnable)};
     * not recommend to be used directly.
     */
    @Override
    public final void attach(final Subscriber<T> subscriber)
    {
        callbacks.add(subscriber);
    }
}
