/**
 * Created on: 23.10.2012 16:32:41
 */
package ws.palladian.extraction;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ws.palladian.extraction.feature.CsvWriter;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since
 */
public class CsvWriterTest {

    @Test
    public void test() throws IOException, DocumentUnprocessableException {
        String expectedResult = "\"test1\",\"test2\",\"test3\"\ntest1-1,test2,?\n";

        File csvFile = File.createTempFile("csvwritertest", "csv");
        CsvWriter objectOfClassUnderTest = new CsvWriter(csvFile.getCanonicalPath(), "test1", "test2", "test3");

        TextDocument document = new TextDocument("");
        document.addFeature(new NominalFeature("test1", "test1-1"));
        document.addFeature(new NumericFeature("test1", 12.0));
        document.addFeature(new NominalFeature("test2", "test2"));
        objectOfClassUnderTest.setInput(PipelineProcessor.DEFAULT_INPUT_PORT_IDENTIFIER, document);
        objectOfClassUnderTest.process();

        String content = FileUtils.readFileToString(csvFile);

        Assert.assertThat(content, Matchers.is(expectedResult));
    }

}
