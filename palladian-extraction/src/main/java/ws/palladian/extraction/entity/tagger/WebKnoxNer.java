package ws.palladian.extraction.entity.tagger;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.extraction.entity.Annotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.helper.ConfigHolder;
import ws.palladian.retrieval.DocumentRetriever;

/**
 * <p>
 * The WebKnoxNer wraps the PalladianNer and offers the functionality over a REST API. See here for more information
 * http://localhost/webknox/api#!/text/entities_GET.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class WebKnoxNer extends NamedEntityRecognizer implements Serializable {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(WebKnoxNer.class);

    private final String appId;
    private final String apiKey;

    public WebKnoxNer(String appId, String apiKey) {
        this.appId = appId;
        this.apiKey = apiKey;
    }

    public WebKnoxNer(Configuration configuration) {
        this.appId = configuration.getString("api.webknox.appId");
        this.apiKey = configuration.getString("api.webknox.apiKey");
    }
    
    @Override
    public Annotations getAnnotations(String inputText, String configModelFilePath) {
        
        DocumentRetriever retriever = new DocumentRetriever();
        String url = "http://webknox.com/api/text/entities?text=";
        try {
            url += URLEncoder.encode(inputText, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            LOGGER.error(e1.getMessage());
        }
        url += "&appId=" + appId;
        url += "&apiKey=" + apiKey;
        JSONArray result = retriever.getJsonArray(url);

        Annotations annotations = new Annotations();
        try {
            for (int i = 0; i < result.length(); i++) {
                JSONObject a = result.getJSONObject(i);
                Annotation annotation = new Annotation(a.getInt("offset"), a.getString("entity"), a.getString("type"));
                annotations.add(annotation);
            }
        } catch (JSONException e) {
            LOGGER.error(e.getMessage());
        }

        return annotations;
    }

    @Override
    public Annotations getAnnotations(String inputText) {
        return getAnnotations(inputText, "");
    }

    @Override
    public String getModelFileEnding() {
        LOGGER.warn(getName() + " does not support loading models, therefore we don't know the file ending");
        return "";
    }

    @Override
    public boolean setsModelFileEndingAutomatically() {
        LOGGER.warn(getName() + " does not support loading models, therefore we don't know the file ending");
        return false;
    }

    @Override
    public boolean train(String trainingFilePath, String modelFilePath) {
        LOGGER.warn(getName() + " does not support training");
        return false;
    }

    @Override
    public boolean loadModel(String configModelFilePath) {
        LOGGER.warn(getName() + " does not support loading models");
        return false;
    }

    public static void main(String[] args) {
        WebKnoxNer webKnoxNer = new WebKnoxNer(ConfigHolder.getInstance().getConfig());
        System.out.println(webKnoxNer.tag("Bill Gates founded Microsoft in April 1975"));
    }
	
}