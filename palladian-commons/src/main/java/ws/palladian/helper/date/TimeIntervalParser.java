package ws.palladian.helper.date;

import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * Extract mentions of time intervals in texts and transform them, e.g. "the movie lasted 2 hours and 5 minutes" => 125
 * minutes.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class TimeIntervalParser {

    /**
     * <p>
     * Return the number of seconds to which the interval in the string was normalized.
     * </p>
     * 
     * @param string The input string.
     * @return The number of seconds of the mentioned time interval. Returns <tt>Null</tt> if nothing was found.
     */
    public static Long parse(String string) {
        int days = 0;
        int hours = 0;
        int minutes = 0;
        long seconds = 0;

        boolean parsed = false;

        try {
            days = Integer.valueOf(StringHelper.getRegexpMatch("[0-9]+(?=\\s?([dD]ays?))", string));
            parsed = true;
        } catch (Exception e) {
        }

        try {
            hours = Integer.valueOf(StringHelper.getRegexpMatch("[0-9]+(?=\\s?([hH]ours?|hrs?))", string));
            parsed = true;
        } catch (Exception e) {
        }

        try {
            minutes = Integer.valueOf(StringHelper.getRegexpMatch("[0-9]+(?=\\s?([mM]inutes?|mins?))", string));
            parsed = true;
        } catch (Exception e) {
        }
        try {
            seconds = Long.valueOf(StringHelper.getRegexpMatch("[0-9]+(?=\\s?([sS]econds?|secs?))", string));
            parsed = true;
        } catch (Exception e) {
        }

        if (parsed) {
            seconds += 86400 * days + 3600 * hours + 60 * minutes;
        } else {
            return null;
        }

        return seconds;
    }

    /**
     * <p>
     * </p>
     * 
     * @param args
     */
    public static void main(String[] args) {
        System.out.println(TimeIntervalParser.parse("the movie lasted 2 hours and 5 minutes"));
        System.out.println(TimeIntervalParser.parse("4 hrs 20 mins"));
        System.out.println(TimeIntervalParser.parse("1 day 23 mins"));
    }

}
