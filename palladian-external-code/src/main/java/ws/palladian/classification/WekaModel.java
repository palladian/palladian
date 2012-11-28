package ws.palladian.classification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
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
    private final List<String> normalFeaturePaths;
    private final List<String> sparseFeaturePaths;

    public WekaModel(Classifier classifier, Instances data, List<String> normalFeaturePaths,
            List<String> sparseFeaturePaths) {
        this.classifier = classifier;
        Enumeration<?> schema = data.enumerateAttributes();
        this.schema = new HashMap<String, Attribute>();
        while (schema.hasMoreElements()) {
            Attribute attribute = (Attribute)schema.nextElement();
            this.schema.put(attribute.name(), attribute);
        }
        this.dataset = data;
        this.normalFeaturePaths = new ArrayList<String>(normalFeaturePaths);
        this.sparseFeaturePaths = new ArrayList<String>(sparseFeaturePaths);
    }

    public Classifier getClassifier() {
        return classifier;
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @return
     */
    public Map<String, Attribute> getSchema() {
        return schema;
    }

    /**
     * @return the dataset
     */
    public Instances getDataset() {
        return dataset;
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @return
     */
    public List<String> getNormalFeaturePaths() {
        return Collections.unmodifiableList(normalFeaturePaths);
    }

    public List<String> getSparseFeaturePaths() {
        return Collections.unmodifiableList(sparseFeaturePaths);
    }

}
