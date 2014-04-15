package ws.palladian.core;

public abstract class AbstractValue implements Value {

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract String toString();

}
