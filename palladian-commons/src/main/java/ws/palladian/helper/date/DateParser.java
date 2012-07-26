package ws.palladian.helper.date;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.helper.DateFormat;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * The {@link DateParser} provides universal parse functionality for dates. Dates can be parsed without knowing their
 * format in advance. The DateParser will try out all known date formats (as defined in {@link RegExp}) and the date
 * accordingly, use method {@link #findDate(String)} in this case. If the date format to be parsed is known a priory,
 * the methods {@link #findDate(String, DateFormat)} and {@link #findDate(String, DateFormat[])} can be used, supplying
 * a {@link DateFormat} as specified in {@link RegExp}. All find methods operate on texts, this means, that the date to
 * be parsed may be surrounded with other text.
 * </p>
 * 
 * @author Martin Gregor
 * @author Philipp Katz
 */
public final class DateParser {

    private DateParser() {
        // utility class, no instances.
    }

    /**
     * <p>
     * Parse a date given a specific format.
     * </p>
     * 
     * @param date The string with the date to be parsed, not <code>null</code>.
     * @param format The format describing the date to be parsed, not <code>null</code>.
     * @return
     */
    public static ExtractedDate parseDate(String date, String format) {
        DateParserLogic parseLogic = new DateParserLogic(date, format);
        parseLogic.parse();
        return new ExtractedDate(parseLogic);
    }

    /**
     * <p>
     * Try to parse a date from a text by trying to match all known date formats, as specified by
     * {@link RegExp#ALL_DATE_FORMATS}.
     * </p>
     * 
     * @param text The text to check for a date, not <code>null</code>.
     * @return The {@link ExtractedDate}, or <code>null</code> if the specified text contained no matching date.
     */
    public static ExtractedDate findDate(String text) {
        return findDate(text, RegExp.ALL_DATE_FORMATS);
    }

    /**
     * <p>
     * Try to parse a date from a text by trying the specified date formats.
     * </p>
     * 
     * @param text The string with the date to be parsed, not <code>null</code>.
     * @param formats An array of formats to try for parsing, not <code>null</code>.
     * @return The {@link ExtractedDate}, or <code>null</code> if the specified string could not be matched by the given
     *         {@link DateFormat}s.
     */
    public static ExtractedDate findDate(String text, DateFormat[] formats) {
        for (DateFormat format : formats) {
            // the old code had a catch Throwable around find date, I removed this for now,
            // if exceptions are encountered, try to solve them, and do not just catch them away.
            ExtractedDate extractedDate = findDate(text, format);
            if (extractedDate != null) {
                return extractedDate;
            }
        }
        return null;
    }

    /**
     * <p>
     * Try to parse a date from a text be trying the specified date format.
     * </p>
     * 
     * @param string The text to check for a date, not <code>null</code>.
     * @param format The format to try for parsing, not <code>null</code>.
     * @return The {@link ExtractedDate} if found, or <code>null</code> if the specified text did not contain a date
     *         matched by the given {@link DateFormat}.
     */
    public static ExtractedDate findDate(String text, DateFormat format) {
        text = StringHelper.removeDoubleWhitespaces(text);
        ExtractedDate result = null;
        Matcher matcher = format.getPattern().matcher(text);
        if (matcher.find()) {
            // Determine, if the found potential date string is directly surrounded by digits.
            // In this case, we skip the pattern and advance to the next one.
            boolean digitNeighbor = false;
            int start = matcher.start();
            if (start > 0) {
                digitNeighbor = Character.isDigit(text.charAt(start - 1));
            }
            int end = matcher.end();
            // if last character is "/" no check for number is needed.
            if (end < text.length() && text.charAt(end - 1) != '/') {
                digitNeighbor = Character.isDigit(text.charAt(end));
            }
            if (!digitNeighbor) {
                result = parseDate(matcher.group(), format.getFormat());
            }
        }
        return result;
    }

    /** package private -- for unit testing only; allows to supply a hypothetical time stamp for the "current" time. */
    static ExtractedDate findRelativeDate(String text, long currentTime) {
        ExtractedDate date = null;
        for (DateFormat dateFormat : RegExp.RELATIVE_DATES) {
            Pattern pattern = dateFormat.getPattern();
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String relativeTime = matcher.group();
                long number = Long.valueOf(relativeTime.split(" ")[0]);

                String format = dateFormat.getFormat();
                long diffTime = 0;
                if (format.equalsIgnoreCase("min")) {
                    diffTime = TimeUnit.MINUTES.toMillis(number);
                } else if (format.equalsIgnoreCase("hour")) {
                    diffTime = TimeUnit.HOURS.toMillis(number);
                } else if (format.equalsIgnoreCase("day")) {
                    diffTime = TimeUnit.DAYS.toMillis(number);
                } else if (format.equalsIgnoreCase("mon")) {
                    diffTime = number * 30 * 24 * 60 * 60 * 1000;
                } else if (format.equalsIgnoreCase("year")) {
                    diffTime = number * 365 * 24 * 60 * 60 * 1000;
                }
                long relTime = currentTime - diffTime;
                date = new ExtractedDate(relTime);
                break;
            }
        }
        return date;
    }

    /**
     * <p>
     * Try to parse a date from text with relatives dates (e.g. <code>1 day ago</code>, ...). Month and year values are
     * rounded values, i.e. the calendar properties are not considered.
     * </p>
     * 
     * @param string The text to check for a date, not <code>null</code>.
     * @return The {@link ExtractedDate} if found, or <code>null</code> if the specified text did not contain a relative
     *         date.
     */
    public static ExtractedDate findRelativeDate(String text) {
        return findRelativeDate(text, System.currentTimeMillis());
    }

}
