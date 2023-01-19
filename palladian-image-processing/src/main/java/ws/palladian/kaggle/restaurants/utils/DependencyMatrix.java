package ws.palladian.kaggle.restaurants.utils;

import ws.palladian.helper.collection.Matrix;
import ws.palladian.kaggle.restaurants.dataset.Label;

/**
 * Wrap matrix.
 */
public class DependencyMatrix {

    private Matrix<Integer, Double> matrix;

    public DependencyMatrix(Matrix<Integer, Double> matrix) {
        this.matrix = matrix;
    }

    public Double getPrior(Label label) {
        return getPrior(label.getLabelId());
    }

    public Double getPrior(int labelId) {
        return matrix.get(labelId, labelId);
    }

    public Double getDependency(Label fromLabel, Label toLabel) {
        return matrix.get(toLabel.getLabelId(), fromLabel.getLabelId());
    }
}
