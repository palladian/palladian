package ws.palladian.kaggle.quora.similarities.wordvectors;

import java.util.List;

import ws.palladian.extraction.text.vector.FloatVectorUtil;

/**
 * Idea from
 * '<a href="http://jmlr.org/proceedings/papers/v37/kusnerb15.pdf">From Word
 * Embeddings To Document Distances</a>'; Matt J. Kusner, Yu Sun, Nicholas I.
 * Kolkin, Kilian Q. Weinberger; 2015.
 * 
 * @author pk
 *
 */
public class WordMoverSimilarity extends AbstractWordVectorSimilarity {

	private final WordVectorDictionary dictionary;

	public WordMoverSimilarity(WordVectorDictionary dictionary) {
		this.dictionary = dictionary;
	}

	@Override
	public double getSimilarity(String i1, String i2) {
		List<String> tokens1 = preprocess(i1);
		List<String> tokens2 = preprocess(i2);

		// swap in case tokens1 holds the smaller set
		if (tokens1.size() > tokens2.size()) {
			List<String> temp = tokens1;
			tokens1 = tokens2;
			tokens2 = temp;
		}

		double similarity = 0;

		for (String token1 : tokens1) {
			double maxSimilarity = 0;
			float[] vector1 = dictionary.getVector(token1);
			if (vector1 == null) {
				continue;
			}
			for (String token2 : tokens2) {
				float[] vector2 = dictionary.getVector(token2);
				if (vector2 == null) {
					continue;
				}
				float currentSimilarity = FloatVectorUtil.cosine(vector1, vector2);
				maxSimilarity = Math.max(maxSimilarity, currentSimilarity);
			}
			similarity += maxSimilarity;
		}

		return similarity / tokens1.size();
	}
	
	@Override
	public String toString() {
		return "WordMoverSimilarity [dictionary=" + dictionary + "]";
	}

}
