package ws.palladian.core.value;

import ws.palladian.core.value.io.AbstractValueParser;
import ws.palladian.core.value.io.ValueParser;
import ws.palladian.core.value.io.ValueParserException;

public final class ImmutableLongValue extends AbstractValue implements LongValue {
	
	public static final ValueParser PARSER = new AbstractValueParser(ImmutableLongValue.class) {
		@Override
		public Value parse(String input) throws ValueParserException {
			try {
				return ImmutableLongValue.valueOf(Long.parseLong(input));
			} catch (NumberFormatException e) {
				throw new ValueParserException(e);
			}
		}
	};
	
	private static final class ValueCache {
		private static final ImmutableLongValue[] CACHE;
		private static final int LOW = -128;
		private static final int HIGH = 127;

		static {
			CACHE = new ImmutableLongValue[HIGH - LOW + 1];
			int j = LOW;
			for (int i = 0; i < CACHE.length; i++) {
				CACHE[i] = new ImmutableLongValue(j++);
			}
		}
	}

	private final long longValue;

	/** @deprecated Use {@link #valueOf(int)} instead, because it provides cached values. */
	@Deprecated
	public ImmutableLongValue(long longValue) {
		this.longValue = longValue;
	}

	public static ImmutableLongValue valueOf(long longValue) {
		if (longValue >= ValueCache.LOW && longValue <= ValueCache.HIGH) {
			return ValueCache.CACHE[(int) (longValue + -ValueCache.LOW)];
		}
		return new ImmutableLongValue(longValue);
	}

	@Override
	public double getDouble() {
		return longValue;
	}

	@Override
	public long getLong() {
		return longValue;
	}

	@Override
	public int hashCode() {
		return Long.valueOf(longValue).hashCode();
	}

	@Override
	protected boolean equalsValue(Value value) {
		ImmutableLongValue other = (ImmutableLongValue) value;
		return longValue == other.longValue;
	}

	@Override
	public String toString() {
		return String.valueOf(longValue);
	}

	@Override
	public float getFloat() {
		return longValue;
	}

	@Override
	public int getInt() {
		return (int) longValue;
	}

	@Override
	public Number getNumber() {
		return longValue;
	}

}
