package ws.palladian.kaggle.santander.utils;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static ws.palladian.kaggle.santander.utils.EvaluationUtils.getAveragePrecision;
import static ws.palladian.kaggle.santander.utils.EvaluationUtils.getMeanAveragePrecision;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class EvaluationUtilsTest {
	
	// Code taken from: https://github.com/benhamner/Metrics/blob/master/Python/ml_metrics/test/test_average_precision.py

	private static final double DELTA = 0.001;

	@Test
	public void testAveragePrecision() {
		assertEquals(0.25, getAveragePrecision(set(range(1, 6)), asList(6, 4, 7, 1, 2), 2), DELTA);
		assertEquals(0.2, getAveragePrecision(set(range(1, 6)), asList(1, 1, 1, 1, 1), 5), DELTA);
		List<Integer> predicted = new ArrayList<>(range(1, 21));
		predicted.addAll(range(200, 600));
		assertEquals(1.0, getAveragePrecision(set(range(1, 100)), predicted, 20), DELTA);
	}

	@Test
	public void testMeanAveragePrecision() {
		Iterable<Pair<Set<Integer>, List<Integer>>> data = singleton(pair(set(range(1, 5)), range(1, 5)));
		assertEquals(1, getMeanAveragePrecision(data, 3), DELTA);

		data = asList( //
				pair(set(asList(1, 3, 4)), range(1, 6)), //
				pair(set(asList(1, 2, 4)), range(1, 6)), //
				pair(set(asList(1, 3)), range(1, 6)));
		assertEquals(0.685185185185185, getMeanAveragePrecision(data, 3), DELTA);

		data = asList( //
				pair(set(range(1, 6)), asList(6, 4, 7, 1, 2)), //
				pair(set(range(1, 6)), asList(1, 1, 1, 1, 1)));
		assertEquals(0.26, getMeanAveragePrecision(data, 5), DELTA);

		data = asList( //
				pair(set(asList(1, 3)), range(1, 6)), //
				pair(set(asList(1, 2, 3)), asList(1, 1, 1)), //
				pair(set(asList(1, 2, 3)), asList(1, 2, 1)));
		assertEquals(11.0 / 18, getMeanAveragePrecision(data, 3), DELTA);

	}

	private static final List<Integer> range(int start, int stop) {
		List<Integer> list = new ArrayList<>();
		for (int i = start; i < stop; i++) {
			list.add(i);
		}
		return list;
	}

	private static final <T> Set<T> set(Collection<? extends T> items) {
		return new HashSet<T>(items);
	}

	private static final <A, B> Pair<A, B> pair(A a, B b) {
		return Pair.of(a, b);
	}

}
