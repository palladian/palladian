package ws.palladian.classification.evaluation.reliability;

import ws.palladian.classification.evaluation.AbstractClassificationEvaluator;
import ws.palladian.classification.evaluation.Graph;
import ws.palladian.classification.evaluation.LogLossEvaluator;
import ws.palladian.classification.evaluation.reliability.ReliabilityDiagramEvaluator.ReliabilityDiagram;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Classifier;
import ws.palladian.core.Instance;
import ws.palladian.core.Model;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.math.SlimStats;
import ws.palladian.helper.math.Stats;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ReliabilityDiagramEvaluator extends AbstractClassificationEvaluator<ReliabilityDiagram> {

    public static class ReliabilityDiagram implements Graph, Iterable<DataPoint> {

        private final List<DataPoint> dataPoints;
        public final double logLoss;

        ReliabilityDiagram(List<DataPoint> dataPoints, double logLoss) {
            this.dataPoints = dataPoints;
            this.logLoss = logLoss;
        }

        @Override
        public void show() {
            new ReliabilityDiagramPainter().add(this, "Reliability").showCurves();
        }

        @Override
        public void save(File file) throws IOException {
            new ReliabilityDiagramPainter().add(this, "Reliability").saveCurves(file);
        }

        @Override
        public Iterator<DataPoint> iterator() {
            return dataPoints.iterator();
        }

    }

    public static final class DataPoint {
        final double mean;
        final int numItems;
        final int numPositiveItems;

        DataPoint(double mean, int numItems, int numPositiveItems) {
            this.mean = mean;
            this.numItems = numItems;
            this.numPositiveItems = numPositiveItems;
        }

        double positiveFraction() {
            return (double) numPositiveItems / numItems;
        }

        @Override
        public String toString() {
            return String.format("%f:%f", mean, positiveFraction());
        }
    }

    private final String trueClass;

    private final int numBins;

    public ReliabilityDiagramEvaluator(String trueClass, int numBins) {
        this.trueClass = trueClass;
        this.numBins = numBins;
    }

    @Override
    public <M extends Model> ReliabilityDiagram evaluate(Classifier<M> classifier, M model, Dataset data) {

        Bag<Integer> binnedItemCounts = new Bag<>();
        Bag<Integer> binnedPositiveCounts = new Bag<>();
        Map<Integer, Stats> binnedPredictionStats = new LazyMap<>(SlimStats::new);
        int n = 0;
        double logLoss = 0;

        for (Instance instance : data) {
            CategoryEntries result = classifier.classify(instance.getVector(), model);
            double prediction = result.getProbability(trueClass);
            boolean positive = instance.getCategory().equals(trueClass);
            int bin = (int) Math.round(numBins * prediction);
            binnedItemCounts.add(bin);
            if (positive) {
                binnedPositiveCounts.add(bin);
            }
            binnedPredictionStats.get(bin).add(prediction);
            logLoss += LogLossEvaluator.logLoss(positive, prediction);
            n++;
        }

        logLoss /= n;

        List<DataPoint> dataPoints = new ArrayList<>();
        for (int i = 0; i < numBins; i++) {
            int numItems = binnedItemCounts.count(i);
            if (numItems == 0) {
                continue; // no empty bins
            }
            int numPositiveItems = binnedPositiveCounts.count(i);
            double predictionMean = binnedPredictionStats.get(i).getMean();
            dataPoints.add(new DataPoint(predictionMean, numItems, numPositiveItems));
        }

        return new ReliabilityDiagram(Collections.unmodifiableList(dataPoints), logLoss);
    }

}
