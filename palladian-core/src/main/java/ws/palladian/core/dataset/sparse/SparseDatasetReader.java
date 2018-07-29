//package ws.palladian.core.dataset.sparse;
//
//import static ws.palladian.helper.io.DelimitedStringHelper.splitLine;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import ws.palladian.core.AbstractFeatureVector;
//import ws.palladian.core.ImmutableFeatureVectorEntry;
//import ws.palladian.core.ImmutableInstance;
//import ws.palladian.core.Instance;
//import ws.palladian.core.dataset.AbstractDataset;
//import ws.palladian.core.dataset.Dataset;
//import ws.palladian.core.dataset.FeatureInformation;
//import ws.palladian.core.dataset.FeatureInformationBuilder;
//import ws.palladian.core.value.ImmutableBooleanValue;
//import ws.palladian.core.value.ImmutableDoubleValue;
//import ws.palladian.core.value.ImmutableFloatValue;
//import ws.palladian.core.value.ImmutableIntegerValue;
//import ws.palladian.core.value.ImmutableLocalDateValue;
//import ws.palladian.core.value.ImmutableLongValue;
//import ws.palladian.core.value.ImmutableStringValue;
//import ws.palladian.core.value.NullValue;
//import ws.palladian.core.value.Value;
//import ws.palladian.core.value.io.ValueParser;
//import ws.palladian.core.value.io.ValueParserException;
//import ws.palladian.helper.StopWatch;
//import ws.palladian.helper.collection.AbstractIterator2;
//import ws.palladian.helper.io.CloseableIterator;
//import ws.palladian.helper.io.FileHelper;
//import ws.palladian.helper.io.LineAction;
//
///**
// * Reader for the sparse dataset format, written by {@link SparseDatasetWriter}.
// * 
// * @author pk
// */
//public class SparseDatasetReader extends AbstractDataset {
//
//	private static final class ArrayFeatureVector extends AbstractFeatureVector {
//		private final String[] names;
//		private final Value[] values;
//
//		ArrayFeatureVector(String[] names, Value[] values) {
//			this.names = names;
//			this.values = values;
//		}
//
//		@Override
//		public Iterator<VectorEntry<String, Value>> iterator() {
//			return new AbstractIterator2<VectorEntry<String, Value>>() {
//				int idx = 0;
//				
//				@Override
//				protected VectorEntry<String, Value> getNext() {
//					if (idx >= names.length)
//						return finished();
//					try {
//						return new ImmutableFeatureVectorEntry(names[idx], values[idx]);
//					} finally {
//						idx++;
//					}
//				}
//			};
//		}
//
//	}
//
//	private final class SparseDatasetIterator extends AbstractIterator2<Instance>
//			implements CloseableIterator<Instance> {
//
//		BufferedReader reader;
//		int lineNumber = 0;
//		final StopWatch stopWatch = new StopWatch();
//
//		public SparseDatasetIterator() {
//			try {
//				this.reader = new BufferedReader(new InputStreamReader(new FileInputStream(sparseDataFile)));
//			} catch (IOException e) {
//				try {
//					reader.close();
//				} catch (IOException ignore) {
//				}
//				throw new IllegalStateException(e);
//			}
//		}
//
//		@Override
//		public void close() throws IOException {
//			reader.close();
//		}
//
//		@Override
//		protected Instance getNext() {
//			try {
//				String line;
//				while ((line = reader.readLine()) != null) {
//					if (line.startsWith("#") || line.isEmpty()) {
//						continue;
//					}
//					List<String> split = splitLine(line, ENTRY_SPLIT_CHARACTER, QUOTE_CHARACTER);
//					String category = split.get(0);
//					String mappedCategory = idxToCategoryName.get(category);
//					if (mappedCategory != null) {
//						category = mappedCategory;
//					}
//					String[] names = new String[split.size() - 1];
//					Value[] values = new Value[split.size() - 1];
//					for (int i = 1; i < split.size(); i++) {
//						String part = split.get(i);
//						List<String> keyValue = splitLine(part, KEY_VALUE_SPLIT_CHARACTER, QUOTE_CHARACTER);
//						String idxKey = keyValue.get(0);
//						names[i - 1] = idxKey;
//						String mappedName = idxToValueName.get(idxKey);
//						if (mappedName != null) {
//							names[i - 1] = mappedName;
//						}
//						if (keyValue.get(1).equals("?")) {
//							values[i - 1] = NullValue.NULL;
//						} else {
//							values[i - 1] = parsers.get(idxKey).parse(keyValue.get(1));
//						}
//					}
//					lineNumber++;
//					if (lineNumber % LOG_EVERY_N_LINES == 0) {
//						LOGGER.debug("Read {} lines in {}", lineNumber, stopWatch);
//					}
//					return new ImmutableInstance(new ArrayFeatureVector(names, values), category);
//				}
//				return finished();
//			} catch (IOException e) {
//				throw new IllegalStateException(e);
//			} catch (ValueParserException e) {
//				throw new IllegalStateException(e);
//			}
//		}
//
//	}
//	
//    /** The logger for this class. */
//    private static final Logger LOGGER = LoggerFactory.getLogger(SparseDatasetReader.class);
//
//	private static final ValueParser[] DEFAULT_PARSERS = new ValueParser[] { ImmutableBooleanValue.PARSER,
//			ImmutableIntegerValue.PARSER, ImmutableLongValue.PARSER, ImmutableFloatValue.PARSER,
//			ImmutableDoubleValue.PARSER, ImmutableLocalDateValue.PARSER, ImmutableStringValue.PARSER };
//	
//	/** Interval for the debug logging output when reading lines. */
//	private static final int LOG_EVERY_N_LINES = 100000;
//	
//	/** Character used for quoting strings with colons or spaces. */
//	static final char QUOTE_CHARACTER = '"';
//	
//	/** Separator for entries. */
//	static final char ENTRY_SPLIT_CHARACTER = ' ';
//	
//	/** Separator between key-value. */
//	static final char KEY_VALUE_SPLIT_CHARACTER = ':';
//
//	private final File sparseDataFile;
//
//	private boolean determinedInfo = false;
//
//	private long size = 0;
//
//	private final Map<String, ValueParser> parsers = new LinkedHashMap<>();
//
//	private FeatureInformation featureInformation;
//	
//	private final Map<String, String> idxToCategoryName = new HashMap<>();
//	
//	private final Map<String, String> idxToValueName = new HashMap<>();
//
//	public SparseDatasetReader(File sparseDataFile) {
//		this.sparseDataFile = sparseDataFile;
//	}
//
//	@Override
//	public CloseableIterator<Instance> iterator() {
//		determineMetaInformationIfNecessary();
//		return new SparseDatasetIterator();
//	}
//
//	@Override
//	public FeatureInformation getFeatureInformation() {
//		determineMetaInformationIfNecessary();
//		return featureInformation;
//	}
//
//	@Override
//	public long size() {
//		determineMetaInformationIfNecessary();
//		return size;
//	}
//	
//	private boolean determineMetaInformationFromHeader() {
//		List<String> headerInfo = new ArrayList<>();
//
//		// collect all comment lines from the beginning until we have the first
//		// actual data line (i.e. starting with a digit)
//		FileHelper.performActionOnEveryLine(sparseDataFile, new LineAction() {
//			@Override
//			public void performAction(String line, int lineNumber) {
//				if (line.isEmpty()) {
//					return;
//				}
//				if (line.startsWith("#")) {
//					headerInfo.add(line);
//					return;
//				}
//				if (Character.isDigit(line.charAt(0))) {
//					// breakLineLoop();
//					size++;
//				}
//			}
//		});
//		
//		boolean readCategories = false;
//		boolean readValues = false;
//		
//		for (String headerLine : headerInfo) {
//			if (headerLine.equals(SparseDatasetWriter.HEADER_CATEGORIES)) {
//				readCategories = true;
//				readValues = false;
//				continue;
//			}
//			if (headerLine.equals(SparseDatasetWriter.HEADER_VALUES)) {
//				readCategories = false;
//				readValues = true;
//				continue;
//			}
//			if (!readCategories && !readValues) {
//				continue;
//			}
//			
//			String trimmedHeaderLine = headerLine.replace("#", "").trim();
//			String[] split = trimmedHeaderLine.split("\t");
//			String idx = split[0];
//			
//			if (readCategories) {
//				String categoryName = split[1];
//				idxToCategoryName.put(idx, categoryName);
//			} else {
//				String valueName = split[1];
//				String valueType = split[2];
//				idxToValueName.put(idx, valueName);
//				ValueParser matchingParser = null;
//				for (ValueParser parser : DEFAULT_PARSERS) {
//					if (parser.getType().getName().equals(valueType)) {
//						matchingParser = parser;
//						break;
//					}
//				}
//				if (matchingParser == null) {
//					LOGGER.warn("No parser for {}, fall back to string value", valueType);
//					matchingParser = ImmutableStringValue.PARSER;
//				}
//				parsers.put(idx, matchingParser);
//			}
//		}
//		
//		if (idxToCategoryName.size() > 0 && idxToValueName.size() > 0) {
//			LOGGER.debug("Successfully read meta information from header");
//			return true;
//		}
//		
//		return false;
//		
//	}
//
//	/**
//	 * Determine all dataset meta information once. Caches all values and
//	 * prevents unnecessary execution when the was already run.
//	 */
//	private void determineMetaInformationIfNecessary() {
//		if (determinedInfo) {
//			return;
//		}
//		if (!determineMetaInformationFromHeader()) {
//			LOGGER.debug("Determine meta information ...");
//			StopWatch stopWatch = new StopWatch();
//			FileHelper.performActionOnEveryLine(sparseDataFile, new LineAction() {
//				@Override
//				public void performAction(String line, int lineNumber) {
//					if (line.startsWith("#") || line.isEmpty()) {
//						return;
//					}
//					List<String> split = splitLine(line, ENTRY_SPLIT_CHARACTER, QUOTE_CHARACTER);
//					for (int i = 1; i < split.size(); i++) {
//						String part = split.get(i);
//						List<String> keyValue = splitLine(part, KEY_VALUE_SPLIT_CHARACTER, QUOTE_CHARACTER);
//						if (!parsers.containsKey(keyValue.get(0))) {
//							for (ValueParser parser : DEFAULT_PARSERS) {
//								if (parser.canParse(keyValue.get(1))) {
//									parsers.put(keyValue.get(0), parser);
//									break;
//								}
//							}
//						}
//					}
////					size++;
//				}
//			});
//			LOGGER.debug("Determined meta information in {}", stopWatch);
//		}
//		FeatureInformationBuilder featureInformationBuilder = new FeatureInformationBuilder();
//		for (Entry<String, ValueParser> entry : parsers.entrySet()) {
//			String valueName = entry.getKey();
//			String mappedValueName = idxToValueName.get(valueName);
//			if (mappedValueName != null) {
//				valueName = mappedValueName;
//			}
//			featureInformationBuilder.set(valueName, entry.getValue().getType());
//		}
//		featureInformation = featureInformationBuilder.create();
//		determinedInfo = true;
//	}
//	
//	@Override
//	public Dataset buffer() {
//		return new SparseBufferedDataset(this);
//	}
//
//}
