package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConceptSearchVocabulary {

    public String mobilePhone;
    public String printer;
    public String headphone;
    public String movie;
    public String car;

    public String weakMIOs;

    public List<String> getVocByConceptName(String conceptName) {
        Map<String, List<String>> attributeMap = attributesToMap();
        return attributeMap.get(conceptName);

    }

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

    private List<String> parseStringToList(String input) {

        List<String> outputList = new ArrayList<String>();
        String outputArray[] = input.split(",");
        for (int i = 0; i < outputArray.length; i++) {
            outputList.add(outputArray[i].trim());

        }
        return outputList;
    }

}
