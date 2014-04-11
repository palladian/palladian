package ws.palladian.core;

public final class NullValue implements Value {
    
    public static final NullValue INSTANCE = new NullValue();
    
    private NullValue() {
        // use the singleton.
    }

}
