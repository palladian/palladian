package ws.palladian.classification.dt;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import quickdt.Node;
import ws.palladian.classification.Model;

public class DecisionTreeModel implements Model {

    private static final long serialVersionUID = 1L;
    
    private final Node tree;

    DecisionTreeModel(Node tree) {
        this.tree = tree;
    }

    public Node getTree() {
        return tree;
    }

    @Override
    public String toString() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);
        tree.dump(printStream);
        return out.toString();
    }

}
