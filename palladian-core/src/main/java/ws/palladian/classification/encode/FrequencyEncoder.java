package ws.palladian.classification.encode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.core.AppendedVector;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.AbstractDatasetFeatureVectorTransformer;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformationBuilder;
import ws.palladian.core.dataset.statistics.DatasetStatistics;
import ws.palladian.core.dataset.statistics.NominalValueStatistics;
import ws.palladian.core.value.ImmutableFloatValue;
import ws.palladian.core.value.NominalValue;
import ws.palladian.core.value.Value;

import java.util.Set;

import static ws.palladian.helper.functional.Predicates.equal;

/**
 * "Frequency encoding" to transform nominal values to numeric ones. For each
 * nominal feature, the frequency the nominal value in the reference dataset is
 * assigned.
 *
 * @author Philipp Katz
 */
public class FrequencyEncoder extends AbstractDatasetFeatureVectorTransformer {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FrequencyEncoder.class);

    /** Describes how to handle null values. */
    public static enum NullValueStrategy {
        /** Null values to be transformed remain null. */
        KEEP_NULL,
        /** Assign the frequency of null values in the reference data. */
        ASSIGN_FREQUENCY;
    }

    private static final String FEATURE_SUFFIX = "_frequency";

    private final DatasetStatistics statistics;

    private final Set<String> nominalValueNames;

    private final long totalCount;

    private final NullValueStrategy nullValueStrategy;

    public FrequencyEncoder(Dataset dataset) {
        this(dataset, NullValueStrategy.KEEP_NULL);
    }

    public FrequencyEncoder(Dataset dataset, NullValueStrategy nullValueStrategy) {
        LOGGER.info("Start initializing FrequencyEncoder");
        nominalValueNames = dataset.getFeatureInformation().getFeatureNamesOfType(NominalValue.class);
        statistics = new DatasetStatistics(dataset.filterFeatures(equal(nominalValueNames)));
        totalCount = dataset.size();
        this.nullValueStrategy = nullValueStrategy;
    }

    @Override
    public FeatureInformation getFeatureInformation(FeatureInformation featureInformation) {
        FeatureInformationBuilder builder = new FeatureInformationBuilder();
        builder.add(featureInformation);
        for (String valueName : nominalValueNames) {
            builder.set(valueName + FEATURE_SUFFIX, ImmutableFloatValue.class);
        }
        return builder.create();
    }

    @Override
    public FeatureVector apply(FeatureVector featureVector) {
        InstanceBuilder builder = new InstanceBuilder();
        for (String valueName : nominalValueNames) {
            Value value = featureVector.get(valueName);
            NominalValueStatistics valueStats = (NominalValueStatistics) statistics.getValueStatistics(valueName);
            if (!value.isNull()) {
                int count = valueStats.getCount(((NominalValue) value).getString());
                float frequency = (float) count / totalCount;
                builder.set(valueName + FEATURE_SUFFIX, frequency);
            } else if (nullValueStrategy == NullValueStrategy.ASSIGN_FREQUENCY) {
                int count = valueStats.getNumNullValues();
                float frequency = (float) count / totalCount;
                builder.set(valueName + FEATURE_SUFFIX, frequency);
            } else if (nullValueStrategy == NullValueStrategy.KEEP_NULL) {
                builder.setNull(valueName + FEATURE_SUFFIX);
            }
        }
        return new AppendedVector(featureVector, builder.create());
    }

}
