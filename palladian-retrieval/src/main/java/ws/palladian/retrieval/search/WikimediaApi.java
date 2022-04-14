package ws.palladian.retrieval.search;

import org.apache.commons.codec.digest.DigestUtils;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.parser.json.JsonObject;

import java.util.HashSet;
import java.util.Set;

/**
 * Access the wikimedia API.
 * See https://www.wikidata.org/wiki/Wikidata:Data_access
 *
 * @author David Urbansky
 * @since 19-Feb-22 at 21:00
 **/
public class WikimediaApi {
    /**
     * Get the object by id, e.g. Q42
     *
     * @param id Must start with Q
     * @return A json object with all information about the entity.
     */
    public static JsonObject getEntityById(String id) {
        JsonObject responseJson = new DocumentRetriever().tryGetJsonObject("https://www.wikidata.org/wiki/Special:EntityData/" + id + ".json");
        if (responseJson == null) {
            return null;
        }
        JsonObject matchingEntity = responseJson.tryQueryJsonObject("entities/" + id);
        if (matchingEntity != null) {
            return matchingEntity;
        }

        JsonObject entities = responseJson.tryGetJsonObject("entities");
        if (entities != null) {
            String firstKey = CollectionHelper.getFirst(entities.keySet());
            return entities.tryGetJsonObject(firstKey);
        }

        return null;
    }

    public static Set<String> getLabelsId(String id) {
        JsonObject jsonObject = new DocumentRetriever().tryGetJsonObject("https://www.wikidata.org/w/api.php?action=wbgetentities&ids=" + id + "&props=labels&format=json");
        JsonObject labelsJso = jsonObject.tryQueryJsonObject("entities/" + id + "/labels");
        Set<String> labels = new HashSet<>();
        for (Object value : labelsJso.values()) {
            JsonObject labelJso = ((JsonObject) value);
            labels.add(labelJso.tryGetString("value"));
        }
        return labels;
    }

    public static String getImageUrl(String fileName) {
        String md5 = DigestUtils.md5Hex(fileName.replace(" ", "_"));
        String hash1 = md5.substring(0, 1);
        String hash2 = md5.substring(0, 2);

        return "https://upload.wikimedia.org/wikipedia/commons/" + hash1 + "/" + hash2 + "/" + fileName;
    }

    public static void main(String[] args) {
        //        System.out.println(WikimediaApi.getEntityById("Q1731").toString(2));
        CollectionHelper.print(WikimediaApi.getLabelsId("Q1731"));
    }
}
