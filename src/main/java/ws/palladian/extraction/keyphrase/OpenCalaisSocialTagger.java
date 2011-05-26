package ws.palladian.extraction.keyphrase;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.collection.CollectionHelper;
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
        PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();
        apiKey = config.getString("api.opencalais.key");
    }

    @Override
    public List<Keyphrase> extract(String inputText) {

        List<Keyphrase> keyphrases = new ArrayList<Keyphrase>();

        HttpPost postMethod = new HttpPost("http://api.opencalais.com/tag/rs/enrich");

        // set mandatory parameters
        postMethod.setHeader("x-calais-licenseID", apiKey);

        // set input content type
        postMethod.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        // set response/output format
        postMethod.setHeader("Accept", "application/json");

        // create the content of the request
        try {
            String paramsXML = "<c:params xmlns:c=\"http://s.opencalais.com/1/pred/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><c:processingDirectives c:contentType=\"text/raw\" c:outputFormat=\"application/json\" c:enableMetadataType=\"SocialTags\"></c:processingDirectives><c:userDirectives c:allowDistribution=\"true\" c:allowSearch=\"true\"></c:userDirectives></c:params>";
            List<NameValuePair> data = new ArrayList<NameValuePair>();
            data.add(new BasicNameValuePair("content", inputText));
            data.add(new BasicNameValuePair("paramsXML", paramsXML));
            HttpEntity entity = new UrlEncodedFormEntity(data);
            postMethod.setEntity(entity);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e);
        }

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
                    LOGGER.debug(name + " " + importance);

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
        List<Keyphrase> keywords = extractor
                .extract("The world's largest maker of solar inverters announced Monday that it will locate its first North American manufacturing plant in Denver. \"We see a huge market coming in the U.S.,\" said Pierre-Pascal Urbon, the company's chief financial officer. Solar inverters convert the direct current created by solar panels into an alternating current accessible to the larger electrical grid. The company, based in Kassel, north of Frankfurt, Germany, boasts growing sales of about $1.2 billion a year. \"We are creating economic opportunity,\" said Gov. Bill Ritter at a press conference. He added that creating core manufacturing jobs will help Colorado escape the recession sooner.");
        CollectionHelper.print(keywords);

    }

}
