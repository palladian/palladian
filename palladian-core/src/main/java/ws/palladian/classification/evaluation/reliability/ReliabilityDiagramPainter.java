package ws.palladian.classification.evaluation.reliability;

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
import ws.palladian.classification.evaluation.reliability.ReliabilityDiagramEvaluator.DataPoint;
import ws.palladian.classification.evaluation.reliability.ReliabilityDiagramEvaluator.ReliabilityDiagram;

import java.awt.*;

public class ReliabilityDiagramPainter extends AbstractGraphPainter<ReliabilityDiagram> {

    @Override
    protected JFreeChart createChart() {

        XYSeriesCollection dataset = new XYSeriesCollection();

        XYSeries random = new XYSeries("Optimal");
        random.add(0, 0);
        random.add(1, 1);
        dataset.addSeries(random);

        for (NamedDiagram<ReliabilityDiagram> namedDiagram : diagrams) {
            XYSeries series = new XYSeries(namedDiagram.name + " [LogLoss = " + format(namedDiagram.diagram.logLoss) + "]");
            for (DataPoint dataPoint : namedDiagram.diagram) {
                series.add(dataPoint.mean, dataPoint.positiveFraction());
            }
            dataset.addSeries(series);
        }

        JFreeChart chart = ChartFactory.createXYLineChart( //
                "Reliability Diagram", // title
                "Probability bins", // x axis
                "Fraction of positives", // y axis
                dataset, // data
                PlotOrientation.VERTICAL, //
                true, // legend
                true, // tooltips
                false); // urls
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
            renderer.setSeriesShape(i + 1, new Rectangle(-2, -2, 4, 4));
            renderer.setSeriesShapesVisible(i + 1, true);
            renderer.setSeriesShapesFilled(i + 1, true);
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
