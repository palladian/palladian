package ws.palladian.helper.date;

import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.helper.DateFormat;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.nlp.StringHelper;

public class DateParser {

    /**
     * <p>
     * Parse a date given a specific format.
     * </p>
     * 
     * @param date The string with the date to be parsed, not <code>null</code>.
     * @param format The format describing the date to be parsed, not <code>null</code>.
     * @return
     */
    public static ExtractedDate parse(String date, String format) {
        DateParserLogic parseLogic = new DateParserLogic();
        parseLogic.parse(date, format);
        return new ExtractedDate(parseLogic.year, parseLogic.month, parseLogic.day, parseLogic.hour, parseLogic.minute,
                parseLogic.second, parseLogic.timezone, date, format);
    }
    
    /**
     * <p>
     * Try to parse a date from a text by trying to match all known date formats, as specified by {@link RegExp#ALL_DATE_FORMATS}.
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
        for (DateFormat dateFormat : formats) {
            // the old code had a catch Throwable around find date, I removed this for now,
            // if exceptions are encountered, try to solve them, and do not just catch them away.
            ExtractedDate extractedDate = findDate(text, dateFormat);
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
        
        text = ExtractedDateHelper.replaceHtmlSymbols(text);
        text = StringHelper.removeDoubleWhitespaces(text); // FIXME is this necessary?
        
        ExtractedDate result = null;
        
        Matcher matcher = format.getPattern().matcher(text);
        if (matcher.find()) {
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
                result = parse(matcher.group(), format.getFormat());
            }
        }
        return result;
    }
    
    //Monat und Jahr sind nur gerundet.
    public static ExtractedDate findRelativeDate(String text) {

        ExtractedDate date = null;
        DateFormat[] relativeDateFormats = RegExp.RELATIVE_DATES;
        for (DateFormat dateFormat : relativeDateFormats) {
            Pattern pattern = dateFormat.getPattern();
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String relativeTime = matcher.group();
                long number = Long.valueOf(relativeTime.split(" ")[0]);

                String format = dateFormat.getFormat();
                GregorianCalendar cal = new GregorianCalendar();
                long actTime = cal.getTimeInMillis();
                long difTime = 0;
                if (format.equalsIgnoreCase("min")) {
                    difTime = number * 60 * 1000;
                } else if (format.equalsIgnoreCase("hour")) {
                    difTime = number * 60 * 60 * 1000;
                } else if (format.equalsIgnoreCase("day")) {
                    difTime = number * 24 * 60 * 60 * 1000;
                } else if (format.equalsIgnoreCase("mon")) {
                    difTime = number * 30 * 24 * 60 * 60 * 1000;
                } else if (format.equalsIgnoreCase("year")) {
                    difTime = number * 365 * 24 * 60 * 60 * 1000;
                }

                long relTime = actTime - difTime;
                date = ExtractedDateHelper.createDate(relTime);
                break;
            }
        }
        return date;
    }

}
