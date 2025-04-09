package MyGame.Client;

/**
 * A CPU-controlled client.
 * <p>
 * This CPU is very bad, it simply plays whichever move is available.
 * It's not meant to be smart, but to show polymorphism in action;
 * that the system works with any kind of client that uses the same protocol,
 * including an algorithm that plays randomly.
 *
 * @author Felix Nguyen
 * @version 1
 */
public final class CPU extends Client
{
    @Override
    int decideMove() 
    {
        return getAvailableMove();
    }

    /**
     * Logs a message to /dev/null
     * <p>
     * This is a CPU, it shouldn't print anything to the screen.
     *
     * @param str the message to be ignored
     */
    @Override
    void log(final String str) {}
}
