package ws.palladian.helper.date;

import org.apache.commons.lang3.Validate;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

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
public class ExtractedDateImpl implements ExtractedDate, Serializable {
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

    private String timeZone = null;
    private int utcOffsetMinutes = 0;
    private boolean ignoreTimeZone = false;

    public ExtractedDateImpl() {
        this(System.currentTimeMillis());
    }

    public ExtractedDateImpl(long milliseconds) {
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
    protected ExtractedDateImpl(ExtractedDate date) {
        Validate.notNull(date, "date must not be null");
        this.dateString = date.getDateString();
        this.format = date.getFormat();
        this.year = date.get(YEAR);
        this.month = date.get(MONTH);
        this.day = date.get(DAY);
        this.hour = date.get(HOUR);
        this.minute = date.get(MINUTE);
        this.second = date.get(SECOND);
        this.timeZone = date.getTimeZone();
    }

    /**
     * <p>
     * Constructor for the {@link DateParser} to create a new {@link ExtractedDate} after the input has been parsed.
     * Only intended for the parser, therefore package private.
     * </p>
     *
     * @param parseLogic The parse logic providing the input for initialization. Not <code>null</code>.
     */
    ExtractedDateImpl(DateParserLogic parseLogic) {
        Validate.notNull(parseLogic, "parseLogic must not be null");
        this.year = parseLogic.year;
        this.month = parseLogic.month;
        this.day = parseLogic.day;
        this.hour = parseLogic.hour;
        this.minute = parseLogic.minute;
        this.second = parseLogic.second;
        this.timeZone = parseLogic.timeZone;
        this.dateString = parseLogic.originalDateString;
        this.utcOffsetMinutes = parseLogic.utcOffsetMinutes;
        this.ignoreTimeZone = parseLogic.ignoreTimeZone;
        this.format = parseLogic.format.getFormat();
    }

    //    @Override
    //    public String getNormalizedDateStringSourceTimeZone() {
    //        long newTs = getNormalizedDate().getTime() + TimeUnit.MINUTES.toMillis(utcOffsetMinutes);
    //        return DateHelper.getDatetime("yyyy-MM-dd HH:mm:ss", newTs);
    //    }

    @Override
    public String getNormalizedDateString() {
        return getNormalizedDateString(true);
    }

    @Override
    public Date getNormalizedDate() {
        return new Date(getLongDate());
    }

    @Override
    public long getLongDate() {
        int year = this.year == -1 ? 0 : this.year;
        int month = this.month == -1 ? 0 : this.month - 1;
        int day = this.day == -1 ? 1 : this.day;
        int hour = this.hour == -1 ? 0 : this.hour;
        int minute = this.minute == -1 ? 0 : this.minute;
        int second = this.second == -1 ? 0 : this.second;

        Calendar cal = new GregorianCalendar();
        if (timeZone != null && !ignoreTimeZone) {
            cal.setTimeZone(TimeZone.getTimeZone(timeZone));
        }
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    @Override
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
                    } else {
                        normalizedDate.append(":00");
                    }
                }
            }
        }

        if (normalizedDate.toString().endsWith("-0")) {
            normalizedDate.delete(normalizedDate.length() - 2, normalizedDate.length());
        }

        return normalizedDate.toString();
    }

    /**
     * Adds a leading zero for numbers less then ten. <br>
     * E.g.: 3 ->"03"; 12 -> "12"; 386 -> "376" ...
     *
     * @param number The number of which we want two digits.
     * @return A minimum two digit number.
     */
    static String get2Digits(int number) {
        return String.format("%02d", number);
    }

    @Override
    public String getDateString() {
        return dateString;
    }

    @Override
    public String getFormat() {
        return format;
    }

    @Override
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

    @Override
    public String getTimeZone() {
        return timeZone;
    }

    @Override
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    @Override
    public int getUtcOffsetMinutes() {
        return utcOffsetMinutes;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getClass().getSimpleName());
        stringBuilder.append(" [normalizedDate=").append(getNormalizedDateString());
        stringBuilder.append(", format=").append(format);
        if (timeZone != null) {
            stringBuilder.append(", timeZone=").append(timeZone);
        }
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    @Override
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

    @Override
    public double getDifference(ExtractedDate date, TimeUnit unit) {
        Validate.notNull(unit, "unit must not be null");

        DateExactness exactness = DateExactness.getCommonExactness(this.getExactness(), date.getExactness());
        Calendar cal1 = this.getCalendar(exactness);
        Calendar cal2 = date.getCalendar(exactness);
        return Math.round(Math.abs(cal1.getTimeInMillis() - cal2.getTimeInMillis()) * 100.0 / unit.toMillis(1)) / 100.0;
    }

    @Override
    public Calendar getCalendar(DateExactness exactness) {
        Calendar calendar = new GregorianCalendar();
        if (exactness.provides(DateExactness.YEAR)) {
            calendar.set(Calendar.YEAR, year);
            if (exactness.provides(DateExactness.MONTH)) {
                calendar.set(Calendar.MONTH, month);
                if (exactness.provides(DateExactness.DAY)) {
                    calendar.set(Calendar.DAY_OF_MONTH, day);
                    if (exactness.provides(DateExactness.HOUR)) {
                        calendar.set(Calendar.HOUR_OF_DAY, hour);
                        if (exactness.provides(DateExactness.MINUTE)) {
                            calendar.set(Calendar.MINUTE, minute);
                            if (exactness.provides(DateExactness.SECOND)) {
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
