package mygame.terminal;

/**
 * Utility class for static methods that use ANSI escape sequences to create a TUI.
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
         * ANSI escape codes for background colors.
         */
        public enum Background
        {
            /**
             * Black background color
             */
            BLACK(40),

            /**
             * Red background color
             */
            RED(41),

            /**
             * Green background color
             */
            GREEN(42),

            /**
             * Yellow background color
             */
            YELLOW(43),

            /**
             * Blue background color
             */
            BLUE(44),

            /**
             * Magenta background color
             */
            MAGENTA(45),

            /**
             * Cyan background color
             */
            CYAN(46),

            /**
             * White background color
             */
            WHITE(47),

            /**
             * Default background color
             */
            DEFAULT(49);

            private final int value;

            Background(final int value) {this.value = value;}

            @Override
            public String toString() {return String.valueOf(value);}
        }

        /**
         * ANSI escape codes for foreground colors.
         */
        public enum Foreground
        {
            /**
             * Black foreground color
             */
            BLACK(30),

            /**
             * Red foreground color
             */
            RED(31),

            /**
             * Green foreground color
             */
            GREEN(32),

            /**
             * Yellow foreground color
             */
            YELLOW(33),

            /**
             * Blue foreground color
             */
            BLUE(34),

            /**
             * Magenta foreground color
             */
            MAGENTA(35),

            /**
             * Cyan foreground color
             */
            CYAN(36),

            /**
             * White foreground color
             */
            WHITE(37),

            /**
             * Default foreground color
             */
            DEFAULT(39);

            private final int value;

            Foreground(final int value) {this.value = value;}

            @Override
            public String toString() {return String.valueOf(value);}
        }

        private static final String BOLD  = "\033[1m";
        private static final String RESET = "\033[0m";

        private Color() {}
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

    private TUI() {}
}
