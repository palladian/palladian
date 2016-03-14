package ws.palladian.classification.quickml;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Set;

import quickml.supervised.classifier.Classifier;
import quickml.supervised.ensembles.randomForest.randomDecisionForest.RandomDecisionForest;
import quickml.supervised.inspection.RandomForestDumper;
import ws.palladian.core.Model;

/**
 * <p>
 * Wrapper for QuickML's predictive models.
 * </p>
 * 
 * @author Philipp Katz
 */
public class QuickMlModel implements Model {

    private static final long serialVersionUID = 1L;

    private final Classifier classifier;

    private final Set<String> classes;

    /** Package visibility, as it is to be instantiated by the QuickDtClassifier only. */
    QuickMlModel(Classifier classifier, Set<String> classes) {
        this.classifier = classifier;
        this.classes = classes;
    }

    public Classifier getClassifier() {
        return classifier;
    }

    @Override
    public Set<String> getCategories() {
        return classes;
    }

	@Override
	public String toString() {
		if (classifier instanceof RandomDecisionForest) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			PrintStream printStream = new PrintStream(out);
			new RandomForestDumper().summarizeForest(printStream, (RandomDecisionForest) classifier);
			return out.toString();
		} else {
			return classifier.toString();
		}
	}

}
