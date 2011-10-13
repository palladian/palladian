package ws.palladian.extraction.keyphrase.extractors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.extraction.keyphrase.Keyphrase;
import ws.palladian.extraction.keyphrase.KeyphraseExtractor;
import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;

/**
 * 
 * 
 * 
 * 
 * 
 * http://www.opencalais.com/documentation/opencalais-web-service-api/api-metadata-english/api-metadata-english-social-
 * tags
 * 
 * 
 * @author Philipp Katz
 * 
 */
public class OpenCalaisSocialTagger extends KeyphraseExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(OpenCalaisSocialTagger.class);

    public static void main(String[] args) {

        OpenCalaisSocialTagger extractor = new OpenCalaisSocialTagger();
        List<Keyphrase> keywords = extractor
                .extract("The world's largest maker of solar inverters announced Monday that it will locate its first North American manufacturing plant in Denver. \"We see a huge market coming in the U.S.,\" said Pierre-Pascal Urbon, the company's chief financial officer. Solar inverters convert the direct current created by solar panels into an alternating current accessible to the larger electrical grid. The company, based in Kassel, north of Frankfurt, Germany, boasts growing sales of about $1.2 billion a year. \"We are creating economic opportunity,\" said Gov. Bill Ritter at a press conference. He added that creating core manufacturing jobs will help Colorado escape the recession sooner.");
        CollectionHelper.print(keywords);

    }

    /** OpenCalais API key. */
    private String apiKey = "";

    public OpenCalaisSocialTagger() {
        final PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();
        apiKey = config.getString("api.opencalais.key");
    }

    @Override
    public List<Keyphrase> extract(String inputText) {

        List<Keyphrase> keyphrases = new ArrayList<Keyphrase>();

        Map<String, String> header = new HashMap<String, String>();

        // set mandatory parameters
        header.put("x-calais-licenseID", apiKey);

        // set input content type
        header.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        // set response/output format
        header.put("Accept", "application/json");

        // create the content of the request
        Map<String, String> content = new HashMap<String, String>();
        String paramsXML = "<c:params xmlns:c=\"http://s.opencalais.com/1/pred/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><c:processingDirectives c:contentType=\"text/raw\" c:outputFormat=\"application/json\" c:enableMetadataType=\"SocialTags\"></c:processingDirectives><c:userDirectives c:allowDistribution=\"true\" c:allowSearch=\"true\"></c:userDirectives></c:params>";
        content.put("content", inputText);
        content.put("paramsXML", paramsXML);

        String response = null;
        DocumentRetriever retriever = new DocumentRetriever();
        try {
            HttpResult postResult = retriever.httpPost("http://api.opencalais.com/tag/rs/enrich", header, content);
            response = new String(postResult.getContent());
        } catch (HttpException e) {
            LOGGER.error(e);
        }

        if (response != null) {
            // parse the JSON response
            try {

                JSONObject json = new JSONObject(response);

                @SuppressWarnings("unchecked")
                Iterator<String> keys = json.keys();
                while (keys.hasNext()) {

                    String key = keys.next();
                    JSONObject jsonObj = json.getJSONObject(key);

                    if (jsonObj.has("_typeGroup") && jsonObj.get("_typeGroup").equals("socialTag")) {

                        String name = jsonObj.getString("name");
                        int importance = jsonObj.getInt("importance");
                        LOGGER.debug(name + " " + importance);

                        keyphrases.add(new Keyphrase(name, importance));

                    }

                }

            } catch (JSONException e) {
                LOGGER.error(e);
            }

        }

        return keyphrases;

    }

    @Override
    public String getExtractorName() {
        return "OpenCalais SocialTags";
    }

    @Override
    public boolean needsTraining() {
        return false;
    }

}
