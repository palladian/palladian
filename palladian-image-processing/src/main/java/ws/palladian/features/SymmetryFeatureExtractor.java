package ws.palladian.features;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.extraction.multimedia.ImageHandler;
import ws.palladian.features.color.ColorExtractor;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;

import static ws.palladian.utils.ImageUtils.*;

/**
 * Detect similarity in an image when mirroring horizontally/vertically/both. A
 * high "horizontal similarity" e.g. denotes that an image is similar to itself,
 * when being flipped (mirrored) on a horizontal axis. "both" means, flipped
 * horizontally and vertically.
 *
 * @author Philipp Katz
 */
public class SymmetryFeatureExtractor implements FeatureExtractor {
    private final ColorExtractor[] extractors;

    public SymmetryFeatureExtractor(ColorExtractor... extractors) {
        this.extractors = Objects.requireNonNull(extractors, "extractors must not be null");
    }

    @Override
    public FeatureVector extract(BufferedImage image) {
        InstanceBuilder instanceBuilder = new InstanceBuilder();
        int[] rgbImage = getRGB(image);
        int[] flippedVertically = getRGB(flipVertically(image));
        int[] flippedHorizontally = getRGB(flipHorizontally(image));
        int[] flippedBoth = getRGB(flipVertically(flipHorizontally(image)));
        int[] rotated180 = getRGB(rotate180(image));
        for (ColorExtractor extractor : extractors) {
            double simHorizontal = similarity(rgbImage, flippedVertically, extractor);
            double simVertical = similarity(rgbImage, flippedHorizontally, extractor);
            double simBoth = similarity(rgbImage, flippedBoth, extractor);
            double simRotated = similarity(rgbImage, rotated180, extractor);
            String extractorName = extractor.toString();
            instanceBuilder.set("symmetry-horizontal-" + extractorName, simHorizontal);
            instanceBuilder.set("symmetry-vertical-" + extractorName, simVertical);
            instanceBuilder.set("symmetry-both-" + extractorName, simBoth);
            instanceBuilder.set("symmetry-180-rotated-" + extractorName, simRotated);
        }
        return instanceBuilder.create();
    }

    /**
     * Minkowsi; see
     * {@link ImageHandler#getSimilarity(BufferedImage, BufferedImage, int)}.
     *
     * @param rgbImage1 First image array.
     * @param rgbImage2 Second image array.
     * @param extractor The color component extractor.
     * @return The similarity.
     */
    // XXX changed to public; move to ImageUtils?
    public static double similarity(int[] rgbImage1, int[] rgbImage2, ColorExtractor extractor) {
        final int r = 2;
        double squaredError = 0;
        for (int idx = 0; idx < rgbImage1.length; idx++) {
            int value1 = extractor.extractValue(new Color(rgbImage1[idx]));
            int value2 = extractor.extractValue(new Color(rgbImage2[idx]));
            squaredError += Math.pow(Math.abs(value1 - value2) / 255., r);
        }
        return 1 - Math.pow(squaredError / rgbImage1.length, 1. / r);
    }
}
