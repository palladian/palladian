/**
 * Created on: 05.02.2013 16:37:54
 */
package ws.palladian.classification.featureselection;

import ws.palladian.processing.features.Feature;

/**
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.0
 */
public final class FeatureDetails {
    private final String featurePath;
    private final Class<? extends Feature<?>> featureType;
    private final boolean isSparse;

    public FeatureDetails(String featurePath, Class<? extends Feature<?>> featureType, final boolean isSparse) {
        this.featurePath = featurePath;
        this.featureType = featureType;
        this.isSparse = isSparse;
    }

    public String getPath() {
        return featurePath;
    }

    public Class<? extends Feature<?>> getType() {
        return featureType;
    }

    public boolean isSparse() {
        return isSparse;
    }
}
