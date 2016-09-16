package ws.palladian.core;

import ws.palladian.core.value.NominalValue;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.Vector;

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
	 * @param key
	 *            The key of the value to get.
	 * @return The numeric value, never <code>null</code>.
	 * @throws ClassCastException
	 *             in case the value is not nominal
	 */
	NumericValue getNumeric(String key);

}
