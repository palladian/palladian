/**
 * 
 * 
 * @author Martin Werner
 */

package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Class ConceptSearchVocabulary.
 */
public class InCoFiConfiguration {

    /** The mobile phone. */
    public transient String mobilePhone;

    /** The printer. */
    public transient String printer;

    /** The headphone. */
    public transient String headphone;

    /** The movie. */
    public transient String movie;

    /** The car. */
    public transient String car;

    /** The weak MIOs. */
    public transient String weakMIOs;
    
    /** The result count. */
    public transient int resultCount;
    
    /** The search engine. */
    public transient int searchEngine;
    
    /** The temp dir path. */
    public transient String tempDirPath;
    
    /** The role page relevance value. */
    public transient int rolePageRelevanceValue;
    
    /** The analyze swf content. */
    public transient boolean analyzeSWFContent;
    
    /** The mio types. */
    public transient String mioTypes;
    
    /** The bad words. */
    public transient String badWords;
    
    /** The weak interaction indicators. */
    public transient String weakInteractionIndicators;
    
    /** The strong interaction indicators. */
    public transient String strongInteractionIndicators;

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
     * Gets the mIO types.
     *
     * @return the mIO types
     */
    public List<String> getMIOTypes(){
        return parseStringToList(mioTypes);
              
    }
    
    /**
     * Gets the bad words.
     *
     * @return the bad words
     */
    public List<String> getBadWords(){
        return parseStringToList(badWords);
    }
    
    /**
     * Gets the strong interaction indicators.
     *
     * @return the strong interaction indicators
     */
    public List<String> getStrongInteractionIndicators(){
        return parseStringToList(strongInteractionIndicators);
    }
    
    /**
     * Gets the weak interaction indicators.
     *
     * @return the weak interaction indicators
     */
    public List<String> getWeakInteractionIndicators(){
        return parseStringToList(weakInteractionIndicators);
    }

    /**
     * Gets the searchVocabulary by concept name.
     * 
     * @param conceptName the concept name
     * @return the searchVocabulary by concept name
     */
    public List<String> getVocByConceptName(final String conceptName) {
        String cName= conceptName;
//        String modConceptName = conceptName.toLowerCase(Locale.ENGLISH);
        if ("mobile phone".equalsIgnoreCase(conceptName)) {
            cName = "mobilePhone";
        }
        final Map<String, List<String>> attributeMap = attributesToMap();
        return attributeMap.get(cName);

    }

    /**
     * Attributes to map.
     * 
     * @return the map
     */
    private Map<String, List<String>> attributesToMap() {
       final Map<String, List<String>> attributeMap = new HashMap<String, List<String>>();
        attributeMap.put("mobilePhone", parseStringToList(mobilePhone));
        attributeMap.put("printer", parseStringToList(printer));
        attributeMap.put("headphone", parseStringToList(headphone));
        attributeMap.put("movie", parseStringToList(movie));
        attributeMap.put("car", parseStringToList(car));

        attributeMap.put("weakMIOs", parseStringToList(weakMIOs));

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
