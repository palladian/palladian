package tud.iir.extraction;

import java.util.HashMap;

import org.apache.log4j.Logger;

/**
 * In the ExtractionType class the different extraction types are defined. Also the trust for each extraction type can be calculated.
 * 
 * @author David Urbansky
 */
public final class ExtractionType {

    public static final int UNKNOWN = 0;

    // entity extraction types
    public static final int USER_INPUT = 15; // user queried for that entity

    // fact extraction types
    public static final int FREE_TEXT_SENTENCE = 1; // sentence from free text, the attribute name appears in the text, e.g.
    // "The i8510 INNOV8 offers 16GB of built-in memory in addition to a microSD card slot for even more storage options"
    public static final int STRUCTURED_PHRASE = 2; // information gained from a structured tags, that is for example a recurring div structure that replaced a
    // typical table structure
    public static final int TABLE_CELL = 3; // content of table cell, e.g. "16GB internal, microSD card slot"
    public static final int PATTERN_PHRASE = 4; // only text after a certain pattern is taken, e.g. "the Y of X is..."
    public static final int COLON_PHRASE = 5; // structured or unstructured but only text after colon ":" is taken
    public static final int IMAGE = 6; // image queries

    // extract entities from phrases
    public static final int ENTITY_PHRASE = 7;

    // use focused crawling for list extraction
    public static final int ENTITY_FOCUSED_CRAWL = 8;

    // use seeds to extract entities from lists
    public static final int ENTITY_SEED = 9;

    public static double initialTrust = 0.5;
    private static Integer[] correctExtractions = { 0, 0, 0, 0, 0, 0, 0 };
    private static Integer[] totalExtractions = { 0, 0, 0, 0, 0, 0, 0 };

    // extraction types can be weighted by type (concept or attribute)
    private static HashMap<String, HashMap<Integer, Integer>> correctExtractionsByType = new HashMap<String, HashMap<Integer, Integer>>();
    private static HashMap<String, HashMap<Integer, Integer>> totalExtractionsByType = new HashMap<String, HashMap<Integer, Integer>>();

    /**
     * Every extraction type has a trust between 0 and 1 (which is the precision of the extraction type).
     * 
     * @param extractionType The extraction type constant.
     * @return The trust for the given extraction type.
     */
    public static double getTrust(int extractionType) {
        try {
            int extractionCount = totalExtractions[extractionType];
            if (extractionCount == 0)
                return initialTrust;
            return (double) correctExtractions[extractionType] / (double) extractionCount;
        } catch (IndexOutOfBoundsException e) {
            Logger.getRootLogger().error("extraction type:" + extractionType, e);
        }
        return 0;
    }

    /**
     * Get the extraction type trust by type (concept, attribute or data type).
     * 
     * @param extractionType The extraction type constant.
     * @param type A string that specifies the type.
     * @return The trust for the given extraction type.
     */
    public static double getTrust(int extractionType, String type) {
        if (totalExtractionsByType.get(type) == null || totalExtractionsByType.get(type).get(extractionType) == null)
            return initialTrust;
        if (correctExtractionsByType.get(type) == null || correctExtractionsByType.get(type).get(extractionType) == null)
            return 0.0;
        return (double) correctExtractionsByType.get(type).get(extractionType) / (double) totalExtractionsByType.get(type).get(extractionType);
    }

    public static void addExtraction(int extractionType, boolean correct) {
        if (correct)
            correctExtractions[extractionType]++;
        totalExtractions[extractionType]++;
    }

    public static void addExtractionByType(int extractionType, String type, boolean correct) {
        int value = 1;
        if (totalExtractionsByType.get(type) == null)
            totalExtractionsByType.put(type, new HashMap<Integer, Integer>());
        if (totalExtractionsByType.get(type).get(extractionType) != null) {
            value = totalExtractionsByType.get(type).get(extractionType) + 1;
        }
        totalExtractionsByType.get(type).put(extractionType, value);
        if (correct) {
            value = 1;
            if (correctExtractionsByType.get(type) == null)
                correctExtractionsByType.put(type, new HashMap<Integer, Integer>());
            if (correctExtractionsByType.get(type).get(extractionType) != null) {
                value = correctExtractionsByType.get(type).get(extractionType) + 1;
            }
            correctExtractionsByType.get(type).put(extractionType, value);
        }
    }
    /*
     * public static void main(String[] a) { addExtraction(ExtractionType.COLON_PHRASE,true); addExtraction(ExtractionType.COLON_PHRASE,false);
     * System.out.println(getTrust(COLON_PHRASE)); System.out.println(getTrust(FREE_TEXT_SENTENCE)); }
     */
}