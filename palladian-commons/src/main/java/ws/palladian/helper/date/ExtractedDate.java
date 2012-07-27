package ws.palladian.helper.date;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;

/**
 * <p>
 * This class represents a date which was extracted e.g. from a text. The date has properties like <code>year</code>,
 * <code>month</code> , …, <code>seconds</code>. As the information extracted varies, not all of these properties need
 * to be set. Internally, unset properties are initialized with a value of <code>-1</code>. The amount of available data
 * (i.e. the exactness) can be determined using {@link #getExactness()}.
 * </p>
 * 
 * @author Martin Gregor
 * @author Philipp Katz
 */
public class ExtractedDate {

    public static final int YEAR = 1;
    public static final int MONTH = 2;
    public static final int DAY = 3;
    public static final int HOUR = 4;
    public static final int MINUTE = 5;
    public static final int SECOND = 6;

    /** Found date as string. */
    private final String dateString;

    /** The format, the dateString is found. */
    private final String format;

    // date values
    private int year = -1;
    private int month = -1;
    private int day = -1;
    private int hour = -1;
    private int minute = -1;
    private int second = -1;

    // FIXME is this considered?
    private String timeZone = null;

    private double rate = 0;
    
    public ExtractedDate() {
        this(System.currentTimeMillis());
    }
    
