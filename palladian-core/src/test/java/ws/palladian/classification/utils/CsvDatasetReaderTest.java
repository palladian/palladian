package ws.palladian.classification.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ws.palladian.core.value.ValueDefinitions.stringValue;
import static ws.palladian.helper.functional.Filters.regex;
import static ws.palladian.helper.io.ResourceHelper.getResourceFile;

import java.io.IOException;

import org.junit.Test;

import ws.palladian.classification.utils.CsvDatasetReaderConfig.Builder;
import ws.palladian.core.Instance;
import ws.palladian.core.value.ImmutableStringValue;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Filters;
import ws.palladian.helper.io.CloseableIterator;

public class CsvDatasetReaderTest {

	private static final double DELTA = 0.1;

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
			assertEquals(25, instance.getVector().getNumeric("0").getDouble(), DELTA);
			assertEquals("Private", instance.getVector().getNominal("1").getString());
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
			assertEquals("<=50K", instance.getVector().getNominal("14").getString());
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
			assertEquals(1.23, instance.getVector().getNumeric("double").getDouble(), DELTA);
			assertEquals(123l, instance.getVector().getNumeric("long").getLong());
			assertEquals("test", instance.getVector().getNominal("string").getString());
			assertEquals(Double.NaN, instance.getVector().getNumeric("NaN").getDouble(), DELTA);
			assertEquals(Double.POSITIVE_INFINITY, instance.getVector().getNumeric("positiveInfinity").getDouble(), DELTA);
			assertEquals(Double.NEGATIVE_INFINITY, instance.getVector().getNumeric("negativeInfinity").getDouble(), DELTA);
			assertTrue(instance.getVector().get("null").isNull());
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
			assertEquals("1.23", instance.getVector().getNominal("double").getString());
			assertEquals("123", instance.getVector().getNominal("long").getString());
			assertEquals("test", instance.getVector().getNominal("string").getString());
			assertEquals("NaN", instance.getVector().getNominal("NaN").getString());
			assertEquals("Infinity", instance.getVector().getNominal("positiveInfinity").getString());
			assertEquals("-Infinity", instance.getVector().getNominal("negativeInfinity").getString());
			assertTrue(instance.getVector().get("null").isNull());
		}
	}
	
	@Test
	public void testCsvReading_skipColumns() throws IOException {
		Builder config = CsvDatasetReaderConfig.filePath(getResourceFile("/csvDatasetSpecialValues.csv"));
		config.readHeader(true);
		config.readClassFromLastColumn(false);
		config.setFieldSeparator(";");
		config.skipColumns(regex("NaN|positiveInfinity|negativeInfinity"));
		CsvDatasetReader reader = config.create();
		try (CloseableIterator<Instance> iterator = reader.iterator()) {
			Instance instance = iterator.next();
			assertEquals(4, instance.getVector().size());
			assertEquals(4, reader.getFeatureInformation().count());
			assertEquals(1.23, instance.getVector().getNumeric("double").getDouble(), DELTA);
			assertEquals(123, instance.getVector().getNumeric("long").getLong(), DELTA);
			assertEquals("test", instance.getVector().getNominal("string").getString());
			assertTrue(instance.getVector().get("null").isNull());
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
	
	@Test
	public void testCsvReading_quotedEntries() throws IOException {
		Builder config = CsvDatasetReaderConfig.filePath(getResourceFile("/csvDatasetQuotedValues.csv"));
		config.readHeader(true);
		config.readClassFromLastColumn(false);
		config.setFieldSeparator(';');
		config.quoteCharacter('"');
		CsvDatasetReader reader = config.create();
		try (CloseableIterator<Instance> iterator = reader.iterator()) {
			Instance instance = iterator.next();
			// System.out.println(instance);
			assertEquals(2, instance.getVector().size());
			assertEquals(2, reader.getFeatureInformation().count());
			assertEquals("value 1", instance.getVector().getNominal("header 1").getString());
			assertEquals("value 2; with one; two; three semicolons", instance.getVector().getNominal("header 2; with semicolon").getString());
		}
	}
	
	@Test
	public void testCsvReading_whitespaceEntries() throws IOException {
		Builder config = CsvDatasetReaderConfig.filePath(getResourceFile("/csvDatasetWhitespaceValues.csv"));
		config.readHeader(true);
		config.readClassFromLastColumn(true);
		config.setFieldSeparator(';');
		config.trim(true);
		config.defaultParsers(stringValue());
		CsvDatasetReader reader = config.create();
		try (CloseableIterator<Instance> iterator = reader.iterator()) {
			Instance instance = iterator.next();
			System.out.println(instance);
			assertEquals(3, instance.getVector().size());
			assertEquals(3, reader.getFeatureInformation().count());
			assertEquals("1", instance.getVector().getNominal("value1").getString());
			assertEquals("2", instance.getVector().getNominal("value2").getString());
			assertEquals("3", instance.getVector().getNominal("value3").getString());
			assertEquals("1", instance.getCategory());
		}
	}

}
