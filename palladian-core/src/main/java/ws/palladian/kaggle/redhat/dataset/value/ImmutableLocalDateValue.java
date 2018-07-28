package ws.palladian.kaggle.redhat.dataset.value;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import ws.palladian.core.value.AbstractValue;
import ws.palladian.core.value.Value;
import ws.palladian.core.value.io.AbstractValueParser;
import ws.palladian.core.value.io.ValueParser;
import ws.palladian.core.value.io.ValueParserException;

public final class ImmutableLocalDateValue extends AbstractValue implements LocalDateValue {

	public static final ValueParser PARSER = new AbstractValueParser(ImmutableLocalDateValue.class) {
		@Override
		public Value parse(String input) throws ValueParserException {
			try {
				return new ImmutableLocalDateValue(LocalDate.parse(input));
			} catch (DateTimeParseException e) {
				throw new ValueParserException(e);
			}
		}
	};

	private final LocalDate localDate;

	public ImmutableLocalDateValue(LocalDate localDate) {
		this.localDate = localDate;
	}

	@Override
	public int hashCode() {
		return localDate.hashCode();
	}

	@Override
	protected boolean equalsValue(Value value) {
		ImmutableLocalDateValue other = (ImmutableLocalDateValue) value;
		return this.localDate.equals(other.localDate);
	}

	@Override
	public String toString() {
		return localDate.toString();
	}

	@Override
	public LocalDate getLocalDate() {
		return localDate;
	}

}
