package ws.palladian.core;

import org.apache.commons.lang3.Validate;

final class ImmutableTextValue extends AbstractValue implements TextValue {

    private final String text;

    public ImmutableTextValue(String text) {
        Validate.notNull(text, "text must not be null");
        this.text = text;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ImmutableTextValue other = (ImmutableTextValue)obj;
        return text.equals(other.text);
    }

}
