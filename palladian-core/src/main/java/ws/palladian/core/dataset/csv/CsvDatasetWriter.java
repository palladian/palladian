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
import ws.palladian.core.dataset.DatasetAppender;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformation.FeatureInformationEntry;
import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.ProgressReporter;

public class CsvDatasetWriter extends AbstractDatasetWriter {
	
	// TODO support writing to GZ files (as also supported by CsvDatasetReader)
	
	private static final class CsvDatasetAppender implements DatasetAppender {
		private final Writer writer;
		private final FeatureInformation featureInformation;
		private final boolean writeCategory;

		CsvDatasetAppender(Writer writer, FeatureInformation featureInformation, boolean writeCategory) {
			this.writer = writer;
			this.featureInformation = featureInformation;
			this.writeCategory = writeCategory;
		}

		@Override
		public void close() throws IOException {
			writer.close();
		}

		@Override
		public void append(Instance instance) {
			StringBuilder line = new StringBuilder();
			int featureCount = 0;
			for (FeatureInformationEntry infoEntry : featureInformation) {
				if (featureCount++ > 0) {
					line.append(DEFAULT_SEPARATOR);
				}
				Value value = instance.getVector().get(infoEntry.getName());
				if (value != NullValue.NULL) {
					line.append(value.toString());
				}

			}
			if (writeCategory) {
				line.append(DEFAULT_SEPARATOR).append(instance.getCategory());
			}
			line.append(NEWLINE_CHARACTER);
			write(line.toString());
		}

		void writeHeader() {
			StringBuilder line = new StringBuilder();
			int headerCount = 0;
			for (FeatureInformationEntry infoEntry : featureInformation) {
				if (headerCount++ > 0) {
					line.append(DEFAULT_SEPARATOR);
				}
				line.append(infoEntry.getName());
			}
			if (writeCategory) {
				line.append(DEFAULT_SEPARATOR).append("targetClass");
			}
			line.append(NEWLINE_CHARACTER);
			write(line.toString());
		}

		void write(String string) {
			try {
				writer.write(string);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}

	}

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
		try (DatasetAppender appender = write(dataset.getFeatureInformation())) {
			progress.startTask("Writing CSV", dataset.size());
			for (Instance instance : dataset) {
				appender.append(instance);
				progress.increment();
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public DatasetAppender write(FeatureInformation featureInformation) {
		try {
			
			Writer writer = new BufferedWriter(
					 new OutputStreamWriter(
				     new FileOutputStream(outputCsv), DEFAULT_ENCODING));

			CsvDatasetAppender appender = new CsvDatasetAppender(writer, featureInformation, writeCategory);
			appender.writeHeader();
			return appender;

		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
