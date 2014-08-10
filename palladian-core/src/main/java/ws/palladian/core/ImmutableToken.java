package ws.palladian.core;

import org.apache.commons.lang3.Validate;

public class ImmutableToken extends AbstractToken {

    private final int startPosition;
    private final String value;

    public ImmutableToken(int startPosition, String value) {
        Validate.isTrue(startPosition >= 0, "startPosition cannot be negative.");
        Validate.notEmpty(value, "value must not be empty");
        this.startPosition = startPosition;
        this.value = value;
    }

    @Override
    public int getStartPosition() {
        return startPosition;
    }

    @Override
    public String getValue() {
        return value;
    }

}
