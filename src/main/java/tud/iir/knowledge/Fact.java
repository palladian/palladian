package tud.iir.knowledge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import tud.iir.extraction.fact.FactValueComparator;
import tud.iir.helper.MathHelper;

/**
 * The knowledge unit fact.
 * 
 * @author David Urbansky
 */
public class Fact {

    /** some values cannot be accurate, allow a margin therefore */
    public static final double CORRECTNESS_MARGIN = 0.15;
    private Attribute attribute;

    /** fact values with corroboration */
    private ArrayList<FactValue> values;

    /** if given, the correct value is known (used for benchmarking) */
    private FactValue correctValue = null;

    public Fact(Attribute attribute) {
        init();
        this.setAttribute(attribute);
    }

    public Fact(Attribute attribute, FactValue value) {
        init();
        this.setAttribute(attribute);
        this.addFactValue(value);
    }

    public Fact(Attribute attribute, String value, Source source, int extractionType) {
        init();
        this.setAttribute(attribute);
        this.addFactValue(new FactValue(value, source, extractionType));
    }

    private void init() {
        this.values = new ArrayList<FactValue>();
    }

    /**
     * Returns an identification string for the fact: "conceptAttribute".
     * 
     * @return An identification string for the fact.
     */
    public String getID() {
        return getAttribute().getConcept().getName() + getAttribute().getName();
    }

    public int getSamePowerFactValues(FactValue fv) {
        int samePowerFactValueNumber = 0;
        int power = MathHelper.getPower(fv.getValue());

        for (int i = 0, l = values.size(); i < l; i++) {
            // if (values.get(i) == fv) continue;
            int powerFactValue;
            try {
                powerFactValue = MathHelper.getPower(values.get(i).getValue());
                if (power == powerFactValue)
                    samePowerFactValueNumber++;
            } catch (NumberFormatException e) {
                Logger.getRootLogger().error(values.get(i).getValue() + "," + e.getMessage());
            }
        }

        return samePowerFactValueNumber;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }

    public ArrayList<FactValue> getValues() {
        return getValues(true, -1);
    }

    public ArrayList<FactValue> getValues(boolean sorted) {
        return getValues(sorted, -1);
    }

    public ArrayList<FactValue> getValues(boolean sorted, int limit) {
        if (sorted)
            Collections.sort(this.values, new FactValueComparator());
        if (limit > 0) {
            List<FactValue> sublist = this.values.subList(0, Math.min(this.values.size(), limit));
            ArrayList<FactValue> arraySubList = new ArrayList<FactValue>();
            Iterator<FactValue> subListIterator = sublist.iterator();
            while (subListIterator.hasNext()) {
                arraySubList.add(subListIterator.next());
            }
            return arraySubList;
        }
        return this.values;
    }

    public FactValue getFactValueForValue(String value) {
        FactValue fv = null;
        Iterator<FactValue> factValueIterator = this.values.iterator();
        while (factValueIterator.hasNext()) {
            fv = factValueIterator.next();
            if (fv.getValue().equalsIgnoreCase(value)) {
                return fv;
            }
        }
        return fv;
    }

    public void setValues(ArrayList<FactValue> value) {
        this.values = value;
    }

    public void addFactValue(FactValue factValue) {

        // check whether fact value has already been entered...
        boolean factValueEntered = false;
        Iterator<FactValue> factValueIterator = this.values.iterator();
        while (factValueIterator.hasNext()) {
            FactValue fv = factValueIterator.next();
            if (fv.getValue().equalsIgnoreCase(factValue.getValue())) {
                factValueEntered = true;
                break;
            }
        }
        // ...if not, enter it..
        if (!factValueEntered) {
            factValue.setFact(this);
            this.values.add(factValue);
        }
        // ...otherwise enter only the source and the extraction type
        else {
            FactValue factValueEntry = this.getFactValueForValue(factValue.getValue());
            factValueEntry.addSource(factValue.getSources().get(0));
            // factValueEntry.addExtractionTypes(factValue.getExtractionTypes());
        }
    }

    /**
     * For benchmarking set the correct value to compare with extracted ones.
     * 
     * @return The fact value.
     */
    public FactValue getCorrectValue() {
        return correctValue;
    }

    public void setCorrectValue(FactValue correctValue) {
        this.correctValue = correctValue;
    }

    /**
     * Return fact value with highest corroboration.
     * 
     * @return The fact value.
     */
    public FactValue getFactValue() {
        Collections.sort(this.values, new FactValueComparator());
        return this.values.get(0);
    }

    /**
     * Returns the value of fact value with highest corroboration.
     * 
     * @return The value of fact value with highest corroboration.
     */
    public String getValue() {
        Collections.sort(this.values, new FactValueComparator());
        return this.values.get(0).getValue();
    }

    /**
     * Get corroboration for the value that is most likely.
     * 
     * @return The trust.
     */
    public double getCorroboration() {
        return this.getFactValue().getCorroboration();
    }

    /**
     * Returns true when given fact value is either correct or almost correct.
     * 
     * @param factValue The fact value.
     * @return True if it is set correct, else false.
     */
    public boolean isCorrect() {
        return isCorrect(getValue());
    }

    public boolean isCorrect(String value) {
        if (isAbsoluteCorrect(value))
            return true;
        if (isAlmostCorrect(value))
            return true;
        return false;
    }

    /**
     * Tell whether most likely fact value is correct.
     * 
     * @return True if the fact is absolutely correct.
     */
    public boolean isAbsoluteCorrect() {
        return isAbsoluteCorrect(getValue());
    }

    public boolean isAbsoluteCorrect(String value) {

        if (correctValue != null) {

            // System.out.print(this.getAttribute().getName()+": "+correctValue.getValue()+" =? "+value);

            if (correctValue.getValue().equalsIgnoreCase(value)) {
                // System.out.println(" = true");
                return true;
            }

            // System.out.println(" = false");

        }
        // else if (this.getCorroboration() >= Filter.minFactCorroboration) return true;

        return false;
    }

    // CORRECTNESS_MARGIN for number values
    public boolean isAlmostCorrect() {
        return isAlmostCorrect(getValue());
    }

    public boolean isAlmostCorrect(String value) {
        if (correctValue != null && attribute.getValueType() == Attribute.VALUE_NUMERIC) {

            try {
                double num = Double.valueOf(value);
                double numMin = num - CORRECTNESS_MARGIN * num;
                double numMax = num + CORRECTNESS_MARGIN * num;

                double realNum = Double.valueOf(correctValue.getValue());

                if (realNum < numMax && realNum > numMin)
                    return true;
            } catch (NumberFormatException e) {
                Logger.getRootLogger().error(value + "," + e.getMessage());
            }

        }

        return false;
    }

    @Override
    public String toString() {
        return getAttribute().getName() + ":" + getFactValue().getValue();
    }

}