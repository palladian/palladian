package tud.iir.reporting;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import tud.iir.control.Controller;
import tud.iir.helper.FileHelper;

/**
 * The ChartCreator creates charts.
 * 
 * @author David Urbansky
 */
public class ChartCreator {

    public static final int XY_LINE_CHART = 1;
    public static final int XY_SCATTER_CHART = 2;

    /**
     * Create a chart, save it to the correct report folder.
     */
    public static void createXYChart(String fileName, ArrayList<ArrayList<Double[]>> dataTuplesSet, String title, String xAxis, String yAxis,
            boolean preciseXAxis, int type) {

        XYSeriesCollection dataset = new XYSeriesCollection();

        // create xy series
        Iterator<ArrayList<Double[]>> dataIterator = dataTuplesSet.iterator();
        int counter = 0;
        while (dataIterator.hasNext()) {
            ArrayList<Double[]> seriesData = dataIterator.next();
            XYSeries series = new XYSeries("Series" + counter);

            // fill series
            int size = seriesData.size();
            for (int i = 0; i < size; ++i) {
                Double[] data = seriesData.get(i);
                // System.out.println(data[0]+","+data[1]+","+data.toString());
                series.add(data[0], data[1]);
            }

            dataset.addSeries(series);
            // System.out.println("series "+counter);
            ++counter;
        }

        // generate the graph
        JFreeChart chart = null;
        if (type == XY_LINE_CHART) {
            chart = ChartFactory.createXYLineChart(title, xAxis, yAxis, dataset, PlotOrientation.VERTICAL, true, true, false);
        } else if (type == XY_SCATTER_CHART) {
            chart = ChartFactory.createScatterPlot(title, xAxis, yAxis, dataset, PlotOrientation.VERTICAL, true, true, false);
        }

        if (!preciseXAxis) {
            chart.getXYPlot().getDomainAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        }

        // chart.getXYPlot().getDomainAxis().
        // change the auto tick unit selection to integer units only...
        // XYPlot plot = chart.getXYPlot();
        // NumberAxis rangeAxis = (NumberAxis) plot.g getRangeAxis();
        // rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        try {
            String subFolderName = "web";
            if (Controller.EXTRACTION_SOURCES == Controller.SELECTION) {
                subFolderName = "selection";
            }
            ChartUtilities.saveChartAsPNG(new File("data/reports/" + subFolderName + "/" + fileName), chart, 700, 400);
        } catch (IOException e) {
            System.err.println("Problem occurred creating chart.");
        }

    }

    /**
     * Create a bar chart.
     */
    public static void createVerticalBarChart(String fileName, CategoryDataset data, String title, String xAxis, String yAxis) {
        createBarChart(fileName, data, title, xAxis, yAxis, PlotOrientation.VERTICAL);
    }

    public static void createHorizontalBarChart(String fileName, CategoryDataset data, String title, String xAxis, String yAxis) {
        createBarChart(fileName, data, title, xAxis, yAxis, PlotOrientation.HORIZONTAL);
    }

    public static void createBarChart(String fileName, CategoryDataset data, String title, String xAxis, String yAxis, PlotOrientation plotOrientation) {

        // create the chart...
        JFreeChart chart = ChartFactory.createBarChart(title, // chart title
                yAxis, // domain axis label
                yAxis, // range axis label
                data, // data
                PlotOrientation.VERTICAL, // orientation
                false, // include legend
                false, // tooltips?
                false // URLs?
                );

        try {
            ChartUtilities.saveChartAsPNG(new File(fileName), chart, 700, 400);
        } catch (IOException e) {
            Logger.getRootLogger().error(fileName, e);
        }
    }

    public static void createLineChart(String fileName, ArrayList<ArrayList<Double[]>> dataTuplesSet, ArrayList<String> seriesNames, String title,
            String xAxis, String yAxis, boolean preciseXAxis) {

        XYSeriesCollection dataset = new XYSeriesCollection();

        // create xy series
        Iterator<ArrayList<Double[]>> dataIterator = dataTuplesSet.iterator();
        int counter = 0;
        while (dataIterator.hasNext()) {
            ArrayList<Double[]> seriesData = dataIterator.next();
            XYSeries series;
            if (seriesNames.size() > counter)
                series = new XYSeries(seriesNames.get(counter));
            else
                series = new XYSeries("Series" + counter);

            // fill series
            int size = seriesData.size();
            for (int i = 0; i < size; ++i) {
                Double[] data = seriesData.get(i);
                // System.out.println(data[0]+","+data[1]+","+data.toString());
                series.add(data[0], data[1]);
            }

            dataset.addSeries(series);
            // System.out.println("series "+counter);
            ++counter;
        }

        // generate the graph
        JFreeChart chart = ChartFactory.createXYLineChart(title, xAxis, yAxis, dataset, PlotOrientation.VERTICAL, true, true, false);

        if (!preciseXAxis)
            chart.getXYPlot().getDomainAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        // final NumberAxis rangeAxis = new LogarithmicAxis("Log(y)");
        // chart.getXYPlot().setRangeAxis(rangeAxis);

        // chart.getXYPlot().getDomainAxis().
        // change the auto tick unit selection to integer units only...
        // XYPlot plot = chart.getXYPlot();
        // NumberAxis rangeAxis = (NumberAxis) plot.g getRangeAxis();
        // rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        try {
            ChartUtilities.saveChartAsPNG(new File(fileName), chart, 1200, 800);
        } catch (IOException e) {
            Logger.getRootLogger().error(fileName, e);
        }

    }

    public static void main(String[] args) {
        String dataString = FileHelper.readFileToString("data/test/tempData.txt");
        Pattern pat = Pattern.compile("(\\d)+((\\.)(\\d))?/");
        Matcher m = pat.matcher(dataString);

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        m.region(0, dataString.length());
        ArrayList<Double> values = new ArrayList<Double>();
        while (m.find()) {
            // System.out.println(m.start()+" "+m.group());
            // System.out.println(m.group().substring(0,m.group().length()-1));
            values.add(Double.valueOf(m.group().substring(0, m.group().length() - 1)));
        }

        Collections.sort(values);
        for (int i = 0, l = values.size(); i < l; i++) {
            System.out.println(values.get(i));
            dataset.addValue(values.get(i), "Data" + i, "Data");
        }

        createVerticalBarChart("data/test/tempDataChart.png", dataset, "Data Visualization", "Data", "Data");

        // ////////////////// test line chart creation ////////////////////////

        // series, data, 0:time,1:value
        ArrayList<ArrayList<Double[]>> quantities = new ArrayList<ArrayList<Double[]>>();

        // collect total entity and total fact extraction
        ArrayList<Double[]> tableTrust = new ArrayList<Double[]>();
        Double[] data = new Double[2];
        data[0] = 4.0;
        data[1] = 0.6;
        tableTrust.add(data);
        data = new Double[2];
        data[0] = 6.0;
        data[1] = 0.3;
        tableTrust.add(data);

        quantities.add(tableTrust);

        createLineChart("data/test/tempDataLineChart.png", quantities, new ArrayList<String>(), "Data Visualization", "Data", "Data", true);
    }
}