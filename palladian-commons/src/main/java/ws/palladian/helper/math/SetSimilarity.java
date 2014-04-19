package ws.palladian.helper.math;

import java.util.Set;

/** A similarity measure between two sets. */
public interface SetSimilarity {
    <T> double calculate(Set<T> s1, Set<T> s2);
}