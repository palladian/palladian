/**
 * 
 */
package ws.palladian.classification.universal;

import java.util.HashMap;
import java.util.Map;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.Instance;

/**
 * @author Klemens Muthmann
 * @version 1.0.0
 * @since 0.1.8
 */
public abstract class AbstractWeightingStrategy implements UniversalClassifierWeightStrategy {

    private UniversalClassifier classifier;

    protected void setClassifier(UniversalClassifier classifier) {
        this.classifier = classifier;
    }

    protected UniversalClassifier getClassifier() {
        return this.classifier;
    }

    protected CategoryEntries evaluateResults(Instance instance, UniversalClassificationResult result,
            UniversalClassifierModel model) {
        Map<CategoryEntries, Double> weightedCategoryEntries = new HashMap<CategoryEntries, Double>();

        // Since there are not weights yet the classifier weights all results with one.
        CategoryEntries textCategories = result.getTextCategories();
        if (model.getTextClassifier() != null
                && textCategories.getMostLikelyCategoryEntry().getName().equals(instance.getTargetClass())) {
            countCorrectlyClassified(0, instance);
            weightedCategoryEntries.put(textCategories, 1.0);
        }
        CategoryEntries numericResults = result.getNumericResults();
        if (model.getKnnModel() != null
                && numericResults.getMostLikelyCategoryEntry().getName().equals(instance.getTargetClass())) {
            countCorrectlyClassified(1, instance);
            weightedCategoryEntries.put(numericResults, 1.0);
        }
        CategoryEntries nominalInstance = result.getNominalResults();
        if (model.getBayesModel() != null
                && nominalInstance.getMostLikelyCategoryEntry().getName().equals(instance.getTargetClass())) {
            countCorrectlyClassified(2, instance);
            weightedCategoryEntries.put(nominalInstance, 1.0);
        }
        CategoryEntries mergedCategoryEntries = classifier.normalize(weightedCategoryEntries);

        return mergedCategoryEntries;
    }

    protected abstract void countCorrectlyClassified(int index, Instance instance);
}
