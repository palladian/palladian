package tud.iir.knowledge;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import tud.iir.normalization.UnitNormalizer;

public class AttributeRange {
    public static final int UNIT_UNITLESS = 0;
    public static final int UNIT_TIME = 1;
    public static final int UNIT_DIGITAL = 2;
    public static final int UNIT_FREQUENCY = 3;
    public static final int UNIT_LENGTH = 4;
    public static final int UNIT_WEIGHT = 5;

    public static final String RANGETYPE_MINMAX = "MINMAX";
    public static final String RANGETYPE_POSS = "POSS";

    private String rangeConcept; // id of the concept this attribute value is connected to
    private String rangeType;
    private int unitType = -1;

    private ArrayList<String> rangePossValues = new ArrayList<String>();
    private String rangeMinValue;
    private String rangeMaxValue;

    private boolean hasPossValue = false;
    private boolean hasMinValue = false;
    private boolean hasMaxValue = false;

    public AttributeRange(String rangeConcept) {
        this.rangeConcept = rangeConcept;
    }

    public boolean hasPossValue() {
        return hasPossValue;
    }

    public boolean hasMinValue() {
        return hasMinValue;
    }

    public boolean hasMaxValue() {
        return hasMaxValue;
    }

    public String getRangeMinValue() {
        return rangeMinValue;
    }

    public String getRangeMaxValue() {
        return rangeMaxValue;
    }

    public ArrayList<String> getRangePossValues() {
        return this.rangePossValues;
    }

    public String getRangeString() {
        String rangeString = "";

        if (this.hasPossValue) {
            Iterator<String> itRangePossValues = this.rangePossValues.iterator();
            while (itRangePossValues.hasNext()) {
                rangeString += itRangePossValues.next();
                if (itRangePossValues.hasNext())
                    rangeString += "; ";
            }
        }
        if (this.hasMinValue) {
            rangeString += this.getRangeMinValue();
        }
        if (this.hasMaxValue) {
            rangeString += " | " + this.getRangeMaxValue();
        }
        return rangeString;
    }

