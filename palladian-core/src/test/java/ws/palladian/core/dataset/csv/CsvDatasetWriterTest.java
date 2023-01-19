package ws.palladian.core.dataset.csv;

import org.junit.Test;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.RandomDataset;
import ws.palladian.helper.io.FileHelper;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class CsvDatasetWriterTest {
    @Test
    public void testCsvDatasetWriter() throws IOException {
        File tempFile = FileHelper.getTempFile();
        Dataset testDataset = new RandomDataset(100);

        CsvDatasetWriter writer = new CsvDatasetWriter(tempFile);
        writer.write(testDataset);

        assertEquals("The result file should contain 101 lines (including header)", 101, FileHelper.getNumberOfLines(tempFile));
    }
}
