package ws.palladian.core.value;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.value.io.AbstractValueParser;
import ws.palladian.core.value.io.ValueParserException;

public final class ImmutableDateValue extends AbstractValue implements DateValue {

	public static final class ValueParser extends AbstractValueParser {

		private final SimpleDateFormat format;
		private String pattern;

		public ValueParser(String pattern) {
			super(ImmutableDateValue.class);
			this.format = new SimpleDateFormat(pattern);
			this.pattern = pattern;
		}

		@Override
		public Value parse(String input) throws ValueParserException {
			try {
				return new ImmutableDateValue(format.parse(input), pattern);
			} catch (ParseException e) {
				throw new ValueParserException(e);
			}
		}

	}
	
	private static final String ISO_8601 = "yyyy-MM-dd'T'HH:mm'Z'";

	private final Date date;
	
	/** The pattern which was originally used for parsing the date. */
	private final String pattern;

	public ImmutableDateValue(Date date) {
		Validate.notNull(date, "date must not be null");
		this.date = date;
		this.pattern = ISO_8601;
	}

	ImmutableDateValue(Date date, String pattern) {
		this.date = date;
		this.pattern = pattern;
	}

	@Override
	public Date getDate() {
		return date;
	}

	@Override
	public int hashCode() {
		return date.hashCode();
	}

	@Override
	protected boolean equalsValue(Value value) {
		ImmutableDateValue other = (ImmutableDateValue) value;
		return date.equals(other.date);
	}

	@Override
	public String toString() {
		return new SimpleDateFormat(pattern).format(getDate());
	}

}
