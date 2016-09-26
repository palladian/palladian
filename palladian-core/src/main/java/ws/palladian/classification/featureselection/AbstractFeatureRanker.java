package ws.palladian.classification.featureselection;

import java.util.Collection;

import ws.palladian.core.Instance;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.DefaultDataset;
import ws.palladian.core.dataset.split.RandomSplit;
import ws.palladian.helper.NoProgress;
import ws.palladian.helper.ProgressReporter;

public abstract class AbstractFeatureRanker implements FeatureRanker {

    @SuppressWarnings("deprecation")
	@Override
    public FeatureRanking rankFeatures(Collection<? extends Instance> dataset) {
        return rankFeatures(dataset, NoProgress.INSTANCE);
    }

    @SuppressWarnings("deprecation")
	@Override
    public FeatureRanking rankFeatures(Collection<? extends Instance> dataset, ProgressReporter progress) {
    	return rankFeatures(new DefaultDataset(dataset), progress);
    }
    
	@Override
	public FeatureRanking rankFeatures(Dataset dataset) {
		return rankFeatures(dataset, NoProgress.INSTANCE);
	}
	
	@Override
	public FeatureRanking rankFeatures(Dataset trainSet, Dataset validationSet) {
		return rankFeatures(trainSet, validationSet, NoProgress.INSTANCE);
	}
	
	@Override
	public FeatureRanking rankFeatures(Dataset trainSet, Dataset validationSet, ProgressReporter progress) {
		return rankFeatures(trainSet, progress);
	}

    @Override
    public FeatureRanking rankFeatures(Dataset dataset, ProgressReporter progress) {
    	RandomSplit split = new RandomSplit(dataset, 0.5);
        return rankFeatures(split.getTrain(), split.getTest());
    }

}
