package ws.palladian.kaggle.restaurants.classifier;

import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSettingBuilder;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.classification.text.PalladianTextClassifier.Scorer;
import ws.palladian.core.AbstractLearner;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Classifier;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Vector.VectorEntry;

/**
 * Wrapper for the {@link PalladianTextClassifier} for classifying image BOWs.
 * 
 * @author pk
 */
public class PalladianDictionaryClassifier extends AbstractLearner<DictionaryModel> implements Classifier<DictionaryModel> {

	/**
	 * It makes no sense to do any customizations here, we simply concatenate
	 * all SIFT features, separated by spaces.
	 */
	private static final FeatureSetting FEATURE_SETTING = FeatureSettingBuilder.words().create();

	private final PalladianTextClassifier textClassifier;

	private final Scorer scorer;

	public PalladianDictionaryClassifier() {
		this(new PalladianTextClassifier.DefaultScorer());
	}

	public PalladianDictionaryClassifier(Scorer scorer) {
		this.textClassifier = new PalladianTextClassifier(FEATURE_SETTING, scorer);
		this.scorer = scorer;
	}

	@Override
	public CategoryEntries classify(FeatureVector featureVector, DictionaryModel model) {
		String text = convertToTextFeatureVector(featureVector);
		return textClassifier.classify(text, model);
	}

	@Override
	public DictionaryModel train(Dataset dataset) {
		return textClassifier.train(convertToTextInstances(dataset));
	}

	private Iterable<Instance> convertToTextInstances(Iterable<? extends Instance> instances) {
		return CollectionHelper.convert(instances, input -> {
			String text = convertToTextFeatureVector(input.getVector());
			return new InstanceBuilder().setText(text).create(input.getCategory());
		});
	}

	private static String convertToTextFeatureVector(FeatureVector vector) {
		StringBuilder dummyText = new StringBuilder();
		for (VectorEntry<String, Value> entry : vector) {
			if (entry.value() instanceof NumericValue) {
				// XXX introduce "BagOfWordsValue"?
				NumericValue count = (NumericValue) entry.value();
				for (long i = 0; i < count.getLong(); i++) {
					dummyText.append(entry.key());
					dummyText.append(" ");
				}
			}
		}
		return dummyText.toString();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " (" + scorer + ")";
	}

}
