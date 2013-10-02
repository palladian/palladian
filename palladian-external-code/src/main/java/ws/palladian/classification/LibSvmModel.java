/**
 * Created on: 14.12.2012 08:11:23
 */
package ws.palladian.classification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import libsvm.svm_model;

import org.apache.commons.lang3.Validate;

/**
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.0
 */
public final class LibSvmModel implements Model {

    private static final long serialVersionUID = 9087669834591504394L;

    private final svm_model model;

    /**
     * <p>
     * A mapping of features to indices as used within the training set. This is necessary to assign the correct indices
     * to the correct features during classification.
     * </p>
     */
    private final Map<String, Integer> schema;

    private final List<String> classes;

    private final Map<String, Normalization> normalizations;

    public LibSvmModel(svm_model model, Map<String, Integer> schema, List<String> classes,
            Map<String, Normalization> normalizations) {
        Validate.notNull(model);
        Validate.notNull(schema);
        Validate.notNull(classes);
        Validate.notNull(normalizations);

        this.model = model;
        this.schema = new HashMap<String, Integer>();
        this.schema.putAll(schema);
        this.classes = new ArrayList<String>(classes);
        this.normalizations = normalizations;
    }

    public svm_model getModel() {
        return model;
    }

    public Map<String, Integer> getSchema() {
        return Collections.unmodifiableMap(schema);
    }

    public Map<String, Normalization> getNormalizations() {
        return Collections.unmodifiableMap(normalizations);
    }

    public String transformClassToString(int classIndex) {
        return classes.get(classIndex);
    }

    @Override
    public Set<String> getCategories() {
        return new HashSet<String>(classes);
    }

}
