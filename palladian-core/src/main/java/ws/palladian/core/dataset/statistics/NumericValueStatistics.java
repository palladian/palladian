package ws.palladian.core.dataset.statistics;

import ws.palladian.core.dataset.statistics.DatasetStatistics.ValueStatistics;
import ws.palladian.core.value.NumericValue;
import ws.palladian.helper.math.SlimStats;
import ws.palladian.helper.math.Stats;

import java.util.Locale;

public class NumericValueStatistics implements ValueStatistics {

    public static class NumericValueStatisticsBuilder extends AbstractValueStatisticsBuilder<NumericValue, NumericValueStatistics> {

        private final SlimStats stats = new SlimStats();

        public NumericValueStatisticsBuilder() {
            super(NumericValue.class);
        }

        @Override
        public NumericValueStatistics create() {
            return new NumericValueStatistics(this);
        }

        @Override
        protected void addValue(NumericValue value) {
            stats.add(value.getNumber());
        }
    }

    private final int numNullValues;
    private final Stats stats;

    protected NumericValueStatistics(NumericValueStatisticsBuilder builder) {
        numNullValues = builder.getNumNullValues();
        stats = new SlimStats(builder.stats);
    }

    @Override
    public int getNumNullValues() {
        return numNullValues;
    }

    public double getMean() {
        return stats.getMean();
    }

    public double getStandardDeviation() {
        return stats.getStandardDeviation();
    }

    public double getMin() {
        return stats.getMin();
    }

    public double getMax() {
        return stats.getMax();
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "mean=%.2f, stdDev=%.2f, min=%.2f, max=%.2f, numNullValues=%s", getMean(), getStandardDeviation(), getMin(), getMax(), getNumNullValues());
    }

}
