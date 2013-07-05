/**
 * Created on: 01.07.2012 09:27:43
 */
package ws.palladian.extraction;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ws.palladian.extraction.feature.SparseArffWriter;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;
import ws.palladian.processing.features.SparseFeature;

/**
 * <p>
 * Tests whether the {@link SparseArffWriter} works correctly or not.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public class SparseArffWriterTest {
    // private final String expectedArffFile =
    // "@relation model\n\n @attribute \"la\" numeric\n@attribute \"blah\" numeric\n@attribute \"da\" numeric\n@attribute \"nominalFeature\" {wekadummy,a}\n@attribute \"numericFeature\" numeric\n\n@data\n{0 1.0,1 1.0,2 1.0,3 a,4 0.78}\n";
    private final String expectedArffFile = "@relation model\n\n@attribute \"la\" numeric\n@attribute \"blah\" numeric\n@attribute \"da\" numeric\n@attribute \"nominalFeature\" {wekadummy,a}\n@attribute \"numericFeature\" numeric\n@attribute \"la-count\" numeric\n@attribute \"blah-count\" numeric\n@attribute \"da-count\" numeric\n\n@data\n{0 1.0,1 1.0,2 1.0,3 a,4 0.78,5 2.0,6 1.0,7 1.0}\n";
    private File tempFile;

    @Before
    public void setUp() throws IOException {
        tempFile = File.createTempFile("sparsearffwritertext", "arff");
    }

    @After
    public void tearDown() {
        if (tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    public void test() throws IOException, DocumentUnprocessableException {
        String nominalFeatureName = "nominalFeature";
        String numericFeatureName = "numericFeature";
        String listFeatureName = "listFeature";
        String numericListFeatureName = "numericListFeature";
        TextDocument document = new TextDocument("This is some test document.");
        FeatureVector featureVector = document.getFeatureVector();

        // la should be only once in the result ARFF.
        List<SparseFeature<String>> listFeatureValue = new ArrayList<SparseFeature<String>>();
        listFeatureValue.add(new SparseFeature<String>("la"));
        listFeatureValue.add(new SparseFeature<String>("blah"));
        listFeatureValue.add(new SparseFeature<String>("da"));
        listFeatureValue.add(new SparseFeature<String>("la"));
        ListFeature<SparseFeature<String>> listFeature = new ListFeature<SparseFeature<String>>(listFeatureName, listFeatureValue);
        featureVector.add(listFeature);

        List<NumericFeature> numericListFeatureValue = new ArrayList<NumericFeature>();
        numericListFeatureValue.add(new NumericFeature("la-count", 2));
        numericListFeatureValue.add(new NumericFeature("blah-count", 1));
        numericListFeatureValue.add(new NumericFeature("da-count", 1));
        ListFeature<NumericFeature> numericListFeature = new ListFeature<NumericFeature>(numericListFeatureName,
                numericListFeatureValue);
        featureVector.add(numericListFeature);

        featureVector.add(new NominalFeature(nominalFeatureName, "a"));
        featureVector.add(new NumericFeature(numericFeatureName, 0.78));

        SparseArffWriter objectOfClassUnderTest = new SparseArffWriter(tempFile.getAbsolutePath());
        objectOfClassUnderTest.getInputPort(PipelineProcessor.DEFAULT_INPUT_PORT_IDENTIFIER).put(document);
        objectOfClassUnderTest.process();
        objectOfClassUnderTest.saveModel();

        String actualArffFile = FileHelper.readFileToString(tempFile);
        assertThat(actualArffFile, is(expectedArffFile));
    }

}
