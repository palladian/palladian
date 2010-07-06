package tud.iir.classification.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import tud.iir.helper.MathHelper;
import tud.iir.knowledge.Concept;
import tud.iir.persistence.DatabaseManager;

public abstract class EntityAssessor {

    // the logger for the evaluation
    protected static final Logger LOGGER = Logger.getLogger(EntityAssessor.class);

    // training percentage step size [0,1]
    protected double trainingPercentageStepSize = 0.1;

    // trust threshold step size [0,1]
    protected double trustThresholdStepSize = 0.1;

    protected DatabaseManager dbm = null;

    public EntityAssessor() {
        dbm = DatabaseManager.getInstance();
    }

    protected ArrayList<Double> calculateMetrics(int totalRealCorrect, int totalAssigned, int totalCorrect, int testSetSize) {

        double precision = (double) totalCorrect / (double) totalAssigned;
        if (totalAssigned == 0) {
            precision = 1.0;
        }
        double recall = (double) totalCorrect / (double) totalRealCorrect;
        double f1 = (2 * precision * recall) / (precision + recall);
        if (precision + recall == 0) {
            f1 = 0.0;
        }

        // double falsePositives = totalAssigned - totalCorrect;
        double falseNegatives = totalRealCorrect - totalCorrect;
        double trueNegatives = (testSetSize - totalAssigned - falseNegatives);

        // double accuracy = (totalCorrect + trueNegatives) / (totalCorrect + trueNegatives + falsePositives + falseNegatives);
        double accuracy = (totalCorrect + trueNegatives) / (testSetSize);

        ArrayList<Double> metrics = new ArrayList<Double>();
        metrics.add(precision);
        metrics.add(recall);
        metrics.add(f1);
        metrics.add((double) totalCorrect);
        metrics.add((double) totalRealCorrect);
        metrics.add((double) totalAssigned);
        metrics.add(accuracy);
        metrics.add(trueNegatives);
        metrics.add((double) testSetSize);

        System.out.println("Precision: " + precision + " (true positive: " + totalCorrect + " true negative: " + trueNegatives + ", real total positive: "
                + totalRealCorrect + ", total assigned positive: " + totalAssigned + ", total test: " + testSetSize + ")");
        System.out.println("Recall: " + recall);
        System.out.println("F1: " + f1);
        System.out.println("Accuracy: " + accuracy);

        return metrics;
    }

    public ArrayList<Double> logMetrics(HashSet<Concept> concepts, HashMap<String, ArrayList<Double>> evaluationMetrics) {
        double totalPrecision = 0.0;
        double totalRecall = 0.0;
        double totalAccuracy = 0.0;

        LOGGER.info("############ evaluation metrics by concept ############");

        for (Concept concept : concepts) {
            ArrayList<Double> metric = evaluationMetrics.get(concept.getName());
            totalPrecision += metric.get(0);
            totalRecall += metric.get(1);
            totalAccuracy += metric.get(6);

            LOGGER.info("Evaluation Metrics for concept " + concept.getName() + ":");
            LOGGER.info("true positive: " + metric.get(3) + " true negative: " + metric.get(7) + ", real total positive: " + metric.get(4)
                    + ", total assigned positive: " + metric.get(5) + ", total test: " + metric.get(8) + ")");
            LOGGER.info("Precision: " + metric.get(0));
            LOGGER.info("Recall: " + metric.get(1));
            LOGGER.info("F1: " + metric.get(2));
            LOGGER.info("Accuracy: " + metric.get(6));
            LOGGER.info("");
        }

        LOGGER.info("############ average evaluation metrics over all concept ############");

        // averaged metric
        double precisionAveraged = totalPrecision / concepts.size();
        double recallAveraged = totalRecall / concepts.size();
        double f1Averaged = (2 * precisionAveraged * recallAveraged) / (precisionAveraged + recallAveraged);
        double accuracyAveraged = totalAccuracy / concepts.size();
        LOGGER.info("");
        LOGGER.info("Average Precision: " + precisionAveraged);
        LOGGER.info("Average Recall: " + recallAveraged);
        LOGGER.info("Average F1: " + f1Averaged);
        LOGGER.info("Average Accuracy: " + accuracyAveraged);

        ArrayList<Double> values = new ArrayList<Double>();
        values.add(precisionAveraged);
        values.add(recallAveraged);
        values.add(f1Averaged);
        values.add(accuracyAveraged);

        return values;
    }

    protected void createFlashChartLog(TreeMap<Double, ArrayList<Double>> graphData) {

        // log datapoints for flash chart
        for (Map.Entry<Double, ArrayList<Double>> dataEntry : graphData.entrySet()) {
            double step = MathHelper.round(Math.floor(10000 * dataEntry.getKey()) / 10000, 4);
            double precision = MathHelper.round(dataEntry.getValue().get(0), 4);
            double recall = MathHelper.round(dataEntry.getValue().get(1), 4);
            double f1 = MathHelper.round(dataEntry.getValue().get(2), 4);
            double accuracy = MathHelper.round(dataEntry.getValue().get(3), 4);
            LOGGER.info("p = new Point(" + step + "," + precision + "); dataPoints10.push(p);");
            LOGGER.info("p = new Point(" + step + "," + recall + "); dataPoints11.push(p);");
            LOGGER.info("p = new Point(" + step + "," + f1 + "); dataPoints12.push(p);");
            LOGGER.info("p = new Point(" + step + "," + accuracy + "); dataPoints13.push(p);");
            LOGGER.info("p = new Point(" + recall + "," + precision + "); dataPoints14.push(p);");
        }

    }

    protected abstract void evaluate();
}
