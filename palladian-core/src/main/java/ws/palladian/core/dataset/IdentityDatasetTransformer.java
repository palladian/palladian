package ws.palladian.core.dataset;

import ws.palladian.core.Instance;

public final class IdentityDatasetTransformer implements DatasetTransformer {

    public static final IdentityDatasetTransformer INSTANCE = new IdentityDatasetTransformer();

    private IdentityDatasetTransformer() {
        // no no.
    }

    @Override
    public Instance apply(Instance input) {
        return input;
    }

    @Override
    public FeatureInformation getFeatureInformation(FeatureInformation featureInformation) {
        return featureInformation;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
