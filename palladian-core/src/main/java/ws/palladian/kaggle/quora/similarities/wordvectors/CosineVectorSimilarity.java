package ws.palladian.kaggle.quora.similarities.wordvectors;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.text.vector.FloatVectorUtil;

public class CosineVectorSimilarity extends AbstractWordVectorSimilarity {

	/** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CosineVectorSimilarity.class);
	
	private final WordVectorDictionary dictionary;
	
	public CosineVectorSimilarity(WordVectorDictionary dictionary) {
		this.dictionary = dictionary;
	}

	@Override
	public double getSimilarity(String i1, String i2) {
		float[] vector1 = getVectorForSentence(i1);
		float[] vector2 = getVectorForSentence(i2);
		return FloatVectorUtil.cosine(vector1, vector2);
	}
	
	public float[] getVectorForSentence(String sentence) {
		List<String> sentenceSplit = preprocess(sentence);
		float[] sentenceVector = new float[dictionary.vectorSize()];
		for (String token : sentenceSplit) {
			float[] tokenVector = dictionary.getVector(token);
			if (tokenVector == null) {
				LOGGER.debug("Token '{}' not found", token);
				continue;
			}
			sentenceVector = FloatVectorUtil.add(sentenceVector, tokenVector);
		}
		sentenceVector = FloatVectorUtil.normalize(sentenceVector);
		return sentenceVector;
	}
	
	@Override
	public String toString() {
		return "WordVectorSentenceSimilarity [dictionary=" + dictionary + "]";
	}

	public static void main(String[] args) {
		// VectorDictionary dictionary = MapVectorDictionary.readFromVecFile(new File("/Users/pk/temp/wiki.en/wiki.en.vec"), 500000);
		WordVectorDictionary dictionary = MapWordVectorDictionary.readFromVecFile(new File("/Users/pk/Downloads/glove.6B/glove.6B.50d.txt"), 500000);
		CosineVectorSimilarity similarity = new CosineVectorSimilarity(dictionary);
		System.out.println("size = " + dictionary.size());
		
		// System.out.println(Arrays.toString(similarity.getVectorForSentence("How do I download content from a kickass torrent without registration?")));
		// System.out.println(Arrays.toString(similarity.getVectorForSentence("Is Kickass Torrents trustworthy?")));
		// System.exit(0);
		
		// float[] vector = dictionary.getVector("and");
		// System.out.println(Arrays.toString(vector));
		
		List<String> sentences = new ArrayList<>();
		sentences.add("apple iphone");
		sentences.add("spaghetti carbonara");
		sentences.add("pizza napoli");
		sentences.add("porsche boxster");
		sentences.add("mercedes slk");
		sentences.add("samsung galaxy");
		
		for (int i = 0; i < sentences.size(); i++) {
			String s1 = sentences.get(i);
			for (int j = i + 1; j < sentences.size(); j++) {
				String s2 = sentences.get(j);
				System.out.println(s1 + " <-> " + s2 + ": " + similarity.getSimilarity(s1, s2));
			}
		}
		
	}


}
