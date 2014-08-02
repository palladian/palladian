package ws.palladian.core.value;

import org.apache.commons.lang3.Validate;

public final class ImmutableTextValue extends AbstractValue implements TextValue {

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
        ImmutableTextValue other = (ImmutableTextValue)value;
        return textValue.equals(other.textValue);
    }

}
