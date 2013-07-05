/**
 * Created on: 20.11.2012 08:37:58
 */
package ws.palladian.extraction.feature;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * Tests whether the {@link AnnotationsToViewProcessor} works correctly or not.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public class AnnotationsToViewProcessorTest {

    @Test
    public void test() throws DocumentUnprocessableException {
        AnnotationsToViewProcessor objectOfClassUnderTest = new AnnotationsToViewProcessor("annotation");
        TextDocument testDocument = new TextDocument("test");
        ListFeature<PositionAnnotation> feature = new ListFeature<PositionAnnotation>("annotation");
        testDocument.add(new PositionAnnotation("The", 0, 3));
        testDocument.add(new PositionAnnotation("Fox", 5, 8));

        objectOfClassUnderTest.getInputPort(PipelineProcessor.DEFAULT_INPUT_PORT_IDENTIFIER).put(testDocument);
        objectOfClassUnderTest.process();
        TextDocument document = (TextDocument)objectOfClassUnderTest.getOutputPort(
                PipelineProcessor.DEFAULT_OUTPUT_PORT_IDENTIFIER).poll();

        String content = document.getContent();
        Assert.assertThat(content, Matchers.is("The Fox"));
    }

}
