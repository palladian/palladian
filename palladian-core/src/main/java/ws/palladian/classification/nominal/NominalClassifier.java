package ws.palladian.classification.nominal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesMap;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Learner;
import ws.palladian.helper.collection.ConstantFactory;
import ws.palladian.helper.collection.CountMatrix;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.NominalFeature;

/**
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class NominalClassifier implements Learner<NominalClassifierModel>, Classifier<NominalClassifierModel> {

    @Override
    public NominalClassifierModel train(Iterable<? extends Trainable> trainables) {

        CountMatrix<String> cooccurrenceMatrix = CountMatrix.create();

        for (Trainable trainable : trainables) {
            String className = trainable.getTargetClass();
            List<NominalFeature> nominalFeatures = trainable.getFeatureVector().getAll(NominalFeature.class);
            for (NominalFeature nominalFeature : nominalFeatures) {
                cooccurrenceMatrix.add(className, nominalFeature.getValue());
            }
        }

        return new NominalClassifierModel(cooccurrenceMatrix);
    }

    @Override
    public CategoryEntries classify(Classifiable classifiable, NominalClassifierModel model) {

        CountMatrix<String> cooccurrenceMatrix = model.getCooccurrenceMatrix();

        // category-probability map, initialized with zeros
        Map<String, Double> scores = LazyMap.create(ConstantFactory.create(0.));

        // category names
        Set<String> categories = cooccurrenceMatrix.getKeysX();

        for (NominalFeature nominalFeature : classifiable.getFeatureVector().getAll(NominalFeature.class)) {

            for (String category : categories) {

                String featureValue = nominalFeature.getValue();
                int cooccurrences = cooccurrenceMatrix.getCount(category, featureValue);
                int rowSum = cooccurrenceMatrix.getRowSum(featureValue);

                double score = (double)cooccurrences / rowSum;
                scores.put(category, scores.get(category) + score);
            }

        }

        // create category entries
        CategoryEntriesMap assignedEntries = new CategoryEntriesMap();
        for (String category : categories) {
            assignedEntries.set(category, scores.get(category));
        }

        return assignedEntries;

    }

}