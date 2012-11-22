/**
 * Created on: 22.11.2012 19:12:20
 */
package ws.palladian.processing;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

/**
 * <p>
 * Tests the correct functionality of the {@link ProcessingPipeline}.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.8
 */
public class ProcessingPipelineTest {

    @Test
    public void testWorkflowSplit() throws DocumentUnprocessableException {
        ProcessingPipeline objectOfClassUnderTest = new ProcessingPipeline();
        AbstractPipelineProcessor outputProcessor = new AbstractPipelineProcessor() {

            @Override
            protected void processDocument() throws DocumentUnprocessableException {
                getOutputPorts().get(0).put(new TextDocument("test"));
            }
        };

        AbstractPipelineProcessor inputProcessor1 = new AbstractPipelineProcessor() {

            @Override
            protected void processDocument() throws DocumentUnprocessableException {
                TextDocument testDocument = (TextDocument)getInputPorts().get(0).poll();
                Assert.assertThat(testDocument.getContent(), Matchers.is("test"));
            }
        };

        AbstractPipelineProcessor inputProcessor2 = new AbstractPipelineProcessor() {

            @Override
            protected void processDocument() throws DocumentUnprocessableException {
                TextDocument testDocument = (TextDocument)getInputPorts().get(0).poll();
                Assert.assertThat(testDocument.getContent(), Matchers.is("test"));
            }
        };
        outputProcessor.getOutputPort(PipelineProcessor.DEFAULT_OUTPUT_PORT_IDENTIFIER).connectWith(
                inputProcessor1.getInputPorts().get(0));
        outputProcessor.getOutputPort(PipelineProcessor.DEFAULT_OUTPUT_PORT_IDENTIFIER).connectWith(
                inputProcessor2.getInputPorts().get(0));

        objectOfClassUnderTest.add(outputProcessor);
        objectOfClassUnderTest.add(inputProcessor1);
        objectOfClassUnderTest.add(inputProcessor2);

        objectOfClassUnderTest.process();
    }
}
