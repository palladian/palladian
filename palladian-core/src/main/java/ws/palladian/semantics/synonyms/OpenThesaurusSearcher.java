package ws.palladian.semantics.synonyms;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * Search Open Thesuaurus for synonyms of German words.
 * </p>
 * 
 * @see http://www.openthesaurus.de/
 * @author David Urbansky
 * 
 */
public class OpenThesaurusSearcher {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenThesaurusSearcher.class);

    /** Hold all groups of versions. */
    private Map<String, Set<String>> synonyms = new HashMap<String, Set<String>>();

    public OpenThesaurusSearcher(String openThesaurusFile) {
        List<String> lines = FileHelper.readFileToArray(openThesaurusFile);

        for (String line : lines) {
            String[] split = line.split(";");
            if (split.length < 2) {
                continue;
            }

            for (int i = 0; i < split.length; i++) {
                String synonym = StringHelper.trim(split[i]);

                Set<String> set = synonyms.get(synonym);
                if (set == null) {
                    set = new HashSet<String>();
                }
                for (int j = 0; j < split.length; j++) {
                    set.add(StringHelper.trim(split[j]));
                }

                synonyms.put(synonym, set);
            }
        }

        LOGGER.info(synonyms.size() + " words with multiple synonyms created");
    }

    /**
     * <p>
     * Get synonyms for a word.
     * </p>
     * 
     * @param inputWord The word for which you need different synonyms.
     * @return All synonyms of that word.
     */
    public Set<String> getSynonyms(String inputWord) {
        return synonyms.get(inputWord);
    }

    public static void main(String[] args) {
        OpenThesaurusSearcher swss = new OpenThesaurusSearcher("openthesaurus.txt");
        CollectionHelper.print(swss.getSynonyms("toll"));
    }
}