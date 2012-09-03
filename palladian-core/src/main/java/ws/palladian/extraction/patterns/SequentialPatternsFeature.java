/**
 * Created on: 20.06.2012 21:29:28
 */
package ws.palladian.extraction.patterns;

import java.util.List;

import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureDescriptor;

/**
 * <p>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since
 */
public final class SequentialPatternsFeature extends Feature<List<SequentialPattern>> {

    /**
     * <p>
     * 
     * </p>
     * 
     * @param descriptor
     * @param value
     */
    public SequentialPatternsFeature(FeatureDescriptor<SequentialPatternsFeature> descriptor,
            List<SequentialPattern> value) {
        super(descriptor, value);
    }

}
