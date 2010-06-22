package tud.iir.persistence;

import java.util.HashSet;

import tud.iir.knowledge.Source;

/**
 * Sources can be predefined in an xml file.
 * 
 * @author David Urbansky
 */
public class PredefinedSource {
    private Source source;
    private String conceptName;
    private HashSet<String> attributeNames;

    public PredefinedSource(Source source, String conceptName) {
        attributeNames = new HashSet<String>();
        this.init(source, conceptName);
    }

    public PredefinedSource(Source source, String conceptName, HashSet<String> attributeNames) {
        this.setAttributeNames(attributeNames);
        this.init(source, conceptName);
    }

    private void init(Source source, String conceptName) {
        this.setSource(source);
        this.setConceptName(conceptName);
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public String getConceptName() {
        return conceptName;
    }

    public void setConceptName(String conceptName) {
        this.conceptName = conceptName;
    }

    public HashSet<String> getAttributeNames() {
        return attributeNames;
    }

    public void setAttributeNames(HashSet<String> attributeNames) {
        this.attributeNames = attributeNames;
    }
}