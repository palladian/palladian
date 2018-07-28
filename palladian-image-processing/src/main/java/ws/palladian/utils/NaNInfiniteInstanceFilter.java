package ws.palladian.utils;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Vector.VectorEntry;
import ws.palladian.helper.functional.Filter;

/**
 * Filters instances which have Infinity/NaN values, which cause trouble to the
 * KNN classifier (and potentially others as well).
 */
public enum NaNInfiniteInstanceFilter implements Filter<Instance> {
	FILTER;

	/** The logger for this class. */
	private static final Logger LOGGER = LoggerFactory.getLogger(NaNInfiniteInstanceFilter.class);

	@Override
	public boolean accept(Instance item) {
		FeatureVector featureVector = item.getVector();
		for (VectorEntry<String, Value> entry : featureVector) {
			Value value = entry.value();
			if (value instanceof NumericValue) {
				double doubleValue = ((NumericValue) value).getDouble();
				if (Double.isNaN(doubleValue)) {
					LOGGER.warn("Skipping instance with NaN @ {}", entry.key());
					return false;
				}
				if (Double.isInfinite(doubleValue)) {
					LOGGER.warn("Skipping instance with Infinity @ {}", entry.key());
					return false;
				}
			}
		}
		return true;
	}
	
	public static void main(String[] args) {
		@SuppressWarnings("deprecation")
		Iterable<Instance> instances = new CsvDatasetReader(new File("/Users/pk/Desktop/tmp/yelp_features_medium_test_1455386873613.csv"));
		instances = CollectionHelper.filter(instances, FILTER);
		for (Instance instance : instances) {
			assert instance != null;
		}
	}
}
