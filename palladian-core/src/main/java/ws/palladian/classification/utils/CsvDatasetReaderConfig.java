package ws.palladian.classification.utils;

import static ws.palladian.helper.functional.Filters.equal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.dataset.io.Compression;
import ws.palladian.core.dataset.io.Compressions;
import ws.palladian.core.value.ImmutableBooleanValue;
import ws.palladian.core.value.ImmutableDoubleValue;
import ws.palladian.core.value.ImmutableStringValue;
import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.io.ValueParser;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Filters;

public class CsvDatasetReaderConfig {
	public static final class Builder implements Factory<CsvDatasetReader> {
		
		public static final String DEFAULT_NULL_VALUE = "?";

		private static final ValueParser[] DEFAULT_PARSERS = new ValueParser[] { ImmutableBooleanValue.PARSER,
				ImmutableDoubleValue.PARSER, ImmutableStringValue.PARSER };

		private final File filePath;
		private boolean readHeader = true;
		private char fieldSeparator = ';';
		private boolean readClassFromLastColumn = true;
		private List<TargetValueParser> parsers = new ArrayList<>();
		private Filter<? super String> nullValues = equal(DEFAULT_NULL_VALUE);
		private Compression compression = Compressions.NONE;
		private List<Filter<? super String>> skipColumns = new ArrayList<>();
		private long limit = Long.MAX_VALUE;
		private List<ValueParser> defaultParsers = Arrays.asList(DEFAULT_PARSERS);
		private char quoteCharacter = '\u0000';
		private boolean trim = false;

