package ws.palladian.kaggle.restaurants.features.descriptors;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import mpicbg.imagefeatures.Feature;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * {@link DescriptorExtractor} based on
 * <a href="https://github.com/axtimwalde/mpicbg">mpicbg</a>.
 *
 * @author pk
 */
abstract class MpicbgDescriptorExtractor implements DescriptorExtractor {

    @Override
    public List<Descriptor> extractDescriptors(BufferedImage image) {
        return extract(new ByteProcessor(image)).stream().map(f -> {
            double[] point = new double[f.descriptor.length];
            for (int i = 0; i < f.descriptor.length; i++) {
                point[i] = f.descriptor[i];
            }
            return new ImmutableDescriptor(point, (int) f.location[0], (int) f.location[1], (int) f.scale, (int) f.orientation);
        }).collect(toList());
    }

    protected abstract Collection<Feature> extract(ImageProcessor processor);

}
