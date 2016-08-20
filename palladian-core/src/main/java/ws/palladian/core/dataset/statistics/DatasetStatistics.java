package ws.palladian.core.dataset.statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.Instance;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformation.FeatureInformationEntry;
import ws.palladian.core.dataset.statistics.NominalValueStatistics.NominalValueStatisticsBuilder;
import ws.palladian.core.dataset.statistics.NumericValueStatistics.NumericValueStatisticsBuilder;
import ws.palladian.core.value.ImmutableStringValue;
import ws.palladian.core.value.NominalValue;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.functional.Factory;

public class DatasetStatistics {

	public interface ValueStatistics {
		int getNumNullValues();
	}

	public interface ValueStatisticsBuilder<S extends ValueStatistics> extends Factory<S> {
		void add(Value value);
	}

	private static final String OUTPUT_FORMAT = "%25s | %-25s | %s";

	private final FeatureInformation featureInformation;

	private final Map<String, ValueStatistics> valueStats;

	private final NominalValueStatistics categoryStats;

	public DatasetStatistics(Dataset dataset) {
		Validate.notNull(dataset, "dataset must not be null");

		Map<String, ValueStatisticsBuilder<?>> statsBuilders = new HashMap<>();
		NominalValueStatisticsBuilder categoryStatsBuilder = new NominalValueStatisticsBuilder();
		featureInformation = dataset.getFeatureInformation();
		for (FeatureInformationEntry entry : featureInformation) {
			ValueStatisticsBuilder<?> statsBuilder = createValueStatsBuilder(entry);
			if (statsBuilder != null) {
				statsBuilders.put(entry.getName(), statsBuilder);
			}
		}

		for (Instance instance : dataset) {
			for (Entry<String, ValueStatisticsBuilder<?>> builder : statsBuilders.entrySet()) {
				Value value = instance.getVector().get(builder.getKey());
				builder.getValue().add(value);
			}
			categoryStatsBuilder.add(ImmutableStringValue.valueOf(instance.getCategory()));
		}

		Map<String, ValueStatistics> valueStats = new HashMap<>();
		for (Entry<String, ValueStatisticsBuilder<?>> builder : statsBuilders.entrySet()) {
			valueStats.put(builder.getKey(), builder.getValue().create());
		}
		this.valueStats = Collections.unmodifiableMap(valueStats);
		categoryStats = categoryStatsBuilder.create();
	}

	private static ValueStatisticsBuilder<?> createValueStatsBuilder(FeatureInformationEntry entry) {
		if (entry.isCompatible(NominalValue.class)) {
			return new NominalValueStatisticsBuilder();
		} else if (entry.isCompatible(NumericValue.class)) {
			return new NumericValueStatisticsBuilder();
		} else {
			return null;
		}
	}

	public ValueStatistics getValueStatistics(String featureName) {
		Validate.notEmpty(featureName, "featureName must not be null or empty");
		return valueStats.get(featureName);
	}

	public NominalValueStatistics getCategoryStatistics() {
		return categoryStats;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		List<String> featureNames = new ArrayList<>(featureInformation.getFeatureNames());
		Collections.sort(featureNames);
		for (String featureName : featureNames) {
			FeatureInformationEntry entry = featureInformation.getFeatureInformation(featureName);
			ValueStatistics stats = valueStats.get(entry.getName());
			builder.append(String.format(OUTPUT_FORMAT, entry.getName(), entry.getType().getSimpleName(), stats));
			builder.append('\n');
		}
		builder.append('\n');
		builder.append(String.format(OUTPUT_FORMAT, "category", "", categoryStats));
		return builder.toString();
	}

}
