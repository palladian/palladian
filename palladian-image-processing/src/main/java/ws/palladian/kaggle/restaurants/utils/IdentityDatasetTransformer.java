package ws.palladian.kaggle.restaurants.utils;

import ws.palladian.core.Instance;
import ws.palladian.core.dataset.DatasetTransformer;
import ws.palladian.core.dataset.FeatureInformation;

public final class IdentityDatasetTransformer implements DatasetTransformer {

	public static final IdentityDatasetTransformer INSTANCE = new IdentityDatasetTransformer();

	private IdentityDatasetTransformer() {
		// no no.
	}

	@Override
	public Instance compute(Instance input) {
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
