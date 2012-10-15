/**
 * 
 */
package ws.palladian.classification.universal;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.Instance;
import ws.palladian.helper.ProgressHelper;

/**
 * @author Klemens Muthmann
 * @version 1.0.0
 * @since 0.1.8
 */
public final class InstanceWeightingStrategy extends AbstractWeightingStrategy {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceWeightingStrategy.class);
    int[] correctlyClassified = new int[3];

    @Override
    public void learnClassifierWeights(List<Instance> instances, UniversalClassifierModel model) {
        correctlyClassified[0] = 0;
        correctlyClassified[1] = 0;
        correctlyClassified[2] = 0;

        int c = 1;
        for (Instance instance : instances) {
            UniversalClassificationResult result = getClassifier().internalClassify(instance.getFeatureVector(),model);
            evaluateResults(instance,result,model);
            ProgressHelper.showProgress(c++, instances.size(), 1);
        }
        
        model.setWeights(correctlyClassified[0] / (double)instances.size(),correctlyClassified[1] / (double)instances.size(),correctlyClassified[2] / (double)instances.size());

        LOGGER.debug("weight text   : " + model.getWeights()[0]);
        LOGGER.debug("weight numeric: " + model.getWeights()[1]);
        LOGGER.debug("weight nominal: " + model.getWeights()[2]);
    }

    @Override
    protected void countCorrectlyClassified(int index, Instance instance) {
        correctlyClassified[index]++;
    }
}
