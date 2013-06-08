package ws.palladian.helper.collection;

import java.util.Comparator;

/**
 * <p>
 * Sort strings by length, the longest strings go first.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class StringLengthComparator implements Comparator<String> {

    @Override
    public int compare(String o1, String o2) {
        return o2.length() - o1.length();
    }

}