		private Builder(File filePath) {
			Validate.notNull(filePath, "filePath must not be null");
			if (!filePath.canRead()) {
				throw new IllegalArgumentException("Cannot find or read file \"" + filePath + "\"");
			}
			this.filePath = filePath;
			this.compression = Compressions.get(filePath);
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
		 * @deprecated Use {@link #fieldSeparator(char)} instead. We changed the
		 *             implementation for efficiency reasons to only accept
		 *             single character separators from now on. In case a string
		 *             with more than one character is given, this method will
		 *             now throw an exception.
		 */
		@Deprecated
		public Builder fieldSeparator(String fieldSeparator) {
			Validate.notEmpty(fieldSeparator, "fieldSeparator must not be empty");
			if (fieldSeparator.length() != 1) {
				throw new IllegalArgumentException("fieldSeparators with a length != 1 are not supported.");
			}
			this.fieldSeparator = fieldSeparator.charAt(0);
			return this;
		}
		
		/**
		 * @param fieldSeparator
		 *            The separator between entries, usually colon or semicolon.
		 * @return The builder.
		 */
		public Builder fieldSeparator(char fieldSeparator) {
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
			this.nullValues = Filters.equal(nullValue);
			return this;
		}

		/**
		 * @param nullValues
		 *            A filter which determines which values to treat as
		 *            {@link NullValue}. Default configuration treats
		 *            {@value #DEFAULT_NULL_VALUE} as NullValue.
		 * @return The builder.
		 */
		public Builder treatAsNullValue(Filter<? super String> nullValues) {
			Validate.notNull(nullValues, "nullValues must not be null");
			this.nullValues = nullValues;
			return this;
		}
		
		/**
		 * @param gzip
		 *            <code>true</code> in case the input file is gzip
		 *            compressed.
		 * @return The builder.
		 * @deprecated Use {@link #compression(Compression)} instead.
		 */
		@Deprecated
		public Builder gzip(boolean gzip) {
			return compression(gzip ? Compressions.GZIP : Compressions.NONE);
		}

		/**
		 * In case the CSV data is to be read from a compressed file, this
		 * allows to specify the compression format. Note: Usually the
		 * compression is auto-detected based on the file name, so this method
		 * does not need to be called explicitly.
		 * 
		 * @param compression
		 *            The compression to use, or <code>null</code> in case an
		 *            uncompressed serves as input.
		 * @return The builder.
		 * @see Compressions
		 */
		public Builder compression(Compression compression) {
			this.compression = compression != null ? compression : Compressions.NONE;
			return this;
		}
		
		/**
		 * Specify, that the columns <i>matching</i> the given filter will be
		 * skipped during parsing. This can yield in a performance gain in
		 * contrast to a filtering applied later, when parsing bug datasets and
		 * not all columns are required.
		 * 
		 * @param name
		 *            Filter for the column names to skip.
		 * @return The builder.
		 */
		public Builder skipColumns(Filter<? super String> name) {
			Validate.notNull(name, "name must not be null");
			this.skipColumns.add(name);
			return this;
		}
		
		/**
		 * Only read the number of specified lines.
		 * 
		 * @param lines
		 *            The number of lines to read.
		 * @return The builder.
		 */
		public Builder limit(long lines) {
			Validate.isTrue(lines > 0, "lines must be greater zero");
			this.limit = lines;
			return this;
		}
		
		/**
		 * Define the default parsers to try, if not explicitly defined via
		 * {@link #parser(Filter, ValueParser)}.
		 * 
		 * @param parsers
		 *            The default parsers.
		 * @return The builder.
		 */
		public Builder defaultParsers(ValueParser... parsers) {
			Validate.notNull(parsers, "parsers must not be null");
			this.defaultParsers = Arrays.asList(parsers);
			return this;
		}
		
		/**
		 * Set the character used for quoting individual entries (thus allowing
		 * {@link #fieldSeparator(String)} to occur within a value and not being
		 * split).
		 * 
		 * @param quote
		 *            The quote character.
		 * @return The builder.
		 */
		public Builder quoteCharacter(char quote) {
			this.quoteCharacter = quote;
			return this;
		}
		
		/**
		 * Enables trimming of whitespace of individual entries.
		 * 
		 * @param trim
		 *            <code>true</code> to trim whitespace.
		 * @return The builder.
		 */
		public Builder trim(boolean trim) {
			this.trim = trim;
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
	
	public static Builder filePath(String filePath) {
		Validate.notEmpty(filePath, "filePath must not be empty or null");
		return new Builder(new File(filePath));
	}

	private final File filePath;
	private final boolean readHeader;
	private final char fieldSeparator;
	private final boolean readClassFromLastColumn;
	private final List<TargetValueParser> parsers;
	private final Filter<? super String> nullValues;
	private final Compression compression;
	private final List<Filter<? super String>> skipColumns;
	private final long limit;
	private final List<ValueParser> defaultParsers;
	private final char quoteCharacter;
	private final boolean trim;

	private CsvDatasetReaderConfig(Builder builder) {
		this.filePath = builder.filePath;
		this.readHeader = builder.readHeader;
		this.fieldSeparator = builder.fieldSeparator;
		this.readClassFromLastColumn = builder.readClassFromLastColumn;
		this.parsers = new ArrayList<>(builder.parsers);
		this.nullValues = builder.nullValues;
		this.compression = builder.compression;
		this.skipColumns = new ArrayList<>(builder.skipColumns);
		this.limit = builder.limit;
		this.defaultParsers = new ArrayList<>(builder.defaultParsers);
		this.quoteCharacter = builder.quoteCharacter;
		this.trim = builder.trim;
	}

	File filePath() {
		return filePath;
	}

	boolean readHeader() {
		return readHeader;
	}

	char fieldSeparator() {
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

	boolean isNullValue(String value) {
		return nullValues.accept(value);
	}
	
	public InputStream openInputStream() throws IOException {
		return compression.getInputStream(filePath());
	}
	
	public boolean isSkippedColumn(String name) {
		for (Filter<? super String> columnFilter : skipColumns) {
			if (columnFilter.accept(name)) {
				return true;
			}
		}
		return false;
	}
	
	long getLimit() {
		return limit;
	}

	List<ValueParser> getDefaultParsers() {
		return defaultParsers;
	}
	
	char quoteCharacter() {
		return quoteCharacter;
	}
	
	boolean isTrim() {
		return trim;
	}
}
