package ws.palladian.features;

import ws.palladian.core.FeatureVector;

import java.awt.image.BufferedImage;

public interface FeatureExtractor {
    FeatureVector extract(BufferedImage image);
}
