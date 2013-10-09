/**
 * Created on: 24.06.2013 18:08:48
 */
package ws.palladian.processing.features;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.utils.WhiteListFeatureVectorFilter;

/**
 * <p>
 * Tests whether the feature vector filter work correct or not.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.2
 */
public class FeatureVectorFilterTest {

    @Test
    public void test() throws DocumentUnprocessableException {
        WhiteListFeatureVectorFilter objectOfClassUnderTest = new WhiteListFeatureVectorFilter();
        objectOfClassUnderTest.addEntry("test");
        objectOfClassUnderTest.addEntry("test2", "v1");
        objectOfClassUnderTest.addEntry("test2", "v2");

        TextDocument textDocument = new TextDocument("test");
        FeatureVector vector = textDocument.getFeatureVector();
        vector.add(new NominalFeature("test", "blah"));
        vector.add(new NominalFeature("tets3", "tuht"));
        List<SparseFeature<String>> values = new ArrayList<SparseFeature<String>>();
        values.add(new SparseFeature<String>("unendliche"));
        values.add(new SparseFeature<String>("Mannigfaltigkeit"));
        values.add(new SparseFeature<String>("in"));
        values.add(new SparseFeature<String>("unendlichen"));
        values.add(new SparseFeature<String>("kombinationen"));
        vector.add(new ListFeature<SparseFeature<String>>("test4", values));

        List<NumericFeature> numericValues = new ArrayList<NumericFeature>();
        numericValues.add(new NumericFeature("v1", 1));
        numericValues.add(new NumericFeature("v2", 2));
        numericValues.add(new NumericFeature("v2", 3));
        vector.add(new ListFeature<NumericFeature>("test2", numericValues));

        objectOfClassUnderTest.getInputPort(PipelineProcessor.DEFAULT_INPUT_PORT_IDENTIFIER).put(textDocument);
        objectOfClassUnderTest.process();
        PipelineDocument<?> result = objectOfClassUnderTest.getOutputPort(
                PipelineProcessor.DEFAULT_OUTPUT_PORT_IDENTIFIER).poll();

        assertThat(result.getFeatureVector().getAll().size(), is(2));
        assertTrue(((ListFeature<SparseFeature<String>>)result.getFeatureVector().get("test2")).getValue().contains(
                new NumericFeature("v1", 1)));
        assertTrue(((ListFeature<SparseFeature<String>>)result.getFeatureVector().get("test2")).getValue().contains(
                new NumericFeature("v2", 2)));
        assertFalse(((ListFeature<SparseFeature<String>>)result.getFeatureVector().get("test2")).getValue().contains(
                new NumericFeature("v3", 3)));
        assertNull(result.getFeatureVector().get("test3"));
        assertNull(result.getFeatureVector().get("test4"));
    }
}
