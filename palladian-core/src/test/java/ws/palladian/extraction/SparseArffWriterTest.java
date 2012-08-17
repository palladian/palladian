/**
 * Created on: 01.07.2012 09:27:43
 */
package ws.palladian.extraction;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import ws.palladian.extraction.feature.SparseArffWriter;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureDescriptor;
import ws.palladian.processing.features.FeatureDescriptorBuilder;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public class SparseArffWriterTest {
    private final String expectedArffFile = "@relation model\n\n @attribute \"la\" numeric\n@attribute \"blah\" numeric\n@attribute \"da\" numeric\n@attribute \"nominalFeature\" {dummy,a,b}\n@attribute \"numericFeature\" numeric\n\n@data\n{0 1.0,1 1.0,2 1.0,3 a,4 0.78}\n"; 

    @Test
    public void test() throws IOException, DocumentUnprocessableException {
        FeatureDescriptor<NominalFeature> nominalFeatureDescriptor = FeatureDescriptorBuilder.build("nominalFeature",
                NominalFeature.class);
        FeatureDescriptor<NumericFeature> numericFeatureDescriptor = FeatureDescriptorBuilder.build("numericFeature",
                NumericFeature.class);
        FeatureDescriptor<ListFeature> listFeatureDescriptor = FeatureDescriptorBuilder.build("listFeature",
                ListFeature.class);
        PipelineDocument<String> document = new PipelineDocument<String>("This is some test document.");
        document.addFeature(new NominalFeature(nominalFeatureDescriptor, "a", "a", "b"));
        document.addFeature(new NumericFeature(numericFeatureDescriptor, 0.78));
        // la should be only once in the result ARFF.
        document.addFeature(new ListFeature(listFeatureDescriptor, new String[] {"la", "blah", "da", "la"}));

        FeatureDescriptor<Feature<?>>[] featureDescriptors = new FeatureDescriptor[] {nominalFeatureDescriptor,
                numericFeatureDescriptor, listFeatureDescriptor};

        File tempFile = File.createTempFile("sparsearffwritertext", "arff");

        SparseArffWriter objectOfClassUnderTest = new SparseArffWriter(tempFile.getAbsolutePath(), featureDescriptors);
        objectOfClassUnderTest.setInput(PipelineProcessor.DEFAULT_INPUT_PORT_IDENTIFIER, document);
        objectOfClassUnderTest.process();
        objectOfClassUnderTest.processingFinished();

        // File arffFile = new File("sparsearffwritertest");

        String actualArffFile = FileUtils.readFileToString(tempFile);
        assertThat(actualArffFile, is(expectedArffFile));
        // FileUtils.forceDelete(arffFile);
    }

}
