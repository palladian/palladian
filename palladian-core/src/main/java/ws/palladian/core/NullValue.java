package ws.palladian.core;

public final class NullValue implements Value {
    
    public static final NullValue NULL = new NullValue();
    
    private NullValue() {
        // use the singleton.
    }

}
