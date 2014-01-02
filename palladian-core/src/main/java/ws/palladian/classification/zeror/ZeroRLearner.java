package ws.palladian.classification.zeror;

import ws.palladian.classification.Learner;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.processing.Trainable;

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
    public ZeroRModel train(Iterable<? extends Trainable> trainables) {
        CountMap<String> categoryCounts = CountMap.create();
        for (Trainable trainingInstance : trainables) {
            categoryCounts.add(trainingInstance.getTargetClass());
        }
        return new ZeroRModel(categoryCounts);
    }

}