    public ExtractedDate(long milliseconds) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(milliseconds);
        this.year = calendar.get(Calendar.YEAR);
        this.month = calendar.get(Calendar.MONTH) + 1;
        this.day = calendar.get(Calendar.DAY_OF_MONTH);
        this.hour = calendar.get(Calendar.HOUR_OF_DAY);
        this.minute = calendar.get(Calendar.MINUTE);
        this.second = calendar.get(Calendar.SECOND);
        this.format = null;
        this.dateString = null;
    }

    /**
     * <p>
     * Copy constructor, to create a new {@link ExtractedDate} with the same properties of the supplied
     * {@link ExtractedDate}.
     * </p>
     * 
     * @param date The date with the properties to copy. Not <code>null</code>.
     */
    public ExtractedDate(ExtractedDate date) {
        super();
        Validate.notNull(date, "date must not be null");
        this.dateString = date.dateString;
        this.format = date.format;
        this.year = date.year;
        this.month = date.month;
        this.day = date.day;
        this.hour = date.hour;
        this.minute = date.minute;
        this.second = date.second;
        this.timeZone = date.timeZone;
    }

    /**
     * <p>
     * Constructor for the {@link DateParser} to create a new {@link ExtractedDate} after the input has been parsed.
     * Only intended for the parser, therefore package private.
     * </p>
     * 
     * @param parseLogic The parse logic providing the input for initialization. Not <code>null</code>.
     */
    ExtractedDate(DateParserLogic parseLogic) {
        Validate.notNull(parseLogic, "parseLogic must not be null");
        this.year = parseLogic.year;
        this.month = parseLogic.month;
        this.day = parseLogic.day;
        this.hour = parseLogic.hour;
        this.minute= parseLogic.minute;
        this.second = parseLogic.second;
        this.timeZone = parseLogic.timeZone;
        this.dateString = parseLogic.originalDateString;
        this.format = parseLogic.format.getFormat();
    }

    /**
     * Constructs a normalized datestring in a format from YYYY-MM-DD HH:MM:SS to YYYY-MM depending of given values.
     * 
     * @param dateParts
     * @return
     */
    public String getNormalizedDateString() {
        return getNormalizedDateString(true);
    }

    /**
     * Converts this extracted-date in a {@link Date}. <br>
     * Be careful, if a datepart is not given, it will be set to 0. (Except day will be set to 1). <br>
     * 
     * @return
     */
    public Date getNormalizedDate() {
        return new Date(getLongDate());
    }

    public long getLongDate() {
        int year = this.year == -1 ? 0 : this.year;
        int month = this.month == -1 ? 0 : this.month - 1;
        int day = this.day == -1 ? 1 : this.day;
        int hour = this.hour == -1 ? 0 : this.hour;
        int minute = this.minute == -1 ? 0 : this.minute;
        int second = this.second == -1 ? 0 : this.second;

        Calendar cal = new GregorianCalendar();
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Constructs a normalized datestring in a format from YYYY-MM-DD HH:MM:SS to YYYY-MM depending of given values
     * 
     * @param time <code>true</code> to include time.
     * @return
     */
    public String getNormalizedDateString(boolean time) {
        StringBuilder normalizedDate = new StringBuilder();
        if (year == -1) {
            normalizedDate.append("0");
        } else {
            normalizedDate.append(year);
        }
        normalizedDate.append("-");
        if (month == -1) {
            normalizedDate.append(0);
        } else {
            normalizedDate.append(get2Digits(month));
        }
        if (day != -1) {
            normalizedDate.append("-").append(get2Digits(day));
            if (hour != -1 && time) {
                normalizedDate.append(" ").append(get2Digits(hour));
                if (minute != -1) {
                    normalizedDate.append(":").append(get2Digits(minute));
                    if (second != -1) {
                        normalizedDate.append(":").append(get2Digits(second));
                    }
                }
            }
        }

        if (normalizedDate.toString().endsWith("-0")) {
            normalizedDate.delete(normalizedDate.length() - 3, normalizedDate.length() - 1);
        }

        return normalizedDate.toString();

    }
    
    /**
     * Adds a leading zero for numbers less then ten. <br>
     * E.g.: 3 ->"03"; 12 -> "12"; 386 -> "376" ...
     * 
     * @param number
     * @return a minimum two digit number
     */
    static String get2Digits(int number) {
        String numberString = String.valueOf(number);
        if (number < 10) {
            numberString = "0" + number;
        }
        return numberString;
    }

    /**
     * 
     * @return
     */
    public String getDateString() {
        return dateString;
    }

    /**
     * 
     * @return
     */
    public String getFormat() {
        return format;
    }

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
    public int get(int field) {
        int value = -1;
        switch (field) {
            case YEAR:
                value = year;
                break;
            case MONTH:
                value = month;
                break;
            case DAY:
                value = day;
                break;
            case HOUR:
                value = hour;
                break;
            case MINUTE:
                value = minute;
                break;
            case SECOND:
                value = second;
                break;
        }
        return value;
    }
    
    public String getTimeZone() {
        return timeZone;
    }

    /**
     * Sets all standard date-properties as an array.<br>
     * From year down to second and timezone.<br>
     * Also date-string and format.<br>
     * <br>
     * Use this this static fields to define a property.
     * 
     * @return
     */
    public void set(int field, int value) {
        switch (field) {
            case YEAR:
                year = value;
                break;
            case MONTH:
                month = value;
                break;
            case DAY:
                day = value;
                break;
            case HOUR:
                hour = value;
                break;
            case MINUTE:
                minute = value;
                break;
            case SECOND:
                second = value;
                break;
        }
    }

    /**
     * String with date properties.<br>
     * Rate, found date string, normalized date, format and technique as string.
     */
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getClass().getSimpleName());
        stringBuilder.append(" [normalizedDate=").append(getNormalizedDateString());
        stringBuilder.append(", format=").append(format);
        stringBuilder.append(", rate=").append(rate);
        if (timeZone != null) {
            stringBuilder.append(", timeZone=").append(timeZone);
        }
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    /**
     * <p>
     * Get the {@link DateExactness} of this {@link ExtractedDate}.
     * </p>
     * 
     * @return The {@link DateExactness} for this {@link ExtractedDate}.
     */
    public DateExactness getExactness() {
        DateExactness exactness = DateExactness.UNSET;
        if (year != -1) {
            exactness = DateExactness.YEAR;
            if (month != -1) {
                exactness = DateExactness.MONTH;
                if (day != -1) {
                    exactness = DateExactness.DAY;
                    if (hour != -1) {
                        exactness = DateExactness.HOUR;
                        if (minute != -1) {
                            exactness = DateExactness.MINUTE;
                            if (second != -1) {
                                exactness = DateExactness.SECOND;
                            }
                        }
                    }
                }
            }
        }
        return exactness;
    }

    /**
     * Set value of date evaluation.
     * 
     * @param rate
     */
    public void setRate(double rate) {
        this.rate = rate;
    }

    /**
     * 
     * @return
     */
    public double getRate() {
        return rate;
    }

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
    public double getDifference(ExtractedDate date, TimeUnit unit) {
        Validate.notNull(unit, "unit must not be null");

        DateExactness exactness = DateExactness.getCommonExactness(this.getExactness(), date.getExactness());
        Calendar cal1 = this.getCalendar(exactness);
        Calendar cal2 = date.getCalendar(exactness);
        return Math.round(Math.abs(cal1.getTimeInMillis() - cal2.getTimeInMillis()) * 100.0 / unit.toMillis(1)) / 100.0;
    }

    private Calendar getCalendar(DateExactness exactness) {
        Calendar calendar = new GregorianCalendar();
        if (DateExactness.YEAR.inRange(exactness)) {
            calendar.set(Calendar.YEAR, year);
            if (DateExactness.MONTH.inRange(exactness)) {
                calendar.set(Calendar.MONTH, month);
                if (DateExactness.DAY.inRange(exactness)) {
                    calendar.set(Calendar.DAY_OF_MONTH, day);
                    if (DateExactness.HOUR.inRange(exactness)) {
                        calendar.set(Calendar.HOUR_OF_DAY, hour);
                        if (DateExactness.MINUTE.inRange(exactness)) {
                            calendar.set(Calendar.MINUTE, minute);
                            if (DateExactness.SECOND.inRange(exactness)) {
                                calendar.set(Calendar.SECOND, second);
                            }
                        }
                    }
                }
            }
        }
        return calendar;
    }
}
