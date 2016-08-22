package ws.palladian.core.dataset.statistics;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import ws.palladian.core.value.NominalValue;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper.Order;

public class NominalValueStatistics implements DatasetStatistics.ValueStatistics {

	public static class NominalValueStatisticsBuilder
			extends AbstractValueStatisticsBuilder<NominalValue, NominalValueStatistics> {
		private final Bag<String> stats = new Bag<>();

		public NominalValueStatisticsBuilder() {
			super(NominalValue.class);
		}

		@Override
		public NominalValueStatistics create() {
			return new NominalValueStatistics(this);
		}

		@Override
		protected void addValue(NominalValue nominalValue) {
			stats.add(nominalValue.getString());
		}
	}

	private static final int MAX_PRINTABLE_VALUES = 10;

	private final int numNullValues;
	private final Bag<String> stats;

	protected NominalValueStatistics(NominalValueStatisticsBuilder builder) {
		numNullValues = builder.getNumNullValues();
		stats = builder.stats.createSorted(Order.DESCENDING);
	}

	@Override
	public int getNumNullValues() {
		return numNullValues;
	}

	public int getNumUniqueValues() {
		return getValues().size();
	}
	
	public int getNumUniqueValuesIncludingNull() {
		return getNumUniqueValues() + (getNumNullValues() > 0 ? 1 : 0);
	}

	public Set<String> getValues() {
		return stats.uniqueItems();
	}

	public int getCount(String value) {
		return stats.count(value);
	}
	
	public Map<String, Integer> getMap() {
		return Collections.unmodifiableMap(stats.toMap());
	}

	@Override
	public String toString() {
		if (getNumUniqueValues() <= MAX_PRINTABLE_VALUES) {
			return String.format("numUniqueValues=%s, counts=%s, numNullValues=%s", getNumUniqueValues(), stats,
					numNullValues);
		} else {
			return String.format("numUniqueValues=%s, numNullValues=%s", getNumUniqueValues(), numNullValues);
		}
	}

}
