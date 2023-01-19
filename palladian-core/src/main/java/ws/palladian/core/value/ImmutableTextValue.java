package ws.palladian.core.value;

import org.apache.commons.lang3.Validate;
import ws.palladian.core.value.io.AbstractValueParser;
import ws.palladian.core.value.io.ValueParser;

public final class ImmutableTextValue extends AbstractValue implements TextValue {

    public static final ValueParser PARSER = new AbstractValueParser(ImmutableTextValue.class) {
        @Override
        public Value parse(String input) {
            return new ImmutableTextValue(input);
        }
    };

    private final String textValue;

    public ImmutableTextValue(String text) {
        Validate.notNull(text, "text must not be null");
        this.textValue = text;
    }

    @Override
    public String getText() {
        return textValue;
    }

    @Override
    public String toString() {
        return textValue;
    }

    @Override
    public int hashCode() {
        return textValue.hashCode();
    }

    @Override
    protected boolean equalsValue(Value value) {
        ImmutableTextValue other = (ImmutableTextValue) value;
        return textValue.equals(other.textValue);
    }

}
