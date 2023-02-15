package ws.palladian.kaggle.restaurants.features.descriptors;

import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_F64;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static boofcv.factory.feature.detdesc.FactoryDetectDescribe.surfStable;

/**
 * @author Philipp Katz
 * @see <a href="http://boofcv.org/index.php?title=Example_SURF_Feature">Example
 * </a>
 */
public class SurfDescriptorExtractor implements DescriptorExtractor {
    public static final SurfDescriptorExtractor SURF = new SurfDescriptorExtractor();

    private SurfDescriptorExtractor() {
        // singleton
    }

    @Override
    public List<Descriptor> extractDescriptors(BufferedImage image) {
        // these values were taken from the example page, they are different
        // from the default configuration however
        // ConfigFastHessian config = new ConfigFastHessian(0, 2, 200, 2, 9, 4, 4);

        ConfigFastHessian config = new ConfigFastHessian();
        DetectDescribePoint<ImageUInt8, BrightFeature> surf = surfStable(config, null, null, ImageUInt8.class);
        ImageUInt8 input = ConvertBufferedImage.extractImageUInt8(image);
        surf.detect(input);
        List<Descriptor> descriptions = new ArrayList<>();
        for (int featureIdx = 0; featureIdx < surf.getNumberOfFeatures(); featureIdx++) {
            Point2D_F64 location = surf.getLocation(featureIdx);
            double orientation = surf.getOrientation(featureIdx);
            double radius = surf.getRadius(featureIdx);
            double[] description = surf.getDescription(featureIdx).value;
            descriptions.add(new ImmutableDescriptor(description, (int) location.x, (int) location.y, (int) radius, (int) orientation));
        }
        return descriptions;
    }

    @Override
    public String toString() {
        return "SURF";
    }

}
