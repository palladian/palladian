package ws.palladian.classification.featureselection;

import java.util.Collection;

import ws.palladian.core.Instance;
import ws.palladian.helper.NoProgress;

public abstract class AbstractFeatureRanker implements FeatureRanker {

    @Override
    public FeatureRanking rankFeatures(Collection<? extends Instance> dataset) {
        return rankFeatures(dataset, NoProgress.INSTANCE);
    }

}
