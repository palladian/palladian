package ws.palladian.core.dataset;

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
 * @author pk
 */
public abstract class AbstractDatasetFeatureVectorTransformer implements DatasetTransformer {

	@Override
	public final Instance compute(final Instance input) {
		return new Instance() {
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
	protected abstract FeatureVector compute(FeatureVector featureVector);

}
