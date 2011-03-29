package ws.palladian.extraction.keyphrase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.retrieval.HTTPPoster;

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

    /** OpenCalais API key. */
    private String apiKey = "";

    public OpenCalaisSocialTagger() {

        try {
            PropertiesConfiguration config = new PropertiesConfiguration("config/apikeys.conf");
            apiKey = config.getString("opencalais.api.key");
        } catch (ConfigurationException e) {
            LOGGER.error("could not get api key from config/apikeys.conf, " + e.getMessage());
        }

    }

    @Override
    public List<Keyphrase> extract(String inputText) {
        
        List<Keyphrase> keyphrases = new ArrayList<Keyphrase>();

        PostMethod postMethod = new PostMethod("http://api.opencalais.com/tag/rs/enrich");

        // set mandatory parameters
        postMethod.setRequestHeader("x-calais-licenseID", apiKey);

        // set input content type
        postMethod.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        // set response/output format
        postMethod.setRequestHeader("Accept", "application/json");

        // create the content of the request
        String paramsXML = "<c:params xmlns:c=\"http://s.opencalais.com/1/pred/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><c:processingDirectives c:contentType=\"text/raw\" c:outputFormat=\"application/json\" c:enableMetadataType=\"SocialTags\"></c:processingDirectives><c:userDirectives c:allowDistribution=\"true\" c:allowSearch=\"true\"></c:userDirectives></c:params>";
        NameValuePair[] data = { 
                new NameValuePair("content", inputText), 
                new NameValuePair("paramsXML", paramsXML) 
        };
        postMethod.setRequestBody(data);

        HTTPPoster poster = new HTTPPoster();
        String response = poster.handleRequest(postMethod);

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
                    LOGGER.info(name + " " + importance);
                    
                    keyphrases.add(new Keyphrase(name, importance));

                }

            }

        } catch (JSONException e) {
            LOGGER.error(e);
        }
        
        return keyphrases;

    }
    
    @Override
    public boolean needsTraining() {
        return false;
    }
    
    @Override
    public String getExtractorName() {
        return "OpenCalais SocialTags";
    }

    public static void main(String[] args) {

        OpenCalaisSocialTagger extractor = new OpenCalaisSocialTagger();
        extractor
                .extract("The world's largest maker of solar inverters announced Monday that it will locate its first North American manufacturing plant in Denver. \"We see a huge market coming in the U.S.,\" said Pierre-Pascal Urbon, the company's chief financial officer. Solar inverters convert the direct current created by solar panels into an alternating current accessible to the larger electrical grid. The company, based in Kassel, north of Frankfurt, Germany, boasts growing sales of about $1.2 billion a year. \"We are creating economic opportunity,\" said Gov. Bill Ritter at a press conference. He added that creating core manufacturing jobs will help Colorado escape the recession sooner.");

    }

}
