package ws.palladian.kaggle.santander.utils;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

public class EvaluationUtils {

	// Code taken from: https://github.com/benhamner/Metrics/blob/master/Python/ml_metrics/average_precision.py
	
	public static <T> double getAveragePrecision(Set<T> actual, List<T> predicted, int k) {
		Objects.requireNonNull(actual, "actual was null");
		Objects.requireNonNull(predicted, "predicted was null");
		if (k <= 0) {
			throw new IllegalStateException("k must be greater one");
		}
		if (actual.isEmpty()) {
			return 0;
		}
		if (predicted.size() > k) {
			predicted = predicted.subList(0, k);
		}
		double score = 0.0;
		int numHits = 0;
		for (int i = 0; i < predicted.size(); i++) {
			T p = predicted.get(i);
			if (actual.contains(p) && !predicted.subList(0, i).contains(p)) {
				numHits++;
				score += numHits / (i + 1.0);
			}
		}
		return score / Math.min(actual.size(), k);
	}

	public static <T> double getMeanAveragePrecision(Iterable<? extends Pair<? extends Set<T>, ? extends List<T>>> data,
			int k) {
		double meanAveragePrecision = 0;
		int n = 0;
		for (Pair<? extends Set<T>, ? extends List<T>> pair : data) {
			n++;
			meanAveragePrecision += getAveragePrecision(pair.getKey(), pair.getValue(), k);
		}
		return meanAveragePrecision / n;
	}

}
