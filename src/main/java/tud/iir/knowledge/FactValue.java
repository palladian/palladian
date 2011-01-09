package tud.iir.knowledge;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.log4j.Logger;

import tud.iir.extraction.ExtractionProcessManager;
import tud.iir.extraction.ExtractionType;
import tud.iir.extraction.fact.NumericFactDistribution;
import tud.iir.helper.MathHelper;
import tud.iir.helper.StringHelper;

/**
 * The knowledge unit fact value.
 * 
 * @author David Urbansky
 */
public class FactValue implements Serializable {

    /**
	 * <p>
	 * 
	 * </p>
	 */
	private static final long serialVersionUID = 1460843012795886296L;

	// the fact the fact value belongs to
    private Fact fact = null;

    // identification
    private String value = "";
    
    private double trust=0;



    // value of fact before normalization
    private String originalValue = "";

    private Date extractedAt = null;
    private ArrayList<Source> sources;

    // private ArrayList<Integer> extractionTypes;

    public FactValue(String value, Source source, int extractionType) {
        this.sources = new ArrayList<Source>();
        // this.extractionTypes = new ArrayList<Integer>();
        setValue(value);
        addSource(source);
        // this.addExtractionType(extractionType);
    }

    public Fact getFact() {
        return fact;
    }

    public void setFact(Fact fact) {
        this.fact = fact;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value.substring(0, Math.min(value.length(), 255));
    }

    public String getOriginalValue() {
        return originalValue;
    }

    public void setOriginalValue(String originalValue) {
        this.originalValue = originalValue.substring(0, Math.min(originalValue.length(), 255));
    }

    public ArrayList<Source> getSources() {
        return sources;
    }

    public void setSources(ArrayList<Source> sources) {
        this.sources = sources;
    }

    public void addSource(Source source) {
        source.setFactValue(this);
        sources.add(source);
    }

    public void removeSource(Source source) {
        sources.remove(source);
    }

    /**
     * Get extraction types used to extract that value.
     * 
     * @param oncePerSource If true each extraction type counts only once per source, e.g. 11 sentence extractions from two different pages will be 2 instead of
     *            11.
     * @return An array of extraction types.
     */
    public ArrayList<Integer> getExtractionTypes(boolean oncePerSource) {
        ArrayList<Integer> extractionTypes = new ArrayList<Integer>();
        HashSet<String> sourcesReviewed = new HashSet<String>();
        for (int i = 0, l = this.sources.size(); i < l; ++i) {
            Source currentSource = this.sources.get(i);
            if (oncePerSource && sourcesReviewed.add(currentSource.getUrl())) {
                extractionTypes.add(currentSource.getExtractionType());
            } else if (!oncePerSource) {
                extractionTypes.add(currentSource.getExtractionType());
            }
        }
        return extractionTypes;
    }

    // public void setExtractionTypes(ArrayList<Integer> extractionTypes) {
    // this.extractionTypes = extractionTypes;
    // }
    // public void addExtractionType(int extractionType) {
    // this.extractionTypes.add(extractionType);
    // }
    // public void addExtractionTypes(ArrayList<Integer> extractionTypes) {
    // this.extractionTypes.addAll(extractionTypes);
    // }

    /**
     * Get the corroboration for the fact value.
     * 
     * @return The trust.
     */
    public double getCorroboration() {
        switch (ExtractionProcessManager.getTrustFormula()) {
            case ExtractionProcessManager.QUANTITY_TRUST:
                return getCorroboration1();
            case ExtractionProcessManager.SOURCE_TRUST:
                return getCorroboration2();
            case ExtractionProcessManager.EXTRACTION_TYPE_TRUST:
                return getCorroboration3();
            case ExtractionProcessManager.COMBINED_TRUST:
                return getCorroboration4();
            case ExtractionProcessManager.CROSS_TRUST:
                return getCorroboration5();
        }
        return getCorroboration1();
    }

