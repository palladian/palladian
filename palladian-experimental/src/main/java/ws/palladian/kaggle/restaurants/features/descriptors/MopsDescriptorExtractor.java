package ws.palladian.kaggle.restaurants.features.descriptors;

import ij.process.ImageProcessor;
import mpicbg.ij.MOPS;
import mpicbg.imagefeatures.Feature;
import mpicbg.imagefeatures.FloatArray2DMOPS;
import mpicbg.imagefeatures.FloatArray2DMOPS.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

public class MopsDescriptorExtractor extends MpicbgDescriptorExtractor {
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MopsDescriptorExtractor.class);
    public static final MopsDescriptorExtractor MOPS = new MopsDescriptorExtractor();

    private MopsDescriptorExtractor() {
        // singleton
    }

    @Override
    protected Collection<Feature> extract(ImageProcessor processor) {
        Param param = new Param();
        param.minOctaveSize = 64;
        param.maxOctaveSize = 128;
        MOPS mops = new MOPS(new FloatArray2DMOPS(param));
        try {
            return mops.extractFeatures(processor);
        } catch (Exception e) {
            // https://github.com/axtimwalde/mpicbg/issues/26
            LOGGER.warn("Encountered {}", e.toString(), e);
            return Collections.emptySet();
        }
    }

    @Override
    public String toString() {
        return "MOPS";
    }
}
