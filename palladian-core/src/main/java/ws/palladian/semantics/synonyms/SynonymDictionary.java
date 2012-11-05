package ws.palladian.semantics.synonyms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * <p>
 * This is a very simple but fast implementation of a simple synonym dictionary. This dictionary can be created with the
 * {@link SynonymDictionaryCreator} in any language.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class SynonymDictionary extends HashMap<String, List<String>> {

    private static final long serialVersionUID = 7243176985494991325L;

    public void addSynonym(String word, String synonym) {
        List<String> list = get(word);
        if (list == null) {
            list = new ArrayList<String>();
            put(word, list);
        }
        if (!list.contains(synonym)) {
            list.add(synonym);
        }
    }

    public List<String> get(String key) {        
        List<String> list = super.get(key);
        if(list == null){
            list = new ArrayList<String>();
            list.add(key);
        }        
        return list;        
    }
    
    

}
