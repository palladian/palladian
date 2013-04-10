package ws.palladian.helper.date;

import java.util.NoSuchElementException;

import org.apache.commons.lang3.Validate;

/**
 * <p>
 * Constants describing the <i>exactness</i> of the {@link ExtractedDate}s. The exactness denotes which date parts of an
 * {@link ExtractedDate} have been set. For example, a date <code>2012-07-24</code> has an exactness {@link #DAY}, where
 * as a date <code>2012-07-24 11:54:23</code> as an exactness {@link #SECOND}. The exactness values build on each other;
 * this means, a DateExactness with a value of {@link #DAY} implicitly provides all "less exact" values (i.e.
 * {@link #MONTH} and {@link #YEAR} in the given example).
 * </p>
 * 
 * @author Philipp Katz
 */
public enum DateExactness {

    /** No exactness. */
    UNSET(0),
    /** Exactness until and including year. */
    YEAR(1),
    /** Exactness until and including month. */
    MONTH(2),
    /** Exactness until and including day. */
    DAY(3),
    /** Exactness until and including hour. */
    HOUR(4),
    /** Exactness until and including minute. */
    MINUTE(5),
    /** Exactness until and including second (i.e. all values). */
    SECOND(6);
    /** Dynamic value, depending on exactness of each date. */
    // DYNAMIC(-1);

    private final int value;

    private DateExactness(int value) {
        this.value = value;
    }

    /**
     * <p>
     * Get a {@link DateExactness} by its int value.
     * </p>
     * 
     * @param value The int value denoting the {@link DateExactness}.
     * @return The {@link DateExactness} for the specified int value.
     */
    public static DateExactness byValue(int value) {
        for (DateExactness compareDepth : values()) {
            if (compareDepth.value == value) {
                return compareDepth;
            }
        }
        throw new NoSuchElementException("No DateExactness with value " + value);
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
     * <p>
     * Get an int value denoting the exactness.
     * </p>
     * 
     * @deprecated Left for legacy reasons. DateExactness should be referenced by explicit type.
     * @return
     */
    @Deprecated
    public int getValue() {
        return value;
    }

    /**
     * <p>
     * Determine whether this {@link DateExactness} is more or equally exact than the supplied {@link DateExactness}.
     * E.g. a {@link DateExactness#YEAR} is less exact than {@link DateExactness#MINUTE} (see documentation at the head
     * for an explanation).
     * </p>
     * 
     * @param exactness The exactness for which to check, whether it is in this exactness, not <code>null</code>.
     * @return <code>true</code> if this exactness in range of the supplied exactness, <code>false</code> otherwise.
     */
    public boolean provides(DateExactness exactness) {
        Validate.notNull(exactness, "exactness must not be null");
        return value >= exactness.value;
    }

}