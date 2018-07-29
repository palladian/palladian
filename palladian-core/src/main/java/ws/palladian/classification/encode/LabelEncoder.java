package ws.palladian.classification.encode;

import static ws.palladian.helper.functional.Filters.equal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.utils.DummyVariableCreator;
import ws.palladian.core.AppendedVector;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.AbstractDatasetFeatureVectorTransformer;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformationBuilder;
import ws.palladian.core.dataset.statistics.DatasetStatistics;
import ws.palladian.core.dataset.statistics.NominalValueStatistics;
import ws.palladian.core.value.ImmutableIntegerValue;
import ws.palladian.core.value.NominalValue;
import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;

/**
 * Transforms nominal values to index-based numeric values, i.e. a value column
 * with n different nominal values will be transformed to numeric values in
 * range [0, n-1].
 * 
 * This can be used as an alternative to the {@link DummyVariableCreator}.
 * 
 * @author pk
 * @see <a href=
 *      "http://scikit-learn.org/stable/modules/generated/sklearn.preprocessing.LabelEncoder.html">sklearn.preprocessing.LabelEncoder</a>
 */
public class LabelEncoder extends AbstractDatasetFeatureVectorTransformer {
	/** The logger for this class. */
	private static final Logger LOGGER = LoggerFactory.getLogger(LabelEncoder.class);


	private final Map<String, Map<String, Integer>> mappings;

	public LabelEncoder(Dataset dataset) {
		LOGGER.info("Start initializing LabelEncoder");
		StopWatch stopWatch = new StopWatch();
		Set<String> nominalValueNames = dataset.getFeatureInformation().getFeatureNamesOfType(NominalValue.class);
		DatasetStatistics statistics = new DatasetStatistics(dataset.filterFeatures(equal(nominalValueNames)));

		Map<String, Map<String, Integer>> mappings = new HashMap<>();
		for (String nominalValueName : nominalValueNames) {
			NominalValueStatistics valueStatistics = (NominalValueStatistics) statistics.getValueStatistics(nominalValueName);
			List<String> list = new ArrayList<>(valueStatistics.getValues());
			Collections.sort(list);
			Map<String, Integer> indexMap = CollectionHelper.createIndexMap(list);
			LOGGER.debug("# unique values for {}: {}", nominalValueName, list.size());
			mappings.put(nominalValueName, indexMap);
		}

		LOGGER.info("Initialized LabelEncoder in {}", stopWatch);
		this.mappings = mappings;
	}

	@Override
	public FeatureInformation getFeatureInformation(FeatureInformation featureInformation) {
		FeatureInformationBuilder builder = new FeatureInformationBuilder();
		builder.add(featureInformation);
		for (String valueName : mappings.keySet()) {
			builder.set(valueName + "_labelEncoded", ImmutableIntegerValue.class);
		}
		return builder.create();
	}

	@Override
	public FeatureVector compute(FeatureVector featureVector) {
		InstanceBuilder builder = new InstanceBuilder();
		for (Entry<String, Map<String, Integer>> mapping : mappings.entrySet()) {
			String valueName = mapping.getKey();
			Value value = featureVector.get(valueName);
			Value mappedValue = NullValue.NULL;
			if (!value.isNull()) {
				Integer mappedInteger = mapping.getValue().get(((NominalValue) value).getString());
				if (mappedInteger != null) {
					mappedValue = ImmutableIntegerValue.valueOf(mappedInteger);
				}
			}
			builder.set(valueName + "_labelEncoded", mappedValue);
		}
		return new AppendedVector(featureVector, builder.create());
	}

}
