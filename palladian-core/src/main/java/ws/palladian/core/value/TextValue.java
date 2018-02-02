package ws.palladian.core.value;

// TODO reconsider whether it's really necessary to have a TextValue and a NominalValue
public interface TextValue extends Value {
    
    String getText();

}
