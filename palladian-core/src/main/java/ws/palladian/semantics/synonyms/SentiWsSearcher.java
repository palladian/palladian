package ws.palladian.semantics.synonyms;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.MapBag;
import ws.palladian.helper.io.FileHelper;

/**
 * <p>
 * Search SentiWs for conjugations of words.
 * </p>
 * 
 * @see http://asv.informatik.uni-leipzig.de/download/sentiws.html
 * @author David Urbansky
 * 
 */
public class SentiWsSearcher {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SentiWsSearcher.class);

    /** Hold all groups of versions. */
    private MapBag versions = new MapBag();

    /** Hold all basic forms. **/
    private Map<Set<String>, String> basics = CollectionHelper.newHashMap();


    public SentiWsSearcher(String sentiWsFilePath) {
        List<String> lines = FileHelper.readFileToArray(sentiWsFilePath);

        for (String line : lines) {
            String[] split = line.split("\t");
            if (split.length < 3) {
                continue;
            }

            String bagKey = split[0].replaceAll("\\|.*", "");
            String[] conjugations = split[2].split(",");
            for (String conjugation : conjugations) {
                versions.add(bagKey, conjugation);
            }

            basics.put(versions.getBag(bagKey),bagKey);
        }

        LOGGER.info(versions.getAllBagEntries().size() + " words with multiple words created");
    }

    /**
     * <p>
     * Get different versions for the input word.
     * </p>
     * 
     * @param inputWord The word for which you need different versions.
     * @return All version of that word.
     */
    public Set<String> getVersions(String inputWord) {
        return versions.getBag(inputWord);
    }

    /**
     * <p>
     * Get the base version of the input word.
     * </p>
     * 
     * @param inputWord The word for which you need basic version.
     * @return The basic version of that word.
     */
    public String getBasicVersion(String inputWord) {
        return basics.get(versions.getBag(inputWord));
    }

    public static void main(String[] args) {
        SentiWsSearcher swss = new SentiWsSearcher("data/models/Synonyms/SentiWS_v1.8c_Positive.txt");
        CollectionHelper.print(swss.getVersions("gut"));
        System.out.println(swss.getBasicVersion("guter"));
    }

}
