package ws.palladian.classification.evaluation.roc;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
//import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.classification.evaluation.AbstractClassificationEvaluator;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Classifier;
import ws.palladian.core.Instance;
import ws.palladian.core.Model;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.helper.collection.AbstractIterator2;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.math.ConfusionMatrix;

/**
 * Evaluation of a classifier via
 * <a href="https://en.wikipedia.org/wiki/Receiver_operating_characteristic">ROC
 * curve</a>. Use the {@link RocCurvesEvaluator} to run the evaluation. The ROC
 * curves can either be plotted to screen or file (using the
 * {@link RocCurvesPainter}), or the individual data points can be iterated
 * through {@link #iterator()}. To get a summary measure of the ROC curve, use
 * {@link #getAreaUnderCurve()}.
 * 
 * @author pk
 */
public class RocCurves implements Iterable<RocCurves.EvaluationPoint> {
	
	public static final class RocCurvesEvaluator extends AbstractClassificationEvaluator<RocCurves> {
		
		private final String trueCategory;
		
		/**
		 * @param trueCategory
		 *            The category name which is to be treated as the "correct"
		 *            class, e.g. "true".
		 */
		public RocCurvesEvaluator(String trueCategory) {
			this.trueCategory = trueCategory;
		}

		@Override
		public <M extends Model> RocCurves evaluate(Classifier<M> classifier, M model, Dataset data) {
			Validate.isTrue(model.getCategories().size() == 2, "binary model required");
			if (!model.getCategories().contains(trueCategory)) {
				throw new IllegalStateException("Model has no category \"" + trueCategory + "\".");
			}
			List<ResultEntry> results = new ArrayList<>();
			for (Instance instance : data) {
				CategoryEntries categoryEntries = classifier.classify(instance.getVector(), model);
				boolean correct = instance.getCategory().equals(trueCategory);
				double confidence = categoryEntries.getProbability(trueCategory);
				results.add(new ResultEntry(correct, confidence));
			}
			return new RocCurves(results);
		}

		@Override
		public String getCsvHeader(RocCurves result) {
			return "AUC";
		}

		@Override
		public String getCsvLine(RocCurves result) {
			return String.valueOf(result.getAreaUnderCurve());
		}
	}
	
	public static class RocCurvesBuilder implements Factory<RocCurves> {
		private final List<ResultEntry> results = new ArrayList<>();
		public void add(boolean correct, double confidence) {
			results.add(new ResultEntry(correct, confidence));
		}
		/**
		 * Add a given {@link RocCurves} instance. The is useful when e.g.
		 * performing cross-validation and results from individual folds should
		 * be combined.
		 * 
		 * @param rocCurves
		 *            The curves to add.
		 */
		public void add(RocCurves rocCurves) {
			Objects.requireNonNull(rocCurves, "rocCurves was null");
			results.addAll(rocCurves.results);
		}
		@Override
		public RocCurves create() {
			return new RocCurves(results);
		}
	}
	
	static final class ResultEntry implements Comparable<ResultEntry> {
		final boolean trueCategory;
		final double confidence;

		ResultEntry(boolean trueCategory, double confidence) {
			this.trueCategory = trueCategory;
			this.confidence = confidence;
		}

		@Override
		public int compareTo(ResultEntry o) {
			return Double.compare(o.confidence, confidence);
		}
	}

	public static final class EvaluationPoint {
		private final double sensitivity;
		private final double specificity;
		private final double threshold;
		EvaluationPoint(double sensitivity, double specificity, double threshold) {
			this.sensitivity = sensitivity;
			this.specificity = specificity;
			this.threshold = threshold;
		}
		public double getSensitivity() {
			return sensitivity;
		}
		public double getSpecificity() {
			return specificity;
		}
		public double getThreshold() {
			return threshold;
		}
		@Override
		public String toString() {
			return format(threshold) + ": sensitivity=" + format(sensitivity) + ", specificity=" + format(specificity);
		}
	}

	/** Sorted list with classified items by confidence. */
	private final List<ResultEntry> results;

	/** Total number of items with the specified class. */
	private final int positives;

	/** Total number of items with the complementary class. */
	private final int negatives;
	
