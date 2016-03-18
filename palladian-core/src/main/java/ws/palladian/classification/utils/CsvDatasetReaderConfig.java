package ws.palladian.classification.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.io.ValueParser;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Filters;

public class CsvDatasetReaderConfig {
	public static final class Builder implements Factory<CsvDatasetReader> {
		
		public static final String DEFAULT_NULL_VALUE = "?";

		private final File filePath;
		private boolean readHeader = true;
		private String fieldSeparator = ";";
		private boolean readClassFromLastColumn = true;
		private List<TargetValueParser> parsers = new ArrayList<>();
		private String nullValue = DEFAULT_NULL_VALUE;
		private boolean gzip = false;

		private Builder(File filePath) {
			Validate.notNull(filePath, "filePath must not be null");
			if (!filePath.canRead()) {
				throw new IllegalArgumentException("Cannot find or read file \"" + filePath + "\"");
			}
			this.filePath = filePath;
			this.gzip = filePath.getName().toLowerCase().endsWith(".gz");
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
		 * Allows to specify a custom parser for the given column name.
		 * 
		 * @param name
		 *            A filter for the column name (thus allowing to specify the
		 *            filter for multiple columns).
		 * @param parser
		 *            The parser.
		 * @return The builder.
		 */
		public Builder parser(Filter<? super String> name, ValueParser parser) {
			Validate.notNull(name, "name must not be null");
			Validate.notNull(parser, "parser must not be null");
			parsers.add(new TargetValueParser(name, parser));
			return this;
		}
		
		/**
		 * Allows to specify a custom parser for the given column name.
		 * 
		 * @param name
		 *            The column name.
		 * @param parser
		 *            The parser.
		 * @return The builder.
		 */
		public Builder parser(String name, ValueParser parser) {
			Validate.notEmpty(name, "name must not be empty");
			Validate.notNull(parser, "parser must not be null");
			parsers.add(new TargetValueParser(name, parser));
			return this;
		}
		
		/**
		 * @param nullValue
		 *            The character(s) to be treated as {@link NullValue}.
		 *            Default configuration treats {@value #DEFAULT_NULL_VALUE}
		 *            as NullValue. Can also be an empty string.
		 * @return The builder.
		 */
		public Builder treatAsNullValue(String nullValue) {
			Validate.notNull(nullValue, "nullValue must not be null");
			this.nullValue = nullValue;
			return this;
		}
		
		/**
		 * @param gzip
		 *            <code>true</code> in case the input file is gzip
		 *            compressed.
		 * @return The builder.
		 */
		public Builder gzip(boolean gzip) {
			this.gzip = gzip;
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
	
	/** {@link ValueParser} targeted on (a) specific column(s). */
	private static final class TargetValueParser {
		final Filter<? super String> columnName;
		final ValueParser parser;
		TargetValueParser(Filter<? super String> columnName, ValueParser parser) {
			this.columnName = columnName;
			this.parser = parser;
		}
		TargetValueParser(String columnName, ValueParser parser) {
			this(Filters.equal(columnName), parser);
		}
	}

	/**
	 * Create a new configuration for the {@link CsvDatasetReader}, which reads
	 * a CSV file from the given path. In case, a file with extension
	 * <tt>.gz</tt> is supplied, the reader will automatically uncompress the
	 * file on the fly (this behavior can be explicitly overridden by
	 * {@link CsvDatasetReaderConfig.Builder#gzip(boolean)}.
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
	private final List<TargetValueParser> parsers;
	private final String nullValue;
	private final boolean gzip;

	private CsvDatasetReaderConfig(Builder builder) {
		this.filePath = builder.filePath;
		this.readHeader = builder.readHeader;
		this.fieldSeparator = builder.fieldSeparator;
		this.readClassFromLastColumn = builder.readClassFromLastColumn;
		this.parsers = new ArrayList<>(builder.parsers);
		this.nullValue = builder.nullValue;
		this.gzip = builder.gzip;
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
	
	/**
	 * Get a ValueParser for the given column name.
	 * @param name The name of the column.
	 * @return The parser.
	 */
	public ValueParser getParser(String name) {
		for (TargetValueParser targetValueParser : parsers) {
			if (targetValueParser.columnName.accept(name)) {
				return targetValueParser.parser;
			}
		}
		return null;
	}
	
	public String nullValue() {
		return nullValue;
	}
	
	public boolean gzip() {
		return gzip;
	}
	
	public InputStream openInputStream() throws IOException {
		InputStream inputStream = new FileInputStream(filePath());
        if (gzip()) {
        	inputStream = new GZIPInputStream(inputStream);
        }
        return inputStream;
	}
}
