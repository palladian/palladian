package ws.palladian.classification.dt;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Set;

import quickdt.Tree;
import ws.palladian.classification.Model;

public class DecisionTreeModel implements Model {

    private static final long serialVersionUID = 2L;

    private final Tree tree;

    private final Set<String> classes;

    DecisionTreeModel(Tree tree, Set<String> classes) {
        this.tree = tree;
        this.classes = classes;
    }

    public Tree getTree() {
        return tree;
    }

    public Set<String> getClasses() {
        return classes;
    }

    @Override
    public String toString() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);
        tree.node.dump(printStream);
        return out.toString();
    }

    @Override
    public Set<String> getCategories() {
        throw new UnsupportedOperationException("Not supported, migrate to QuickDtClassifier.");
    }

}
