package ws.palladian.core;

import ws.palladian.core.dataset.DefaultDataset;

public abstract class AbstractLearner<M extends Model> implements Learner<M> {

	@SuppressWarnings("deprecation")
	@Override
	public M train(Iterable<? extends Instance> instances) {
		return train(new DefaultDataset(instances));
	}

}