	RocCurves(List<ResultEntry> results) {
		this.results = new ArrayList<>(results);
		Collections.sort(this.results);
		int positives = 0;
		int negatives = 0;
		for (ResultEntry result : this.results) {
			if (result.trueCategory) {
				positives++;
			} else {
				negatives++;
			}
		}
		this.positives = positives;
		this.negatives = negatives;
	}
	
//	/**
//	 * Read results from a CSV file; first column is a boolean, whether the
//	 * entry is of relevant class, second column is a double with the
//	 * classification confidence.
//	 * 
//	 * @param csvFile
//	 *            The CSV file, not <code>null</code>.
//	 * @return The parsed ROC curves.
//	 * @throws IOException
//	 *             In case reading the CSV file fails.
//	 */
//	static RocCurves parse(File csvFile) throws IOException {
//		Objects.requireNonNull(csvFile, "csvFile");
//		try (Stream<String> lines = Files.lines(csvFile.toPath())) {
//			List<ResultEntry> results = lines.map(line -> {
//				String[] split = line.split(";");
//				boolean correct = Boolean.parseBoolean(split[0]);
//				double confidence = Double.parseDouble(split[1]);
//				return new ResultEntry(correct, confidence);
//			}).collect(Collectors.toList());
//			return new RocCurves(results);
//		}
//	}
	
	/**
	 * Get the confusion matrix for the given threshold.
	 * 
	 * @param threshold
	 *            The threshold.
	 * @return The confusion matrix.
	 */
	public ConfusionMatrix getConfusionMatrix(double threshold) {
		ConfusionMatrix matrix = new ConfusionMatrix();
		for (ResultEntry result : results) {
			matrix.add(Boolean.toString(result.trueCategory), Boolean.toString(result.confidence >= threshold));
		}
		return matrix;
	}

	@Override
	public Iterator<EvaluationPoint> iterator() {
		return new AbstractIterator2<EvaluationPoint>() {
			Iterator<ResultEntry> iterator = results.iterator();
			int truePositives = 0;
			int trueNegatives = negatives;
			@Override
			protected EvaluationPoint getNext() {
				if (iterator.hasNext()) {
					ResultEntry current = iterator.next();
					if (current.trueCategory) {
						truePositives++;
					} else {
						trueNegatives--;
					}
					double sensitivity = (double) truePositives / positives;
					double specificity = (double) trueNegatives / negatives;
					return new EvaluationPoint(sensitivity, specificity, current.confidence);
				} else {
					return finished();
				}
			}
		};
	}

	/**
	 * @return The area under the ROC curve.
	 */
	public double getAreaUnderCurve() {
		double areaUnderCurve = 0;
		EvaluationPoint previous = null;
		for (EvaluationPoint current : this) {
			if (previous != null) {
				// calculate the area under curve using trapezoidal integration;
				// see https://en.wikipedia.org/wiki/Trapezoidal_rule
				areaUnderCurve += (-current.specificity + previous.specificity)
						* (current.sensitivity + previous.sensitivity);
			}
			previous = current;
		}
		return areaUnderCurve / 2;
	}
	
	public void showCurves() {
		new RocCurvesPainter().add(this, "ROC").showCurves();
	}

	public void saveCurves(File file) throws IOException {
		new RocCurvesPainter().add(this, "ROC").saveCurves(file);
	}
	
	/**
	 * Write the data points to a CSV file. First entry is a boolean indicating
	 * whether the item is of relevant class, second entry the classifier
	 * confidence. Result is ordered by confidence. directly.
	 * 
	 * @param stream
	 *            The destination stream, not <code>null</code>.
	 * @param separator
	 *            The separator between the two entries, e.g. <tt>;</tt>.
	 */
	public void writeEntries(PrintStream stream, char separator) {
		Objects.requireNonNull(stream, "stream must not be null");
		for (ResultEntry result : this.results) {
			List<String> line = new ArrayList<>();
			line.add(String.valueOf(result.trueCategory));
			line.add(String.valueOf(result.confidence));
			stream.println(StringUtils.join(line, separator));
		}
	}
	
	static final String format(double v) {
		return new DecimalFormat("#.####", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(v);
	}

}
