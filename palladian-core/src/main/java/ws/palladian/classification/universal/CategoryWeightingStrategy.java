/**
 * 
 */
package ws.palladian.classification.universal;

import java.util.List;

import ws.palladian.classification.Instance;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.collection.CountMap2D;

/**
 * @author Klemens Muthmann
 * @version 1.0.0
 * @since 0.1.8
 */
public final class CategoryWeightingStrategy extends AbstractWeightingStrategy {
    
    private CountMap2D correctlyClassified = new CountMap2D();

    @Override
    public void learnClassifierWeights(List<Instance> instances, UniversalClassifierModel model) {
        correctlyClassified.clear();

        int c = 1;
        for (Instance instance : instances) {
            UniversalClassificationResult result = getClassifier().internalClassify(instance.getFeatureVector(),model);
            evaluateResults(instance,result,model);
            ProgressHelper.showProgress(c++, instances.size(), 1);
        }
    }
    
    @Override
    protected void countCorrectlyClassified(int index, Instance instance) {
        correctlyClassified.increment(String.valueOf(index), instance.targetClass);
    }

}
