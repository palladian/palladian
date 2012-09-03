package ws.palladian.classification.dt;

import quickdt.Node;
import ws.palladian.classification.Model;

public class DecisionTreeModel implements Model {

    private final Node tree;

    public DecisionTreeModel(Node tree) {
        this.tree = tree;
    }
    
    public Node getTree() {
        return tree;
    }

}
