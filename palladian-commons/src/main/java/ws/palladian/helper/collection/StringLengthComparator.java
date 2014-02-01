package ws.palladian.helper.collection;

import java.util.Comparator;

/**
 * <p>
 * Sort strings by length, the longest strings go first. If they are the same length, we order alphabetically.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class StringLengthComparator implements Comparator<String> {

    @Override
    public int compare(String o1, String o2) {
        int n = o2.length() - o1.length();
        if (n == 0) {
            return o1.compareToIgnoreCase(o2);
        }
        return n;
    }

}
