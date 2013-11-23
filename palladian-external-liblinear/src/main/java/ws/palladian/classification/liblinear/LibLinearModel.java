package ws.palladian.classification.liblinear;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import ws.palladian.classification.Model;

/**
 * <p>
 * Model for the {@link LibLinear} classifier.
 * </p>
 * 
 * @author pk
 */
public class LibLinearModel implements Model {

    private static final long serialVersionUID = 1L;

    private final de.bwaldvogel.liblinear.Model model;

    private final Set<String> featureLabels;

    private final List<String> classIndices;

    public LibLinearModel(de.bwaldvogel.liblinear.Model model, Set<String> featureLabels, List<String> classIndices) {
        this.model = model;
        this.featureLabels = featureLabels;
        this.classIndices = classIndices;
    }

    de.bwaldvogel.liblinear.Model getLLModel() {
        return model;
    }

    Set<String> getFeatureLabels() {
        return Collections.unmodifiableSet(featureLabels);
    }

    @Override
    public Set<String> getCategories() {
        return Collections.unmodifiableSet(new TreeSet<String>(classIndices));
    }

    String getCategoryForIndex(int i) {
        return classIndices.get(i);
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
