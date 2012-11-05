/**
 * Created on: 01.07.2012 09:27:43
 */
package ws.palladian.extraction;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.processing.DocumentUnprocessableException;

/**
 * <p>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
@Ignore
public class SparseArffWriterTest {
    private final String expectedArffFile = "@relation model\n\n @attribute \"la\" numeric\n@attribute \"blah\" numeric\n@attribute \"da\" numeric\n@attribute \"nominalFeature\" {wekadummy,a}\n@attribute \"numericFeature\" numeric\n\n@data\n{0 1.0,1 1.0,2 1.0,3 a,4 0.78}\n";

    @Test
    public void test() throws IOException, DocumentUnprocessableException {
//        FeatureDescriptor<NominalFeature> nominalFeatureDescriptor = FeatureDescriptorBuilder.build("nominalFeature",
//                NominalFeature.class);
//        FeatureDescriptor<NumericFeature> numericFeatureDescriptor = FeatureDescriptorBuilder.build("numericFeature",
//                NumericFeature.class);
//        FeatureDescriptor<ListFeature> listFeatureDescriptor = FeatureDescriptorBuilder.build("listFeature",
//                ListFeature.class);
//        TextDocument document = new TextDocument("This is some test document.");
//        document.addFeature(new NominalFeature(nominalFeatureDescriptor, "a"));
//        document.addFeature(new NumericFeature(numericFeatureDescriptor, 0.78));
//        // la should be only once in the result ARFF.
//        document.addFeature(new ListFeature(listFeatureDescriptor, new String[] {"la", "blah", "da", "la"}));
//
//        @SuppressWarnings("unchecked")
//        FeatureDescriptor<Feature<?>>[] featureDescriptors = new FeatureDescriptor[] {nominalFeatureDescriptor,
//                numericFeatureDescriptor, listFeatureDescriptor};
//
//        File tempFile = File.createTempFile("sparsearffwritertext", "arff");
//
//        SparseArffWriter objectOfClassUnderTest = new SparseArffWriter(tempFile.getAbsolutePath(), featureDescriptors);
//        objectOfClassUnderTest.setInput(PipelineProcessor.DEFAULT_INPUT_PORT_IDENTIFIER, document);
//        objectOfClassUnderTest.process();
//        objectOfClassUnderTest.processingFinished();
//
//        // File arffFile = new File("sparsearffwritertest");
//
//        String actualArffFile = FileUtils.readFileToString(tempFile);
//        assertThat(actualArffFile, is(expectedArffFile));
//        // FileUtils.forceDelete(arffFile);
    }

}
