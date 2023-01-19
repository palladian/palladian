package ws.palladian.extraction.text.vector;

import org.apache.commons.math3.util.FastMath;
import org.junit.Test;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSettingBuilder;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.DefaultDataset;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TextVectorizerTest {
    private final Dataset docs;
    private final FeatureSetting featureSetting = FeatureSettingBuilder.words().termLength(1, 100).create();

    {
        List<Instance> data = new ArrayList<>();
        data.add(createDoc("The sky is blue."));
        data.add(createDoc("The sun is bright today."));
        data.add(createDoc("The sun in the sky is bright."));
        data.add(createDoc("We can see the shining sun, the bright sun."));
        docs = new DefaultDataset(data);
    }

    @Test
    public void testTextVectorizer_binary() {
        TextVectorizer vectorizer = new TextVectorizer("text", featureSetting, docs, TextVectorizer.TFStrategy.BINARY, TextVectorizer.IDFStrategy.UNARY, 100);
        Instance vectorizedDocument = vectorizer.apply(createDoc("The sky is blue."));
        assertEquals(5, vectorizedDocument.getVector().size());
        assertEquals(1, vectorizedDocument.getVector().getNumeric("sky").getFloat(), 0.0001);
        assertEquals(1, vectorizedDocument.getVector().getNumeric("blue").getFloat(), 0.0001);
        assertTrue(vectorizedDocument.getVector().get("bright").isNull());
    }

    @Test
    public void testTextVectorizer_count_alpha1() {
        TextVectorizer vectorizer = new TextVectorizer("text", featureSetting, docs, TextVectorizer.TFStrategy.RAW_COUNT, TextVectorizer.IDFStrategy.UNARY, 100, 1);
        Instance vectorizedDocument = vectorizer.apply(createDoc("The sky is blue."));
        assertEquals(14, vectorizedDocument.getVector().size());
        assertEquals(2, vectorizedDocument.getVector().getNumeric("sky").getFloat(), 0.0001);
        assertEquals(1, vectorizedDocument.getVector().getNumeric("bright").getFloat(), 0.0001);
    }

    @Test
    public void testTextVectorizer_tf() {
        TextVectorizer vectorizer = new TextVectorizer("text", featureSetting, docs, TextVectorizer.TFStrategy.TERM_FREQUENCY, TextVectorizer.IDFStrategy.UNARY, 100);
        Instance vectorizedDocument = vectorizer.apply(createDoc("The sky is blue."));
        assertEquals(5, vectorizedDocument.getVector().size());
        assertEquals(1. / 5, vectorizedDocument.getVector().getNumeric("sky").getFloat(), 0.0001);
    }

    @Test
    public void testTextVectorizer_tf_idf() {
        TextVectorizer vectorizer = new TextVectorizer("text", featureSetting, docs, TextVectorizer.TFStrategy.TERM_FREQUENCY, TextVectorizer.IDFStrategy.IDF_SMOOTH, 100);
        Instance vectorizedDocument = vectorizer.apply(createDoc("The sky is blue."));
        assertEquals(5, vectorizedDocument.getVector().size());
        assertEquals(1. / 5 * FastMath.log(4. / 3), vectorizedDocument.getVector().getNumeric("sky").getFloat(), 0.0001);
    }

    private static Instance createDoc(String text) {
        return new InstanceBuilder().set("text", text).create(true);
    }

}
