package tud.iir.knowledge;

import java.util.HashSet;
import java.util.Iterator;

import tud.iir.helper.DateHelper;
import tud.iir.helper.StringHelper;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * The knowledge unit attribute.
 * 
 * @author David Urbansky
 */
public class Attribute extends Extractable {

    private static final long serialVersionUID = -5223874467162620482L;

    // types for extraction
    public static final int VALUE_NUMERIC = 1;
    public static final int VALUE_STRING = 2;
    public static final int VALUE_DATE = 3;
    public static final int VALUE_BOOLEAN = 4;
    public static final int VALUE_IMAGE = 5;
    public static final int VALUE_VIDEO = 6;
    public static final int VALUE_AUDIO = 7;
    public static final int VALUE_MIXED = 8; // string and number
    public static final int VALUE_URI = 9; // string and number

    private HashSet<String> synonyms; // different names for the attribute
    private int valueType; // format for extraction
    private String saveType; // format type for owl or database
    private String regExp = null;
    private int valueCount = 1; // how many values are expected for the attribute
    private Concept concept;
    private HashSet<Source> predefinedSources;

    private HashSet<AttributeRange> attributeRanges = new HashSet<AttributeRange>();

    // needed for ontofly edit mode
    private HashSet<String> newSynonyms = new HashSet<String>();
    boolean hasNewSynonyms = false;
    private HashSet<AttributeRange> attributeRangesToDelete = new HashSet<AttributeRange>();
    private String newName;

    public Attribute(String name, int valueType, Concept concept) {
        init(name, valueType, concept);
    }

    public Attribute(String name, int valueType, Concept concept, String regExp) {
        setRegExp(regExp);
        init(name, valueType, concept);
    }

    public Attribute(String name, int valueType, Concept concept, double trust) {
        setTrust(trust);
        init(name, valueType, concept);
    }

    private void init(String name, int valueType, Concept concept) {
        setName(name.toLowerCase());
        setValueType(valueType);
        setConcept(concept);
        predefinedSources = new HashSet<Source>();
        synonyms = new HashSet<String>();
        sources = new Sources<Source>();
    }

    public static int getValueTypeByName(String name) {
        if (name.equalsIgnoreCase("number")) {
            return VALUE_NUMERIC;
        }
        if (name.equalsIgnoreCase("string")) {
            return VALUE_STRING;
        }
        if (name.equalsIgnoreCase("date")) {
            return VALUE_DATE;
        }
        if (name.equalsIgnoreCase("boolean")) {
            return VALUE_BOOLEAN;
        }
        if (name.equalsIgnoreCase("image")) {
            return VALUE_IMAGE;
        }
        if (name.equalsIgnoreCase("video")) {
            return VALUE_VIDEO;
        }
        if (name.equalsIgnoreCase("mixed")) {
            return VALUE_MIXED;
        }
        return -1;
    }

