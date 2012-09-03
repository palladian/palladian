package ws.palladian.helper.date;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public interface ExtractedDate {

    public static final int YEAR = 1;
    public static final int MONTH = 2;
    public static final int DAY = 3;
    public static final int HOUR = 4;
    public static final int MINUTE = 5;
    public static final int SECOND = 6;

    /**
     * Constructs a normalized datestring in a format from YYYY-MM-DD HH:MM:SS to YYYY-MM depending of given values.
     * 
     * @param dateParts
     * @return
     */
    String getNormalizedDateString();

    /**
     * Converts this extracted-date in a {@link Date}. <br>
     * Be careful, if a datepart is not given, it will be set to 0. (Except day will be set to 1). <br>
     * 
     * @return
     */
    Date getNormalizedDate();

    long getLongDate();

    /**
     * <p>
     * Constructs a normalized date string in a format from <code>YYYY-MM-DD HH:MM:SS</code> to <code>YYYY-MM</code>
     * depending of given values.
     * </p>
     * 
     * @param time <code>true</code> to include time (the time is only included, if time values are actually given, i.e.
     *            in an {@link ExtractedDate} where time parts are not set, they will not be returned, altough this
     *            parameter might be set to <code>true</code>).
     * @return A normalized date string as described above.
     */
    String getNormalizedDateString(boolean time);

    /**
     * 
     * @return
     */
    String getDateString();

    /**
     * 
     * @return
     */
    String getFormat();

    /**
     * Returns date values. <br>
     * To get a value use static date fields of this class.<br>
     * <br>
     * Only for date properties. For date-technique use getType(). <br>
     * Use this static fields to define a property.
     * 
     * @param field
     * @return
     */
    int get(int field);

    String getTimeZone();

    /**
     * Sets all standard date-properties as an array.<br>
     * From year down to second and timezone.<br>
     * Also date-string and format.<br>
     * <br>
     * Use this this static fields to define a property.
     * 
     * @return
     */
    void set(int field, int value);

    /**
     * <p>
     * Get the {@link DateExactness} of this {@link ExtractedDate}.
     * </p>
     * 
     * @return The {@link DateExactness} for this {@link ExtractedDate}.
     */
    DateExactness getExactness();

    /**
     * <p>
     * Returns the difference between this and another {@link ExtractedDate}. If dates cannot be compared, a value of
     * <code>-1</code> will be returned. Otherwise, the difference is calculated with the maximum possible exactness
     * (year—month—day—hour—minute—second), and as absolute, positive value. The measure of the result can be set using
     * the {@link TimeUnit} parameter.
     * </p>
     * 
     * @param date The other date for which to calculate the difference from this one, not <code>null</code>.
     * @param unit The time unit for the result.
     * @return A positive difference, or <code>-1</code> in case of any error.
     */
    double getDifference(ExtractedDate date, TimeUnit unit);
    
    Calendar getCalendar(DateExactness exactness);

}