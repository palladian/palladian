package ws.palladian.extraction.feature;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.AppendedVector;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.AbstractDatasetFeatureVectorTransformer;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformationBuilder;
import ws.palladian.core.value.ImmutableStringValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.MultiMap;

/**
 * Reduce a value's domain: When all values 'A' and all values 'B' for a feature
 * in a dataset map to category 'X', we can simply rename 'A' and 'B' to 'X', in
 * regards to classification.
 * 
 * Note: This may in some cases improve classification, in other cases lead to
 * inferior results. During my experiments, it improved the gbtree model, but
 * degraded the gblinear model.
 * 
 * @author pk
 */
public class DomainValueReducer extends AbstractDatasetFeatureVectorTransformer {

	/** The logger for this class. */
	private static final Logger LOGGER = LoggerFactory.getLogger(DomainValueReducer.class);

	private final Map<Value, String> domainMapping = new HashMap<>();
	private final String featureName;
	private final boolean keepOriginal;

	public DomainValueReducer(Dataset dataset, String featureName) {
		this(dataset, featureName, false);
	}
	
	public DomainValueReducer(Dataset dataset, String featureName, boolean keepOriginal) {
		MultiMap<Value, String> valueToCategory = DefaultMultiMap.createWithSet();
		Set<String> categories = new HashSet<>();
		for (Instance instance : dataset) {
			String category = instance.getCategory();
			valueToCategory.add(instance.getVector().get(featureName), category);
			categories.add(category);
		}
		Set<String> mappedCategories = new HashSet<>();
		for (Entry<Value, Collection<String>> valueToCategoryEntry : valueToCategory.entrySet()) {
			if (valueToCategoryEntry.getValue().size() == 1) {
				// value has only one class
				String category = valueToCategoryEntry.getValue().iterator().next();
				domainMapping.put(valueToCategoryEntry.getKey(), category);
				mappedCategories.add(category);
			}
		}
		int oldSize = valueToCategory.size();
		int newSize = oldSize - domainMapping.size() + mappedCategories.size();
		if (domainMapping.size() > 0 && newSize < oldSize) {
			LOGGER.info("{}: Can reduce from {} to {} values", featureName, oldSize, newSize);
		} else {
			LOGGER.info("{}: Cannot reduce", featureName);
		}
		this.featureName = featureName;
		this.keepOriginal = keepOriginal;
	}

	@Override
	public FeatureVector compute(FeatureVector featureVector) {
		Value value = featureVector.get(featureName);
		String mapping = domainMapping.get(value);
		if (mapping != null) {
			value = ImmutableStringValue.valueOf("reduced_" + mapping);
		}
		if (keepOriginal) {
			InstanceBuilder builder = new InstanceBuilder();
			builder.set(featureName + "_reduced", value);
			return new AppendedVector(featureVector, builder.create());
		} else {
			InstanceBuilder builder = new InstanceBuilder();
			builder.add(featureVector);
			builder.set(featureName, value);
			return builder.create();
		}
	}

	@Override
	public FeatureInformation getFeatureInformation(FeatureInformation featureInformation) {
		if (keepOriginal) {
			return new FeatureInformationBuilder().add(featureInformation)
					.set(featureName + "_reduced", ImmutableStringValue.class).create();
		} else {
			return featureInformation;
		}
	}

}
