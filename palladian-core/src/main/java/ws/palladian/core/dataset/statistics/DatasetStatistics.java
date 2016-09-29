package ws.palladian.core.dataset.statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.FeatureInformation.FeatureInformationEntry;
import ws.palladian.core.dataset.statistics.NominalValueStatistics.NominalValueStatisticsBuilder;
import ws.palladian.core.dataset.statistics.NumericValueStatistics.NumericValueStatisticsBuilder;
import ws.palladian.core.value.ImmutableStringValue;
import ws.palladian.core.value.NominalValue;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.functional.Factory;

/**
 * Allows to calculate value statistics for a {@link Dataset}.
 * 
 * @author pk
 */
public class DatasetStatistics {
	
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetStatistics.class);

	public interface ValueStatistics {
		int getNumNullValues();
	}

	public interface ValueStatisticsBuilder<S extends ValueStatistics> extends Factory<S> {
		void add(Value value);
	}

	private static final String OUTPUT_FORMAT = "%25s | %-25s | %s";

	private final Dataset dataset;
	
	private Map<String, ValueStatistics> valueStats;

	private NominalValueStatistics categoryStats;

	public DatasetStatistics(Dataset dataset) {
		Validate.notNull(dataset, "dataset must not be null");
		this.dataset = dataset;
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
		if (valueStats == null) {
			calculateStatistics(true);
		}
		return valueStats.get(featureName);
	}

	/**
	 * Calculate the statistics.
	 * 
	 * @param includeValueStatistics
	 *            <code>true</code> to include the value statistics. Calculating
	 *            them might takes some time, thus we only do so, when
	 *            explicitly requested (ie. {@link #getCategoryStatistics()} was
	 *            called) and cache the values afterwards.
	 */
	private void calculateStatistics(boolean includeValueStatistics) {
		if (includeValueStatistics) {
			LOGGER.info("Calculate category and value statistics");
		} else {
			LOGGER.info("Calculate category statistics");
		}
		
		Map<String, ValueStatisticsBuilder<?>> statsBuilders = new HashMap<>();
		NominalValueStatisticsBuilder categoryStatsBuilder = new NominalValueStatisticsBuilder();

		if (includeValueStatistics) {
			for (FeatureInformationEntry entry : dataset.getFeatureInformation()) {
				ValueStatisticsBuilder<?> statsBuilder = createValueStatsBuilder(entry);
				if (statsBuilder != null) {
					statsBuilders.put(entry.getName(), statsBuilder);
				}
			}
		}
		
		for (Instance instance : dataset) {
			if (includeValueStatistics) {
				FeatureVector vector = instance.getVector();
				for (Entry<String, ValueStatisticsBuilder<?>> builder : statsBuilders.entrySet()) {
					Value value = vector.get(builder.getKey());
					builder.getValue().add(value);
				}
			}
			categoryStatsBuilder.add(ImmutableStringValue.valueOf(instance.getCategory()));
		}

		if (includeValueStatistics) {
			Map<String, ValueStatistics> valueStats = new HashMap<>();
			for (Entry<String, ValueStatisticsBuilder<?>> builder : statsBuilders.entrySet()) {
				valueStats.put(builder.getKey(), builder.getValue().create());
			}
			this.valueStats = Collections.unmodifiableMap(valueStats);
		}

		categoryStats = categoryStatsBuilder.create();
	}

	public NominalValueStatistics getCategoryStatistics() {
		if (categoryStats == null) {
			calculateStatistics(false);
		}
		return categoryStats;
	}

	@Override
	public String toString() {
		if (categoryStats == null) {
			calculateStatistics(true);
		}
		StringBuilder builder = new StringBuilder();
		List<String> featureNames = new ArrayList<>(dataset.getFeatureInformation().getFeatureNames());
		Collections.sort(featureNames);
		for (String featureName : featureNames) {
			FeatureInformationEntry entry = dataset.getFeatureInformation().getFeatureInformation(featureName);
			ValueStatistics stats = valueStats.get(entry.getName());
			builder.append(String.format(OUTPUT_FORMAT, entry.getName(), entry.getType().getSimpleName(), stats));
			builder.append('\n');
		}
		builder.append('\n');
		builder.append(String.format(OUTPUT_FORMAT, "category", "", categoryStats));
		return builder.toString();
	}

}
