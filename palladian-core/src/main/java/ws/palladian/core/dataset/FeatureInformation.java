package ws.palladian.core.dataset;

import java.util.Set;

import ws.palladian.core.value.Value;

/**
 * Meta information about features within a {@link Dataset}. This currently
 * provides the names and types of the features.
 * 
 * @author pk
 */
public interface FeatureInformation extends Iterable<FeatureInformation.FeatureInformationEntry> {
	
	public interface FeatureInformationEntry {
		String getName();
		Class<? extends Value> getType();
	}
	
	Set<String> getFeatureNames();
	
	Set<String> getFeatureNamesOfType(Class<? extends Value> valueType);

}
