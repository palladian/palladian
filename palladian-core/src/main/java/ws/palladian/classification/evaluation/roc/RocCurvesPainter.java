package ws.palladian.classification.evaluation.roc;

import static java.awt.Color.blue;
import static java.awt.Color.cyan;
import static java.awt.Color.green;
import static java.awt.Color.magenta;
import static java.awt.Color.orange;
import static java.awt.Color.pink;
import static java.awt.Color.red;
import static java.awt.Color.yellow;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

import ws.palladian.classification.evaluation.roc.RocCurves.EvaluationPoint;

/**
 * Draws one or more ROC curves (hence, the class name should be RocCurvesDrawer
 * and not *Painter, but that sounds weird).
 * 
 * @author pk
 */
public class RocCurvesPainter {

	/** The colors to use for painting the ROC curves. */
	private static final Color[] COLORS = new Color[] { red, pink, orange, yellow, green, magenta, cyan, blue };

	private static final class NamedRocCurve {
		private final RocCurves curve;
		private final String name;
		NamedRocCurve(RocCurves curve, String name) {
			this.curve = curve;
			this.name = name;
		}
	}
	private final List<NamedRocCurve> curves = new ArrayList<>();
	
	public RocCurvesPainter add(RocCurves curves, String name) {
		Objects.requireNonNull(curves, "curves must not be null");
		Objects.requireNonNull(name, "name must not be null");
		this.curves.add(new NamedRocCurve(curves, name));
		return this;
	}
	
//	public RocCurvesPainter add(File csvFile, String name) throws IOException {
//		Objects.requireNonNull(csvFile, "csvFile must not be null");
//		Objects.requireNonNull(name, "name must not be null");
//		RocCurves curve = RocCurves.parse(csvFile);
//		this.curves.add(new NamedRocCurve(curve, name));
//		return this;
//	}
	
	private JFreeChart createChart() {
		XYSeriesCollection dataset = new XYSeriesCollection();
		
		XYSeries random = new XYSeries("Random");
		random.add(0, 0);
		random.add(1, 1);
		dataset.addSeries(random);
		
		for (NamedRocCurve namedCurve : curves) {
			XYSeries series = new XYSeries(
					namedCurve.name + " [AUC = " + RocCurves.format(namedCurve.curve.getAreaUnderCurve()) + "]");
			for (EvaluationPoint current : namedCurve.curve) {
				series.add(1 - current.getSpecificity(), current.getSensitivity());
			}
			dataset.addSeries(series);
		}
		
        JFreeChart chart = ChartFactory.createXYLineChart(
            "ROC Curves",
            "False Positive Rate (1 – Specificity)",
            "True Positive Rate (Sensitivity)",
            dataset,
            PlotOrientation.VERTICAL,
            /** legend: */ true, 
            /** tooltips: */ true, 
            /** urls: */ false 
        );
        chart.setBackgroundPaint(Color.white);
        
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesLinesVisible(0, true);
        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesPaint(0, Color.darkGray);
		for (int i = 0; i < curves.size(); i++) {
			renderer.setSeriesLinesVisible(i + 1, true);
			renderer.setSeriesShapesVisible(i + 1, false);
			renderer.setSeriesPaint(i + 1, COLORS[i % COLORS.length]);
		}
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

}
