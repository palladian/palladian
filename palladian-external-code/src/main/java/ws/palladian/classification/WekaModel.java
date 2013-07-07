package ws.palladian.classification;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instances;
import ws.palladian.processing.features.FeatureVector;

/**
 * <p>
 * A Palladian model wrapping a Weka classifier and all information necessary to apply that classifier to new
 * {@link FeatureVector}s containing the same features.
 * </p>
 * 
 * @author Philipp Katz
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.8
 */
public final class WekaModel implements Model {

    private static final long serialVersionUID = 1L;

    private final Classifier classifier;
    private final Map<String, Attribute> schema;
    private final Instances dataset;

    public WekaModel(Classifier classifier, Instances data) {
        this.classifier = classifier;
        Enumeration<?> schema = data.enumerateAttributes();
        this.schema = new HashMap<String, Attribute>();
        while (schema.hasMoreElements()) {
            Attribute attribute = (Attribute)schema.nextElement();
            this.schema.put(attribute.name(), attribute);
        }
        this.dataset = data;
    }

    public Classifier getClassifier() {
        return classifier;
    }

    public Map<String, Attribute> getSchema() {
        return schema;
    }

    /**
     * @return the dataset
     */
    public Instances getDataset() {
        return dataset;
    }

}
