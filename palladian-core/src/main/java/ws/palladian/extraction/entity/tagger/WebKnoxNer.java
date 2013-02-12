package ws.palladian.extraction.entity.tagger;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.entity.Annotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.HttpHelper;

/**
 * <p>
 * The WebKnoxNer wraps the PalladianNer and offers the functionality over a REST API. See here for more information
 * http://localhost/webknox/api#!/text/entities_GET.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class WebKnoxNer extends NamedEntityRecognizer {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WebKnoxNer.class);

    private final HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();

    private final String apiKey;

    public WebKnoxNer(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
    }

    public WebKnoxNer(Configuration configuration) {
        this(configuration.getString("api.webknox.apiKey"));
    }

    @Override
    public Annotations getAnnotations(String inputText, String configModelFilePath) {

        HttpRequest request = new HttpRequest(HttpMethod.POST, "http://46.4.89.232:8080/text/entities?apiKey=" + apiKey);
        request.addParameter("text", inputText);

        Annotations annotations = new Annotations();
        String content;
        try {
            HttpResult httpResult = httpRetriever.execute(request);
            content = HttpHelper.getStringContent(httpResult);
        } catch (HttpException e) {
            throw new IllegalStateException("HTTP error while accessing the service: " + e.getMessage(), e);
        }

        try {
            JSONArray result = new JSONArray(content);
            for (int i = 0; i < result.length(); i++) {
                JSONObject currentItem = result.getJSONObject(i);
                if (currentItem.has("entity")) {
                    int offset = currentItem.getInt("offset");
                    String entity = currentItem.getString("entity");
                    String type = currentItem.getString("type");
                    annotations.add(new Annotation(offset, entity, type));
                } else {
                    LOGGER.debug("Ignore malformed entry in JSON response.");
                    /**
                     * FIXME There is a bug in the REST service, which might return things like the following, where one
                     * entry in the array is just empty. This should be fixed in WebKnox. 2013-02-12, Philipp.
                     * 
                     * <pre>
                     *        […]
                     *        {
                     *            "entity":"Jazz Club",
                     *            "length":9,
                     *            "type":"PER",
                     *            "normalizedEntity":"Jazz Club",
                     *            "offset":3267
                     *         },
                     *         {
                     * 
                     *         },
                     *         {
                     *            "entity":"Ronnie Scott",
                     *            "length":12,
                     *            "type":"PER",
                     *            "normalizedEntity":"Ronnie Scott",
                     *            "offset":3344
                     *         }, 
                     *         […]
                     * </pre>
                     */
                }
            }
        } catch (JSONException e) {
            throw new IllegalStateException("JSON parse error while processing response '" + content + "': "
                    + e.getMessage(), e);
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
        // WebKnoxNer webKnoxNer = new WebKnoxNer(ConfigHolder.getInstance().getConfig());
        WebKnoxNer webKnoxNer = new WebKnoxNer("v30170b8523o23il4bz3v04");
        System.out.println(webKnoxNer.tag("Bill Gates founded Microsoft in April 1975"));

        // System.out
        // .println(webKnoxNer
        // .tag("Andrew Mitchell resigns following allegations he called police 'plebs' Chief whip's resignation comes amid row over whether George Osborne tried to travel first-class on standard train ticket David Cameron has been delivered a humiliating blow, with a broken Andrew Mitchell quitting as chief whip, abandoning a month-long fight to save his career and fend off claims that he had referred to a police officer as a  'pleb '. Mitchell's decision – relayed to the prime minister personally in a meeting at Chequers after Cameron returned from an EU summit in Brussels – is a huge blow to the prime minister, who had stood by him in the face of an onslaught by the opposition and the Police Federation and growing doubts among his own backbenchers. Cameron used his authority at prime minister's questions on Wednesday to defend Mitchell from an attack by Ed Miliband and private threats to resign by the deputy chief whip, John Randall. The meeting between Mitchell and Cameron, at 4pm on Friday, came as a separate row was breaking over whether George Osborne had travelled in a first-class train carriage on a standard ticket. In his resignation statement, Mitchell said:  'Over the last two days it has become clear to me that whatever the rights and wrongs of the matter I will not be able to fulfil my duties as we would both wish. Nor is it fair to continue to put my family and colleagues through this upsetting and damaging publicity. ' He continued to defend himself from claims that he had called the police morons or plebs, claims made by the police officers in Downing Street who faced a bitter verbal volley from Mitchell. The row developed after police refused to open the main gates to allow him to take his bicycle through, instead forcing him to use the side gate. The exact words used by Mitchell on the night have been disputed ever since, with the former chief whip in effect saying the account in the subsequently leaked police log book on the night had been inaccurate. In his resignation letter to Cameron, Mitchell said:  'I have made clear to you – and I give you my categorical assurance again – that I did not, never have and never would call a police officer a 'pleb' or a 'moron' or use any of the other pejorative descriptions attributed to me.  'The offending comment and the reason for my apology to the police was my parting remark: 'I thought you guys were supposed to f***ing help us.' It was obviously wrong of me to use such bad language and I am very sorry about it and grateful to the police officer for accepting my apology."));

    }

}