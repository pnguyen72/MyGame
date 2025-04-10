package mygame;

import java.util.Optional;

import mygame.terminal.Reader;
import mygame.terminal.TUI;
import mygame.terminal.Option;
import mygame.game.TicTacToe;
import mygame.multiplayer.Protocol;
import mygame.multiplayer.client.Client;
import mygame.multiplayer.client.CPU;
import mygame.multiplayer.client.Player;
import mygame.multiplayer.server.MainServer;
import mygame.multiplayer.services.Scheduler;

/**
 * A multiplayer tic-tac-toe game.
 * <p>
 * This program does not implement or handle anything network-related.
 * It's multiplayer in the sense that there is a Server (see {@link MainServer})
 * and multiple Clients (see {@link Client}), each is an individual Java program
 * that runs independently of the others and communicate via a protocol.
 * However, they all run on the same machine. To make it really "multiplayer",
 * users have to remote-connect to the same machine using a third-party tool (e.g. SSH).
 * <p>
 * For how multiplayer is implemented, see {@link Protocol}.
 * For how the game itself works, see {@link TicTacToe}.
 *
 * @author Felix Nguyen
 * @version 1
 */
public final class Main
{
    private static final Reader terminal = Reader.getInstance();

    /**
     * Entry point to the program to play the game.
     * <p>
     * To play this game, the {@link Server} program must be running.
     * Start it first and keep it running in the background.
     *
     * @param args an optional subcommand (case-insensitive):
     *             <ul>
     *             <li>PvP: start a player vs player game</li>
     *             <li>CPU: start a player vs CPU game</li>
     *             </ul>
     *             If no subcommand is given, the user will be prompted to choose.
     */
    public static void main(final String[] args)
    {
        if(!MainServer.isRunning())
        {
            System.err.println("Server is not running.");
            System.exit(1);
        }

        if(args.length == 0)
        {
            playLoop(null);
            return;
        }

        final String             input;
        final Optional<Opponent> opponent;

        input    = args[0];
        opponent = Option.match(input, Opponent.class);

        if(opponent.isPresent())
        {
            playLoop(opponent.get());
            return;
        }

        System.err.println("Invalid command: " + input);
        System.err.printf("Usage: play [%s]",
                          Option.format(Opponent.class, "|"));
        System.out.println();
        System.exit(1);
    }

    /**
     * Plays the game repeatedly until the user chooses to quit.
     *
     * @param opponent the opponent to play against,
     *                 or {@code null} to prompt the user to choose
     */
    private static void playLoop(Opponent opponent)
    {
        do
        {
            final TicTacToe.Status gameResult;

            TUI.clearScreen();
            if(opponent == null)
            {
                System.out.println("Welcome to multiplayer Tic Tac Toe!");
                System.out.println();
                opponent = terminal.prompt("Play against CPU or another player?",
                                           Opponent.class);
            }

            gameResult = switch(opponent)
            {
                case CPU -> playVsCPU();
                case PvP -> playVsPlayer();
            };

            if(gameResult == TicTacToe.Status.ERROR)
            {
                System.exit(1);
            }

            System.out.println();
        } while(terminal.prompt("Play again?"));

        System.out.println("Thanks for playing!");
    }

    /**
     * Starts a game with the CPU.
     * This blocks the current thread until the game finishes.
     *
     * @return the game result
     */
    private static TicTacToe.Status playVsCPU()
    {
        /*
         * This is a hack. You're not really playing against the CPU, but a CPU is
         * created to play with you.
         *
         * This is problematic because this CPU may accidentally be matched with another
         * user who's expecting to play against a human.
         *
         * It's unlikely though, and I don't have time to fix this.
         */
        new CPU();
        return playVsPlayer();
    }

    /**
     * Starts a game with another player.
     * This blocks the current thread until the game finishes.
     *
     * @return the game result
     */
    private static TicTacToe.Status playVsPlayer()
    {
        return Scheduler.wait(new Player());
    }

    private enum Opponent
    {
        CPU, PvP
    }

    private Main() {}
}