    /**
     * Simple counting corroboration, the more sources the higher corroboration.
     * 
     * @return The trust.
     */
    public double getCorroboration1() {
        double corroboration = 0.0;

        // count each url only once for each fact value
        HashSet<String> sourceURLs = new HashSet<String>();

        Iterator<Source> sIt = getSources().iterator();
        while (sIt.hasNext()) {
            Source s = sIt.next();
            if (sourceURLs.add(s.getUrl())) {
                corroboration += s.getTrust();
            }
        }
        return MathHelper.round(corroboration, 2);
    }

    /**
     * Variety corroboration. The more different extraction types and sources were used to extract that value, the higher the corroboration.
     * 
     * @return The trust.
     */
    public double getCorroboration2() {

        double corroboration = 0.0;

        // same trust in every extraction type, only number of different types has influence
        double etn = getNumberOfDifferentExtractionTypes() * ExtractionType.initialTrust;

        double sn = getNumberOfDifferentSources();

        // count each url only once for each fact value
        HashSet<String> sourceURLs = new HashSet<String>();

        double addedSourceTrust = 0.0;
        Iterator<Source> sIt = getSources().iterator();
        while (sIt.hasNext()) {
            Source s = sIt.next();
            if (sourceURLs.add(s.getUrl())) {
                addedSourceTrust += s.getTrust();
            }
        }

        corroboration = etn + sn * addedSourceTrust;

        return MathHelper.round(corroboration, 2);
    }

    public double getCorroboration3() {

        double corroboration = 0.0;

        // calculate sum of extraction type trust
        double addedETypeTrust = 0.0;
        ArrayList<Integer> eTypes = getExtractionTypes(true);
        for (int i = 0, l = eTypes.size(); i < l; i++) {
            addedETypeTrust += ExtractionType.getTrust(eTypes.get(i));
            // addedETypeTrust += ExtractionType.getTrust(eTypes.get(i),this.getFact().getAttribute().getDomain().getName());
        }

        // double etn = getNumberOfDifferentExtractionTypes();

        corroboration = addedETypeTrust;

        return MathHelper.round(corroboration, 2);
    }

    /**
     * Extraction type trust and source applicability.
     * 
     * @return The trust.
     */
    public double getCorroboration4() {

        double corroboration = 0.0;

        // calculate sum of extraction type trust
        double addedETypeTrust = 0.0;
        ArrayList<Integer> eTypes = getExtractionTypes(true);
        for (int i = 0, l = eTypes.size(); i < l; i++) {
            addedETypeTrust += ExtractionType.getTrust(eTypes.get(i));
        }

        // double etn = getNumberOfDifferentExtractionTypes();
        //
        // double sn = getNumberOfDifferentSources();

        // count each url only once for each fact value
        // HashSet<String> sourceURLs = new HashSet<String>();

        double addedSourceTrust = 0.0;
        Iterator<Source> sIt = getSources().iterator();
        while (sIt.hasNext()) {
            Source s = sIt.next();
            // if (sourceURLs.add(s.getUrl()))
            addedSourceTrust += s.getTrust();
        }
        // double averageSourceTrust = addedSourceTrust / sourceURLs.size();

        // corroboration = addedETypeTrust + addedSourceTrust;
        // corroboration = addedETypeTrust + (sn * addedSourceTrust);
        // corroboration = addedETypeTrust * averageSourceTrust;

        // corroboration = addedETypeTrust * averageSourceTrust;
        // if (getFact().getAttribute().getValueType() == Attribute.VALUE_NUMBER) {
        // corroboration *= NumberFactDistribution.getPowerDistributionFactor(getFact().getID(), Double.valueOf(getValue()));
        // }

        corroboration = addedSourceTrust;// * etn * sn;

        return MathHelper.round(corroboration, 2);
    }

