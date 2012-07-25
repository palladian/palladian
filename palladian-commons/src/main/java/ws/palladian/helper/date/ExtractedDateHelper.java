package ws.palladian.helper.date;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.dates.DateParser;
import ws.palladian.helper.date.dates.ExtractedDate;
import ws.palladian.helper.nlp.StringHelper;

/**
 * This class contains methods, that help ExtractedDate to define the correct date values.
 * 
 * @author Martin Gregor
 * 
 */
public class ExtractedDateHelper {



    /**
     * Adds a leading zero for numbers less then ten. <br>
     * E.g.: 3 ->"03"; 12 -> "12"; 386 -> "376" ...
     * 
     * @param number
     * @return a minimum two digit number
     */
    public static String get2Digits(int number) {
        String numberString = String.valueOf(number);
        if (number < 10) {
            numberString = "0" + number;
        }
        return numberString;
    }

    /**
     * <p>
     * Creates an {@link ExtractedDate} with current date and time in UTC time zone. Thereby format
     * <code>YYYY-MM-DDTHH:MM:SSZ</code> is used.
     * </p>
     * 
     * @return An {@link ExtractedDate} initialized to current date and time.
     */
    public static ExtractedDate getCurrentDate() {
        return getCurrentDate(null);

    }

    /**
     * <p>
     * Creates an {@link ExtractedDate} with current date and time in specified time zone. Thereby format
     * <code>YYYY-MM-DDTHH:MM:SSZ</code> is used.
     * </p>
     * 
     * @param locale The locale specifying the time zone. <code>null</code> signifies to use UTC time zone.
     * @return An {@link ExtractedDate} initialized to current date and time in the specified time zone.
     */
    public static ExtractedDate getCurrentDate(Locale locale) {
        Calendar cal;
        if (locale != null) {
            cal = new GregorianCalendar(locale);
        } else {
            cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        }

        // cal.getMonth + 1 = month, because in Calendar Jan has number 0.
        String dateString = cal.get(Calendar.YEAR) + "-" + ExtractedDateHelper.get2Digits(cal.get(Calendar.MONTH) + 1)
                + "-" + ExtractedDateHelper.get2Digits(cal.get(Calendar.DAY_OF_MONTH)) + "T"
                + ExtractedDateHelper.get2Digits(cal.get(Calendar.HOUR_OF_DAY)) + ":"
                + ExtractedDateHelper.get2Digits(cal.get(Calendar.MINUTE)) + ":"
                + ExtractedDateHelper.get2Digits(cal.get(Calendar.SECOND)) + "Z";
        String format = RegExp.DATE_ISO8601_YMD_T.getFormat();

        // return new ExtractedDate(dateString, format);
        return DateParser.parse(dateString, format);

    }
    
    /**
     * Creates an ExtrextedDate with given timestamp as date.
     * 
     * @param time timestamp in millisec
     * @return
     */
    public static ExtractedDate createDate(long time) {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(time);
        

        // cal.getMonth + 1 = month, because in Calendar Jan has number 0.
        String dateString = cal.get(Calendar.YEAR) + "-" + ExtractedDateHelper.get2Digits(cal.get(Calendar.MONTH) + 1)
                + "-" + ExtractedDateHelper.get2Digits(cal.get(Calendar.DAY_OF_MONTH)) + "T"
                + ExtractedDateHelper.get2Digits(cal.get(Calendar.HOUR_OF_DAY)) + ":"
                + ExtractedDateHelper.get2Digits(cal.get(Calendar.MINUTE)) + ":"
                + ExtractedDateHelper.get2Digits(cal.get(Calendar.SECOND)) + "Z";
        String format = RegExp.DATE_ISO8601_YMD_T.getFormat();

        //ExtractedDate date = new ExtractedDate(dateString, format);
        //return date;
        return DateParser.parse(dateString, format);

    }
    
    /**
     * Returns a string of "x"s as long as the parameter string.
     * 
     * @param text
     * @return String of "x"s.
     */
    public static String getXs(String text) {
//        StringBuffer sb = new StringBuffer();
//        for (int i = 0; i < text.length(); i++) {
//            sb.append("x");
//        }
//        return sb.toString();
        return StringUtils.repeat('x', text.length());
    }

    /**
     * Check a string for keywords. Used to look in tag-values for date-keys.
     * 
     * @param text string with possible keywords.
     * @param keys a array of keywords.
     * @return the found keyword.
     */
    // TODO move to KEyWords
    public static String hasKeyword(String text, String[] keys) {
        String keyword = null;
        for (String key : keys) {
            Pattern pattern = Pattern.compile(key.toLowerCase());
            Matcher matcher = pattern.matcher(text.toLowerCase());
            if (matcher.find()) {
                keyword = key;
                break;
            }
        }
        return keyword;
    }
    
    /**
     * <p>
     * Sometimes texts in webpages have special code for character. E.g. <i>&ampuuml;</i> or whitespace. To evaluate
     * this text reasonably you need to convert this code.
     * </p>
     * 
     * @param text
     * @return
     */
    public static String replaceHtmlSymbols(String text) {
    
        String result = StringEscapeUtils.unescapeHtml(text);
        result = StringHelper.replaceProtectedSpace(result);
    
        // remove undesired characters
        result = result.replace("&#8203;", " "); // empty whitespace
        result = result.replace("\n", " ");
        result = result.replace("&#09;", " "); // html tabulator
        result = result.replace("\t", " ");
        result = result.replace(" ,", " ");
    
        return result;
    }

}
