package ws.palladian.core;

import ws.palladian.core.value.BooleanValue;
import ws.palladian.core.value.NominalValue;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.Vector;
import java.util.function.Predicate;

public interface FeatureVector extends Vector<String, Value> {

	/**
	 * Get a nominal value.
	 * 
	 * @param key
	 *            The key of the value to get.
	 * @return The nominal value, never <code>null</code>.
	 * @throws ClassCastException
	 *             in case the value is not nominal
	 */
	NominalValue getNominal(String key);

	/**
	 * Get a numeric value.
	 * 
	 * @param key
	 *            The key of the value to get.
	 * @return The numeric value, never <code>null</code>.
	 * @throws ClassCastException
	 *             in case the value is not nominal
	 */
	NumericValue getNumeric(String key);

	/**
	 * Get a boolean value.
	 * 
	 * @param key
	 *            The key of the value to get.
	 * @return The numeric value, never <code>null</code>.
	 * @throws ClassCastException
	 *             in case the value is not boolean
	 */
	BooleanValue getBoolean(String key);
	
	// TODO add this in the future?
	// Iterator<VectorEntry<String, Value>> iteratorNonNull();
	
	/**
	 * Get a filtered vector which only contains the values accepted by the
	 * filter.
	 * 
	 * @param nameFilter
	 *            The filter, not <code>null</code>.
	 * @return A filtered version of this vector.
	 */
	FeatureVector filter(Predicate<? super String> nameFilter);

}
