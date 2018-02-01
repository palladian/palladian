package ws.palladian.classification.text.nbsvm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSettingBuilder;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.DefaultDataset;
import ws.palladian.extraction.text.vector.TextVectorizer;
import ws.palladian.extraction.text.vector.TextVectorizer.IDFStrategy;
import ws.palladian.extraction.text.vector.TextVectorizer.TFStrategy;

public class NbSvmTest {

	private static final String WEATHER = "0";
	private static final String ANIMAL = "1";

	@Test
	public void test() {
		List<Instance> data = new ArrayList<>();
		data.add(newInstance("The quick brown fox.", ANIMAL));
		data.add(newInstance("The fox chases the cat.", ANIMAL));
		data.add(newInstance("The dog jumps over the lazy fox.", ANIMAL));
		data.add(newInstance("The sky is blue.", WEATHER));
		data.add(newInstance("The sun is bright today.", WEATHER));
		data.add(newInstance("The sun in the sky is bright.", WEATHER));
		data.add(newInstance("We can see the shining sun, the bright sun.", WEATHER));
		DefaultDataset dataset = new DefaultDataset(data);

		FeatureSetting featureSetting = FeatureSettingBuilder.words(1).termLength(1, 100).create();
		TextVectorizer vectorizer = new TextVectorizer("text", featureSetting, dataset, TFStrategy.RAW_COUNT,
				IDFStrategy.UNARY, Integer.MAX_VALUE);
		NbSvmLearner learner = new NbSvmLearner(vectorizer);
		NbSvmClassifier classifier = new NbSvmClassifier(vectorizer);
		NbSvmModel model = learner.train(dataset);

		CategoryEntries result1 = classifier.classify(newDoc("Is a fox an animal?"), model);
		assertEquals(ANIMAL, result1.getMostLikelyCategory());
		assertTrue(result1.getProbability(ANIMAL) > 0.64);

		CategoryEntries result2 = classifier.classify(newDoc("Ze sun is shining."), model);
		assertEquals(WEATHER, result2.getMostLikelyCategory());
		assertTrue(result2.getProbability(WEATHER) > 0.82);
	}

	private static FeatureVector newDoc(String content) {
		return new InstanceBuilder().setText(content).create();
	}

	private static Instance newInstance(String text, String clazz) {
		return new InstanceBuilder().setText(text).create(clazz);
	}

}
