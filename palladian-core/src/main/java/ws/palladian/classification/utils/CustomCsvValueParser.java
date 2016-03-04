package ws.palladian.classification.utils;

import ws.palladian.classification.utils.CsvDatasetReader.DefaultCsvValueParser;
import ws.palladian.core.value.ImmutableStringValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Filters;

public class CustomCsvValueParser extends DefaultCsvValueParser {

	public static final class Builder implements Factory<CsvValueParser> {
		private Filter<? super String> stringValues;

		/**
		 * Explicitly specify, that the given values should be parsed as
		 * strings.
		 * 
		 * @param stringValues
		 *            The values to be parsed as strings.
		 * @return The builder.
		 */
		public Builder stringValues(Filter<? super String> stringValues) {
			this.stringValues = stringValues;
			return this;
		}

		/**
		 * Explicitly specify, that the given values should be parsed as
		 * strings.
		 * 
		 * @param stringValues
		 *            The values to be parsed as strings.
		 * @return The builder.
		 */
		public Builder stringValues(String... stringValues) {
			this.stringValues = Filters.equal(stringValues);
			return this;
		}

		@Override
		public CsvValueParser create() {
			return new CustomCsvValueParser(stringValues);
		}

	}

	private final Filter<? super String> stringValues;

	private CustomCsvValueParser(Filter<? super String> stringValues) {
		this.stringValues = stringValues;
	}

	@Override
	public Value parse(String name, String input) {
		if (stringValues.accept(name)) {
			return new ImmutableStringValue(input);
		}
		return super.parse(name, input);
	}

}
