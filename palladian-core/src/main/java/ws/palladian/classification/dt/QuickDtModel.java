package ws.palladian.classification.dt;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import quickdt.PredictiveModel;
import quickdt.Tree;
import quickdt.randomForest.RandomForest;
import ws.palladian.classification.Model;

/**
 * <p>
 * Wrapper for quickdt's predictive models.
 * </p>
 * 
 * @author Philipp Katz
 */
public class QuickDtModel implements Model {

    private static final long serialVersionUID = 1L;

    private final PredictiveModel model;

    private final Set<String> classes;

    /** Package visibility, as it is to be instantiated by the QuickDtClassifier only. */
    QuickDtModel(PredictiveModel tree, Set<String> classes) {
        this.model = tree;
        this.classes = classes;
    }

    public PredictiveModel getModel() {
        return model;
    }

    public Set<String> getClasses() {
        return classes;
    }

    @Override
    public String toString() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);
        // XXX should go into the predictive model
        if (model instanceof Tree) {
            Tree tree = (Tree)model;
            tree.node.dump(printStream);
        } else if (model instanceof RandomForest) {
            RandomForest forest = (RandomForest)model;
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
