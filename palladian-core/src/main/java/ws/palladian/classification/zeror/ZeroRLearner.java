package ws.palladian.classification.zeror;

import ws.palladian.core.Instance;
import ws.palladian.core.Learner;
import ws.palladian.helper.collection.CountMap;

/**
 * <p>
 * Baseline classifier which does not consider any features but just learns the class distribution during training.
 * </p>
 * 
 * @author pk
 * @see <a href="http://www.saedsayad.com/zeror.htm">ZeroR</a>
 */
public final class ZeroRLearner implements Learner<ZeroRModel> {

    @Override
    public ZeroRModel train(Iterable<? extends Instance> instances) {
        CountMap<String> categoryCounts = CountMap.create();
        for (Instance trainingInstance : instances) {
            categoryCounts.add(trainingInstance.getCategory());
        }
        return new ZeroRModel(categoryCounts);
    }

}
