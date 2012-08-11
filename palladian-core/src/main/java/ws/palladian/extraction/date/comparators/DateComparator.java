package ws.palladian.extraction.date.comparators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.date.DateExactness;
import ws.palladian.helper.date.ExtractedDate;

/**
 * <p>
 * This class gives the ability to compare {@link ExtractedDate}s by age. Be careful when using it as a comparator in
 * sort functions. Dates can have different exactness, e.g. one has a time and the other no day. For more information
 * see at particular methods, therefore exactness should be determined based on the available dates and
 * {@link DateComparator} should be initialized with explicitly determined {@link DateExactness} values.
 * </p>
 * 
 * @author Martin Gregor
 * @author Philipp Katz
 */
public class DateComparator implements Comparator<ExtractedDate> {

    /**
     * The exactness with which this comparator operates, date fields which are outside of the defined exactness are
     * ignored.
     */
    private final DateExactness dateExactness;

    /**
     * <p>
     * Create a new {@link DateComparator} with the specified exactness.
     * </p>
     * 
     * @param dateExactness The exactness until which the comparison is carried out. Not {@link DateExactness#UNSET} or
     *            <code>null</code>.
     */
    public DateComparator(DateExactness dateExactness) {
        Validate.notNull(dateExactness, "dateExactness must not be null");
        if (dateExactness == DateExactness.UNSET) {
            throw new IllegalArgumentException("DateExactness must not be \"UNSET\"");
        }
        this.dateExactness = dateExactness;
    }

    /**
     * <p>
     * Create a new {@link DateComparator} with exactness set to {@link DateExactness#SECOND}.
     * </p>
     */
    public DateComparator() {
        this(DateExactness.SECOND);
    }

    /**
     * Compares two dates.<br>
     * Returns -1, 0 or 1 if date1 is newer, equals or older then date2.<br>
     * If both dates are not comparable, for e.g. date1.month is not set, the returning value will be -2.<br>
     * <br>
     * This does only matter, if the higher parameter are equal.<br>
     * For e.g.:<br>
     * date.year = 2007 and date2.year =2006; date1.month=11 and date2.month =-1.<br>
     * Then the returning value will be -1, because 2007>2006.<br>
     * If date1.year is 2006 as well, then the return value will be -2, because the years are equal and the month can
     * not be compared.
     * 
     */
    @Override
    public int compare(ExtractedDate date1, ExtractedDate date2) {
        int returnValue = compare(date1.get(ExtractedDate.YEAR), date2.get(ExtractedDate.YEAR));
        if (returnValue == 0 && DateExactness.MONTH.inRange(dateExactness)) {
            returnValue = compare(date1.get(ExtractedDate.MONTH), date2.get(ExtractedDate.MONTH));
            if (returnValue == 0 && DateExactness.DAY.inRange(dateExactness)) {
                returnValue = compare(date1.get(ExtractedDate.DAY), date2.get(ExtractedDate.DAY));
                if (returnValue == 0 && DateExactness.HOUR.inRange(dateExactness)) {
                    returnValue = compare(date1.get(ExtractedDate.HOUR), date2.get(ExtractedDate.HOUR));
                    if (returnValue == 0 && DateExactness.MINUTE.inRange(dateExactness)) {
                        returnValue = compare(date1.get(ExtractedDate.MINUTE), date2.get(ExtractedDate.MINUTE));
                        if (returnValue == 0 && DateExactness.SECOND.inRange(dateExactness)) {
                            returnValue = compare(date1.get(ExtractedDate.SECOND), date2.get(ExtractedDate.SECOND));
                        }
                    }
                }
            }

        }
        return returnValue;
    }

    /**
     * Compares a parameter of two dates. (date1.getYear() and date2.getYear()). <br>
     * If i or k equals -1, then -2 will be returned.<br>
     * Otherwise -1 for i > k, 0 for i=k, 1 for i&lt; k; <br>
     * If k=i=-1 -> 0 will be returned.
     * 
     * @param i
     * @param k
     * @return
     */
    private int compare(int i, int k) {
        // int returnValue = -2;
        // if (i == k) {
        // returnValue = 0;
        // } else {
        // if (i != -1 && k != -1) {
        // if (i < k) {
        // returnValue = 1;
        // } else {
        // returnValue = -1;
        // }
        // } else {
        // if (i == -1) {
        // returnValue = -2;
        // } else {
        // returnValue = -3;
        // }
        // }
        // }
        // return returnValue;

        // for Java 1.7 compatibility I had to change the above code, as it would violate the Comparator's contract.
        // Considering the available test cases, the following implementation also works, but I haven't tested it
        // extensively. If we have unset date particles (i.e. field equals -1) we return Integer.MAX_VALUE, or
        // Integer.MIN_VALUE instead of generally returning a value of -2. -- Philipp, 2011-12-15
        if (i == -1 && k != -1) {
            return Integer.MAX_VALUE;
        } else if (k == -1 && i != -1) {
            return Integer.MIN_VALUE;
        } else {
            return Integer.valueOf(k).compareTo(i);
        }
    }

    /**
     * <p>
     * Orders a {@link List} of dates, beginning with oldest date.
     * </p>
     * 
     * @param <T>
     * @param dates
     * @param reverse <code>true</code> is youngest first. <code>false</code> is oldest first.
     * @return A sorted {@link List} of dates.
     * @deprecated Use {@link Collections#sort(List)} with {@link DateComparator} instead.
     */
    @Deprecated
    public <T extends ExtractedDate> List<T> orderDates(Collection<T> dates, boolean reverse) {
        List<T> result = new ArrayList<T>(dates);
        Comparator<ExtractedDate> comparator = new DateComparator();
        if (!reverse) {
            comparator = Collections.reverseOrder(comparator);
        }
        Collections.sort(result, comparator);
        return result;
    }

    /**
     * <p>
     * Returns the oldest date.
     * </p>
     * 
     * @param <T>
     * @param dates {@link Collection} of {@link ExtractedDate}s.
     * @return The oldest date from the Collection.
     */
    public <T extends ExtractedDate> T getOldestDate(Collection<T> dates) {
        List<T> orderDates = orderDates(dates, false);
        T date = null;
        if (orderDates.size() > 0) {
            date = orderDates.get(0);
        }
        return date;
    }

    /**
     * <p>
     * Returns the youngest date.
     * </p>
     * 
     * @param <T>
     * @param dates {@link Collection} of {@link ExtractedDate}s.
     * @return The youngest date from the Collection.
     */
    public <T extends ExtractedDate> T getYoungestDate(Collection<T> dates) {
        List<T> orderDates = orderDates(dates, true);
        T date = null;
        if (orderDates.size() > 0) {
            date = orderDates.get(0);
        }
        return date;
    }
}
