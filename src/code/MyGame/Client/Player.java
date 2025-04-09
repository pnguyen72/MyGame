package MyGame.Client;

import MyGame.Terminal.Reader;
import MyGame.Terminal.Reader.Key;
import MyGame.Game.TicTacToe;
import MyGame.Game.TicTacToeUI;
import MyGame.Interfaces.Service;
import MyGame.Services.PublisherService;
import MyGame.Services.Scheduler;

/**
 * A human-controlled client.
 * <p>
 * Implements {@link #decideMove()} that prompts the player to pick their move,
 * overrides {@link #createGame} so that it returns a {@link TicTacToeUI},
 * and allows the user to cancel the join request.
 *
 * @author Felix Nguyen
 * @version 1
 */
public final class Player extends Client
{
    private static final Reader keyReader = Reader.getInstance();

    private final Service cancelHandler;

    private TicTacToeUI game;

    /**
     * Creates a human-controlled client.
     * <p>
     * Overrides the parent's constructor to create a service that
     * allows the user to cancel a join request.
     *
     * @see Client#Client()
     */
    public Player()
    {
        super();
        /*
         * This only lets user cancel the join request.
         * Once the game starts, the user cannot force-quit it
         * (without quitting the whole program).
         * Work is in progress to implement this. Soon, hopefully.
         */
        cancelHandler = new UserCancel().then(this::stop);
    }

    /**
     * Overrides {@link Client#createGame} to return a {@link TicTacToeUI}
     * instead of a {@link TicTacToe}, the former being capable of
     * printing the board to the screen and prompting the user
     * to choose a move.
     * <p>
     * Also cancels the service that lets user cancel a join request,
     * because when this method is called, the game has already started.
     *
     * @see Client#createGame
     */
    @Override
    TicTacToe createGame(final String opponentID)
    {
        cancelHandler.stop();
        game = new TicTacToeUI(getClientID(), opponentID);
        return game;
    }

    /**
     * Prompts the user to choose the move.
     *
     * @return the move to be played
     * @see TicTacToeUI#prompt()
     */
    @Override
    int decideMove()
    {
        return game.prompt();
    }

    @Override
    public void stop()
    {
        super.stop();
        cancelHandler.stop();
    }

    /**
     * A {@link PublisherService} that triggers
     * when the user chooses to cancel the join request,
     * then stops itself.
     */
    private class UserCancel extends PublisherService<Void>
    {
        private static final int PATIENCE_MILLIS = 5000;

        final Service timer;
        final Service waiter;

        /**
         * Instantiates and starts the service.
         */
        UserCancel()
        {
            timer = Scheduler.await(PATIENCE_MILLIS)
                             .then(this::showPrompt);

            waiter = Scheduler.await(this::untilUserPressQ)
                              .then(this::stop)
                              .then(this::publish);
        }

        private void showPrompt()
        {
            log("Press Q to cancel, or feel free to keep waiting...");
        }

        private void untilUserPressQ()
        {
            Key keyPressed;
            do
            {
                keyPressed = keyReader.readKey();
            } while(Key.Q != keyPressed);
        }

        @Override
        public void stop()
        {
            timer.stop();
            waiter.stop();
        }
    }
}
