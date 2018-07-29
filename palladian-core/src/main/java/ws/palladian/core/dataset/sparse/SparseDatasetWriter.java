//package ws.palladian.core.dataset.sparse;
//
//import static ws.palladian.core.dataset.sparse.SparseDatasetReader.ENTRY_SPLIT_CHARACTER;
//import static ws.palladian.core.dataset.sparse.SparseDatasetReader.KEY_VALUE_SPLIT_CHARACTER;
//import static ws.palladian.core.dataset.sparse.SparseDatasetReader.QUOTE_CHARACTER;
//
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.OutputStreamWriter;
//import java.io.UnsupportedEncodingException;
//import java.io.Writer;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Objects;
//
//import ws.palladian.core.Instance;
//import ws.palladian.core.dataset.AbstractDatasetWriter;
//import ws.palladian.core.dataset.Dataset;
//import ws.palladian.core.dataset.DatasetAppender;
//import ws.palladian.core.dataset.FeatureInformation;
//import ws.palladian.core.dataset.statistics.DatasetStatistics;
//import ws.palladian.core.value.NullValue;
//import ws.palladian.core.value.Value;
//import ws.palladian.helper.ProgressReporter;
//import ws.palladian.helper.collection.Vector.VectorEntry;
//import ws.palladian.helper.io.FileHelper;
//
//public class SparseDatasetWriter extends AbstractDatasetWriter {
//	
//	static final String HEADER_CATEGORIES = "# ---------- categories ----------";
//	
//	static final String HEADER_VALUES = "# ---------- values ----------";
//
//	private final Writer writer;
//	
//	public SparseDatasetWriter(File outputFile) {
//		this(outputFile, false);
//	}
//	
//	public SparseDatasetWriter(File outputFile, boolean overwrite) {
//		Objects.requireNonNull(outputFile, "outputFile must not be null");
//		if (outputFile.exists()) {
//			if (overwrite) {
//				if (!outputFile.delete()) {
//					throw new IllegalStateException(outputFile + " already exists and cannot be deleted");
//				}
//			} else {
//				throw new IllegalArgumentException(outputFile + " already exists");
//			}
//		}
//		try {
//			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), FileHelper.DEFAULT_ENCODING));
//		} catch (UnsupportedEncodingException e) {
//			throw new IllegalStateException(e);
//		} catch (FileNotFoundException e) {
//			throw new IllegalStateException(e);
//		}
//	}
//
//	
//	@Override
//	public void write(Dataset dataset, ProgressReporter progress) {
//		try {
//			internalWrite(dataset, progress);
//		} catch (IOException e) {
//			throw new IllegalStateException(e);
//		}
//	}
//
//	private void internalWrite(Dataset dataset, ProgressReporter progress) throws IOException {
//		List<String> featureIndices = new ArrayList<>(dataset.getFeatureInformation().getFeatureNames());
//		List<String> categoryIndices = new ArrayList<>(new DatasetStatistics(dataset).getCategoryStatistics().getValues());
//		progress.startTask("Writing dataset", dataset.size());
//		
//		// write header information which gives association between category and
//		// feature indices and actual name, prepend with # to denote a comment
//		writer.write(HEADER_CATEGORIES + "\n");
//		for (int i = 0; i < categoryIndices.size(); i++) {
//			writer.write(String.format("# %d\t%s\n", i, categoryIndices.get(i)));
//		}
//		writer.write("\n" + HEADER_VALUES + "\n");
//		for (int i = 0; i < featureIndices.size(); i++) {
//			String featureName = featureIndices.get(i);
//			String featureType = dataset.getFeatureInformation().getFeatureInformation(featureName).getType().getName();
//			writer.write(String.format("# %d\t%s\t%s\n", i, featureName, featureType));
//		}
//		writer.write("\n");
//
//		for (Instance instance : dataset) {
//			StringBuilder line = new StringBuilder();
//			int categoryIndex = categoryIndices.indexOf(instance.getCategory());
//			line.append(categoryIndex);
//			for (VectorEntry<String, Value> entry : instance.getVector()) {
//				if (entry.value() == NullValue.NULL) {
//					continue;
//				}
//				String value = entry.value().toString();
//				if (value.equals("0") || value.equals("0.0")) {
//					continue;
//				}
//				int featureIndex = featureIndices.indexOf(entry.key());
//				String quotedValue = quote(value);
//				line.append(' ').append(featureIndex).append(':').append(quotedValue);
//			}
//			line.append('\n');
//			writer.write(line.toString());
//			progress.increment();
//		}
//		writer.close();
//	}
//
//	/**
//	 * Put a value in quotes when necessary (when contains a space or colon).
//	 * 
//	 * @param value
//	 *            The value to quote (maybe).
//	 * @return The (maybe) quotes value.
//	 * @throws IllegalStateException
//	 *             In case the given value contains the quote character.
//	 */
//	private static String quote(String value) {
//		if (value.indexOf(QUOTE_CHARACTER) != -1) {
//			throw new IllegalStateException("Value contains quote character: " + value);
//		}
//		if (value.indexOf(ENTRY_SPLIT_CHARACTER) != -1 || value.indexOf(KEY_VALUE_SPLIT_CHARACTER) != -1) {
//			return QUOTE_CHARACTER + value + QUOTE_CHARACTER;
//		}
//		return value;
//	}
//
//	@Override
//	public DatasetAppender write(FeatureInformation featureInformation) {
//		throw new UnsupportedOperationException();
//	}
//
//}
