/**
 * 
 */
package ws.palladian.classification.universal;

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

    protected CategoryEntries evaluateResults(Instance instance, UniversalClassificationResult result, UniversalClassifierModel model) {
        CategoryEntries mergedCategoryEntries = new CategoryEntries();

        CategoryEntries textCategories = result.getTextCategories();
        if (model.getTextClassifier()!=null
                && textCategories.getMostLikelyCategoryEntry().getCategory().getName().equals(instance.targetClass)) {
            countCorrectlyClassified(0,instance);
            mergedCategoryEntries.addAllRelative(textCategories);
        }
        CategoryEntries numericResults = result.getNumericResults();
        if (model.getKnnModel()!=null
                && numericResults.getMostLikelyCategoryEntry().getCategory().getName().equals(instance.targetClass)) {
            countCorrectlyClassified(1,instance);
            mergedCategoryEntries.addAllRelative(numericResults);
        }
        CategoryEntries nominalInstance = result.getNominalResults();
        if (model.getBayesModel()!=null
                && nominalInstance.getMostLikelyCategoryEntry().getCategory().getName().equals(instance.targetClass)) {
            countCorrectlyClassified(2,instance);
            mergedCategoryEntries.addAllRelative(nominalInstance);
        }

        return mergedCategoryEntries;
    }
    
    protected abstract void countCorrectlyClassified(int index, Instance instance);
}
