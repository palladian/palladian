package ws.palladian.extraction.feature;

import java.util.Objects;

import ws.palladian.core.AppendedVector;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.AbstractDatasetFeatureVectorTransformer;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformationBuilder;
import ws.palladian.core.value.ImmutableStringValue;

public class FeatureCombiner extends AbstractDatasetFeatureVectorTransformer {

	private static final String SEPARATOR = "__";
	private final String feature1;
	private final String feature2;

	public FeatureCombiner(String feature1, String feature2) {
		this.feature1 = Objects.requireNonNull(feature1);
		this.feature2 = Objects.requireNonNull(feature2);
	}

	@Override
	public FeatureInformation getFeatureInformation(FeatureInformation featureInformation) {
		FeatureInformationBuilder builder = new FeatureInformationBuilder();
		builder.add(featureInformation);
		builder.set(feature1 + SEPARATOR + feature2, ImmutableStringValue.class);
		return builder.create();
	}

	@Override
	public FeatureVector compute(FeatureVector featureVector) {
		String value1 = featureVector.get(feature1).toString();
		String value2 = featureVector.get(feature2).toString();
		InstanceBuilder builder = new InstanceBuilder();
		builder.set(feature1 + SEPARATOR + feature2, value1 + SEPARATOR + value2);

		return new AppendedVector(featureVector, builder.create());

	}

}
