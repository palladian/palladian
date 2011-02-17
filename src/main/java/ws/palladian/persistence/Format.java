package ws.palladian.persistence;

/**
 * A format for an attribute that can be specified in an xml file if xsd data types are not enough.
 * 
 * @author David Urbansky
 */
public class Format {
    private String concept;
    private String attribute;
    private String description;

    public Format(String concept, String attribute, String description) {
        setConcept(concept);
        setAttribute(attribute);
        setDescription(description);
    }

    public String getConcept() {
        return concept;
    }

    public void setConcept(String concept) {
        this.concept = concept;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}