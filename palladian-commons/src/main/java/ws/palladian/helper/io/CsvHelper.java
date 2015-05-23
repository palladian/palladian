package ws.palladian.helper.io;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;

public final class CsvHelper {

    private CsvHelper() {
        // no instance
    }

    /**
     * Split a CSV line into parts.
     * 
     * @param line The line to split, not <code>null</code>.
     * @param separator The separator character.
     * @return A list with parts.
     */
    public static List<String> splitCsvLine(String line, char separator) {
        Validate.notNull(line, "line must not be null");
        List<String> split = new ArrayList<>();
        boolean insideQuote = false;
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char currentChar = line.charAt(i);
            if (currentChar == '"' && (buffer.length() == 0 || buffer.charAt(buffer.length() - 1) != '\\')) {
                insideQuote ^= true;
                continue;
            }
            if (currentChar == separator && !insideQuote) {
                split.add(buffer.toString());
                buffer = new StringBuilder();
            } else {
                buffer.append(currentChar);
            }
        }
        if (buffer.length() > 0) {
            split.add(buffer.toString());
        }
        return split;
    }

}
