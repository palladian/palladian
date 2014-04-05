package ws.palladian.classification.text;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.ClassifiedTextDocument;

public class PalladianTextClassifierTest {
    
    private static final FeatureSetting featureSetting = FeatureSettingBuilder.words().create();
    
    private static List<ClassifiedTextDocument> docs;

    @BeforeClass
    public static void setUp() {
        List<ClassifiedTextDocument> docs = CollectionHelper.newArrayList();
        docs.add(new ClassifiedTextDocument("yes", "Chinese Beijing Chinese"));
        docs.add(new ClassifiedTextDocument("yes", "Chinese Chinese Shanghai"));
        docs.add(new ClassifiedTextDocument("yes", "Chinese Macao"));
        docs.add(new ClassifiedTextDocument("no", "Tokyo Japan Chinese"));
        PalladianTextClassifierTest.docs = docs;
    }
    
    @Test
    public void testPalladianTextClassifier_PalladianScorer() {
        PalladianTextClassifier classifier = new PalladianTextClassifier(featureSetting, new PalladianTextClassifier.DefaultScorer());
        DictionaryModel model = classifier.train(docs);
        CategoryEntries result = classifier.classify("Chinese Chinese Chinese Tokyo Japan", model);
        assertEquals("no", result.getMostLikely().getName());
        assertEquals(.79, result.getMostLikely().getProbability(), 0.01);
    }

    @Test
    public void testPalladianTextClassifier_BayesScorer() {
        PalladianTextClassifier classifier = new PalladianTextClassifier(featureSetting, BayesScorer.LAPLACE_SMOOTHING);
        DictionaryModel model = classifier.train(docs);
        CategoryEntries result = classifier.classify("Chinese Chinese Chinese Tokyo Japan", model);
        assertEquals("yes", result.getMostLikely().getName());
        CollectionHelper.print(result);
        // CollectionHelper.print(model);
        // model.toCsv(System.out);
    }

}
