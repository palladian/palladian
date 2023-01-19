package ws.palladian.core;

import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.DefaultDataset;

public abstract class AbstractLearner<M extends Model> implements Learner<M> {

    @SuppressWarnings("deprecation")
    @Override
    public M train(Iterable<? extends Instance> instances) {
        return train(new DefaultDataset(instances));
    }

    /* The default implementation simply ignores the validation set. */
    @Override
    public M train(Dataset training, Dataset validation) {
        return train(training);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
