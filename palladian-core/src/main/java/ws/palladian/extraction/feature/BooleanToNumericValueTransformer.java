package ws.palladian.extraction.feature;

import java.util.Set;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.AbstractDatasetFeatureVectorTransformer;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformationBuilder;
import ws.palladian.core.value.BooleanValue;
import ws.palladian.core.value.ImmutableIntegerValue;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.Vector.VectorEntry;

/**
 * Transforms all {@link BooleanValue}s to {@link NumericValue}s:
 * <code>true</code> becomes 1, <code>false</code> becomes 0.
 * 
 * @author pk
 */
public class BooleanToNumericValueTransformer extends AbstractDatasetFeatureVectorTransformer {

	@Override
	public FeatureInformation getFeatureInformation(FeatureInformation featureInformation) {
		Set<String> booleanValueNames = featureInformation.getFeatureNamesOfType(BooleanValue.class);
		FeatureInformationBuilder builder = new FeatureInformationBuilder();
		builder.add(featureInformation);
		builder.set(booleanValueNames, ImmutableIntegerValue.class);
		return builder.create();
	}

	@Override
	public FeatureVector apply(FeatureVector featureVector) {
		InstanceBuilder builder = new InstanceBuilder();
		for (VectorEntry<String, Value> entry : featureVector) {
			if (entry.value() instanceof BooleanValue) {
				builder.set(entry.key(), 1);
			} else {
				builder.set(entry.key(), entry.value());
			}
		}
		return builder.create();
	}

}
