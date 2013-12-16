package ws.palladian.classification.liblinear;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import ws.palladian.classification.Model;
import ws.palladian.classification.utils.DummyVariableCreator;
import ws.palladian.classification.utils.Normalization;

/**
 * <p>
 * Model for the {@link LibLinearClassifier}. Use the {@link LibLinearLearner} to train.
 * </p>
 * 
 * @author pk
 */
public class LibLinearModel implements Model {

    private static final long serialVersionUID = 2L;

    private final de.bwaldvogel.liblinear.Model model;

    private final List<String> featureLabels;

    private final List<String> classIndices;

    private final Normalization normalization;

    private final DummyVariableCreator dummyCoder;

    /** Instances are created package-internally. */
    LibLinearModel(de.bwaldvogel.liblinear.Model model, List<String> featureLabels, List<String> classIndices,
            Normalization normalization, DummyVariableCreator dummyCoder) {
        this.model = model;
        this.featureLabels = featureLabels;
        this.classIndices = classIndices;
        this.normalization = normalization;
        this.dummyCoder = dummyCoder;
    }

    de.bwaldvogel.liblinear.Model getLLModel() {
        return model;
    }

    List<String> getFeatureLabels() {
        return Collections.unmodifiableList(featureLabels);
    }

    @Override
    public Set<String> getCategories() {
        return Collections.unmodifiableSet(new TreeSet<String>(classIndices));
    }

    String getCategoryForIndex(int i) {
        return classIndices.get(i);
    }

    Normalization getNormalization() {
        return normalization;
    }

    DummyVariableCreator getDummyCoder() {
        return dummyCoder;
    }

    @Override
    public String toString() {
        try {
            Writer writer = new StringWriter();
            model.save(writer);
            return writer.toString();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
