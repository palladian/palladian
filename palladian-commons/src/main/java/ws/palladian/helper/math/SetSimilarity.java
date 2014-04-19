package ws.palladian.helper.math;

import java.util.Set;

/**
 * A similarity measure between two sets.
 * 
 * @author pk
 */
public interface SetSimilarity {
    /**
     * Calculate the similarity between the two given sets.
     * 
     * @param s1 The first set, not <code>null</code>.
     * @param s2 The second set, not <code>null</code>.
     * @return A similarity measure in the range [0, 1].
     */
    <T> double calculate(Set<T> s1, Set<T> s2);
}
