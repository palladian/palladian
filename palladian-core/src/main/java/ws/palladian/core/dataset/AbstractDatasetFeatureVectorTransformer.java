package ws.palladian.core.dataset;

import ws.palladian.core.AbstractInstance;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;

/**
 * A {@link DatasetTransformer} which performs transformations only on the
 * {@link FeatureVector}. This is the usual case (i.e. the
 * {@link Instance#getCategory()} is not manipulated by transformers), thus the
 * actual transformation of the {@link FeatureVector} can be lazily executed
 * when needed, thus making iteration over the dataset much faster, when the
 * feature vector is not requested.
 * 
 * In case the set of features is modified (feature added, feature removed, type
 * changed), override {@link #getFeatureInformation(FeatureInformation)} and
 * return the updated meta information.
 * 
 * @author pk
 */
public abstract class AbstractDatasetFeatureVectorTransformer implements DatasetTransformer {

	@Override
	public final Instance compute(final Instance input) {
		return new AbstractInstance() {
			@Override
			public int getWeight() {
				return input.getWeight();
			}

			@Override
			public FeatureVector getVector() {
				return compute(input.getVector());
			}

			@Override
			public String getCategory() {
				return input.getCategory();
			}
		};
	}

	// TODO refactor this to compute(FeatureInformation, FeatureVector)
	public abstract FeatureVector compute(FeatureVector featureVector);
	
	@Override
	public FeatureInformation getFeatureInformation(FeatureInformation featureInformation) {
		// per default, assume that only feature values are updated
		return featureInformation;
	}

}
