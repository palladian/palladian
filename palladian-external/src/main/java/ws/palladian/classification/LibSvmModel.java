/**
 * Created on: 14.12.2012 08:11:23
 */
package ws.palladian.classification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import libsvm.svm_model;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.utils.DummyVariableCreator;
import ws.palladian.classification.utils.Normalization;
import ws.palladian.core.Model;

/**
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @version 1.0
 * @since 0.2.0
 */
public final class LibSvmModel implements Model {

    private static final long serialVersionUID = 3l;

    private final svm_model model;

    /**
     * <p>
     * A mapping of features to indices as used within the training set. This is necessary to assign the correct indices
     * to the correct features during classification.
     * </p>
     */
    private final List<String> schema;

    private final List<String> classes;

    private final Normalization normalization;

    private final DummyVariableCreator dummyCoder;

    /* To be instantiated from within the package only. */
    LibSvmModel(svm_model model, List<String> schema, List<String> classes, Normalization normalization,
            DummyVariableCreator dummyCoder) {
        Validate.notNull(model);
        Validate.notNull(schema);
        Validate.notNull(classes);
        Validate.notNull(normalization);

        this.model = model;
        this.schema = new ArrayList<String>(schema);
        this.classes = new ArrayList<String>(classes);
        this.normalization = normalization;
        this.dummyCoder = dummyCoder;
    }

    public svm_model getModel() {
        return model;
    }

    public List<String> getSchema() {
        return Collections.unmodifiableList(schema);
    }

    public Normalization getNormalization() {
        return normalization;
    }

    public String transformClassToString(int classIndex) {
        return classes.get(classIndex);
    }

    @Override
    public Set<String> getCategories() {
        return new HashSet<String>(classes);
    }

    public DummyVariableCreator getDummyCoder() {
        return dummyCoder;
    }

}
