package ws.palladian.helper.collection;

import java.util.Comparator;

/**
 * <p>
 * Sort strings by length, the longest strings go first. If they are the same length, we order alphabetically.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class StringLengthComparator implements Comparator<String> {

    public static final StringLengthComparator INSTANCE = new StringLengthComparator();

    /** @deprecated Use the singleton {@link #INSTANCE} instead. */
    @Deprecated
    public StringLengthComparator() {
        // no op.
    }

    @Override
    public int compare(String s1, String s2) {
        int n = s2.length() - s1.length();
        return n != 0 ? n : s1.compareToIgnoreCase(s2);
    }

}
