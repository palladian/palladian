package ws.palladian.core;

public class ImmutableSpan extends AbstractSpan {

    private final int startPosition;
    private final String value;

    public ImmutableSpan(int startPosition, String value) {
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
