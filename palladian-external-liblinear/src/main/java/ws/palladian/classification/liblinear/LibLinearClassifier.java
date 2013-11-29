package ws.palladian.classification.liblinear;

import java.io.PrintStream;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.bwaldvogel.liblinear.Linear;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesMap;
import ws.palladian.classification.Classifier;
import ws.palladian.helper.io.Slf4JOutputStream;
import ws.palladian.helper.io.Slf4JOutputStream.Level;
import ws.palladian.processing.Classifiable;

/**
 * <p>
 * Classifier for models created via {@link LibLinearLearner}.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class LibLinearClassifier implements Classifier<LibLinearModel> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LibLinearClassifier.class);

    static {
        // redirect debug output to logger.
        Linear.setDebugOutput(new PrintStream(new Slf4JOutputStream(LOGGER, Level.DEBUG)));
    }

    @Override
    public CategoryEntries classify(Classifiable classifiable, LibLinearModel model) {
        Validate.notNull(classifiable, "classifiable must not be null");
        Validate.notNull(model, "model must not be null");
        model.getNormalization().normalize(classifiable);
        de.bwaldvogel.liblinear.Feature[] instance = LibLinearLearner.makeInstance(model.getFeatureLabels(),
                classifiable, model.getLLModel().getBias());
        double[] probabilities = new double[model.getCategories().size()];
        Linear.predictProbability(model.getLLModel(), instance, probabilities);
        CategoryEntriesMap categoryEntries = new CategoryEntriesMap();
        for (int i = 0; i < probabilities.length; i++) {
            categoryEntries.add(model.getCategoryForIndex(i), probabilities[i]);
        }
        return categoryEntries;
    }

}
