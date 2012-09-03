package ws.palladian.extraction.date.dates;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.date.DateExactness;
import ws.palladian.helper.date.ExtractedDate;

/**
 * <p>
 * A {@link RatedDate} decorates an {@link ExtractedDate} and adds rating capabilities. The wrapped
 * {@link ExtractedDate} with its specific type can be accessed using {@link #getDate()}.
 * </p>
 * 
 * @author Philipp Katz
 * 
 * @param <D> (Sub)type of the decorated {@link ExtractedDate}.
 */
public final class RatedDate<D extends ExtractedDate> implements ExtractedDate {

    /** The decorated date. */
    private final D date;

    /** The rate assigned to the decorated date. */
    private final double rate;

    /**
     * Private constructor, instances are created through the factory method {@link #create(ExtractedDate, double)},
     * this saves generic parameters.
     */
    private RatedDate(D date, double rate) {
        this.date = date;
        this.rate = rate;
    }

    /**
     * <p>
     * Create a new {@link RatedDate}.
     * </p>
     * 
     * @param date The {@link ExtractedDate} to decorate, not <code>null</code>.
     * @param rate The rate to assign to the ExtractedDate.
     */
    public static <T extends ExtractedDate> RatedDate<T> create(T date, double rate) {
        Validate.notNull(date, "date must not be null");
        return new RatedDate<T>(date, rate);
    }

    @Override
    public String getNormalizedDateString() {
        return date.getNormalizedDateString();
    }

    @Override
    public Date getNormalizedDate() {
        return date.getNormalizedDate();
    }

    @Override
    public long getLongDate() {
        return date.getLongDate();
    }

    @Override
    public String getNormalizedDateString(boolean time) {
        return date.getNormalizedDateString(time);
    }

    @Override
    public String getDateString() {
        return date.getDateString();
    }

    @Override
    public String getFormat() {
        return date.getFormat();
    }

    @Override
    public int get(int field) {
        return date.get(field);
    }

    @Override
    public String getTimeZone() {
        return date.getTimeZone();
    }

    @Override
    public void set(int field, int value) {
        date.set(field, value);
    }

    @Override
    public DateExactness getExactness() {
        return date.getExactness();
    }

    @Override
    public double getDifference(ExtractedDate date, TimeUnit unit) {
        return date.getDifference(date, unit);
    }

    @Override
    public Calendar getCalendar(DateExactness exactness) {
        return date.getCalendar(exactness);
    }

    /**
     * <p>
     * Get the wrapped date.
     * </p>
     * 
     * @return the date
     */
    public D getDate() {
        return date;
    }

    /**
     * <p>
     * Get the assigned rate value.
     * </p>
     * 
     * @return the rate
     */
    public double getRate() {
        return rate;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RatedDate [date=");
        builder.append(date);
        builder.append(", rate=");
        builder.append(rate);
        builder.append("]");
        return builder.toString();
    }

}
