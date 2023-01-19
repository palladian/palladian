package ws.palladian.core.dataset;

import ws.palladian.core.value.Value;

import java.util.Set;

/**
 * Meta information about features within a {@link Dataset}. This currently
 * provides the names and types of the features.
 *
 * @author Philipp Katz
 * @see FeatureInformationBuilder
 */
public interface FeatureInformation extends Iterable<FeatureInformation.FeatureInformationEntry> {

    public interface FeatureInformationEntry {
        String getName();

        Class<? extends Value> getType();

        boolean isCompatible(Class<? extends Value> type);
    }

    Set<String> getFeatureNames();

    Set<String> getFeatureNamesOfType(Class<? extends Value> valueType);

    FeatureInformationEntry getFeatureInformation(String name);

    int count();

}
