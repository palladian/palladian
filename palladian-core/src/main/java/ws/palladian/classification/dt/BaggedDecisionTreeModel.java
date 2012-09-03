package ws.palladian.classification.dt;

import java.util.List;

import ws.palladian.classification.Model;

public class BaggedDecisionTreeModel implements Model {

    private List<DecisionTreeModel> classifiers;

    public BaggedDecisionTreeModel(List<DecisionTreeModel> classifiers) {
        this.classifiers = classifiers;
    }
    
    public List<DecisionTreeModel> getClassifiers() {
        return classifiers;
    }

}