    public boolean addRangeValue(String rangeValue, int valueType) {

        // ADD POSSIBLE VALUE
        if (this.rangeType == RANGETYPE_POSS) {
            if (valueType == Attribute.VALUE_NUMERIC) {
                if (this.unitType == -1)
                    this.unitType = UnitNormalizer.getUnitType(rangeValue);
                else if (this.unitType != UnitNormalizer.getUnitType(rangeValue))
                    return false; // unit type of new range value has to be the same
                try {
                    double newValue = UnitNormalizer.getNormalizedNumber(rangeValue);
                    String newValueUnit = UnitNormalizer.getUnitTypeName(rangeValue);
                    String newValueString = String.valueOf(newValue) + " " + newValueUnit;
                    this.rangePossValues.add(newValueString);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            if (valueType == Attribute.VALUE_DATE) {
                // DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                // try {
                // Date attributeValueDate = df.parse(rangeValue); // checks if the date format is correct
                this.rangePossValues.add(rangeValue); // if there is no error, the string will be added
                // } catch (ParseException e) {
                // e.printStackTrace();
                // return false;
                // }
            }
            if (valueType == Attribute.VALUE_STRING) {
                this.rangePossValues.add(rangeValue);
            }
            if (this.hasPossValue == false)
                this.hasPossValue = true;
            return true;
        }
        // ADD MIN MAX VALUE
        else if (this.rangeType == RANGETYPE_MINMAX) {

            if (valueType == Attribute.VALUE_NUMERIC) {
                if (this.unitType == -1)
                    this.unitType = UnitNormalizer.getUnitType(rangeValue);
                else if (this.unitType != UnitNormalizer.getUnitType(rangeValue))
                    return false; // unit type of new range value has to be the same

                double newValue = UnitNormalizer.getNormalizedNumber(rangeValue);
                String newValueUnit = UnitNormalizer.getUnitTypeName(rangeValue);
                if (this.hasMaxValue && this.hasMinValue) { // both already set
                    double minValue = UnitNormalizer.getNormalizedNumber(this.rangeMinValue);
                    double maxValue = UnitNormalizer.getNormalizedNumber(this.rangeMaxValue);
                    if (newValue > maxValue) {
                        this.rangeMaxValue = String.valueOf((newValue)) + " " + newValueUnit;
                    } else if (newValue < minValue) {
                        this.rangeMinValue = String.valueOf((newValue)) + " " + newValueUnit;
                    }
                } else if (this.hasMinValue) { // only a min value is set
                    double minValue = UnitNormalizer.getNormalizedNumber(this.rangeMinValue);
                    if (newValue > minValue) {
                        this.rangeMaxValue = String.valueOf((newValue)) + " " + newValueUnit;
                        this.hasMaxValue = true;
                    } else if (newValue < minValue) {
                        this.rangeMaxValue = this.rangeMinValue;
                        this.rangeMinValue = String.valueOf((newValue)) + " " + newValueUnit;
                        this.hasMaxValue = true;
                    }
                } else { // no values set yet
                    this.rangeMinValue = String.valueOf((newValue)) + " " + newValueUnit;
                    this.hasMinValue = true;
                }
                return true;
            } else if (valueType == Attribute.VALUE_DATE) {
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    Date attributeValueDate = df.parse(rangeValue);
                    if (this.hasMaxValue && this.hasMinValue) { // both already set
                        Date minValueDate = df.parse(this.rangeMinValue);
                        Date maxValueDate = df.parse(this.rangeMaxValue);
                        if (attributeValueDate.compareTo(minValueDate) < 0) {
                            this.rangeMinValue = rangeValue;
                        } else if (attributeValueDate.compareTo(maxValueDate) > 0) {
                            this.rangeMaxValue = rangeValue;
                        }
                    } else if (this.hasMinValue) { // only a min value is set
                        Date minValueDate = df.parse(this.rangeMinValue);
                        if (attributeValueDate.compareTo(minValueDate) > 0) {
                            this.rangeMaxValue = rangeValue;
                            this.hasMaxValue = true;
                        } else if (attributeValueDate.compareTo(minValueDate) < 0) {
                            this.rangeMaxValue = this.rangeMinValue;
                            this.rangeMinValue = rangeValue;
                            this.hasMaxValue = true;
                        }
                    } else { // no values set yet
                        this.rangeMinValue = rangeValue;
                        this.hasMinValue = true;
                    }

                    return true;
                } catch (ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return false;
                }
            } else if (valueType == Attribute.VALUE_BOOLEAN) {
                if (rangeValue.equalsIgnoreCase("true")) {
                    this.rangeMinValue = rangeValue;
                    this.hasMinValue = true;
                }
                if (rangeValue.equalsIgnoreCase("false")) {
                    this.rangeMaxValue = rangeValue;
                    this.hasMaxValue = true;
                }
            }
        }
        return false;

    }

    public void removeRangeValue(String rangeValueString) {
        if (this.rangeType == RANGETYPE_POSS) {
            this.rangePossValues.remove(rangeValueString);
            if (this.rangePossValues.isEmpty())
                this.hasPossValue = false;
        } else if (this.rangeType == RANGETYPE_MINMAX) {
            if (rangeValueString.equalsIgnoreCase(this.rangeMaxValue))
                this.hasMaxValue = false;
            if (rangeValueString.equalsIgnoreCase(this.rangeMinValue))
                this.hasMinValue = false;
        }
    }

    public void clearRangeValues() {
        this.rangePossValues.clear();
        this.rangeMaxValue = "";
        this.rangeMinValue = "";
        this.hasPossValue = false;
        this.hasMinValue = false;
        this.hasMaxValue = false;
    }

    public String getRangeType() {
        return rangeType;
    }

    public void setRangeType(String rangeType) {
        this.rangeType = rangeType;
    }

    public String getRangeConcept() {
        return rangeConcept;
    }
}
