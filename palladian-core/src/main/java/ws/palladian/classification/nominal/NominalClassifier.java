package ws.palladian.classification.nominal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Instance;
import ws.palladian.classification.text.evaluation.Dataset;
import ws.palladian.helper.collection.CountMap2D;
import ws.palladian.helper.collection.Factory;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;

/**
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class NominalClassifier implements Classifier<NominalClassifierModel> {

    @Override
    public NominalClassifierModel train(List<Instance> instances) {

        CountMap2D<String> cooccurrenceMatrix = CountMap2D.create();

        for (Instance instance : instances) {
            String className = instance.getTargetClass();
            List<NominalFeature> nominalFeatures = instance.getFeatureVector().getAll(NominalFeature.class);
            for (NominalFeature nominalFeature : nominalFeatures) {
                cooccurrenceMatrix.increment(nominalFeature.getValue(), className);
            }
        }

        return new NominalClassifierModel(cooccurrenceMatrix);
    }

    @Override
    public NominalClassifierModel train(Dataset dataset) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CategoryEntries classify(FeatureVector vector, NominalClassifierModel model) {

        CountMap2D<String> cooccurrenceMatrix = model.getCooccurrenceMatrix();

        // category-probability map, initialized with zeros
        Map<String, Double> scores = LazyMap.create(new Factory<Double>() {
            @Override
            public Double create() {
                return 0.;
            }
        });

        // category names
        Set<String> categories = cooccurrenceMatrix.keySet();

        for (NominalFeature nominalFeature : vector.getAll(NominalFeature.class)) {

            for (String category : categories) {

                String featureValue = nominalFeature.getValue();
                int cooccurrences = cooccurrenceMatrix.getCount(featureValue, category);
                int rowSum = cooccurrenceMatrix.getColumnSum(featureValue);

                double score = (double)cooccurrences / rowSum;
                scores.put(category, scores.get(category) + score);
            }

        }

        // create category entries
        CategoryEntries assignedEntries = new CategoryEntries();
        for (String category : categories) {
            assignedEntries.add(new CategoryEntry(category, scores.get(category)));
        }

        return assignedEntries;

    }

}