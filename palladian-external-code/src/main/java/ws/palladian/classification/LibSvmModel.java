/**
 * Created on: 14.12.2012 08:11:23
 */
package ws.palladian.classification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import libsvm.svm_model;

/**
 * <p>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.0.2
 */
public final class LibSvmModel implements Model {
    /**
     * <p>
     * 
     * </p>
     */
    private static final long serialVersionUID = 9087669834591504394L;
    private final svm_model model;
    private final List<String> normalFeaturePaths;
    private final List<String> sparseFeaturePaths;
    /**
     * <p>
     * A mapping of features to indices as used within the training set. This is necessary to assign the correct indices
     * to the correct features during classification.
     * </p>
     */
    private final Map<String, Integer> schema;
    private final List<String> classes;

    /**
     * <p>
     * 
     * </p>
     * 
     * @param model
     * @param sparseFeaturePaths
     * @param normalFeaturePaths
     */
    public LibSvmModel(svm_model model, List<String> normalFeaturePaths, List<String> sparseFeaturePaths,
            Map<String, Integer> schema, List<String> classes) {
        super();

        this.model = model;
        this.normalFeaturePaths = new ArrayList<String>(normalFeaturePaths);
        this.sparseFeaturePaths = new ArrayList<String>(sparseFeaturePaths);
        this.schema = new HashMap<String, Integer>();
        this.schema.putAll(schema);
        this.classes = new ArrayList<String>(classes);
    }

    /**
     * @return the model
     */
    public svm_model getModel() {
        return model;
    }

    /**
     * @return the normalFeaturePaths
     */
    public List<String> getNormalFeaturePaths() {
        return normalFeaturePaths;
    }

    /**
     * @return the sparseFeaturePaths
     */
    public List<String> getSparseFeaturePaths() {
        return sparseFeaturePaths;
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @return
     */
    public Map<String, Integer> getSchema() {
        return Collections.unmodifiableMap(schema);
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param classIndex
     * @return
     */
    public String transformClassToString(int classIndex) {
        return classes.get(classIndex);
    }

}
