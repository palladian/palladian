package ws.palladian.kaggle.redhat.dataset.transformers;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.AbstractDatasetFeatureVectorTransformer;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformation.FeatureInformationEntry;
import ws.palladian.core.dataset.FeatureInformationBuilder;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.Vector.VectorEntry;
import ws.palladian.helper.functional.Function;

public final class FeatureRenamer extends AbstractDatasetFeatureVectorTransformer {

	private final Function<String, String> featureNameMapping;

	public FeatureRenamer(Function<String, String> featureNameMapping) {
		Validate.notNull(featureNameMapping, "featureNameMapping must not be null");
		this.featureNameMapping = featureNameMapping;
	}

	public FeatureRenamer(String regex, String replacement) {
		Validate.notEmpty(regex, "regex must not be null or empty");
		Validate.notEmpty(replacement, "replacement must not be null or empty");
		this.featureNameMapping = new Function<String, String>() {
			@Override
			public String compute(String input) {
				return input.replaceAll(regex, replacement);
			}
		};
	}

	@Override
	public FeatureInformation getFeatureInformation(FeatureInformation featureInformation) {
		FeatureInformationBuilder builder = new FeatureInformationBuilder();
		for (FeatureInformationEntry fiEntry : featureInformation) {
			String mappedName = featureNameMapping.compute(fiEntry.getName());
			builder.set(mappedName != null ? mappedName : fiEntry.getName(), fiEntry.getType());
		}
		return builder.create();
	}

	@Override
	public FeatureVector compute(FeatureVector featureVector) {
		// return new RenamedFeatureVector(featureVector, featureNameMapping);

		// XXX not very efficient to create a whole new FeatureVector here,
		// but is solves above's bug for now
		InstanceBuilder builder = new InstanceBuilder();
		for (VectorEntry<String, Value> entry : featureVector) {
			builder.set(featureNameMapping.compute(entry.key()), entry.value());
		}
		return builder.create();
	}

}
