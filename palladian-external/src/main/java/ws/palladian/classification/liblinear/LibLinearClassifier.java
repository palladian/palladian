package ws.palladian.classification.liblinear;

import java.io.PrintStream;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.bwaldvogel.liblinear.Linear;
import ws.palladian.core.AbstractClassifier;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.CategoryEntriesBuilder;
import ws.palladian.core.FeatureVector;
import ws.palladian.helper.io.Slf4JOutputStream;
import ws.palladian.helper.io.Slf4JOutputStream.Level;

/**
 * <p>
 * Classifier for models created via {@link LibLinearLearner}.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class LibLinearClassifier extends AbstractClassifier<LibLinearModel> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LibLinearClassifier.class);

    static {
        // redirect debug output to logger.
        Linear.setDebugOutput(new PrintStream(new Slf4JOutputStream(LOGGER, Level.DEBUG)));
    }

    @Override
    public CategoryEntries classify(FeatureVector featureVector, LibLinearModel model) {
        Validate.notNull(featureVector, "featureVector must not be null");
        Validate.notNull(model, "model must not be null");
        featureVector = model.getNormalization().normalize(featureVector);
        featureVector = model.getDummyCoder().convert(featureVector);
        de.bwaldvogel.liblinear.Feature[] instance = LibLinearLearner.makeInstance(model.getFeatureLabelIndices(),
                featureVector, model.getLLModel().getBias());
        CategoryEntriesBuilder categoryEntriesBuilder = new CategoryEntriesBuilder();
        if (model.getLLModel().isProbabilityModel()) {
            double[] probabilities = new double[model.getCategories().size()];
            Linear.predictProbability(model.getLLModel(), instance, probabilities);
            for (int i = 0; i < probabilities.length; i++) {
                categoryEntriesBuilder.add(model.getCategoryForIndex(i), probabilities[i]);
            }
        } else {
            int classIdx = (int)Linear.predict(model.getLLModel(), instance);
            categoryEntriesBuilder.set(model.getCategories(), 0.);
            categoryEntriesBuilder.add(model.getCategoryForIndex(classIdx), 1.);
        }
        return categoryEntriesBuilder.create();
    }

}
