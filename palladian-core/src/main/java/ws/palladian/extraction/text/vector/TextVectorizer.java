package ws.palladian.extraction.text.vector;

import java.util.Iterator;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TextVectorizer.class);

	public static enum TFStrategy {
		BINARY {
			@Override
			float calc(int count, int numWordsInDoc, int maxCount) {
				return count > 0 ? 1 : 0;
			}
		},
		RAW_COUNT {
			@Override
			float calc(int count, int numWordsInDoc, int maxCount) {
				return count;
			}
		},
		TERM_FREQUENCY {
			@Override
			float calc(int count, int numWordsInDoc, int maxCount) {
				return (float) count / numWordsInDoc;
			}
		},
		LOG_NORMALIZATION {
			@Override
			float calc(int count, int numWordsInDoc, int maxCount) {
				return (float) (1 + Math.log(count));
			}
		},
		DOUBLE_NORMALIZATION {
			@Override
			float calc(int count, int numWordsInDoc, int maxCount) {
				return 0.5f + 0.5f * ((float) count / maxCount);
			}
		},

		;

		abstract float calc(int count, int numWordsInDoc, int maxCount);

	}

	public static enum IDFStrategy {
		UNARY {
			@Override
			float calc(int corpusCount, int numDocsInCorpus, int maxCount) {
				return 1;
			}
		},
		IDF {
			@Override
			float calc(int corpusCount, int numDocsInCorpus, int maxCount) {
				return (float) Math.log((float) numDocsInCorpus / corpusCount);
			}
		},
		IDF_SMOOTH {
			@Override
			float calc(int corpusCount, int numDocsInCorpus, int maxCount) {
				return (float) Math.log((float) numDocsInCorpus / (corpusCount + 1));
			}
		},
		IDF_MAX {
			@Override
			float calc(int corpusCount, int numDocsInCorpus, int maxCount) {
				return (float) Math.log((float) (maxCount * corpusCount) / (1 + corpusCount));
			}
		},

		;
		abstract float calc(int corpusCount, int numDocsInCorpus, int maxCount);
	}


	private final String inputFeatureName;
	private final FeatureSetting featureSetting;
	private final Preprocessor preprocessor;
	private final TermCorpus termCorpus;
	private final TFStrategy tfStrategy;
	private final IDFStrategy idfStrategy;
	private final int alpha;

	public TextVectorizer(String inputFeatureName, FeatureSetting featureSetting, Dataset dataset,
			TFStrategy tfStrategy, IDFStrategy idfStrategy, int vectorSize) {
		this(inputFeatureName, featureSetting, dataset, tfStrategy, idfStrategy, vectorSize, 0);
	}

	public TextVectorizer(String inputFeatureName, FeatureSetting featureSetting, Dataset dataset,
			TFStrategy tfStrategy, IDFStrategy idfStrategy, int vectorSize, int alpha) {
		this.inputFeatureName = inputFeatureName;
		this.featureSetting = featureSetting;
		preprocessor = new Preprocessor(featureSetting);

		MapTermCorpus termCorpus = new MapTermCorpus();
		for (Instance instance : dataset) {
			String text = getTextValue(instance.getVector());
			Iterator<String> tokenIterator = preprocessor.compute(text);
			termCorpus.addTermsFromDocument(CollectionHelper.newHashSet(tokenIterator));
		}
		this.termCorpus = termCorpus.getReducedCorpus(vectorSize);
		{
			int sizeBeforeReduction = termCorpus.getNumUniqueTerms();
			int sizeAfterReduction = this.termCorpus.getNumUniqueTerms();
			if (sizeAfterReduction < sizeBeforeReduction) {
				LOGGER.debug("Reduced term corpus from {} to {}", sizeBeforeReduction, sizeAfterReduction);
			}
		}
		this.tfStrategy = tfStrategy;
		this.idfStrategy = idfStrategy;
		this.alpha = alpha;
	}

	private TextVectorizer(String inputFeatureName, FeatureSetting featureSetting, TermCorpus termCorpus,
			TFStrategy tfStrategy, IDFStrategy idfStrategy, int alpha) {
		this.inputFeatureName = inputFeatureName;
		this.featureSetting = featureSetting;
		this.preprocessor = new Preprocessor(featureSetting);
		this.termCorpus = termCorpus;
		this.tfStrategy = tfStrategy;
		this.idfStrategy = idfStrategy;
		this.alpha = alpha;
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
		Entry<String, Integer> maxTokenEntry = tokens.getMax();
		Integer maxTokenCount = maxTokenEntry != null ? maxTokenEntry.getValue() : 0;
		
		// in case alpha is zero, only the document's tokens need to be iterated;
		// and zero values can simply be skipped. Else we need to iterate over the 
		// entire corpus (smoothing)
		TermCorpus vocabulary = alpha == 0 ? new MapTermCorpus(tokens, 1) : termCorpus;

		for (String token : vocabulary) {
			Integer count = tokens.count(token) + alpha;
			float tf = tfStrategy.calc(count, tokens.size(), maxTokenCount);
			float idf = idfStrategy.calc(termCorpus.getCount(token), termCorpus.getNumDocs(), maxTokenCount);
			float value = tf * idf;
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

	/**
	 * Create a copy with the same corpus and feature setting but different scoring
	 * strategies.
	 * 
	 * @param tfStrategy
	 *            The new {@link TFStrategy} to use.
	 * @param idfStrategy
	 *            The new {@link IDFStrategy} to use.
	 * @param alpha
	 *            The new alpha value to use.
	 * @return A new TextVectorizer with changed settings.
	 */
	public TextVectorizer copyWithDifferentStrategy(TFStrategy tfStrategy, IDFStrategy idfStrategy, int alpha) {
		return new TextVectorizer(inputFeatureName, featureSetting, termCorpus, tfStrategy, idfStrategy, alpha);
	}

	@Override
	public String toString() {
		return String.format("%s [%s, %s, alpha=%s, %s]", this.getClass().getSimpleName(), tfStrategy, idfStrategy,
				alpha, featureSetting);
	}

}
