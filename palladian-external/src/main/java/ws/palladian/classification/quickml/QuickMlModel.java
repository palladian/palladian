package ws.palladian.classification.quickml;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import quickml.supervised.classifier.Classifier;
import quickml.supervised.classifier.decisionTree.Tree;
import quickml.supervised.classifier.randomForest.RandomForest;
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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);
        // XXX should go into the predictive model
        if (classifier instanceof Tree) {
            Tree tree = (Tree)classifier;
            tree.node.dump(printStream);
        } else if (classifier instanceof RandomForest) {
            RandomForest forest = (RandomForest)classifier;
            int i = 1;
            for (Tree tree : forest.trees) {
                printStream.println("Tree #" + i++);
                printStream.println();
                tree.node.dump(printStream);
                printStream.println(StringUtils.repeat('-', 100));
            }
        }
        return out.toString();
    }

}
