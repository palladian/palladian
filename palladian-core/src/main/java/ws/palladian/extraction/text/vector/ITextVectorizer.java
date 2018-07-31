package ws.palladian.extraction.text.vector;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.dataset.DatasetTransformer;

public interface ITextVectorizer extends DatasetTransformer {
	
	// TODO refactor this to compute(FeatureInformation, FeatureVector)
	// TODO copied from AbstractDatasetFeatureVectorTransformer
	public abstract FeatureVector apply(FeatureVector featureVector);

}
