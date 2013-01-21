package ws.palladian.processing.features;

public interface Annotated {

    public abstract int getStartPosition();

    public abstract int getEndPosition();

    public abstract int getIndex();
    
    public abstract String getTag();
    
    public abstract String getValue();

}
