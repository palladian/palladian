package ws.palladian.features;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.features.color.ColorExtractor;
import ws.palladian.utils.HistogramStats;
import ws.palladian.utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

public class StatisticsFeatureExtractor implements FeatureExtractor {
    private final ColorExtractor[] extractors;

    public StatisticsFeatureExtractor(ColorExtractor... extractors) {
        this.extractors = extractors;
    }

    @Override
    public FeatureVector extract(BufferedImage image) {
        HistogramStats[] stats = new HistogramStats[extractors.length];
        for (int i = 0; i < extractors.length; i++) {
            stats[i] = new HistogramStats(256);
        }
        int[] rgbArray = ImageUtils.getRGB(image);
        for (int rgb : rgbArray) {
            Color color = new Color(rgb);
            for (int i = 0; i < extractors.length; i++) {
                int value = extractors[i].extractValue(color);
                stats[i].add(value, 1);
            }
        }
        InstanceBuilder instanceBuilder = new InstanceBuilder();
        for (int i = 0; i < extractors.length; i++) {
            String extractorName = extractors[i].toString();
            instanceBuilder.set(extractorName + "_max", stats[i].getMax());
            instanceBuilder.set(extractorName + "_mean", stats[i].getMean());
            instanceBuilder.set(extractorName + "_min", stats[i].getMin());
            instanceBuilder.set(extractorName + "_range", stats[i].getRange());
            instanceBuilder.set(extractorName + "_stdDev", stats[i].getStandardDeviation());
            instanceBuilder.set(extractorName + "_relStdDev", stats[i].getRelativeStandardDeviation());
            instanceBuilder.set(extractorName + "_sum", stats[i].getSum());
            instanceBuilder.set(extractorName + "_count", stats[i].getCount());
            instanceBuilder.set(extractorName + "_10-percentile", stats[i].getPercentile(10));
            instanceBuilder.set(extractorName + "_20-percentile", stats[i].getPercentile(20));
            instanceBuilder.set(extractorName + "_30-percentile", stats[i].getPercentile(30));
            instanceBuilder.set(extractorName + "_40-percentile", stats[i].getPercentile(40));
            instanceBuilder.set(extractorName + "_50-percentile", stats[i].getPercentile(50));
            instanceBuilder.set(extractorName + "_60-percentile", stats[i].getPercentile(60));
            instanceBuilder.set(extractorName + "_70-percentile", stats[i].getPercentile(70));
            instanceBuilder.set(extractorName + "_80-percentile", stats[i].getPercentile(80));
            instanceBuilder.set(extractorName + "_90-percentile", stats[i].getPercentile(90));
            instanceBuilder.set(extractorName + "_skewness", stats[i].getSkewness());
            instanceBuilder.set(extractorName + "_kurtosis", stats[i].getKurtosis());
        }
        return instanceBuilder.create();
    }
}
