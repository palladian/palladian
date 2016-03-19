package ws.palladian.utils;

import org.apache.commons.lang3.Validate;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Classifier;
import ws.palladian.core.Instance;
import ws.palladian.core.Model;
import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.math.AbstractClassificationEvaluator;
import ws.palladian.helper.math.ConfusionMatrix;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.List;

/**
 * Evaluation of a classifier via
 * <a href="https://en.wikipedia.org/wiki/Receiver_operating_characteristic">ROC
 * curve</a>. Use the {@link RocCurvesEvaluator} to run the evaluation. The ROC
 * curves can either be plotted to screen or file, or the individual data points
 * can be iterated through {@link #iterator()}. To get a summary measure of the
 * ROC curve, use {@link #getAreaUnderCurve()}.
 * 
 * @author pk
 */
public class RocCurves implements Iterable<RocCurves.EvaluationPoint> {
	
	public static final class RocCurvesEvaluator extends AbstractClassificationEvaluator<RocCurves> {
		
		private final String correctCategory;
		
		/**
		 * @param correctCategory
		 *            The category name which is to be treated as the "correct"
		 *            class, e.g. "true".
		 */
		public RocCurvesEvaluator(String correctCategory) {
			this.correctCategory = correctCategory;
		}

		@Override
		public <M extends Model> RocCurves evaluate(Classifier<M> classifier, M model, Iterable<? extends Instance> data) {
			Validate.isTrue(model.getCategories().size() == 2, "binary model required");
			if (!model.getCategories().contains(correctCategory)) {
				throw new IllegalStateException("Model has not category \"" + correctCategory + "\".");
			}
			List<ResultEntry> results = new ArrayList<>();
			for (Instance instance : data) {
				CategoryEntries categoryEntries = classifier.classify(instance.getVector(), model);
				boolean correct = instance.getCategory().equals(correctCategory);
				double confidence = categoryEntries.getProbability(correctCategory);
				results.add(new ResultEntry(correct, confidence));
			}
			return new RocCurves(results);
		}
	}
	
	static final class ResultEntry implements Comparable<ResultEntry> {
		final boolean correct;
		final double confidence;

		ResultEntry(boolean correct, double confidence) {
			this.correct = correct;
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
			if (result.correct) {
				positives++;
			} else {
				negatives++;
			}
		}
		this.positives = positives;
		this.negatives = negatives;
	}
	
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
			matrix.add(Boolean.toString(result.correct), Boolean.toString(result.confidence >= threshold));
		}
		return matrix;
	}

	@Override
	public Iterator<EvaluationPoint> iterator() {
		return new AbstractIterator<EvaluationPoint>() {
			Iterator<ResultEntry> iterator = results.iterator();
			int truePositives = 0;
			int trueNegatives = negatives;
			@Override
			protected EvaluationPoint getNext() throws Finished {
				if (iterator.hasNext()) {
					ResultEntry current = iterator.next();
					if (current.correct) {
						truePositives++;
					} else {
						trueNegatives--;
					}
					double sensitivity = (double) truePositives / positives;
					double specificity = (double) trueNegatives / negatives;
					return new EvaluationPoint(sensitivity, specificity, current.confidence);
				} else {
					throw FINISHED;
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
	
	private JFreeChart createChart() {
		XYSeriesCollection dataset = new XYSeriesCollection();
		
		XYSeries random = new XYSeries("Random");
		random.add(0, 0);
		random.add(1, 1);
		dataset.addSeries(random);
		
		XYSeries series = new XYSeries("ROC [AUC = " + format(getAreaUnderCurve()) + "]");
		for (EvaluationPoint current : this) {
			series.add(1 - current.specificity, current.sensitivity);
		}
		dataset.addSeries(series);
		
        JFreeChart chart = ChartFactory.createXYLineChart(
            "ROC Curves",
            "False Positive Rate (1 – Specificity)",
            "True Positive Rate (Sensitivity)",
            dataset,
            PlotOrientation.VERTICAL,
            true, 
            true, 
            false 
        );
        chart.setBackgroundPaint(Color.white);
        
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesLinesVisible(0, true);
        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesLinesVisible(1, true);
        renderer.setSeriesShapesVisible(1, false);
        renderer.setSeriesPaint(0, Color.darkGray);
        renderer.setSeriesPaint(1, Color.red);
        plot.setRenderer(renderer);
        
		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setRange(0, 1);
		rangeAxis.setTickUnit(new NumberTickUnit(0.1));
		NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
		domainAxis.setRange(0, 1);
		domainAxis.setTickUnit(new NumberTickUnit(0.1));
		return chart;
	}
	
	public void showCurves() {
		ChartPanel chartPanel = new ChartPanel(createChart());
        chartPanel.setPreferredSize(new Dimension(800, 600));
        ApplicationFrame frame = new ApplicationFrame("ROC");
        frame.setContentPane(chartPanel);
        frame.pack();
        RefineryUtilities.centerFrameOnScreen(frame);
        frame.setVisible(true);
	}
	
	public void saveCurves(File file) throws IOException {
        ChartUtilities.saveChartAsPNG(file, createChart(), 800, 600);
	}
	
	private static final String format(double v) {
		return new DecimalFormat("#.####", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(v);
	}

}
