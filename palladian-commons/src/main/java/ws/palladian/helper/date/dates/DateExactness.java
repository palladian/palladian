package ws.palladian.helper.date.dates;

import java.util.NoSuchElementException;

import org.apache.commons.lang3.Validate;


/**
 * <p>
 * Constants describing the <i>exactness</i> of the {@link ExtractedDate}s. The exactness denotes which date parts of an
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
    
    /**
     * <p>
     * Determine the common exactness of two {@link DateExactness}es. This is the smallest common denominator which can
     * be used for comparing two dates with potentially different exactness values.
     * </p>
     * 
     * @param exactness1 The first exactness, not <code>null</code>.
     * @param exactness2 The second exactness, not <code>null</code>.
     * @return The common exactness for the two specified exactness values.
     */
    public static DateExactness getCommonExactness(DateExactness exactness1, DateExactness exactness2) {
        Validate.notNull(exactness1, "exactness1 must not be null");
        Validate.notNull(exactness2, "exactness2 must not be null");
        return byValue(Math.min(exactness1.value, exactness2.value));
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
     * @deprecated Use {@link #getCommonExactness(DateExactness, DateExactness)} instead.
     */
    @Deprecated
    public static DateExactness getCommonExactness(ExtractedDate date1, ExtractedDate date2) {
//        DateExactness value = DYNAMIC;
//        if (!(date1.get(ExtractedDate.YEAR) == -1 ^ date2.get(ExtractedDate.YEAR) == -1)) {
//            value = YEAR;
//            if (!(date1.get(ExtractedDate.MONTH) == -1 ^ date2.get(ExtractedDate.MONTH) == -1)) {
//                value = MONTH;
//                if (!(date1.get(ExtractedDate.DAY) == -1 ^ date2.get(ExtractedDate.DAY) == -1)) {
//                    value = DAY;
//                    if (!(date1.get(ExtractedDate.HOUR) == -1 ^ date2.get(ExtractedDate.HOUR) == -1)) {
//                        value = HOUR;
//                        if (!(date1.get(ExtractedDate.MINUTE) == -1 ^ date2.get(ExtractedDate.MINUTE) == -1)) {
//                            value = MINUTE;
//                            if (!(date1.get(ExtractedDate.SECOND) == -1 ^ date2.get(ExtractedDate.SECOND) == -1)) {
//                                value = SECOND;
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        return value;
        return getCommonExactness(date1.getExactness(), date2.getExactness());
    }
    
    /**
     * <p>
     * Determine whether the supplied {@link DateExactness} is in range of this {@link DateExactness}. E.g.
     * {@link DateExactness#YEAR} is in range of {@link DateExactness#MINUTE}, because it is assumed that the finer data
     * implies the coarser data.
     * </p>
     * 
     * @param exactness The exactness for which to check, whether it is in this exactness, not <code>null</code>.
     * @return <code>true</code> if this exactness in range of the supplied exactness, <code>false</code> otherwise.
     */
    public boolean inRange(DateExactness exactness) {
        Validate.notNull(exactness, "exactness must not be null");
        return value <= exactness.value;
    }
}