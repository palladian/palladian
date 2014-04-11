package ws.palladian.classification.nominal;

import java.util.Set;

import ws.palladian.classification.CategoryEntriesBuilder;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Classifier;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.Learner;
import ws.palladian.core.NominalValue;
import ws.palladian.core.Value;
import ws.palladian.helper.collection.CountMatrix;
import ws.palladian.helper.collection.Vector.VectorEntry;

/**
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class NominalClassifier implements Learner<NominalClassifierModel>, Classifier<NominalClassifierModel> {

    @Override
    public NominalClassifierModel train(Iterable<? extends Instance> instances) {

        CountMatrix<String> cooccurrenceMatrix = CountMatrix.create();

        for (Instance instance : instances) {
            String categoryName = instance.getCategory();
            FeatureVector featureVector = instance.getVector();
            for (VectorEntry<String, Value> entry : featureVector) {
                Value value = entry.value();
                if (value instanceof NominalValue) {
                    cooccurrenceMatrix.add(categoryName, ((NominalValue)value).getString());
                }
            }
        }

        return new NominalClassifierModel(cooccurrenceMatrix);
    }

    @Override
    public CategoryEntries classify(FeatureVector featureVector, NominalClassifierModel model) {

        CountMatrix<String> cooccurrenceMatrix = model.getCooccurrenceMatrix();

        // category-probability map, initialized with zeros
        CategoryEntriesBuilder builder = new CategoryEntriesBuilder();

        // category names
        Set<String> categories = cooccurrenceMatrix.getColumnKeys();

        for (VectorEntry<String, Value> entry : featureVector) {
            Value value = entry.value();
            if (value instanceof NominalValue) {
                String nominalValue = ((NominalValue)value).getString();
                for (String category : categories) {
                    int cooccurrences = cooccurrenceMatrix.getCount(category, nominalValue);
                    int rowSum = cooccurrenceMatrix.getRow(nominalValue).getSum();
                    double score = (double)cooccurrences / rowSum;
                    builder.add(category, score);
                }
            }
        }
        return builder.create();
    }

}