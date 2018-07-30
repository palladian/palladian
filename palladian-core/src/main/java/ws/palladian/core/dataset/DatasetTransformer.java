package ws.palladian.core.dataset;

import ws.palladian.core.Instance;
import java.util.function.Function;

/**
 * Performs transformations on a {@link Dataset}. <b>Note:</b> In general, consider
 * extending {@link AbstractDatasetFeatureVectorTransformer}.
 * 
 * @author pk
 */
public interface DatasetTransformer extends Function<Instance, Instance> {

	FeatureInformation getFeatureInformation(FeatureInformation featureInformation);

}
