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
public class ExtractedDateImpl implements ExtractedDate {

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
        super();
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
        this.minute= parseLogic.minute;
        this.second = parseLogic.second;
        this.timeZone = parseLogic.timeZone;
        this.dateString = parseLogic.originalDateString;
        this.format = parseLogic.format.getFormat();
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.date.IExtractedDate#getNormalizedDateString()
     */
    @Override
    public String getNormalizedDateString() {
        return getNormalizedDateString(true);
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.date.IExtractedDate#getNormalizedDate()
     */
    @Override
    public Date getNormalizedDate() {
        return new Date(getLongDate());
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.date.IExtractedDate#getLongDate()
     */
    @Override
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

    /* (non-Javadoc)
     * @see ws.palladian.helper.date.IExtractedDate#getNormalizedDateString(boolean)
     */
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

    /* (non-Javadoc)
     * @see ws.palladian.helper.date.IExtractedDate#getDateString()
     */
    @Override
    public String getDateString() {
        return dateString;
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.date.IExtractedDate#getFormat()
     */
    @Override
    public String getFormat() {
        return format;
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.date.IExtractedDate#get(int)
     */
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
    
    /* (non-Javadoc)
     * @see ws.palladian.helper.date.IExtractedDate#getTimeZone()
     */
    @Override
    public String getTimeZone() {
        return timeZone;
    }

//    /* (non-Javadoc)
//     * @see ws.palladian.helper.date.IExtractedDate#set(int, int)
//     */
//    @Override
//    public void set(int field, int value) {
//        switch (field) {
//            case YEAR:
//                year = value;
//                break;
//            case MONTH:
//                month = value;
//                break;
//            case DAY:
//                day = value;
//                break;
//            case HOUR:
//                hour = value;
//                break;
//            case MINUTE:
//                minute = value;
//                break;
//            case SECOND:
//                second = value;
//                break;
//        }
//    }

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
//        stringBuilder.append(", rate=").append(rate);
        if (timeZone != null) {
            stringBuilder.append(", timeZone=").append(timeZone);
        }
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.date.IExtractedDate#getExactness()
     */
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

    /* (non-Javadoc)
     * @see ws.palladian.helper.date.IExtractedDate#getDifference(ws.palladian.helper.date.ExtractedDate, java.util.concurrent.TimeUnit)
     */
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
