package ws.palladian.features;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.extraction.multimedia.ImageHandler;
import ws.palladian.helper.math.FatStats;
import ws.palladian.helper.math.Stats;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import static ws.palladian.features.SymmetryFeatureExtractor.similarity;
import static ws.palladian.features.color.Luminosity.LUMINOSITY;
import static ws.palladian.utils.ImageUtils.getRGB;

public class GridSimilarityExtractor implements FeatureExtractor {

    private final int divisions;

    public GridSimilarityExtractor(int divisions) {
        if (divisions < 2) {
            throw new IllegalArgumentException("divisions must be at least 2, but was " + divisions);
        }
        this.divisions = divisions;
    }

    @Override
    public FeatureVector extract(BufferedImage image) {
        int cellWidth = image.getWidth() / divisions;
        int cellHeight = image.getHeight() / divisions;
        int[][] cells = new int[divisions * divisions][];
        for (int xIdx = 0; xIdx < divisions; xIdx++) {
            for (int yIdx = 0; yIdx < divisions; yIdx++) {
                int x = xIdx * cellWidth;
                int y = yIdx * cellHeight;
                cells[xIdx * divisions + yIdx] = getRGB(image.getSubimage(x, y, cellWidth, cellHeight));
            }
        }
        Stats stats = new FatStats();
        for (int i = 0; i < cells.length; i++) {
            for (int j = i + 1; j < cells.length; j++) {
                stats.add(similarity(cells[i], cells[j], LUMINOSITY));
            }
        }
        InstanceBuilder instanceBuilder = new InstanceBuilder();
        String name = String.format("%sx%s-similarity", divisions, divisions);
        instanceBuilder.set(name + "_max", stats.getMax());
        instanceBuilder.set(name + "_mean", stats.getMean());
        instanceBuilder.set(name + "_min", stats.getMin());
        instanceBuilder.set(name + "_range", stats.getRange());
        instanceBuilder.set(name + "_stdDev", stats.getStandardDeviation());
        instanceBuilder.set(name + "_sum", stats.getSum());
        // instanceBuilder.set(name + "_count", stats.getCount());
        instanceBuilder.set(name + "_10-percentile", stats.getPercentile(10));
        instanceBuilder.set(name + "_20-percentile", stats.getPercentile(20));
        instanceBuilder.set(name + "_30-percentile", stats.getPercentile(30));
        instanceBuilder.set(name + "_40-percentile", stats.getPercentile(40));
        instanceBuilder.set(name + "_50-percentile", stats.getPercentile(50));
        instanceBuilder.set(name + "_60-percentile", stats.getPercentile(60));
        instanceBuilder.set(name + "_70-percentile", stats.getPercentile(70));
        instanceBuilder.set(name + "_80-percentile", stats.getPercentile(80));
        instanceBuilder.set(name + "_90-percentile", stats.getPercentile(90));
        return instanceBuilder.create();
    }

    public static void main(String[] args) throws IOException {
        File[] images = new File("/Users/pk/Desktop").listFiles((FileFilter) pathname -> pathname.getName().endsWith(".jpg"));
        for (File imageFile : images) {
            BufferedImage image = ImageHandler.load(imageFile);
            FeatureVector vector = new GridSimilarityExtractor(4).extract(image);
            System.out.println(imageFile + ": " + vector);
        }
    }

}
