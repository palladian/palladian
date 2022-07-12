package ws.palladian.classification.text;

import static org.junit.Assert.assertEquals;
import static ws.palladian.classification.text.BayesScorer.Options.COMPLEMENT;
import static ws.palladian.classification.text.BayesScorer.Options.PRIORS;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;

public class PalladianTextClassifierTest {

    private static final FeatureSetting FEATURE_SETTING = FeatureSettingBuilder.words().create();

    private static final List<Instance> DOCS = createDocs();

    private static final String TEST_TEXT = "Chinese Chinese Chinese Tokyo Japan";

    private static List<Instance> createDocs() {
        // sample data taken from "An Introduction to Information Retrieval";
        // Christopher D. Manning; Prabhakar Raghavan; Hinrich Schütze; 2009, chapter 13 (pp. 253).
        List<Instance> docs = new ArrayList<>();
        docs.add(new InstanceBuilder().setText("Chinese Beijing Chinese").create("yes"));
        docs.add(new InstanceBuilder().setText("Chinese Chinese Shanghai").create("yes"));
        docs.add(new InstanceBuilder().setText("Chinese Macao").create("yes"));
        docs.add(new InstanceBuilder().setText("Tokyo Japan Chinese").create("no"));
        return docs;
    }

    @Test
    public void testPalladianTextClassifier_PalladianScorer() {
        PalladianTextClassifier classifier = new PalladianTextClassifier(FEATURE_SETTING,
                PalladianTextClassifier.DEFAULT_SCORER);
        DictionaryModel model = classifier.train(DOCS);
        CategoryEntries result = classifier.classify(TEST_TEXT, model);
        assertEquals("no", result.getMostLikely().getName());
        assertEquals(.79, result.getMostLikely().getProbability(), 0.01);
    }

    @Test
    public void testPalladianTextClassifier_BayesScorer() {
        PalladianTextClassifier classifier = new PalladianTextClassifier(FEATURE_SETTING, new BayesScorer(PRIORS));
        DictionaryModel model = classifier.train(DOCS);
        CategoryEntries result = classifier.classify(TEST_TEXT, model);
        assertEquals("yes", result.getMostLikely().getName());
        assertEquals(0.74, result.getMostLikely().getProbability(), 0.01);
    }

    @Test
    public void testPalladianTextClassifier_BayesScorerComplement() {
        PalladianTextClassifier classifier = new PalladianTextClassifier(FEATURE_SETTING, new BayesScorer(PRIORS,
                COMPLEMENT));
        DictionaryModel model = classifier.train(DOCS);
        CategoryEntries result = classifier.classify(TEST_TEXT, model);
        assertEquals("yes", result.getMostLikely().getName());
        assertEquals(0.88, result.getMostLikely().getProbability(), 0.01);
    }
    
}
