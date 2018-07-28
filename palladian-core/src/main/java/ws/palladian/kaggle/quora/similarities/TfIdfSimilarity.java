package ws.palladian.kaggle.quora.similarities;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.Preprocessor;
import ws.palladian.core.Instance;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.extraction.feature.MapTermCorpus;
import ws.palladian.extraction.feature.TermCorpus;
import ws.palladian.extraction.text.vector.FloatVectorUtil;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.nlp.AbstractStringMetric;

public class TfIdfSimilarity extends AbstractStringMetric {

	private final FeatureSetting featureSetting;

	private final TermCorpus termCorpus;

	private final Preprocessor preprocessor;

	private final boolean binarizeTermCount;

	public TfIdfSimilarity(FeatureSetting featureSetting, TermCorpus termCorpus, boolean binarizeTermCount) {
		this.featureSetting = featureSetting;
		this.termCorpus = termCorpus;
		this.preprocessor = new Preprocessor(featureSetting);
		this.binarizeTermCount = binarizeTermCount;
	}

	@Override
	public double getSimilarity(String i1, String i2) {

		List<String> t1 = CollectionHelper.newArrayList(preprocessor.compute(i1));
		List<String> t2 = CollectionHelper.newArrayList(preprocessor.compute(i2));

		Set<String> uniqueTerms = new HashSet<>();
		uniqueTerms.addAll(t1);
		uniqueTerms.addAll(t2);

		float[] v1 = createVector(t1, uniqueTerms);
		float[] v2 = createVector(t2, uniqueTerms);

		return FloatVectorUtil.cosine(v1, v2);

	}

	private float[] createVector(List<String> terms, Set<String> uniqTerms) {

		float[] vector = new float[uniqTerms.size()];
		int idx = 0;
		for (String term : uniqTerms) {

			int termCount = Collections.frequency(terms, term);
			if (binarizeTermCount) {
				termCount = termCount > 0 ? 1 : 0;
			}

			float tf = (float) termCount / terms.size();
			float idf = (float) termCorpus.getIdf(term, true);

			vector[idx++] = (float) (tf * Math.log(idf));

		}

		// System.out.println(Arrays.toString(vector));
		return vector;

	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TfIdfSimilarity [featureSetting=");
		builder.append(featureSetting);
		builder.append(", termCorpus=");
		builder.append(termCorpus);
		builder.append(", binarizeTermCount=");
		builder.append(binarizeTermCount);
		builder.append("]");
		return builder.toString();
	}

	public static TermCorpus createTermCorpus(FeatureSetting featureSetting, Dataset dataset) {
		Preprocessor preprocessor = new Preprocessor(featureSetting);
		MapTermCorpus termCorpus = new MapTermCorpus();
		Set<Integer> textHashes = new HashSet<>();
		for (Instance instance : dataset) {
			String question1 = instance.getVector().getNominal("question1").getString();
			String question2 = instance.getVector().getNominal("question2").getString();
			if (textHashes.add(question1.hashCode())) {
				List<String> tokens = CollectionHelper.newArrayList(preprocessor.compute(question1));
				termCorpus.addTermsFromDocument(new HashSet<>(tokens));
			}
			if (textHashes.add(question2.hashCode())) {
				List<String> tokens = CollectionHelper.newArrayList(preprocessor.compute(question2));
				termCorpus.addTermsFromDocument(new HashSet<>(tokens));
			}

		}

		return termCorpus;

	}

}