    public boolean hasSynonym(String name) {
        Iterator<String> synIterator = getSynonyms().iterator();
        while (synIterator.hasNext()) {
            String synonym = synIterator.next();
            if (synonym.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public HashSet<String> getSynonyms() {
        return synonyms;
    }

    public String getSynonymsToString() {
        StringBuilder synonymString = new StringBuilder();

        Iterator<String> sIterator = getSynonyms().iterator();
        while (sIterator.hasNext()) {
            synonymString.append(sIterator.next()).append("; ");
        }

        return synonymString.toString().substring(0, Math.max(0, synonymString.length() - 2));
    }

    public void setSynonyms(HashSet<String> synonyms) {
        this.synonyms = synonyms;
    }

    public void addSynonym(String synonym) {
        if (!synonym.equalsIgnoreCase(getName())) {
            this.synonyms.add(synonym);
        }
    }

    /**
     * Take a fact string of unknown value type and try to guess which type it is.
     * 
     * @param factString The unknown fact string.
     * @param mode If 1 a numeric data type is assumed if fact string contains a number, in mode 2 fact string must start with a number.
     * @return The guessed value type.
     */
    public static int guessValueType(String factString, int mode) {
        // if string starts with a number, it is assumed to be of the numeric data type
        factString = StringHelper.trim(factString);

        if (factString.length() == 0) {
            return VALUE_MIXED;
        }

        if (StringHelper.containsNumber(factString) && mode == 1) {
            return VALUE_NUMERIC;
        } else if (StringHelper.isNumber(factString.substring(0, 1)) && mode == 2) {
            return VALUE_NUMERIC;
        } else

        // if string is "yes" or "no",it is assumed to be of the boolean data type
        if (factString.toLowerCase().startsWith("yes") || factString.toLowerCase().startsWith("no")) {
            return VALUE_BOOLEAN;
        } else

        // if string represents a date, it is assumed to be of the date data type
        if (DateHelper.containsDate(factString)) {
            return VALUE_DATE;
        } else

        // if string contains a proper noun, it is assumed to be of the string data type
        if (StringHelper.containsProperNoun(factString)) {
            return VALUE_STRING;
        }

        // if everything fails, the mixed data type is returned
        return VALUE_MIXED;
    }

    public int getValueType() {
        return valueType;
    }

    public String getValueTypeName() {
        switch (valueType) {
            case VALUE_NUMERIC:
                return "number";
            case VALUE_STRING:
                return "string";
            case VALUE_DATE:
                return "date";
            case VALUE_BOOLEAN:
                return "boolean";
            case VALUE_IMAGE:
                return "image";
            case VALUE_VIDEO:
                return "video";
            case VALUE_MIXED:
                return "mixed";
            default:
                break;
        }

        return "unknown";
    }

    public Resource getValueTypeXSD() {
        switch (valueType) {
            case VALUE_NUMERIC:
                return XSD.xdouble;
            case VALUE_STRING:
                return XSD.xstring;
            case VALUE_DATE:
                return XSD.date;
            case VALUE_BOOLEAN:
                return XSD.xboolean;
            case VALUE_IMAGE:
                return XSD.xstring;
            case VALUE_VIDEO:
                return XSD.xstring;
            case VALUE_MIXED:
                return XSD.xstring;
            default:
                break;
        }

        return XSD.xstring;
    }

    public void setValueType(int valueType) {
        this.valueType = valueType;
    }

    public String getSaveType() {
        return saveType;
    }

    public void setSaveType(String saveType) {
        this.saveType = saveType;
    }

    public String getRegExp() {
        if (regExp != null)
            return regExp;
        return RegExp.getRegExp(this.getValueType());
    }

    public void setRegExp(String regExp) {
        this.regExp = regExp;
    }

    public Concept getConcept() {
        return concept;
    }

    private void setConcept(Concept concept) {
        concept.addAttribute(this);
        this.concept = concept;
    }

    public HashSet<Source> getPredefinedSources() {
        return predefinedSources;
    }

    public void setPredefinedSources(HashSet<Source> predefinedSources) {
        this.predefinedSources = predefinedSources;
    }

    public void addPredefinedSource(Source source) {
        this.predefinedSources.add(source);
    }

    public int getValueCount() {
        return valueCount;
    }

    public void setValueCount(int valueCount) {
        this.valueCount = valueCount;
    }

    public boolean isExtracted() {
        if (getExtractedAt() != null) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return getName() + "(Trust:" + getTrust() + ")";
    }

    /*
     * ONTOFLY METHODS
     */

    public String getNewName() {
        return newName;
    }

    public String getSafeNewName() {
        return StringHelper.makeSafeName(getNewName());
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    public String getRangeString() {
        Iterator<AttributeRange> itAttrRanges = this.attributeRanges.iterator();
        String rangeString = "";
        while (itAttrRanges.hasNext()) {
            AttributeRange range = itAttrRanges.next();
            rangeString += range.getRangeConcept() + " <span class='range'>[ " + range.getRangeString() + " ]</span><br/>";
        }
        return rangeString;
    }

    public HashSet<AttributeRange> getAttributeRanges() {
        return this.attributeRanges;
    }

    public HashSet<AttributeRange> getAttributeRangesToDelete() {
        return attributeRangesToDelete;
    }

    public void addRangeNodeDummies(String rangeConceptName, String rangeType) {
        if (rangeType.equalsIgnoreCase("MINMAX")) {
            AttributeRange attributeRangeMinMax = new AttributeRange(rangeConceptName);
            attributeRangeMinMax.setRangeType(AttributeRange.RANGETYPE_MINMAX);

            if (this.valueType == Attribute.VALUE_BOOLEAN) { // if the range is boolean we have to add true and false as min max value
                attributeRangeMinMax.addRangeValue("true", this.valueType);
                attributeRangeMinMax.addRangeValue("false", this.valueType);
            }

            this.attributeRanges.add(attributeRangeMinMax);
        } else if (rangeType.equalsIgnoreCase("POSS")) {
            AttributeRange attributeRangePoss = new AttributeRange(rangeConceptName);
            attributeRangePoss.setRangeType(AttributeRange.RANGETYPE_POSS);
            this.attributeRanges.add(attributeRangePoss);
        }
    }

    public void addRangeValue(AttributeRange rangeValueItem) {
        this.attributeRanges.add(rangeValueItem);
    }

    public AttributeRange getRange(String conceptName) {
        Iterator<AttributeRange> it = this.attributeRanges.iterator();
        while (it.hasNext()) {
            AttributeRange attr = it.next();
            if (attr.getRangeConcept().equalsIgnoreCase(conceptName)) {
                return attr;
            }
        }
        return null;
    }

    public void removeRange(AttributeRange range) {
        this.attributeRanges.remove(range);
        this.attributeRangesToDelete.add(range);
    }

    public boolean addRangeValue(String rangeValueString, String rangeConceptName) {
        // AttributeRange newRangeValueItem = new AttributeRange(rangeConceptName);
        AttributeRange attrange = this.getRange(rangeConceptName);
        if (attrange != null) {
            return attrange.addRangeValue(rangeValueString, this.getValueType());
        } else {
            return false;
        }
    }

    public void removeRangeValue(String rangeValue, String rangeConceptName) {
        for (AttributeRange rangeValueItem : this.attributeRanges) {
            if (rangeValueItem.getRangeConcept().equalsIgnoreCase(rangeConceptName)) {
                rangeValueItem.removeRangeValue(rangeValue);
                break;
            }
        }
    }

    public void clearRangeValues() {
        for (AttributeRange rangeValueItem : this.attributeRanges) {
            rangeValueItem.clearRangeValues();
        }
    }

    public HashSet<String> getNewSynonyms() {
        return newSynonyms;
    }

    public void setNewSynonyms(HashSet<String> newSynonyms) {
        this.newSynonyms = newSynonyms;
        this.hasNewSynonyms = true;
    }

    public boolean hasNewSynonyms() {
        return hasNewSynonyms;
    }
}