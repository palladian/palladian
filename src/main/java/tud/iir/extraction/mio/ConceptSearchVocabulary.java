/**
 * 
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
public class ConceptSearchVocabulary {

    /** The mobile phone. */
    public String mobilePhone;

    /** The printer. */
    public String printer;

    /** The headphone. */
    public String headphone;

    /** The movie. */
    public String movie;

    /** The car. */
    public String car;

    /** The weak MIOs. */
    public String weakMIOs;

    /**
     * Gets the searchVocabulary by concept name.
     * 
     * @param conceptName the concept name
     * @return the searchVocabulary by concept name
     */
    public List<String> getVocByConceptName(String conceptName) {
        String modConceptName = conceptName.toLowerCase(Locale.ENGLISH);
        if (modConceptName.equals("mobile phone")){
            modConceptName="mobilePhone";
        }
        Map<String, List<String>> attributeMap = attributesToMap();
        return attributeMap.get(modConceptName);

    }

    /**
     * Attributes to map.
     * 
     * @return the map
     */
    private Map<String, List<String>> attributesToMap() {
        Map<String, List<String>> attributeMap = new HashMap<String, List<String>>();
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
    private List<String> parseStringToList(String input) {

        List<String> outputList = new ArrayList<String>();
        String outputArray[] = input.split(",");
        for (int i = 0; i < outputArray.length; i++) {
            outputList.add(outputArray[i].trim());

        }
        return outputList;
    }

}
