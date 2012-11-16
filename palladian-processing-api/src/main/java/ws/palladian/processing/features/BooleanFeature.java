/**
 * Created on: 13.06.2012 14:03:39
 */
package ws.palladian.processing.features;

/**
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class BooleanFeature extends Feature<Boolean> {

    /**
     * @param name
     * @param value
     */
    public BooleanFeature(String name, Boolean value) {
        super(name, value);
    }

//    public BooleanFeature(FeatureDescriptor<BooleanFeature> descriptor, Boolean value) {
//        super(descriptor.getIdentifier(), value);
//    }

}
