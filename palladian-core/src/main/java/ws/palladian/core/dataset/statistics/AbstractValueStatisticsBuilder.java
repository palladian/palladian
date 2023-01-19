package ws.palladian.core.dataset.statistics;

import org.apache.commons.lang3.Validate;
import ws.palladian.core.dataset.statistics.DatasetStatistics.ValueStatistics;
import ws.palladian.core.dataset.statistics.DatasetStatistics.ValueStatisticsBuilder;
import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.Value;

/**
 * Base implementation for {@link ValueStatisticsBuilder}s. Takes care of
 * checking for valid type and counting null values.
 *
 * @param <V>
 * @param <S>
 * @author Philipp Katz
 */
public abstract class AbstractValueStatisticsBuilder<V extends Value, S extends ValueStatistics> implements ValueStatisticsBuilder<S> {
    private final Class<V> valueType;
    private int numNullValues;

    protected AbstractValueStatisticsBuilder(Class<V> valueType) {
        Validate.notNull(valueType, "valueType must not be null");
        this.valueType = valueType;
    }

    @Override
    public final void add(Value value) {
        if (value instanceof NullValue) {
            numNullValues++;
        } else if (valueType.isInstance(value)) {
            addValue(valueType.cast(value));
        } else {
            throw new IllegalArgumentException("Expected value to be of type " + valueType + ", but was " + value.getClass());
        }
    }

    protected abstract void addValue(V value);

    protected final int getNumNullValues() {
        return numNullValues;
    }

}
