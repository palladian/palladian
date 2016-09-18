package ws.palladian.core.dataset.csv;

import static ws.palladian.classification.utils.ClassificationUtils.DEFAULT_SEPARATOR;
import static ws.palladian.helper.io.FileHelper.DEFAULT_ENCODING;
import static ws.palladian.helper.io.FileHelper.NEWLINE_CHARACTER;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Objects;

import ws.palladian.core.Instance;
import ws.palladian.core.dataset.AbstractDatasetWriter;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.collection.Vector.VectorEntry;

public class CsvDatasetWriter extends AbstractDatasetWriter {
	
	// TODO support writing to GZ files (as also supported by CsvDatasetReader)

	private final File outputCsv;

	private final boolean writeCategory;

	/**
	 * Create a new {@link CsvDatasetWriter} with the given destination file.
	 * 
	 * @param outputCsv
	 *            The destination file.
	 */
	public CsvDatasetWriter(File outputCsv) {
		this(outputCsv, false, true);
	}

	/**
	 * Create a new {@link CsvDatasetWriter} with the given destination file.
	 * 
	 * @param outputCsv
	 *            The destination file.
	 * @param overwrite
	 *            <code>true</code> to overwrite, in case the file already
	 *            exists. If the file exists and this value is
	 *            <code>false</code>, an exception will be thrown.
	 * @param writeCategory
	 *            <code>true</code> to write the category column,
	 *            <code>false</code> to skip.
	 */
	public CsvDatasetWriter(File outputCsv, boolean overwrite, boolean writeCategory) {
		Objects.requireNonNull(outputCsv, "outputCsv must not be null");
		if (outputCsv.exists()) {
			if (overwrite) {
				if (!outputCsv.delete()) {
					throw new IllegalStateException(outputCsv + " already exists and cannot be deleted");
				}
			} else {
				throw new IllegalArgumentException(outputCsv + " already exists");
			}
		}
		this.outputCsv = outputCsv;
		this.writeCategory = writeCategory;
	}

	@Override
	public void write(Dataset dataset, ProgressReporter progress) {
		try (Writer writer = new BufferedWriter(
							 new OutputStreamWriter(
						     new FileOutputStream(outputCsv), DEFAULT_ENCODING))) {
			
			progress.startTask("Writing CSV", dataset.size());
			boolean writeHeader = true;
			for (Instance instance : dataset) {
				StringBuilder line = new StringBuilder();
				if (writeHeader) {
					int headerCount = 0;
					for (VectorEntry<String, Value> feature : instance.getVector()) {
						if (headerCount++ > 0) {
							line.append(DEFAULT_SEPARATOR);
						}
						line.append(feature.key());
					}
					if (writeCategory) {
						line.append(DEFAULT_SEPARATOR).append("targetClass");
					}
					line.append(NEWLINE_CHARACTER);
				}
				int featureCount = 0;
				for (VectorEntry<String, Value> vectorEntry : instance.getVector()) {
					if (featureCount++ > 0) {
						line.append(DEFAULT_SEPARATOR);
					}
					Value value = vectorEntry.value();
					if (value != NullValue.NULL) {
						line.append(value.toString());
					}
				}
				if (writeCategory) {
					line.append(DEFAULT_SEPARATOR).append(instance.getCategory());
				}
				line.append(NEWLINE_CHARACTER);
				writer.write(line.toString());
				writeHeader = false;
				progress.increment();
			}
		} catch (IOException e) {
			throw new IllegalStateException();
		}
	}

}
