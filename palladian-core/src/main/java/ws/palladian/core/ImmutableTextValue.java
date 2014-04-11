package ws.palladian.core;

import org.apache.commons.lang3.Validate;

final class ImmutableTextValue implements TextValue {
    
    private final String text;

    public ImmutableTextValue(String text) {
        Validate.notNull(text, "text must not be null");
        this.text = text;
    }

    @Override
    public String getText() {
        return text;
    }

}
