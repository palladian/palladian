package ws.palladian.extraction.feature;

import java.util.Iterator;

import ws.palladian.core.AbstractFeatureVector;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.ImmutableFeatureVectorEntry;
import ws.palladian.core.dataset.AbstractDatasetFeatureVectorTransformer;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.value.ImmutableIntegerValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.AbstractIterator2;

public class NullValueReplacer extends AbstractDatasetFeatureVectorTransformer {

	@Override
	public FeatureInformation getFeatureInformation(FeatureInformation featureInformation) {
		return featureInformation;
	}

	@Override
	public FeatureVector compute(FeatureVector featureVector) {
		return new AbstractFeatureVector() {
			Iterator<VectorEntry<String, Value>> iterator = featureVector.iterator();
			@Override
			public Iterator<VectorEntry<String, Value>> iterator() {
				return new AbstractIterator2<VectorEntry<String, Value>>() {
					@Override
					protected VectorEntry<String, Value> getNext() {
						if (iterator.hasNext()) {
							VectorEntry<String, Value> current = iterator.next();
							Value value = current.value();
							if (value.isNull()) {
//								System.out.println(current.key() + " is null and replaced with zero");
								value = ImmutableIntegerValue.valueOf(-1);
//								value = ImmutableIntegerValue.valueOf(-999);
							}
							return new ImmutableFeatureVectorEntry(current.key(), value);
						}
						return finished();
					}
				};
			}
		};
	}

}
