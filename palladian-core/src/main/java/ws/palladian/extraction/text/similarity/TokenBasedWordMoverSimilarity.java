package ws.palladian.extraction.text.similarity;

import java.util.Collection;
import java.util.Iterator;

import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.Preprocessor;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.nlp.AbstractStringMetric;
import ws.palladian.helper.nlp.StringMetric;

public class TokenBasedWordMoverSimilarity extends AbstractStringMetric {

	private final FeatureSetting featureSetting;

	private final StringMetric tokenSimilarity;

	private final Preprocessor preprocessor;

	public TokenBasedWordMoverSimilarity(FeatureSetting featureSetting, StringMetric tokenSimilarity) {
		this.featureSetting = featureSetting;
		this.tokenSimilarity = tokenSimilarity;
		this.preprocessor = new Preprocessor(featureSetting);
	}

	@Override
	public double getSimilarity(String s1, String s2) {
		Collection<String> tokens1 = preprocess(s1);
		Collection<String> tokens2 = preprocess(s2);

		if (tokens1.isEmpty() && tokens2.isEmpty()) {
			return 1;
		}

		if (tokens1.size() < tokens2.size()) {
			Collection<String> temp = tokens1;
			tokens1 = tokens2;
			tokens2 = temp;
		}

		double similarity = 0;

		for (String token1 : tokens1) {
			double maxSimilarty = 0;
			for (String token2 : tokens2) {
				maxSimilarty = Math.max(maxSimilarty, tokenSimilarity.getSimilarity(token1, token2));
			}
			similarity += maxSimilarty;
		}

		return similarity / tokens1.size();

	}

	private Collection<String> preprocess(String s) {
		Iterator<String> featureIterator = preprocessor.apply(s);
		return CollectionHelper.newHashSet(featureIterator);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TokenBasedWordMoverSimilarity [featureSetting=");
		builder.append(featureSetting);
		builder.append(", tokenSimilarity=");
		builder.append(tokenSimilarity);
		builder.append("]");
		return builder.toString();
	}

}
