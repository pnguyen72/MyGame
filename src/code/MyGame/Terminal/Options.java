package MyGame.Terminal;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Utility class for handling user options in the terminal.
 * <p>
 * Provides methods to format an Enum class into a string (to display options to the user),
 * and to match a string to an Enum (handle user input)
 *
 * @author Felix Nguyen
 * @version 1
 */
final public class Options
{
    /**
     * Formats an Enum class into a string.
     *
     * @param options   an Enum class of options
     * @param separator the separator to use between options
     * @return a formatted string
     */
    public static String format(final Class<? extends Enum<?>> options,
                                final String separator)
    {
        return Arrays.stream(options.getEnumConstants())
                     .map(Enum::name)
                     .map(String::toLowerCase)
                     .collect(Collectors.joining(separator));
    }

    /**
     * Matches a string to an Enum class, case-insensitive.
     *
     * @param <T>     the type of the Enum
     * @param input   the input string to match
     * @param options the Enum class to match against
     * @return an Optional containing the matching Enum, or empty if no match is found
     */
    public static <T extends Enum<?>> Optional<T> match(final String input,
                                                        final Class<T> options)
    {
        return Arrays.stream(options.getEnumConstants())
                     .filter(option -> option.name().equalsIgnoreCase(input))
                     .findAny();
    }
}