    public double getCorroboration5() {

        double corroboration = 0.0;

        // calculate sum of extraction type trust
        double addedETypeTrust = 0.0;
        ArrayList<Integer> eTypes = getExtractionTypes(true);
        for (int i = 0, l = eTypes.size(); i < l; i++) {
            addedETypeTrust += ExtractionType.getTrust(eTypes.get(i));
        }

        // double etn = getNumberOfDifferentExtractionTypes();

        // double sn = getNumberOfDifferentSources();
        //
        // // count each url only once for each fact value
        // HashSet<String> sourceURLs = new HashSet<String>();
        //
        double addedSourceTrust = 0.0;
        Iterator<Source> sIt = getSources().iterator();
        while (sIt.hasNext()) {
            Source s = sIt.next();
            // if (sourceURLs.add(s.getUrl()))
            addedSourceTrust += s.getTrust();
        }
        // double averageSourceTrust = addedSourceTrust / sourceURLs.size();

        // corroboration = addedETypeTrust + addedSourceTrust;
        // corroboration = addedETypeTrust + (sn * addedSourceTrust);
        // corroboration = addedETypeTrust * averageSourceTrust;

        // corroboration = addedETypeTrust * averageSourceTrust;
        // if (getFact().getAttribute().getValueType() == Attribute.VALUE_NUMBER) {
        // corroboration *= NumberFactDistribution.getPowerDistributionFactor(getFact().getID(), Double.valueOf(getValue()));
        // }

        corroboration = addedSourceTrust;// * etn * sn;

        if (getFact().getAttribute().getValueType() == Attribute.VALUE_NUMERIC && StringHelper.isNumericExpression(getValue())) {
            // get number of fact values for that fact with the same power
            int samePowerValues = getFact().getSamePowerFactValues(this);
            try {
                corroboration *= Math.pow(NumericFactDistribution.getPowerDistributionFactor(getFact().getID(), Double.valueOf(getValue())), samePowerValues);
            } catch (NumberFormatException e) {
                Logger.getRootLogger().error(getValue() + ", " + e.getMessage());
            }
        }

        return MathHelper.round(corroboration, 2);
    }

    public double getRelativeTrust() {
        double relativeTrust = 0.0;

        double trustSum = 0.0;

        Fact fact = getFact();
        ArrayList<FactValue> allFactValues = fact.getValues(false);
        Iterator<FactValue> factValueIterator = allFactValues.iterator();
        while (factValueIterator.hasNext()) {
            FactValue fv = factValueIterator.next();
            trustSum += fv.getCorroboration();
        }

        relativeTrust = getCorroboration() / trustSum;

        return relativeTrust;
    }

    private int getNumberOfDifferentSources() {
        HashSet<String> differentSources = new HashSet<String>();

        for (int i = 0, l = this.sources.size(); i < l; ++i) {
            differentSources.add(this.sources.get(i).getUrl());
        }

        return differentSources.size();
    }

    private int getNumberOfDifferentExtractionTypes() {
        int extractionTypesUsed = 0;
        if (getNumberOfFreeTextExtractions() > 0) {
            ++extractionTypesUsed;
        }
        if (getNumberOfPatternPhraseExtractions() > 0) {
            ++extractionTypesUsed;
        }
        if (getNumberOfColonPhraseExtractions() > 0) {
            ++extractionTypesUsed;
        }
        if (getNumberOfStructuredPhraseExtractions() > 0) {
            ++extractionTypesUsed;
        }
        if (getNumberOfTableCellExtractions() > 0) {
            ++extractionTypesUsed;
        }
        return extractionTypesUsed;
    }

    private int getNumberOfFreeTextExtractions() {
        int countFreeTextSentence = 0;

        HashSet<String> sourcesReviewed = new HashSet<String>();
        for (int i = 0, l = this.sources.size(); i < l; ++i) {
            Source currentSource = this.sources.get(i);
            if (sourcesReviewed.add(currentSource.getUrl())) {
                if (currentSource.getExtractionType() == ExtractionType.FREE_TEXT_SENTENCE) {
                    ++countFreeTextSentence;
                }
            }
        }

        return countFreeTextSentence;
    }

    private int getNumberOfPatternPhraseExtractions() {
        int countPatternPhrase = 0;

        HashSet<String> sourcesReviewed = new HashSet<String>();
        for (int i = 0, l = this.sources.size(); i < l; ++i) {
            Source currentSource = this.sources.get(i);
            if (sourcesReviewed.add(currentSource.getUrl())) {
                if (currentSource.getExtractionType() == ExtractionType.PATTERN_PHRASE) {
                    ++countPatternPhrase;
                }
            }
        }

        return countPatternPhrase;
    }

