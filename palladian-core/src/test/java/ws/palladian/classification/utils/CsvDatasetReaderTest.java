package ws.palladian.classification.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ws.palladian.helper.functional.Filters.regex;
import static ws.palladian.helper.io.ResourceHelper.getResourceFile;

import java.io.IOException;

import org.junit.Test;

import ws.palladian.classification.utils.CsvDatasetReaderConfig.Builder;
import ws.palladian.core.Instance;
import ws.palladian.core.value.ImmutableDoubleValue;
import ws.palladian.core.value.ImmutableStringValue;
import ws.palladian.core.value.NullValue;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Filters;
import ws.palladian.helper.io.CloseableIterator;

public class CsvDatasetReaderTest {

	@Test
	public void testCsvReading() throws IOException {
		Builder config = CsvDatasetReaderConfig.filePath(getResourceFile("/classifier/adultData.txt"));
		config.readHeader(false);
		config.readClassFromLastColumn(true);
		config.setFieldSeparator(";");
		CsvDatasetReader reader = config.create();
		try (CloseableIterator<Instance> iterator = reader.iterator()) {
			assertTrue(iterator.hasNext());
			Instance instance = iterator.next();
			assertEquals(14, instance.getVector().size());
			assertEquals(14, reader.getFeatureInformation().count());
			assertEquals(new ImmutableDoubleValue(25), instance.getVector().get("0"));
			assertEquals(new ImmutableStringValue("Private"), instance.getVector().get("1"));
			assertEquals("<=50K", instance.getCategory());
			assertEquals(1000, CollectionHelper.count(reader.iterator()));
		}
	}

	@Test
	public void testCsvReading_withoutClass() throws IOException {
		Builder config = CsvDatasetReaderConfig.filePath(getResourceFile("/classifier/adultData.txt"));
		config.readHeader(false);
		config.readClassFromLastColumn(false);
		config.setFieldSeparator(";");
		CsvDatasetReader reader = config.create();
		try (CloseableIterator<Instance> iterator = reader.iterator()) {
			Instance instance = iterator.next();
			assertEquals(15, instance.getVector().size());
			assertEquals(15, reader.getFeatureInformation().count());
			assertEquals(new ImmutableStringValue("<=50K"), instance.getVector().get("14"));
			assertEquals(1000, CollectionHelper.count(reader.iterator()));
		}
	}

	@Test
	public void testCsvReading_header() throws IOException {
		Builder config = CsvDatasetReaderConfig.filePath(getResourceFile("/classifier/diabetes2.csv"));
		config.readHeader(true);
		config.readClassFromLastColumn(true);
		config.setFieldSeparator(";");
		CsvDatasetReader reader = config.create();
		try (CloseableIterator<Instance> iterator = reader.iterator()) {
			Instance instance = iterator.next();
			assertEquals(8, instance.getVector().size());
			assertEquals(8, reader.getFeatureInformation().count());
			assertTrue(instance.getVector().keys().contains("numPregnant"));
			assertEquals(768, CollectionHelper.count(reader.iterator()));
		}
	}
	
	@Test
	public void testCsvReading_specialValues() throws IOException {
		Builder config = CsvDatasetReaderConfig.filePath(getResourceFile("/csvDatasetSpecialValues.csv"));
		config.readHeader(true);
		config.readClassFromLastColumn(false);
		config.setFieldSeparator(";");
		CsvDatasetReader reader = config.create();
		try (CloseableIterator<Instance> iterator = reader.iterator()) {
			Instance instance = iterator.next();
			assertEquals(7, instance.getVector().size());
			assertEquals(7, reader.getFeatureInformation().count());
			assertEquals(new ImmutableDoubleValue(1.23), instance.getVector().get("double"));
			assertEquals(new ImmutableDoubleValue(123), instance.getVector().get("long"));
			assertEquals(new ImmutableStringValue("test"), instance.getVector().get("string"));
			assertEquals(new ImmutableDoubleValue(Double.NaN), instance.getVector().get("NaN"));
			assertEquals(new ImmutableDoubleValue(Double.POSITIVE_INFINITY), instance.getVector().get("positiveInfinity"));
			assertEquals(new ImmutableDoubleValue(Double.NEGATIVE_INFINITY), instance.getVector().get("negativeInfinity"));
			assertEquals(NullValue.NULL, instance.getVector().get("null"));
		}
	}
	
	@Test
	public void testCsvReading_customParser() throws IOException {
		Builder config = CsvDatasetReaderConfig.filePath(getResourceFile("/csvDatasetSpecialValues.csv"));
		config.readHeader(true);
		config.readClassFromLastColumn(false);
		config.setFieldSeparator(";");
		config.parser(Filters.ALL, ImmutableStringValue.PARSER);
		CsvDatasetReader reader = config.create();
		try (CloseableIterator<Instance> iterator = reader.iterator()) {
			Instance instance = iterator.next();
			assertEquals(7, instance.getVector().size());
			assertEquals(7, reader.getFeatureInformation().count());
			assertEquals(new ImmutableStringValue("1.23"), instance.getVector().get("double"));
			assertEquals(new ImmutableStringValue("123"), instance.getVector().get("long"));
			assertEquals(new ImmutableStringValue("test"), instance.getVector().get("string"));
			assertEquals(new ImmutableStringValue("NaN"), instance.getVector().get("NaN"));
			assertEquals(new ImmutableStringValue("Infinity"), instance.getVector().get("positiveInfinity"));
			assertEquals(new ImmutableStringValue("-Infinity"), instance.getVector().get("negativeInfinity"));
			assertEquals(NullValue.NULL, instance.getVector().get("null"));
		}
	}
	
	@Test
	public void testCsvReading_skipColumns() throws IOException {
		Builder config = CsvDatasetReaderConfig.filePath(getResourceFile("/csvDatasetSpecialValues.csv"));
		config.readHeader(true);
		config.readClassFromLastColumn(false);
		config.fieldSeparator(";");
		config.skipColumns(regex("NaN|positiveInfinity|negativeInfinity"));
		CsvDatasetReader reader = config.create();
		try (CloseableIterator<Instance> iterator = reader.iterator()) {
			Instance instance = iterator.next();
			assertEquals(4, instance.getVector().size());
			assertEquals(4, reader.getFeatureInformation().count());
			assertEquals(new ImmutableDoubleValue(1.23), instance.getVector().get("double"));
			assertEquals(new ImmutableDoubleValue(123), instance.getVector().get("long"));
			assertEquals(new ImmutableStringValue("test"), instance.getVector().get("string"));
			assertEquals(NullValue.NULL, instance.getVector().get("null"));
		}
	}
	
	@Test
	public void testSize() throws IOException {
		Builder config = CsvDatasetReaderConfig.filePath(getResourceFile("/classifier/adultData.txt"));
		config.readHeader(false);
		config.readClassFromLastColumn(true);
		config.setFieldSeparator(";");
		CsvDatasetReader reader = config.create();
		assertEquals(1000, reader.size());
	}

}
