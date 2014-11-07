package ws.palladian.classification.liblinear;

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
//        try {
//            Writer writer = new StringWriter();
//            model.save(writer);
//            return writer.toString() + "\n" + featureLabels;
//        } catch (IOException e) {
//            throw new IllegalStateException(e);
//        }
        StringBuilder builder = new StringBuilder();
        builder.append("# classes\t").append(model.getNrClass()).append('\n');
        builder.append("# features\t").append(model.getNrFeature()).append('\n');
        builder.append("bias\t").append(model.getBias()).append('\n');
        builder.append("normalization\t").append(normalization.getClass().getSimpleName());
        builder.append("\n\n");
        if (model.getNrClass() > 2) {
            for (int i = 0; i < model.getNrClass(); i++) {
                builder.append('\t').append(classIndices.get(i));
            }
            builder.append('\n');
        }
        int nrWeights = model.getNrFeature() + (model.getBias() >= 0 ? 1 : 0);
        int nrColumns = model.getNrClass() > 2 ? model.getNrClass() : 1;
        for (int i = 0; i < nrWeights; i++) {
            String weightLabel = i < featureLabels.size() ? featureLabels.get(i) : "bias";
            builder.append(weightLabel);
            for (int j = 0; j < nrColumns; j++) {
                double weight = model.getFeatureWeights()[i * nrColumns + j];
                builder.append('\t').append(weight);
                if (model.getNrClass() == 2) {
                    break;
                }
            }
            builder.append('\n');
        }
        return builder.toString();
    }

}
