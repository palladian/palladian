package ws.palladian.helper.io;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Utility for working with delimited strings, typically within CSV files.
 *
 * @author Philipp Katz
 */
public final class DelimitedStringHelper {

    /**
     * Splits a delimited (e.g. CSV) line into parts.
     *
     * @param line                 The line to split, not <code>null</code>.
     * @param splitCharacter       The split character.
     * @param quoteCharacter       The quote character (inside parts surrounded by this
     *                             character, no split is performed). The quotes surrounding a
     *                             part are automatically removed.
     * @param unescapeDoubleQuotes Whether to treat two consecutive quotes as escaped.
     * @return A list with entries. <code>null</code> in case the input line is
     * not well-formed, i.e. a quote is not closed (happens in case of a
     * line break within quotes).
     */
    public static List<String> splitLine(String line, char splitCharacter, char quoteCharacter, boolean unescapeDoubleQuotes) {
        Objects.requireNonNull(line, "line must not be null");
        List<String> split = new ArrayList<>();
        boolean inQuotes = false;
        int previousIdx = 0;
        for (int idx = 0; idx < line.length(); idx++) {
            char c = line.charAt(idx);
            if (c == splitCharacter && !inQuotes) {
                split.add(trimQuotes(line.substring(previousIdx, idx), quoteCharacter, unescapeDoubleQuotes));
                previousIdx = idx + 1;
            } else if (c == quoteCharacter) {
                inQuotes = !inQuotes;
            }
        }
        if (inQuotes) {
            return null;
        }
        split.add(trimQuotes(line.substring(previousIdx), quoteCharacter, unescapeDoubleQuotes));
        return split;
    }

    /**
     * Splits a delimited (e.g. CSV) line into parts.
     *
     * @param line           The line to split, not <code>null</code>.
     * @param splitCharacter The split character.
     * @param quoteCharacter The quote character (inside parts surrounded by this
     *                       character, no split is performed). The quotes surrounding a
     *                       part are automatically removed.
     * @return A list with entries. <code>null</code> in case the input line is
     * not well-formed, i.e. a quote is not closed (happens in case of a
     * line break within quotes).
     */
    public static List<String> splitLine(String line, char splitCharacter, char quoteCharacter) {
        return splitLine(line, splitCharacter, quoteCharacter, false);
    }

    private static String trimQuotes(String string, char quoteCharacter, boolean unescapeDoubleQuotes) {
        if (string.length() < 2) {
            return string;
        }

        if (string.charAt(0) == quoteCharacter && string.charAt(string.length() - 1) == quoteCharacter) {
            string = string.substring(1, string.length() - 1);
        }

        // replace consecutive quotes
        if (unescapeDoubleQuotes) {
            string = string.replace("" + quoteCharacter + quoteCharacter, "" + quoteCharacter);
        }

        return string;
    }

    private DelimitedStringHelper() {
        // no instantiation
    }

}
