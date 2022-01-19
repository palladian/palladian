package ws.palladian.classification.evaluation;

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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public abstract class AbstractGraphPainter<D extends Graph> {
    /**
     * The colors to use for painting the ROC curves.
     */
    protected static final Color[] COLORS = new Color[]{red, pink, orange, yellow, green, magenta, cyan, blue};

    protected static final class NamedDiagram<D extends Graph> {
        public final D diagram;
        public final String name;

        NamedDiagram(D diagram, String name) {
            this.diagram = diagram;
            this.name = name;
        }
    }

    protected final List<NamedDiagram<D>> diagrams = new ArrayList<>();

    public AbstractGraphPainter<D> add(D diagram, String name) {
        Objects.requireNonNull(diagram, "diagram must not be null");
        Objects.requireNonNull(name, "name must not be null");
        this.diagrams.add(new NamedDiagram<>(diagram, name));
        return this;
    }

    public void showCurves() {
        ChartPanel chartPanel = new ChartPanel(createChart());
        chartPanel.setPreferredSize(new Dimension(800, 600));
        ApplicationFrame frame = new ApplicationFrame("");
        frame.setContentPane(chartPanel);
        frame.pack();
        RefineryUtilities.centerFrameOnScreen(frame);
        frame.setVisible(true);
    }

    public void saveCurves(File file) throws IOException {
        ChartUtilities.saveChartAsPNG(file, createChart(), 800, 600);
    }

    protected abstract JFreeChart createChart();

    public static final String format(double v) {
        return new DecimalFormat("#.####", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(v);
    }
}
