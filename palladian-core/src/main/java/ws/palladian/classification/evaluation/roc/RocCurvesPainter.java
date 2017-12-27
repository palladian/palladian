package ws.palladian.classification.evaluation.roc;

import java.awt.Color;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import ws.palladian.classification.evaluation.AbstractGraphPainter;
import ws.palladian.classification.evaluation.roc.RocCurves.EvaluationPoint;

/**
 * Draws one or more ROC curves (hence, the class name should be RocCurvesDrawer
 * and not *Painter, but that sounds weird).
 * 
 * @author pk
 */
public class RocCurvesPainter extends AbstractGraphPainter<RocCurves> {
	
//	public RocCurvesPainter add(File csvFile, String name) throws IOException {
//		Objects.requireNonNull(csvFile, "csvFile must not be null");
//		Objects.requireNonNull(name, "name must not be null");
//		RocCurves curve = RocCurves.parse(csvFile);
//		this.curves.add(new NamedRocCurve(curve, name));
//		return this;
//	}
	
	@Override
	protected JFreeChart createChart() {
		XYSeriesCollection dataset = new XYSeriesCollection();
		
		XYSeries random = new XYSeries("Random");
		random.add(0, 0);
		random.add(1, 1);
		dataset.addSeries(random);
		
		for (NamedDiagram<RocCurves> namedCurve : diagrams) {
			XYSeries series = new XYSeries(
					namedCurve.name + " [AUC = " + format(namedCurve.diagram.getAreaUnderCurve()) + "]");
			for (EvaluationPoint current : namedCurve.diagram) {
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
		for (int i = 0; i < diagrams.size(); i++) {
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

}
