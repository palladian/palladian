package ws.palladian.utils;

import ws.palladian.helper.collection.CollectionHelper;

import java.util.Set;

public class MultilabelEvaluator {

	public static final class Result {
		private final double precision;
		private final double recall;

		protected Result(double precision, double recall) {
			this.precision = precision;
			this.recall = recall;
		}

		public double getPrecision() {
			return precision;
		}

		public double getRecall() {
			return recall;
		}

		public double getF1() {
			return 2 * getPrecision() * getRecall() / (getPrecision() + getRecall());
		}

		@Override
		public String toString() {
			return "Precision=" + precision + ", Recall=" + recall + ", F1=" + getF1();
		}
		
		
	}

	private double cumulatedPrecision;

	private double cumulatedRecall;

	private int count;

	// http://www.godbole.net/shantanu/pubs/multilabelsvm-pakdd04.pdf ; sec. 5.2
	public <T> Result add(Set<T> trueLabels, Set<T> assignedLabels) {
		int intersectionSize = CollectionHelper.intersect(trueLabels, assignedLabels).size();
		int trueSize = trueLabels.size();
		int assignedSize = assignedLabels.size();
		double precision = assignedSize != 0 ? (double) intersectionSize / assignedSize : 1;
		double recall = trueSize != 0 ? (double) intersectionSize / trueSize : 1;
		cumulatedPrecision += precision;
		cumulatedRecall += recall;
		count++;
		return new Result(precision, recall);
	}

	public Result getResult() {
		if (count == 0) {
			throw new IllegalStateException("no values were added");
		}
		// http://stackoverflow.com/questions/20556990/is-it-correct-to-average-precision-recall-for-global-multilabel-performance-eval
		return new Result(cumulatedPrecision / count, cumulatedRecall / count);
	}

	public int getCount() {
		return count;
	}

}
