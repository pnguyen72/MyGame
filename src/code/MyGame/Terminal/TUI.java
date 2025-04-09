package MyGame.Terminal;

import MyGame.MyGame;

/**
 * Utility class for static methods that use ANSI escape sequences to create a TUI.
 * <p>
 * Most essential for the operation of {@link MyGame}, but also used throughout the program
 * for a more pleasant terminal experience.
 *
 * @author Felix Nguyen
 * @version 1
 */
public final class TUI
{
    /**
     * ANSI escape codes for colors.
     */
    public static final class Color
    {
        /**
         * Background colors.
         */
        public enum Background
        {
            BLACK(40),
            RED(41),
            GREEN(42),
            YELLOW(43),
            BLUE(44),
            MAGENTA(45),
            CYAN(46),
            WHITE(47),
            DEFAULT(49);

            private final int value;

            Background(final int value) {this.value = value;}

            @Override
            public String toString() {return String.valueOf(value);}
        }

        /**
         * Foreground colors
         */
        public enum Foreground
        {
            BLACK(30),
            RED(31),
            GREEN(32),
            YELLOW(33),
            BLUE(34),
            MAGENTA(35),
            CYAN(36),
            WHITE(37),
            DEFAULT(39);

            private final int value;

            Foreground(final int value) {this.value = value;}

            @Override
            public String toString() {return String.valueOf(value);}
        }

        private static final String BOLD  = "\033[1m";
        private static final String RESET = "\033[0m";
    }

    /**
     * Makes a string bold when printed to the console
     *
     * @param str a string
     * @return a bold string
     */
    public static String bold(final String str)
    {
        return Color.BOLD + str + Color.RESET;
    }

    /**
     * Gives a string color when printed to the console.
     * <p>
     * Sets background and foreground colors.
     *
     * @param str        a string
     * @param background background color
     * @param foreground foreground color
     * @return a colored string
     */
    public static String colorize(final String str,
                                  final Color.Background background,
                                  final Color.Foreground foreground)
    {
        return String.format("\033[%s;%sm", background, foreground) +
               str +
               Color.RESET;
    }

    /**
     * Gives a string color when printed to the console.
     * <p>
     * Sets the background color, leaves the foreground color default.
     *
     * @param str        a string
     * @param background background color
     * @return a colored string
     */
    public static String colorize(final String str,
                                  final Color.Background background)
    {
        return colorize(str,
                        background,
                        Color.Foreground.DEFAULT);
    }

    /**
     * Gives a string color when printed to the console.
     * <p>
     * Sets the foreground color, leaves the background color default.
     *
     * @param str        a string
     * @param foreground foreground color
     * @return a colored string
     */
    public static String colorize(final String str,
                                  final Color.Foreground foreground)
    {
        return colorize(str,
                        Color.Background.DEFAULT,
                        foreground);
    }

    /**
     * Clears the console.
     */
    public static void clearScreen()
    {
        System.out.print("\033[H\033[J");
    }

    /**
     * Clears the current line in the console.
     */
    public static void clearLine()
    {
        System.out.print("\033[2K");
    }

    /**
     * Moves the cursor up
     *
     * @param lines the number of lines to move up
     */
    public static void cursorUp(final int lines)
    {
        if(lines < 0)
        {
            cursorDown(-lines);
        } else if(lines > 0)
        {
            System.out.printf("\033[%dA\r", lines);
        }
    }

    /**
     * Moves the cursor up one line.
     */
    public static void cursorUp()
    {
        cursorUp(1);
    }

    /**
     * Moves the cursor down
     *
     * @param lines the number of lines to move down
     */
    public static void cursorDown(final int lines)
    {
        if(lines < 0)
        {
            cursorUp(-lines);
        } else if(lines > 0)
        {
            System.out.printf("\033[%dB\r", lines);
        }
    }

    /**
     * Moves the cursor down one line.
     */
    public static void cursorDown()
    {
        cursorDown(1);
    }
}
