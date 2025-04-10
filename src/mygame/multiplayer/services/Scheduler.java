package mygame.multiplayer.services;

import java.io.InterruptedIOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

import mygame.multiplayer.Service;

/**
 * Utility class for service-related operations.
 *
 * @author Felix Nguyen
 * @version 1
 */
public final class Scheduler
{
    /**
     * The game clients and server's tick rate, in milliseconds.
     * {@link Connection}s, {@link Monitor}s, etc. are run at this rate.
     * <p>
     * It's tempting to make this number as small as possible,
     * but if it's too small, the OS complains with a bunch of IO errors.
     */
    static final int CLOCK_PERIOD_MILLIS = 300;

    private static final int IMMEDIATELY = 0;

    /**
     * Run a task repeatedly in another thread.
     * <p>
     * If the task throws an exception, the program will crash.
     * This method is meant for mission-critical tasks that cannot not fail
     * (e.g. the game's main loop).
     * <p>
     * Returns a {@link Service} that can be used to stop the repetition.
     * It, however, does not interrupt a task currently running.
     *
     * @param task the task to be run repeatedly
     * @return a {@link Service}
     * @see Monitor
     */
    public static Service repeat(final Runnable task)
    {
        final Timer timer;
        timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                try
                {
                    task.run();
                } catch(final Exception e)
                {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }, IMMEDIATELY, CLOCK_PERIOD_MILLIS);

        return timer::cancel;
    }

    /**
     * Asynchronously wait for a task to finish.
     * <p>
     * Returns a {@link PublisherService} that publishes the task's return value.
     * <p>
     * Does not actually start the service.
     * As normal, it only starts when a callback is attached.
     *
     * @param task the task to run
     * @param <T>  the task's return type
     * @return a {@link PublisherService}
     */
    public static <T> PublisherService<T> await(final Supplier<T> task)
    {
        return new Future<>(task);
    }

    /**
     * Asynchronously wait for a task to finish.
     * <p>
     * Returns a {@link PublisherService} that triggers when the task finishes.
     * <p>
     * Does not actually start the service.
     * As normal, it only starts when a callback is attached.
     *
     * @param task the task to run
     * @return a {@link PublisherService}
     */
    public static PublisherService<Void> await(final Runnable task)
    {
        return new Future<>(() ->
                            {
                                task.run();
                                return null;
                            });
    }

    /**
     * Asynchronously wait for a given amount of time.
     * <p>
     * Returns a {@link PublisherService} that triggers when the wait is over.
     *
     * @param durationMillis wait duration in milliseconds
     * @return a {@link PublisherService}
     */
    public static PublisherService<Void> await(final int durationMillis)
    {
        return await(() -> wait(durationMillis));
    }

    /**
     * Blocks the current thread until a {@link PublisherService} publishes a value,
     * then returns that value.
     * <p>
     * If the service can publish multiple times,
     * it will keep doing so in its own thread.
     * This only waits until the first value is received.
     *
     * @param publisher a {@link PublisherService}
     * @param <T>       the publisher's message type
     * @return the first value the publisher publishes
     */
    public static <T> T wait(final PublisherService<T> publisher)
    {
        final var status = new Object()
        {
            T       value;
            boolean received = false;
        };

        publisher.then((result) ->
                       {
                           status.value    = result;
                           status.received = true;
                       });

        do
        {
            wait(CLOCK_PERIOD_MILLIS);
        } while(!status.received);
        return status.value;
    }

    /**
     * Pause the current thread for a given amount of time.
     *
     * @param durationMillis the length of time in milliseconds
     */
    public static void wait(final int durationMillis)
    {
        try
        {
            Thread.sleep(durationMillis);
        } catch(final InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

    private Scheduler() {}

    /**
     * Runs a task in another thread and publishes when the task returns.
     * <p>
     * Wrapped by {@link #await}, not for external use.
     *
     * @param <T> the task's return type
     */
    private static class Future<T> extends PublisherService<T>
    {
        private Thread thread;

        /**
         * Creates a thread that runs the task and publishes the task's return value.
         *
         * @param task the task to wait for
         */
        Future(final Supplier<T> task)
        {
            thread = new Thread(
                    () ->
                    {
                        try
                        {
                            publish(task.get());
                        } catch(final Exception e)
                        {
                            /*
                             * Ignore interrupt exceptions.
                             * They are expected when the thread is interrupted,
                             * which is when the service is stopped.
                             */
                            if(!(e.getCause() instanceof InterruptedException ||
                                 e.getCause() instanceof InterruptedIOException))
                            {
                                throw e;
                            }
                        }
                    });
        }

        @Override
        public void start() {thread.start();}

        @Override
        public void stop()
        {
            if(thread != null)
            {
                thread.interrupt();
                thread = null;
            }
        }
    }
}