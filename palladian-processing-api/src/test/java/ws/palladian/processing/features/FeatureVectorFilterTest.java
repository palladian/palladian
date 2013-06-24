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
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since
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
        List<String> values = new ArrayList<String>();
        values.add("unendliche");
        values.add("Mannigfaltigkeit");
        values.add("in");
        values.add("unendlichen");
        values.add("kombinationen");
        vector.add(new ListFeature<String>("test4", values));

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
        assertTrue(((ListFeature<String>)result.getFeatureVector().get("test2")).getValue().contains(
                new NumericFeature("v1", 1)));
        assertTrue(((ListFeature<String>)result.getFeatureVector().get("test2")).getValue().contains(
                new NumericFeature("v2", 2)));
        assertFalse(((ListFeature<String>)result.getFeatureVector().get("test2")).getValue().contains(
                new NumericFeature("v3", 3)));
        assertNull(result.getFeatureVector().get("test3"));
        assertNull(result.getFeatureVector().get("test4"));
    }
}