    private int getNumberOfColonPhraseExtractions() {
        int countColonPhrase = 0;

        HashSet<String> sourcesReviewed = new HashSet<String>();
        for (int i = 0, l = this.sources.size(); i < l; ++i) {
            Source currentSource = this.sources.get(i);
            if (sourcesReviewed.add(currentSource.getUrl())) {
                if (currentSource.getExtractionType() == ExtractionType.COLON_PHRASE) {
                    ++countColonPhrase;
                }
            }
        }

        return countColonPhrase;
    }

    private int getNumberOfStructuredPhraseExtractions() {
        int countStructuredPhrase = 0;

        HashSet<String> sourcesReviewed = new HashSet<String>();
        for (int i = 0, l = this.sources.size(); i < l; ++i) {
            Source currentSource = this.sources.get(i);
            if (sourcesReviewed.add(currentSource.getUrl())) {
                if (currentSource.getExtractionType() == ExtractionType.STRUCTURED_PHRASE) {
                    ++countStructuredPhrase;
                }
            }
        }

        return countStructuredPhrase;
    }

    private int getNumberOfTableCellExtractions() {
        int countTableCell = 0;

        HashSet<String> sourcesReviewed = new HashSet<String>();
        for (int i = 0, l = this.sources.size(); i < l; ++i) {
            Source currentSource = this.sources.get(i);
            if (sourcesReviewed.add(currentSource.getUrl())) {
                if (currentSource.getExtractionType() == ExtractionType.TABLE_CELL) {
                    ++countTableCell;
                }
            }
        }

        return countTableCell;
    }

    public Date getExtractedAt() {
        return extractedAt;
    }

    public void setExtractedAt(Date extractedAt) {
        this.extractedAt = extractedAt;
    }
    
    public double getTrust() {
        return trust;
    }

    public void setTrust(double trust) {
        this.trust = trust;
    }

    @Override
    public String toString() {
        String extractionTypeInformation = " (";

        int countFreeTextSentence = getNumberOfFreeTextExtractions();
        int countPatternPhrase = getNumberOfPatternPhraseExtractions();
        int countColonPhrase = getNumberOfColonPhraseExtractions();
        int countStructuredPhrase = getNumberOfStructuredPhraseExtractions();
        int countTableCell = getNumberOfTableCellExtractions();

        boolean shownSomething = false;
        if (countFreeTextSentence > 0) {
            extractionTypeInformation += countFreeTextSentence + "x sentence";
            shownSomething = true;
        }
        if (countPatternPhrase > 0) {
            if (shownSomething) {
                extractionTypeInformation += ", ";
            }
            extractionTypeInformation += countPatternPhrase + "x pattern phrase";
            shownSomething = true;
        }
        if (countColonPhrase > 0) {
            if (shownSomething) {
                extractionTypeInformation += ", ";
            }
            extractionTypeInformation += countColonPhrase + "x colon pattern";
            shownSomething = true;
        }
        if (countStructuredPhrase > 0) {
            if (shownSomething) {
                extractionTypeInformation += ", ";
            }
            extractionTypeInformation += countStructuredPhrase + "x structured phrase";
            shownSomething = true;
        }
        if (countTableCell > 0) {
            if (shownSomething) {
                extractionTypeInformation += ", ";
            }
            extractionTypeInformation += countTableCell + "x table";
        }

        ArrayList<Source> sources = getSources();
        StringBuilder sourcesString = new StringBuilder();
        for (int i = 0, l = sources.size(); i < l; ++i) {
            sourcesString.append(sources.get(i).getUrl()).append("(").append(sources.get(i).getTrust()).append(")");
            if (i < l - 1) {
                sourcesString.append(",");
            }
        }

        extractionTypeInformation += ")";
        // if (getFact().getAttribute().getValueType() == Attribute.VALUE_NUMBER)
        // return
        // this.getValue()+"/"+this.getCorroboration()+extractionTypeInformation+" factor "+NumberFactDistribution.getPowerDistributionFactor(getFact().getID(),
        // Double.valueOf(getValue()))+"("+getFact().getSamePowerFactValues(this)+") from "+sourcesString.toString();
        return getValue() + "/" + MathHelper.round(getRelativeTrust(), 3) + "/" + getCorroboration() + extractionTypeInformation + " from "
                + sourcesString.toString();
    }
}