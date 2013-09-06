package ws.palladian.classification.dt;

import java.util.List;

import ws.palladian.classification.Model;

public class BaggedDecisionTreeModel implements Model {

    private static final long serialVersionUID = 2L;
    
    private final List<DecisionTreeModel> models;

    BaggedDecisionTreeModel(List<DecisionTreeModel> models) {
        this.models = models;
    }

    public List<DecisionTreeModel> getModels() {
        return models;
    }

    @Override
    public String toString() {
        StringBuilder buildToString = new StringBuilder();
        buildToString.append("BaggedDecisionTreeClassifier").append('\n');
        buildToString.append("# classifiers: ").append(models.size()).append('\n');
        for (int i = 0; i < models.size(); i++) {
            buildToString.append("classifier ").append(i).append(":").append('\n');
            buildToString.append(models.get(i));
            buildToString.append('\n');
        }
        return buildToString.toString();
    }

}
