package ws.palladian.helper.normalization;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The DateNormalizer normalizes dates.
 */
public class DateNormalizer {
    
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DateNormalizer.class);

    public static String normalizeDateFormat(Date date, String format) {
        // Locale.setDefault(Locale.ENGLISH);
        Calendar c = Calendar.getInstance(Locale.ENGLISH);
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        c.setTime(date);
        String dateString = c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DATE) + " "
        + c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND);
        return normalizeDateFormat(dateString, format);
    }

    private static String normalizeDateFormat(String dateString, String format) {
        String normalizedDate = "";

        // Locale.setDefault(Locale.ENGLISH);
        DateFormat dfm = new SimpleDateFormat(format, Locale.ENGLISH);

        try {
            Date rfcDate = dfm.parse(dateString);
            Calendar c = Calendar.getInstance();
            c.setTimeZone(TimeZone.getTimeZone("UTC"));
            c.setTime(rfcDate);

            String year = String.valueOf(c.get(Calendar.YEAR));
            String month = String.valueOf(c.get(Calendar.MONTH) + 1);
            String date = String.valueOf(c.get(Calendar.DATE));
            String hour = String.valueOf(c.get(Calendar.HOUR_OF_DAY));
            String minute = String.valueOf(c.get(Calendar.MINUTE));
            String second = String.valueOf(c.get(Calendar.SECOND));

            if (month.length() < 2) {
                month = "0" + month;
            }
            if (date.length() < 2) {
                date = "0" + date;
            }
            if (hour.length() < 2) {
                hour = "0" + hour;
            }
            if (minute.length() < 2) {
                minute = "0" + minute;
            }
            if (second.length() < 2) {
                second = "0" + second;
            }

            normalizedDate = year + "-" + month + "-" + date + " " + hour + ":" + minute + ":" + second;

        } catch (ParseException e) {
            LOGGER.debug("{} could not be parsed for {}", format, dateString);
        }

        return normalizedDate;
    }

}