package ws.palladian.classification.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ws.palladian.helper.io.ResourceHelper.getResourceFile;

import java.io.IOException;

import org.junit.Test;

import ws.palladian.classification.utils.CsvDatasetReaderConfig.Builder;
import ws.palladian.core.ImmutableLongValue;
import ws.palladian.core.Instance;
import ws.palladian.core.value.ImmutableStringValue;
import ws.palladian.helper.io.CloseableIterator;

public class CsvDatasetReaderTest {

	@Test
	public void testCsvReading() throws IOException {
		Builder config = CsvDatasetReaderConfig.filePath(getResourceFile("/classifier/adultData.txt"));
		config.readHeader(false);
		config.readClassFromLastColumn(true);
		config.fieldSeparator(";");
		CsvDatasetReader reader = new CsvDatasetReader(config.create());
		try (CloseableIterator<Instance> iterator = reader.iterator()) {
			assertTrue(iterator.hasNext());
			Instance instance = iterator.next();
			assertEquals(14, instance.getVector().size());
			assertEquals(new ImmutableLongValue(25), instance.getVector().get("0"));
			assertEquals(new ImmutableStringValue("Private"), instance.getVector().get("1"));
			assertEquals("<=50K", instance.getCategory());
		}
	}

	@Test
	public void testCsvReading_withoutClass() throws IOException {
		Builder config = CsvDatasetReaderConfig.filePath(getResourceFile("/classifier/adultData.txt"));
		config.readHeader(false);
		config.readClassFromLastColumn(false);
		config.fieldSeparator(";");
		CsvDatasetReader reader = new CsvDatasetReader(config.create());
		try (CloseableIterator<Instance> iterator = reader.iterator()) {
			Instance instance = iterator.next();
			assertEquals(15, instance.getVector().size());
			assertEquals(new ImmutableStringValue("<=50K"), instance.getVector().get("14"));
		}
	}

	@Test
	public void testCsvReading_header() throws IOException {
		Builder config = CsvDatasetReaderConfig.filePath(getResourceFile("/classifier/diabetes2.csv"));
		config.readHeader(true);
		config.readClassFromLastColumn(true);
		config.fieldSeparator(";");
		CsvDatasetReader reader = new CsvDatasetReader(config.create());
		try (CloseableIterator<Instance> iterator = reader.iterator()) {
			Instance instance = iterator.next();
			assertEquals(8, instance.getVector().size());
			assertTrue(instance.getVector().keys().contains("numPregnant"));
		}
	}

}
