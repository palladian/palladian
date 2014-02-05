package ws.palladian.helper.math;

import java.util.Map;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;

abstract class AbstractNumericVector<K> implements NumericVector<K> {

    @SuppressWarnings("unchecked")
    @Override
    public NumericVector<K> add(NumericVector<K> other) {
        Validate.notNull(other, "other must not be null");
        Map<K, Double> addedVector = CollectionHelper.newHashMap();
        for (K key : CollectionHelper.distinct(keys(), other.keys())) {
            double thisValue = get(key);
            double otherValue = other.get(key);
            addedVector.put(key, thisValue + otherValue);
        }
        return new ImmutableNumericVector<K>(addedVector);
    }

    @Override
    public double norm() {
        double norm = 0;
        for (VectorEntry<K, Double> entry : this) {
            double value = entry.value();
            norm += value * value;
        }
        return Math.sqrt(norm);
    }

    @Override
    public double dot(NumericVector<K> other) {
        Validate.notNull(other, "other must not be null");
        double dotProduct = 0;
//        for (VectorEntry<K, Double> entry : this) {
//            Double otherValue = other.get(entry.key());
//            if (otherValue != null) {
//                dotProduct += entry.value() * otherValue;
//            }
//        }
        for (K key : CollectionHelper.intersect(keys(), other.keys())) {
            double thisValue = get(key);
            double otherValue = other.get(key);
            dotProduct += thisValue * otherValue;
        }
        return dotProduct;
    }

    @Override
    public double sum() {
        double sum = 0;
        for (VectorEntry<K, Double> entry : this) {
            sum += entry.value();
        }
        return sum;
    }

    @Override
    public double cosine(NumericVector<K> other) {
        Validate.notNull(other, "other must not be null");
        double dotProduct = dot(other);
        return dotProduct != 0 ? dotProduct / (norm() * other.norm()) : 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public double euclidean(NumericVector<K> other) {
        Validate.notNull(other, "other must not be null");
        double distance = 0;
        for (K key : CollectionHelper.distinct(keys(), other.keys())) {
            double value = get(key) - other.get(key);
            distance += value * value;
        }
        return Math.sqrt(distance);
    }
    
    @Override
    public int size() {
        return keys().size();
    }

}
