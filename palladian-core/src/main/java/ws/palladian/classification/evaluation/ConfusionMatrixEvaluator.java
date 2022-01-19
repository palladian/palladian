package ws.palladian.classification.evaluation;

import org.apache.commons.lang3.StringUtils;
import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.core.Classifier;
import ws.palladian.core.Model;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.helper.math.ConfusionMatrix;

import java.util.TreeSet;

public class ConfusionMatrixEvaluator extends AbstractClassificationEvaluator<ConfusionMatrix> {
    @SuppressWarnings({"unchecked", "deprecation"})
    @Override
    public <M extends Model> ConfusionMatrix evaluate(Classifier<M> classifier, M model, Dataset data) {
        return ClassifierEvaluation.evaluate(classifier, data, model);
    }

    @Override
    public String getCsvHeader(ConfusionMatrix result) {
        TreeSet<String> categoryNames = new TreeSet<>(result.getCategories());
        StringBuilder header = new StringBuilder("avgPr;avgRc;avgF1;accuracy;");
        for (String categoryName : categoryNames) {
            header.append("pr-").append(categoryName).append(';');
            header.append("rc-").append(categoryName).append(';');
            header.append("f1-").append(categoryName).append(';');
            header.append("acc-").append(categoryName);
        }
        return header.toString();
    }

    @SuppressWarnings("deprecation")
    @Override
    public String getCsvLine(ConfusionMatrix result) {
        StringBuilder resultLine = new StringBuilder();
        resultLine.append(result.getAveragePrecision(true)).append(';');
        resultLine.append(result.getAverageRecall(true)).append(';');
        resultLine.append(result.getAverageF(1, true)).append(';');
        resultLine.append(result.getAverageAccuracy(true)).append(';');
        // precision, recall, f1 for each individual classes
        TreeSet<String> categoryNames = new TreeSet<String>(result.getCategories());
        for (String categoryName : categoryNames) {
            double pr = result.getPrecision(categoryName);
            double f1 = result.getF(1, categoryName);
            resultLine.append(Double.isNaN(pr) ? StringUtils.EMPTY : pr).append(';');
            resultLine.append(result.getRecall(categoryName)).append(';');
            resultLine.append(Double.isNaN(f1) ? StringUtils.EMPTY : f1).append(';');
            resultLine.append(result.getAccuracy(categoryName));
        }
        return resultLine.toString();
    }
}
