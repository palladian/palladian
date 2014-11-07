package ws.palladian.helper.math;

import java.util.Set;

import ws.palladian.helper.collection.Vector;

/**
 * <p>
 * A numeric vector with arbitrary keys and algebraic operations, like dot product, norm etc.
 * </p>
 * 
 * @author pk
 * 
 * @param <K>
 */
public interface NumericVector<K> extends Vector<K, Double> {

    /**
     * <p>
     * <b>Important:</b> Return zero in case the value does not exist; never <code>null</code>.
     * </p>
     */
    @Override
    Double get(K k);

    /**
     * <p>
     * Add another vector and return the result as a new vector.
     * </p>
     * 
     * @param other The vector to add, not <code>null</code>.
     * @return A new vector with added values.
     */
    NumericVector<K> add(NumericVector<K> other);

    /**
     * @return The norm of this vector.
     */
    double norm();

    /**
     * <p>
     * Calculate the dot product between this and another vector.
     * </p>
     * 
     * @param other The other vector, not <code>null</code>.
     * @return The dot product between this and the given vector.
     */
    double dot(NumericVector<K> other);

    /**
     * @return The sum of all elements in this vector.
     */
    double sum();

    /**
     * <p>
     * Calculate the cosine similarity between this and another vector.
     * </p>
     * 
     * @param other The other vector, not <code>null</code>.
     * @return The cosine similarity between this and the given vector.
     */
    double cosine(NumericVector<K> other);

    /**
     * <p>
     * Calculate the euclidean distance between this and another vector.
     * </p>
     * 
     * @param other The other vector, not <code>null</code>.
     * @return The euclidean distance between this and the given vector.
     */
    double euclidean(NumericVector<K> other);

    /**
     * @return The keys in this vector.
     */
    Set<K> keys();

    /**
     * @return The number of elements in this vector.
     */
    int size();

}
