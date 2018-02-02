package ws.palladian.extraction.text.vector;

import java.util.Iterator;
import java.util.Map.Entry;

import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.Preprocessor;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.AbstractDatasetFeatureVectorTransformer;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformationBuilder;
import ws.palladian.core.value.ImmutableFloatValue;
import ws.palladian.core.value.NominalValue;
import ws.palladian.core.value.TextValue;
import ws.palladian.core.value.Value;
import ws.palladian.core.value.ValueDefinitions;
import ws.palladian.extraction.feature.MapTermCorpus;
import ws.palladian.extraction.feature.TermCorpus;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper;

public class TextVectorizer extends AbstractDatasetFeatureVectorTransformer {

	public static enum VectorizationStrategy {
		BINARY {
			@Override
			float calc(float tf, float idf) {
				return tf > 0 ? 1 : 0;
			}
		},

		TF {
			@Override
			float calc(float tf, float idf) {
				return tf;
			}
		},

		TF_IDF {
			@Override
			float calc(float tf, float idf) {
				return tf * idf;
			}
		};

		abstract float calc(float tf, float idf);
	}

	private final String inputFeatureName;
	private final Preprocessor preprocessor;
	private final TermCorpus termCorpus;
	private VectorizationStrategy strategy;

	public TextVectorizer(String inputFeatureName, FeatureSetting featureSetting, Dataset dataset,
			VectorizationStrategy strategy, int vectorSize) {
		this.inputFeatureName = inputFeatureName;
		this.preprocessor = new Preprocessor(featureSetting);

		MapTermCorpus termCorpus = new MapTermCorpus();
		for (Instance instance : dataset) {
			String text = getTextValue(instance.getVector());
			Iterator<String> tokenIterator = preprocessor.compute(text);
			termCorpus.addTermsFromDocument(CollectionHelper.newHashSet(tokenIterator));
		}
		this.termCorpus = termCorpus.getReducedCorpus(vectorSize);
		this.strategy = strategy;
	}

	@Override
	public FeatureInformation getFeatureInformation(FeatureInformation featureInformation) {
		return new FeatureInformationBuilder().set(termCorpus, ValueDefinitions.floatValue()).create();
	}

	@Override
	public FeatureVector compute(FeatureVector featureVector) {
		String text = getTextValue(featureVector);
		Iterator<String> tokenIterator = preprocessor.compute(text);
		Bag<String> tokens = new Bag<>(CollectionHelper.newArrayList(tokenIterator));

		InstanceBuilder instanceBuilder = new InstanceBuilder();
		for (Entry<String, Integer> tokenEntry : tokens.unique()) {
			String token = tokenEntry.getKey();
			Integer count = tokenEntry.getValue();
			if (count == 0) {
				continue;
			}
			float tf = (float) count / tokens.size();
			float idf = (float) termCorpus.getIdf(token, true);
			float value = strategy.calc(tf, idf);
			instanceBuilder.set(token, new ImmutableFloatValue(value));
		}
		return instanceBuilder.create();
	}

	private String getTextValue(FeatureVector featureVector) {
		Value value = featureVector.get(inputFeatureName);
		if (value instanceof NominalValue) {
			return ((NominalValue) value).getString();
		} else if (value instanceof TextValue) {
			return ((TextValue) value).getText();
		}
		throw new IllegalArgumentException("Invalid type: " + value.getClass().getName());
	}

}
