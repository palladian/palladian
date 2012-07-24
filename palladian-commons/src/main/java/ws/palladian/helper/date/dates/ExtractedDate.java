package ws.palladian.helper.date.dates;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.date.ExtractedDateHelper;

/**
 * Represents a date, found in a webpage. <br>
 * A object will be created with a date-string and a possible format. <br>
 * It can be asked for year, month, day and time. If some values can not be constructed the value will be -1.
 * 
 * @author Martin Gregor
 */
public class ExtractedDate implements AbstractDate {

    /** Found date as string. */
    private final String dateString;

    /** The format, the dateString is found. */
    private final String format;

    /** URL */
    private String url = null;

    // date values
    private int year = -1;
    private int month = -1;
    private int day = -1;
    private int hour = -1;
    private int minute = -1;
    private int second = -1;

    // FIXME is this considered?
    private String timezone = null;

    private double rate = 0;

    /**
     * creates a new date and sets dateString and format
     * 
     * @param dateString
     * @param format
     */
    protected ExtractedDate(String dateString, String format) {
        super();
        this.dateString = dateString;
        this.format = format;
        // setDateParticles();
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
        this.timezone = date.timezone;
        this.url = date.url;
    }

    @Override
    public String getNormalizedDateString() {
        return getNormalizedDate(true);
    }

    @Override
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

    @Override
    public String getNormalizedDate(boolean time) {
        String normalizedDate;
        if (year == -1) {
            normalizedDate = "0";
        } else {
            normalizedDate = String.valueOf(year);
        }
        if (month == -1) {
            normalizedDate += "-0";
        } else {
            normalizedDate += "-" + ExtractedDateHelper.get2Digits(month);
        }
        if (day != -1) {
            normalizedDate += "-" + ExtractedDateHelper.get2Digits(day);
            if (hour != -1 && time) {
                normalizedDate += " " + ExtractedDateHelper.get2Digits(hour);
                if (minute != -1) {
                    normalizedDate += ":" + ExtractedDateHelper.get2Digits(minute);
                    if (second != -1) {
                        normalizedDate += ":" + ExtractedDateHelper.get2Digits(second);
                    }
                }
            }
        }

        if (normalizedDate.endsWith("-0")) {
            normalizedDate = normalizedDate.replace("-0", "");
        }

        return normalizedDate;

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

    @Override
    public int get(int field) {
        int value = -1;
        switch (field) {
            case YEAR:
                value = this.year;
                break;
            case MONTH:
                value = this.month;
                break;
            case DAY:
                value = this.day;
                break;
            case HOUR:
                value = this.hour;
                break;
            case MINUTE:
                value = this.minute;
                break;
            case SECOND:
                value = this.second;
                break;
        }
        return value;
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
                this.year = value;
                break;
            case MONTH:
                this.month = value;
                break;
            case DAY:
                this.day = value;
                break;
            case HOUR:
                this.hour = value;
                break;
            case MINUTE:
                this.minute = value;
                break;
            case SECOND:
                this.second = value;
                break;
        }
    }

    /**
     * String with date properties.<br>
     * Rate, found date string, normalized date, format and technique as string.
     */
    @Override
    public String toString() {
        return "rate: " + rate + " " + dateString + " -> " + this.getNormalizedDateString() + " Format: " + this.format;// +
                                                                                                                        // " Technique: "
                                                                                                                        // +
                                                                                                                        // getType();
    }

    /**
     * This field gives you the possibility to store the url, the date was found at.
     * 
     * @param url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * 
     * @return
     */
    public String getUrl() {
        return url;
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
        if (this.year != -1) {
            exactness = DateExactness.YEAR;
            if (this.month != -1) {
                exactness = DateExactness.MONTH;
                if (this.day != -1) {
                    exactness = DateExactness.DAY;
                    if (this.hour != -1) {
                        exactness = DateExactness.HOUR;
                        if (this.minute != -1) {
                            exactness = DateExactness.MINUTE;
                            if (this.second != -1) {
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
     * Extracted date has no keyword. But is needed for toString.
     * 
     * @return
     */
    public String getKeyword() {
        return "";
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
