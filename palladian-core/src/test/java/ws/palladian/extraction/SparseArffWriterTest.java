/**
 * Created on: 01.07.2012 09:27:43
 */
package ws.palladian.extraction;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import ws.palladian.extraction.feature.SparseArffWriter;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.FeatureVector;
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
    // private final String expectedArffFile =
    // "@relation model\n\n @attribute \"la\" numeric\n@attribute \"blah\" numeric\n@attribute \"da\" numeric\n@attribute \"nominalFeature\" {wekadummy,a}\n@attribute \"numericFeature\" numeric\n\n@data\n{0 1.0,1 1.0,2 1.0,3 a,4 0.78}\n";
    private final String expectedArffFile = "@relation model\n\n @attribute \"nominalFeature\" {wekadummy,a}\n@attribute \"numericFeature\" numeric\n@attribute \"la\" numeric\n@attribute \"blah\" numeric\n@attribute \"da\" numeric\n\n@data\n{0 a,1 0.78,2 1.0,3 1.0,4 1.0}\n";

    @Test
    public void test() throws IOException, DocumentUnprocessableException {
        String nominalFeatureName = "nominalFeature";
        String numericFeatureName = "numericFeature";
        String listFeatureName = "listFeature";
        TextDocument document = new TextDocument("This is some test document.");
        FeatureVector featureVector = document.getFeatureVector();
        featureVector.add(new NominalFeature(nominalFeatureName, "a"));
        featureVector.add(new NumericFeature(numericFeatureName, 0.78));
        // la should be only once in the result ARFF.
        featureVector.add(new NominalFeature(listFeatureName, "la"));
        featureVector.add(new NominalFeature(listFeatureName, "blah"));
        featureVector.add(new NominalFeature(listFeatureName, "da"));
        featureVector.add(new NominalFeature(listFeatureName, "la"));

        String[] featureNames = new String[] {nominalFeatureName, numericFeatureName, listFeatureName};

        File tempFile = File.createTempFile("sparsearffwritertext", "arff");

        SparseArffWriter objectOfClassUnderTest = new SparseArffWriter(tempFile.getAbsolutePath(), featureNames);
        objectOfClassUnderTest.getInputPort(PipelineProcessor.DEFAULT_INPUT_PORT_IDENTIFIER).setPipelineDocument(document);
        objectOfClassUnderTest.process();
        objectOfClassUnderTest.processingFinished();

        // File arffFile = new File("sparsearffwritertest");

        String actualArffFile = FileHelper.readFileToString(tempFile);
        System.out.println(actualArffFile);
        assertThat(actualArffFile, is(expectedArffFile));
        // // FileUtils.forceDelete(arffFile);
    }

}
