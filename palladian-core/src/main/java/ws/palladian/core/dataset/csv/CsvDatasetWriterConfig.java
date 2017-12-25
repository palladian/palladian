package ws.palladian.core.dataset.csv;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

import ws.palladian.core.dataset.io.Compression;
import ws.palladian.core.dataset.io.Compressions;
import ws.palladian.helper.functional.Factory;

public class CsvDatasetWriterConfig {
	public static final class Builder implements Factory<CsvDatasetWriter> {

		private final File outputCsv;
		private boolean overwrite = false;
		private boolean writeCategory = true;
		private Compression compression = Compressions.NONE;
		private char fieldSeparator = ';';

		private Builder(File outputCsv) {
			this.outputCsv = outputCsv;
		}

		public Builder overwrite(boolean overwrite) {
			this.overwrite = overwrite;
			return this;
		}

		public Builder writeCategory(boolean writeCategory) {
			this.writeCategory = writeCategory;
			return this;
		}

		public Builder compression(Compression compression) {
			this.compression = compression;
			return this;
		}

		public Builder fieldSeparator(char fieldSeparator) {
			this.fieldSeparator = fieldSeparator;
			return this;
		}

		public CsvDatasetWriterConfig createConfig() {
			return new CsvDatasetWriterConfig(this);
		}

		@Override
		public CsvDatasetWriter create() {
			return new CsvDatasetWriter(createConfig());
		}

	}

	public static Builder filePath(File outputCsv) {
		Objects.requireNonNull(outputCsv, "outputCsv must not be null");
		return new Builder(outputCsv);
	}

	public static Builder filePath(String outputCsv) {
		Objects.requireNonNull(outputCsv, "outputCsv must not be null");
		return new Builder(new File(outputCsv));
	}

	private final File outputCsv;
	private final boolean overwrite;
	private final boolean writeCategory;
	private final Compression compression;
	private final char fieldSeparator;

	public CsvDatasetWriterConfig(Builder builder) {
		this.outputCsv = builder.outputCsv;
		this.overwrite = builder.overwrite;
		this.writeCategory = builder.writeCategory;
		this.compression = builder.compression;
		this.fieldSeparator = builder.fieldSeparator;
	}

	File getOutputCsv() {
		return outputCsv;
	}

	boolean isOverwrite() {
		return overwrite;
	}

	boolean isWriteCategory() {
		return writeCategory;
	}

	OutputStream getOutputStream() throws IOException {
		return compression.getOutputStream(outputCsv);
	}
	
	char getFieldSeparator() {
		return fieldSeparator;
	}
}
