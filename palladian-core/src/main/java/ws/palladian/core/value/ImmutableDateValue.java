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

		public ValueParser(String format) {
			super(ImmutableDateValue.class);
			this.format = new SimpleDateFormat(format);
		}

		@Override
		public Value parse(String input) throws ValueParserException {
			try {
				return new ImmutableDateValue(format.parse(input));
			} catch (ParseException e) {
				throw new ValueParserException(e);
			}
		}

	}

	private final Date date;

	public ImmutableDateValue(Date date) {
		Validate.notNull(date, "date must not be null");
		this.date = date;
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
		return date.toString();
	}

}
