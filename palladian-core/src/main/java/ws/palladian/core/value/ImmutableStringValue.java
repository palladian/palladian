package ws.palladian.core.value;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.value.io.AbstractValueParser;
import ws.palladian.core.value.io.ValueParser;

public final class ImmutableStringValue extends AbstractValue implements NominalValue {
	
	public static final ValueParser PARSER = new AbstractValueParser(ImmutableStringValue.class) {
		@Override
		public Value parse(String input) {
			return ImmutableStringValue.valueOf(input);
		}
	};
	
	private static final int MAX_CACHE_SIZE = 10000;

	private static final ConcurrentMap<String, ImmutableStringValue> CACHE = new ConcurrentHashMap<>();

    private final String stringValue;

    /** @deprecated Use {@link #valueOf(String)} instead, because it provides cached values. */
    @Deprecated
    public ImmutableStringValue(String value) {
        Validate.notNull(value, "value must not be null");
        this.stringValue = value;
    }
    
    public static ImmutableStringValue valueOf(String string) {
    	if (CACHE.size() > MAX_CACHE_SIZE) { // brute-force
    		CACHE.clear();
    	}
    	ImmutableStringValue value = CACHE.get(string);
    	if (value == null) {
    		final ImmutableStringValue newValue = new ImmutableStringValue(string);
    		value = CACHE.putIfAbsent(string, newValue);
    		if (value == null) {
    			value = newValue;
    		}
    	}
    	return value;
	}

    @Override
    public String getString() {
        return stringValue;
    }

    @Override
    public String toString() {
        return stringValue;
    }

    @Override
    public int hashCode() {
        return stringValue.hashCode();
    }

    @Override
    protected boolean equalsValue(Value value) {
        ImmutableStringValue other = (ImmutableStringValue)value;
        return stringValue.equals(other.stringValue);
    }

}
