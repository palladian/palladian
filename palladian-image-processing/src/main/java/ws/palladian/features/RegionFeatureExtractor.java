package ws.palladian.features;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.extraction.multimedia.ImageHandler;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.math.SlimStats;

import javax.media.jai.KernelJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.DilateDescriptor;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public enum RegionFeatureExtractor implements FeatureExtractor {
    REGION;

    @Override
    public FeatureVector extract(BufferedImage image) {
        InstanceBuilder instanceBuilder = new InstanceBuilder();

        // paint detected colors white
        float[] floats = new float[25];
        for (int i = 0; i < 25; i++) {
            floats[i] = 1;
        }
        RenderedOp dilateOperation = DilateDescriptor.create(image, new KernelJAI(5, 5, floats), null);
        BufferedImage dilatedImage = dilateOperation.getAsBufferedImage();

        // get colors in the middle square 20% of the shortest side
        int squareSize = Math.min(image.getWidth(), image.getHeight()) / 5;
        int xOffset = image.getWidth() / 2 - squareSize / 2;
        int yOffset = image.getHeight() / 2 - squareSize / 2;
        BufferedImage subimage = dilatedImage.getSubimage(xOffset, yOffset, squareSize, squareSize);

        // what's the most frequent color in the entire image?
        LinkedHashMap<Color, Integer> mainImageColorFrequencies = ImageHandler.getColorFrequencies(dilatedImage);
        Color mainBackgroundColor = mainImageColorFrequencies.keySet().iterator().next();

        // follow colors -> all colors in the center square that are not the background
        LinkedHashMap<Color, Integer> colorFrequencies = ImageHandler.getColorFrequencies(subimage);
        if (colorFrequencies.size() > 1) {
            colorFrequencies.remove(mainBackgroundColor);
        }

        // what colors are in the corners? remove them
        LinkedHashMap<Color, Integer> cornerColors = new LinkedHashMap<>();
        LinkedHashMap<Color, Integer> topLeft = ImageHandler.getColorFrequencies(dilatedImage.getSubimage(0, 0, 10, 10));
        LinkedHashMap<Color, Integer> topRight = ImageHandler.getColorFrequencies(dilatedImage.getSubimage(dilatedImage.getWidth() - 10, 0, 10, 10));
        LinkedHashMap<Color, Integer> bottomLeft = ImageHandler.getColorFrequencies(dilatedImage.getSubimage(0, dilatedImage.getHeight() - 10, 10, 10));
        LinkedHashMap<Color, Integer> bottomRight = ImageHandler.getColorFrequencies(dilatedImage.getSubimage(dilatedImage.getWidth() - 10, dilatedImage.getHeight() - 10, 10, 10));

        cornerColors.putAll(topLeft);
        cornerColors.putAll(topRight);
        cornerColors.putAll(bottomLeft);
        cornerColors.putAll(bottomRight);
        for (Map.Entry<Color, Integer> colorEntry : cornerColors.entrySet()) {
            if (colorEntry.getValue() > 2 && colorFrequencies.size() > 1) {
                colorFrequencies.remove(colorEntry.getKey());
            }
        }

        // delete all pixels not in allowed colors
        for (int x = 0; x < dilatedImage.getWidth(); x++) {
            for (int y = 0; y < dilatedImage.getHeight(); y++) {
                Color color = new Color(dilatedImage.getRGB(x, y));
                if (colorFrequencies.containsKey(color)) {
                    dilatedImage.setRGB(x, y, Color.WHITE.getRGB());
                } else {
                    dilatedImage.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }

        // label connected surfaces
        Map<Set<Point>, Integer> regions = new LinkedHashMap<>();

        // regions that cover more than 0.05% of the image
        int numberOfMainRegions = 0;
        int pixelsForMainRegion = (int) (0.0005 * dilatedImage.getWidth() * dilatedImage.getHeight());
        SlimStats slimStats = new SlimStats();
        for (int x = 0; x < dilatedImage.getWidth(); x++) {
            for (int y = 0; y < dilatedImage.getHeight(); y++) {
                Color color = new Color(dilatedImage.getRGB(x, y));
                if (color.equals(Color.WHITE)) {
                    Color rc = ImageHandler.getRandomColor();
                    Set<Point> pixels = new HashSet<>();
                    ImageHandler.floodFill(dilatedImage, x, y, color, rc, pixels);
                    regions.put(pixels, pixels.size());
                    if (pixels.size() > pixelsForMainRegion) {
                        numberOfMainRegions++;
                    }
                    slimStats.add(pixels.size());
                }
            }
        }

        if (slimStats.getCount() == 0) {
            slimStats.add(0);
        }

        // remove all smaller surfaces and crop to main surface
        Map<Set<Point>, Integer> setIntegerMap = CollectionHelper.sortByValue(regions, CollectionHelper.Order.DESCENDING);
        Rectangle rectangle = null;
        for (Set<Point> points : setIntegerMap.keySet()) {
            // find rect for cropping
            for (Point point : points) {
                if (rectangle == null) {
                    rectangle = new Rectangle(point);
                } else {
                    rectangle.add(point);
                }
            }
            break;
        }

        if (rectangle == null) {
            rectangle = new Rectangle(1, 1, 1, 1);
        }

        // wanna see?
        // ImageHandler.saveImage(dilatedImage, "testimage.jpg");

        instanceBuilder.set("number_regions", regions.size());
        instanceBuilder.set("number_main_regions", numberOfMainRegions);
        instanceBuilder.set("mean_region_size", slimStats.getMean());
        instanceBuilder.set("main_region_size", slimStats.getMax());
        instanceBuilder.set("main_region_width", (double) (rectangle.width + 1) / dilatedImage.getWidth());
        instanceBuilder.set("main_region_height", (double) (rectangle.height + 1) / dilatedImage.getHeight());
        instanceBuilder.set("main_region_dominance", slimStats.getMax() / (dilatedImage.getWidth() * dilatedImage.getHeight()));
        instanceBuilder.set("main_region_coverage", slimStats.getMax() / ((rectangle.width + 1) * (rectangle.height + 1)));

        return instanceBuilder.create();
    }
}
