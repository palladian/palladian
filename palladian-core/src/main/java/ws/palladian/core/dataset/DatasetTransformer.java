package ws.palladian.core.dataset;

import ws.palladian.core.Instance;
import ws.palladian.helper.functional.Function;

public interface DatasetTransformer extends Function<Instance, Instance> {

	FeatureInformation getFeatureInformation(FeatureInformation featureInformation);

}
