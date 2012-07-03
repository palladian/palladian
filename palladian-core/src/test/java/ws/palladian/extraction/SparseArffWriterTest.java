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
    private final String expectedArffFile = "@attribute \"nominalFeature\" {dummy,a,b}\n@attribute \"numericFeature\" numeric\n\n@data\n{0 a,1 0.78}\n";

    @Test
    public void test() throws IOException, DocumentUnprocessableException {
        FeatureDescriptor<NominalFeature> nominalFeatureDescriptor = FeatureDescriptorBuilder.build("nominalFeature",
                NominalFeature.class);
        FeatureDescriptor<NumericFeature> numericFeatureDescriptor = FeatureDescriptorBuilder.build("numericFeature",
                NumericFeature.class);
        PipelineDocument<String> document = new PipelineDocument<String>("This is some test document.");
        document.addFeature(new NominalFeature(nominalFeatureDescriptor, "a", "a", "b"));
        document.addFeature(new NumericFeature(numericFeatureDescriptor, 0.78));

        FeatureDescriptor<Feature<?>>[] featureDescriptors = new FeatureDescriptor[] {nominalFeatureDescriptor,
                numericFeatureDescriptor};

        SparseArffWriter objectOfClassUnderTest = new SparseArffWriter("sparsearffwritertest", featureDescriptors);
        objectOfClassUnderTest.setInput(PipelineProcessor.DEFAULT_INPUT_PORT_IDENTIFIER, document);
        objectOfClassUnderTest.process();
        objectOfClassUnderTest.processingFinished();

        String actualArffFile = FileUtils.readFileToString(new File("sparsearffwritertest"));
        System.out.println(actualArffFile);
        assertThat(actualArffFile, is(expectedArffFile));
    }

}
