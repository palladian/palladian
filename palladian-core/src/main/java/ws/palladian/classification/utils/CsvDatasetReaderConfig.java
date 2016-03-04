package ws.palladian.classification.utils;

import java.io.File;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.functional.Factory;

public class CsvDatasetReaderConfig {
	public static final class Builder implements Factory<CsvDatasetReader> {

		private final File filePath;
		private boolean readHeader = true;
		private String fieldSeparator = ";";
		private boolean readClassFromLastColumn = true;
		private CsvValueParser parser = new CsvDatasetReader.DefaultCsvValueParser();

		private Builder(File filePath) {
			Validate.notNull(filePath, "filePath must not be null");
			if (!filePath.canRead()) {
				throw new IllegalArgumentException("Cannot find or read file \"" + filePath + "\"");
			}
			this.filePath = filePath;
		}

		/**
		 * @param readHeader
		 *            Specify whether to treat the first line as header with
		 *            names of the features.
		 * @return The builder.
		 */
		public Builder readHeader(boolean readHeader) {
			this.readHeader = readHeader;
			return this;
		}

		/**
		 * @param fieldSeparator
		 *            The separator between entries, usually colon or semicolon.
		 * @return The builder.
		 */
		public Builder fieldSeparator(String fieldSeparator) {
			Validate.notEmpty(fieldSeparator, "fieldSeparator must not be empty");
			this.fieldSeparator = fieldSeparator;
			return this;
		}

		/**
		 * @param readClassFromLastColumn
		 *            <code>true</code> to tread the last column in the CSV as
		 *            the category, <code>false</code> to treat it as a normal
		 *            feature (in this case, the category of the created
		 *            instances is set to some dummy value).
		 * @@return The builder.
		 */
		public Builder readClassFromLastColumn(boolean readClassFromLastColumn) {
			this.readClassFromLastColumn = readClassFromLastColumn;
			return this;
		}
		
		/**
		 * @param parser
		 *            allows to specify a custom parser. Per default, the data
		 *            types are detected automatically, which may fail in some
		 *            cases. By specifying a custom parser, this behavior can
		 *            be adapted as necessary.
		 * @return The builder.
		 */
		public Builder parser(CsvValueParser parser) {
			this.parser = parser;
			return this;
		}

		@Override
		public CsvDatasetReader create() {
			return new CsvDatasetReader(createConfig());
		}
		
		CsvDatasetReaderConfig createConfig() {
			return new CsvDatasetReaderConfig(this);	
		}

	}

	/**
	 * Create a new configuration for the {@link CsvDatasetReader}, which reads
	 * a CSV file from the given path.
	 * 
	 * @param filePath
	 *            The path to the CSV file.
	 * @return A builder for further configuration.
	 */
	public static Builder filePath(File filePath) {
		return new Builder(filePath);
	}

	private final File filePath;
	private final boolean readHeader;
	private final String fieldSeparator;
	private final boolean readClassFromLastColumn;
	private final CsvValueParser parser;

	private CsvDatasetReaderConfig(Builder builder) {
		this.filePath = builder.filePath;
		this.readHeader = builder.readHeader;
		this.fieldSeparator = builder.fieldSeparator;
		this.readClassFromLastColumn = builder.readClassFromLastColumn;
		this.parser = builder.parser;
	}

	File filePath() {
		return filePath;
	}

	boolean readHeader() {
		return readHeader;
	}

	String fieldSeparator() {
		return fieldSeparator;
	}

	boolean readClassFromLastColumn() {
		return readClassFromLastColumn;
	}
	
	public CsvValueParser parser() {
		return parser;
	}
}
