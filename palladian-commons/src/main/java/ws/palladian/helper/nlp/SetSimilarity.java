package ws.palladian.helper.nlp;

import java.util.Collection;

/** A similarity measure between two sets. */
public interface SetSimilarity {
    <T> double calculate(Collection<T> c1, Collection<T> c2);
}