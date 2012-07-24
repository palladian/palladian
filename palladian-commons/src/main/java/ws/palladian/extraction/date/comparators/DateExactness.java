package ws.palladian.extraction.date.comparators;

import java.util.NoSuchElementException;

import ws.palladian.helper.date.dates.ExtractedDate;

/**
 * <p>
 * Constants describing the exactness of the {@link ExtractedDate}s. The exactness denotes which date parts of an
 * {@link ExtractedDate} have been set. For example, a date "2012-07-24" has an exactness {@link #DAY}, where
 * "2012-07-24 11:54:23" as an exactness {@link #SECOND}.
 * </p>
 * 
 * @author Philipp Katz
 */
public enum DateExactness {
    /** No exactness. */
    UNSET(0),
    /** Compare will stop after year. */
    YEAR(1), 
    /** Compare will stop after month. */
    MONTH(2), 
    /** Compare will stop after day. */
    DAY(3), 
    /** Compare will stop after hour. */
    HOUR(4), 
    /** Compare will stop after minute. */
    MINUTE(5), 
    /** Compare will not stop (after second there are no more comparable values). */
    SECOND(6), 
    /** Use for methods providing a dynamic stop, depending on exactness of each date. */
    DYNAMIC(-1);
    
    private final int value;

    DateExactness(int value) {
        this.value = value;
    }
    
    /**
     * @deprecated Reference by explicit type if possible.
     * @param value
     * @return
     */
    @Deprecated
    public static DateExactness byValue(int value) {
        for (DateExactness compareDepth : values()) {
            if (compareDepth.value == value) {
                return compareDepth;
            }
        }
        throw new NoSuchElementException("No CompareDepth with value " + value);
    }
    
    // TODO rename to "getCommonExactness" or "getMutualExactness" or so.
    // TODO isn't this duplicate of getCommonExactness?
    public static DateExactness min(DateExactness depth1, DateExactness depth2) {
        return byValue(Math.min(depth1.value, depth2.value));
    }
    
    /**
     * <p>Get an int value denoting the exactness.</p>
     * @deprecated Left for legacy reasons. DateExactness should be referenced by explicit type.
     * @return
     */
    @Deprecated
    public int getValue() {
        return value;
    }

    /**
     * Finds out, until which depth two dates are comparable. <br>
     * Order is year, month, day,hour, minute and second.
     * 
     * @param date1
     * @param date2
     * @return Integer with the value of stop_property. Look for it in static properties.
     */
    public static DateExactness getCommonExactness(ExtractedDate date1, ExtractedDate date2) {
        DateExactness value = DYNAMIC;
        if (!(date1.get(ExtractedDate.YEAR) == -1 ^ date2.get(ExtractedDate.YEAR) == -1)) {
            value = YEAR;
            if (!(date1.get(ExtractedDate.MONTH) == -1 ^ date2.get(ExtractedDate.MONTH) == -1)) {
                value = MONTH;
                if (!(date1.get(ExtractedDate.DAY) == -1 ^ date2.get(ExtractedDate.DAY) == -1)) {
                    value = DAY;
                    if (!(date1.get(ExtractedDate.HOUR) == -1 ^ date2.get(ExtractedDate.HOUR) == -1)) {
                        value = HOUR;
                        if (!(date1.get(ExtractedDate.MINUTE) == -1 ^ date2.get(ExtractedDate.MINUTE) == -1)) {
                            value = MINUTE;
                            if (!(date1.get(ExtractedDate.SECOND) == -1 ^ date2.get(ExtractedDate.SECOND) == -1)) {
                                value = SECOND;
                            }
                        }
                    }
                }
            }
        }
        return value;
    }
}