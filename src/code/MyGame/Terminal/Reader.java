package MyGame.Terminal;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.Optional;
import java.util.function.IntConsumer;

import MyGame.Game.TicTacToeUI;
import MyGame.Terminal.TUI.Color;

/**
 * Class for reading user inputs from the terminal.
 * Provides methods for reading raw key input and prompting user for lines of text.
 * <p>
 * This class is a singleton, and should be accessed through {@link #getInstance}.
 *
 * @author Felix Nguyen
 * @version 1
 */
public final class Reader
{
    private static Reader INSTANCE;

    private final Terminal       terminal;
    private final java.io.Reader keyReader;
    private final LineReader     lineReader;

    /**
     * Gets a {@link Reader} instance.
     * Returns {@code null} if one cannot be created,
     * such as when the terminal emulator is not supported.
     *
     * @return a {@link Reader} object
     */
    public static Reader getInstance()
    {
        if(INSTANCE == null)
        {
            try
            {
                INSTANCE = new Reader();
            } catch(final Exception e)
            {
                System.out.println("Your terminal emulator is not supported. " +
                                   "Please use a different one.");
                System.exit(1);
            }
        }
        return INSTANCE;
    }

    /**
     * Initializes a terminal reader.
     * <p>
     * Wrapped by {@link #getInstance()}, not for external use.
     *
     * @throws IOException if the terminal emulator is not supported
     */
    private Reader() throws IOException
    {
        terminal   = TerminalBuilder.builder().dumb(false).build();
        lineReader = LineReaderBuilder.builder().terminal(terminal).build();
        keyReader  = terminal.reader();
        terminal.enterRawMode();
    }

    /**
     * Keys used in this program.
     *
     * @see TicTacToeUI#prompt
     */
    public enum Key
    {
        /**
         * The Enter key
         */
        ENTER,

        /**
         * The up arrow key
         */
        UP,

        /**
         * The down arrow key
         */
        DOWN,

        /**
         * The right arrow key
         */
        RIGHT,

        /**
         * The left arrow key
         */
        LEFT,

        /**
         * The Q key, either upper or lower case
         */
        Q; // don't use Escape for quitting, it's already used by ANSI escape codes

        /**
         * Converts a key code to a {@link Key}.
         *
         * @param keyCode a key code
         * @return the corresponding {@code Key}, or {@code null} if the key is not
         * recognized
         * @see #readKey()
         */
        private static Key fromKeyCode(final int keyCode)
        {
            return switch(keyCode)
            {
                case 13 -> ENTER;
                case 65 -> UP;
                case 66 -> DOWN;
                case 67 -> RIGHT;
                case 68 -> LEFT;
                case 81, 113 -> Q;
                default -> null;
            };
        }
    }

    /**
     * Reads a keypress from the console.
     *
     * @return a {@link Key}, or {@code null} if it's not one of the recognized keys
     */
    public Key readKey()
    {
        final int keyValue;

        try
        {
            keyValue = keyReader.read();
        } catch(final IOException e)
        {
            throw new RuntimeException(e);
        }

        return Key.fromKeyCode(keyValue);
    }

    /**
     * Reads an input line from the user.
     *
     * @return the string user enters.
     */
    public String readLine()
    {
        String input = "";
        try
        {
            input = lineReader.readLine();
        } catch(final UserInterruptException | EndOfFileException e)
        {
            System.exit(130);
        }
        return input;
    }

    /**
     * Prompts the user for input until they enters a valid choice
     * (case-insensitive), then returns the choice.
     *
     * @param prompt  the prompt to display to the user
     * @param options an Enum class of valid inputs
     * @return the corresponding Enum to the user's input
     */
    public <T extends Enum<?>> T prompt(final String prompt, final Class<T> options)
    {
        int               maxCursorDY;
        final IntConsumer discarded;

        /*
         * Keep track of how far the cursor moves to handle when user enters invalid
         * input
         */
        maxCursorDY = 1;
        discarded   = _ ->
        {
            /*
             * terminal.getCursorPosition takes an IntConsumer, but we don't need to do
             * anything with the value
             */
        };

        if(prompt != null)
        {
            System.out.print(prompt);
            System.out.printf(" [%s]", Options.format(options, "|"));
            System.out.println();
        }

        while(true)
        {
            final String      input;
            final Optional<T> choice;
            final int         cursorY;
            final int         cursorDY;

            // Read cursor position, read console line, then read cursor position again
            cursorY     = terminal.getCursorPosition(discarded).getY();
            input       = readLine();
            cursorDY    = terminal.getCursorPosition(discarded).getY() - cursorY;
            maxCursorDY = Math.max(maxCursorDY, cursorDY);

            // Jump cursor down to the output line
            TUI.cursorDown(maxCursorDY - cursorDY);
            TUI.clearLine();

            // validate, return if valid
            choice = Options.match(input, options);
            if(choice.isPresent())
            {
                return choice.get();
            }

            // Print error message
            System.out.print(TUI.colorize("Valid choices are: ", Color.Foreground.RED));
            System.out.print(Options.format(options, ", "));

            // Clear what the user typed and return the cursor to original position
            for(int i = 0; i < maxCursorDY; i++)
            {
                TUI.cursorUp();
                TUI.clearLine();
            }
        }
    }

    /**
     * Prompts the user a yes/no question.
     *
     * @param prompt the prompt to display to the user
     * @return {@code true} if the user answered yes, {@code false} otherwise
     */
    public boolean prompt(final String prompt)
    {
        return prompt(prompt, YesNo.class) == YesNo.Y;
    }

    /**
     * Enum for yes/no prompts.
     */
    private enum YesNo
    {
        Y, N
    }
}
