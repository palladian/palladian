package ws.palladian.features;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.extraction.multimedia.ImageHandler;

import java.awt.image.BufferedImage;

public enum ColorFeatureExtractor implements FeatureExtractor {
    COLOR;

    @Override
    public FeatureVector extract(BufferedImage image) {
        InstanceBuilder instanceBuilder = new InstanceBuilder();
        ImageHandler.COLORS.forEach(c -> instanceBuilder.set(createName(c.getMainColorName()), false));
        ImageHandler.detectColors(image).forEach(c -> instanceBuilder.set(createName(c.getMainColorName()), true));
        return instanceBuilder.create();
    }

    private String createName(String name) {
        return "main_color-" + name;
    }

}
