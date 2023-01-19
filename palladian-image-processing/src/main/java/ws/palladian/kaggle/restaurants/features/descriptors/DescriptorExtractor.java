package ws.palladian.kaggle.restaurants.features.descriptors;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

import static java.util.stream.Collectors.toList;

public interface DescriptorExtractor {
    interface Descriptor {
        double[] vector();

        int x();

        int y();

        int radius();

        int orientation();

        void draw(Graphics g);
    }

    default List<double[]> extract(BufferedImage image) {
        return extractDescriptors(image).stream().map(d -> d.vector()).collect(toList());
    }

    List<Descriptor> extractDescriptors(BufferedImage image);

}
