package ws.palladian.extraction.feature;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class MapTermCorpusTest {

	private static final double DELTA = 0.00001;

	private final MapTermCorpus corpus;
	{
		corpus = new MapTermCorpus();
		corpus.addTermsFromDocument(tokenize("the sky is blue."));
		corpus.addTermsFromDocument(tokenize("the sun is bright today."));
		corpus.addTermsFromDocument(tokenize("the sun in the sky is bright."));
		corpus.addTermsFromDocument(tokenize("we can see the shining sun, the bright sun."));
	}

	@Test
	public void test_getCount() {
		assertEquals(4, corpus.getCount("the"));
		assertEquals(2, corpus.getCount("sky"));
		assertEquals(0, corpus.getCount("moon"));
	}

	@Test
	public void test_getNumDocs() {
		assertEquals(4, corpus.getNumDocs());
	}

	@Test
	public void test_getProbability() {
		assertEquals(2. / 4, corpus.getProbability("sky"), DELTA);
	}

	@Test
	public void test_getNumUniqueTerms() {
		assertEquals(12, corpus.getNumUniqueTerms());
	}

	@Test
	public void test_getNumTerms() {
		assertEquals(22, corpus.getNumTerms());
	}

	@Test
	public void test_getIdf() {
		assertEquals(1 + Math.log(4. / 2), corpus.getIdf("sky", false), DELTA);
		assertEquals(1 + Math.log(4. / 3), corpus.getIdf("sky", true), DELTA);
		assertEquals(Double.POSITIVE_INFINITY, corpus.getIdf("moon", false), DELTA);
		assertEquals(1 + Math.log(4. / 1), corpus.getIdf("moon", true), DELTA);
	}

	private static Set<String> tokenize(String text) {
		List<String> split = Arrays.asList(text.split("[ ,.]+"));
		return new HashSet<>(split);
	}

}
