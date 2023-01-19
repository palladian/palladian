package ws.palladian.features;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.extraction.multimedia.ColorSpaceConverter;
import ws.palladian.extraction.multimedia.ImageHandler;
import ws.palladian.utils.HistogramStats;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Detect how "edgy" an image is. Spaghetti are super edgy while a photo of a menu is not.
 */
public enum EdginessFeatureExtractor implements FeatureExtractor {
    EDGINESS;

    @Override
    public FeatureVector extract(BufferedImage image) {
        InstanceBuilder instanceBuilder = new InstanceBuilder();
        HistogramStats edginessStats = detectEdginess(image);
        instanceBuilder.set("edginess-max", edginessStats.getMax());
        instanceBuilder.set("edginess-min", edginessStats.getMin());
        instanceBuilder.set("edginess-median", edginessStats.getMedian());
        instanceBuilder.set("edginess-mean", edginessStats.getMean());
        instanceBuilder.set("edginess-relStdDev", edginessStats.getRelativeStandardDeviation());
        instanceBuilder.set("edginess-kurtosis", edginessStats.getKurtosis());
        instanceBuilder.set("edginess-skewness", edginessStats.getSkewness());
        instanceBuilder.set("edginess-variance", edginessStats.getVariance());
        instanceBuilder.set("edginess-10-percentile", edginessStats.getPercentile(10));
        instanceBuilder.set("edginess-20-percentile", edginessStats.getPercentile(20));
        instanceBuilder.set("edginess-30-percentile", edginessStats.getPercentile(30));
        instanceBuilder.set("edginess-40-percentile", edginessStats.getPercentile(40));
        instanceBuilder.set("edginess-50-percentile", edginessStats.getPercentile(50));
        instanceBuilder.set("edginess-60-percentile", edginessStats.getPercentile(60));
        instanceBuilder.set("edginess-70-percentile", edginessStats.getPercentile(70));
        instanceBuilder.set("edginess-80-percentile", edginessStats.getPercentile(80));
        instanceBuilder.set("edginess-90-percentile", edginessStats.getPercentile(90));
        return instanceBuilder.create();
    }

    private HistogramStats detectEdginess(BufferedImage image) {
        ColorSpaceConverter csc = new ColorSpaceConverter();
        HistogramStats stats = new HistogramStats();
        image = ImageHandler.detectEdges(image);
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                Color thisColor = new Color(image.getRGB(i, j));
                double[] doubles = csc.rgbToHsb(thisColor);
                stats.add((int) (doubles[2] * 255), 1);
            }
        }
        return stats;
    }

}
