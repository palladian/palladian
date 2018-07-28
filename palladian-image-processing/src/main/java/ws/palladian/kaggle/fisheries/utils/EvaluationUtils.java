package ws.palladian.kaggle.fisheries.utils;

import static java.lang.Math.max;
import static java.lang.Math.min;

import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.core.CategoryEntries;

public class EvaluationUtils {

	private static final double MARGIN = Math.pow(10, -15);

	public static double logLoss(Iterable<? extends Pair<String, CategoryEntries>> data) {
		double logloss = 0;
		int count = 0;
		for (Pair<String, CategoryEntries> item : data) {
			count++;
			logloss += Math.log(max(min(item.getRight().getProbability(item.getLeft()), 1 - MARGIN), MARGIN));
		}
		return -logloss * (1. / count);
	}

}
