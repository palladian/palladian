/**
 * This is the central configuration of InCoFi. It is automatically mapped from InCoFiConfiguration.yml.
 * 
 * @author Martin Werner
 */

package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Class ConceptSearchVocabulary.
 */
public class InCoFiConfiguration {

    /** The mobile phone. */
    public transient String mobilephone;

    /** The printer. */
    public transient String printer;

    /** The headphone. */
    public transient String headphone;

    /** The movie. */
    public transient String movie;

    /** The car. */
    public transient String car;

    /** The universal. */
    public transient String universal;

    /** The weak MIOs. */
    public transient String weakMIOs;

    /** The result count. */
    public transient int resultCount;

    /** The search engine. */
    public transient int searchEngine;

    /** The url downloader threads. */
    public transient int urlDownloaderThreads;

    /** The excluding trust limit. */
    public transient double excludingTrustLimit;

    /** The tempDirectoryPath. */
    public transient String tempDirPath;

    /** The RolePage Trust Limit *. */
    public transient double rolePageTrustLimit;

    /** The role page relevance value. */
    public transient int rolePageRelevanceValue;

    /** Analyze SWFContent. */
    public transient boolean analyzeSWFContent;

    /** Indicator for limiting the linkAnalyzing. */
    public transient boolean limitLinkAnalyzing;

    /** The relevant MIOTypes. */
    public transient String mioTypes;

    /** Check if redo with focus on weak-interaction. */
    public transient boolean redoWeak;

    /** The bad words. */
    public transient String badWords;

    /** The weak interaction indicators. */
    public transient String weakInteractionIndicators;

    /** The strong interaction indicators. */
    public transient String strongInteractionIndicators;

    /** Unclear-interactive MIOs are associated with. */
    public transient String associateUnclearMIOsWith;

    /** The instance. */
    public static InCoFiConfiguration instance = null;

    /**
     * Gets the single instance of InCoFiConfiguration.
     * 
     * @return single instance of InCoFiConfiguration
     */
    public static InCoFiConfiguration getInstance() {
        return instance;
    }

    /**
     * Gets the mioTypes.
     * 
     * @return the mioTypes
     */
    public List<String> getMIOTypes() {
        return parseStringToList(mioTypes);
    }

    /**
     * Gets the bad words.
     * 
     * @return the bad words
     */
    public List<String> getBadWords() {
        return parseStringToList(badWords);
    }

    /**
     * Gets the strong interaction indicators.
     * 
     * @return the strong interaction indicators
     */
    public List<String> getStrongInteractionIndicators() {
        return parseStringToList(strongInteractionIndicators);
    }

    /**
     * Gets the weak interaction indicators.
     * 
     * @return the weak interaction indicators
     */
    public List<String> getWeakInteractionIndicators() {
        return parseStringToList(weakInteractionIndicators);
    }

    /**
     * Gets the weak MIO searchVocabulary.
     * 
     * @return the weak MIO searchVocabulary
     */
    public List<String> getWeakMIOVocabulary() {
        return parseStringToList(weakMIOs);
    }

    /**
     * Gets the searchVocabulary by concept name. If there doesn't exist a concrete conceptVocabulary, the universal-Set
     * is used.
     * 
     * @param conceptName the concept name
     * @return the searchVocabulary by concept name
     */
    public List<String> getVocByConceptName(String conceptName) {

        String cName = conceptName.toLowerCase(Locale.ENGLISH);
        Map<String, List<String>> attributeMap = attributesToMap();
        if (!attributeMap.containsKey(cName)) {
            return attributeMap.get("universal");
        }
        return attributeMap.get(cName);
    }

    /**
     * This method parses a string to generate a list, finally all list are put into a map.
     * 
     * @return the map
     */
    private Map<String, List<String>> attributesToMap() {
        final Map<String, List<String>> attributeMap = new HashMap<String, List<String>>();
        attributeMap.put("mobilephone", parseStringToList(mobilephone));
        attributeMap.put("printer", parseStringToList(printer));
        attributeMap.put("headphone", parseStringToList(headphone));
        attributeMap.put("movie", parseStringToList(movie));
        attributeMap.put("car", parseStringToList(car));
        attributeMap.put("universal", parseStringToList(universal));

        attributeMap.put("weakmios", parseStringToList(weakMIOs));

        return attributeMap;
    }

    /**
     * Parses the string to list.
     * 
     * @param input the input
     * @return the list
     */
    private List<String> parseStringToList(final String input) {

        final List<String> outputList = new ArrayList<String>();
        final String outputArray[] = input.split(",");
        for (int i = 0; i < outputArray.length; i++) {
            outputList.add(outputArray[i].trim());

        }
        return outputList;
    }

}
