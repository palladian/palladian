package ws.palladian.extraction.entity.tagger;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.core.Annotation;
import ws.palladian.core.ImmutableAnnotation;
import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.*;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * The WebKnoxNer wraps the PalladianNer and offers the functionality over a REST API. See here for more information
 * http://localhost/webknox/api#!/text/entities_GET.
 * </p>
 *
 * @author David Urbansky
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
    public List<Annotation> getAnnotations(String inputText) {

        HttpRequest2Builder requestBuilder = new HttpRequest2Builder(HttpMethod.POST, "http://46.4.89.232:8080/text/entities?apiKey=" + apiKey);
        requestBuilder.setEntity(new FormEncodedHttpEntity.Builder().addData("text", inputText).create());

        List<Annotation> annotations = new ArrayList<>();
        String content;
        try {
            HttpResult httpResult = httpRetriever.execute(requestBuilder.create());
            content = httpResult.getStringContent();
        } catch (HttpException e) {
            throw new IllegalStateException("HTTP error while accessing the service: " + e.getMessage(), e);
        }

        try {
            JsonArray result = new JsonArray(content);
            for (int i = 0; i < result.size(); i++) {
                JsonObject currentItem = result.getJsonObject(i);
                if (currentItem.get("entity") != null) {
                    int offset = currentItem.getInt("offset");
                    String entity = currentItem.getString("entity");
                    String type = currentItem.getString("type");
                    annotations.add(new ImmutableAnnotation(offset, entity, type));
                } else {
                    LOGGER.debug("Ignore malformed entry in JSON response.");
                    /**
                     * TODO There is a bug in the REST service, which might return things like the following, where one
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
        } catch (JsonException e) {
            throw new IllegalStateException("JSON parse error while processing response '" + content + "': " + e.getMessage(), e);
        }

        return annotations;
    }

    @Override
    public String getName() {
        return "WebKnoxNer";
    }

    public static void main(String[] args) {
        // WebKnoxNer webKnoxNer = new WebKnoxNer(ConfigHolder.getInstance().getConfig());
        WebKnoxNer webKnoxNer = new WebKnoxNer("v30170b8523o23il4bz3v04");
        String text = "";
        text = "Bill Gates founded Microsoft in April 1975";
        text = "On November 27, the third committee of the UN General Assembly, the Social, Humanitarian and Cultural Affairs Committee, passed a resolution condemning the human rights situation in North Korea.";
        text = "Shining a Light on North Korea’s Human Rights Crisis";

        System.out.println(webKnoxNer.tag(text));

        // System.out
        // .println(webKnoxNer
        // .tag("Andrew Mitchell resigns following allegations he called police 'plebs' Chief whip's resignation comes amid row over whether George Osborne tried to travel first-class on standard train ticket David Cameron has been delivered a humiliating blow, with a broken Andrew Mitchell quitting as chief whip, abandoning a month-long fight to save his career and fend off claims that he had referred to a police officer as a  'pleb '. Mitchell's decision – relayed to the prime minister personally in a meeting at Chequers after Cameron returned from an EU summit in Brussels – is a huge blow to the prime minister, who had stood by him in the face of an onslaught by the opposition and the Police Federation and growing doubts among his own backbenchers. Cameron used his authority at prime minister's questions on Wednesday to defend Mitchell from an attack by Ed Miliband and private threats to resign by the deputy chief whip, John Randall. The meeting between Mitchell and Cameron, at 4pm on Friday, came as a separate row was breaking over whether George Osborne had travelled in a first-class train carriage on a standard ticket. In his resignation statement, Mitchell said:  'Over the last two days it has become clear to me that whatever the rights and wrongs of the matter I will not be able to fulfil my duties as we would both wish. Nor is it fair to continue to put my family and colleagues through this upsetting and damaging publicity. ' He continued to defend himself from claims that he had called the police morons or plebs, claims made by the police officers in Downing Street who faced a bitter verbal volley from Mitchell. The row developed after police refused to open the main gates to allow him to take his bicycle through, instead forcing him to use the side gate. The exact words used by Mitchell on the night have been disputed ever since, with the former chief whip in effect saying the account in the subsequently leaked police log book on the night had been inaccurate. In his resignation letter to Cameron, Mitchell said:  'I have made clear to you – and I give you my categorical assurance again – that I did not, never have and never would call a police officer a 'pleb' or a 'moron' or use any of the other pejorative descriptions attributed to me.  'The offending comment and the reason for my apology to the police was my parting remark: 'I thought you guys were supposed to f***ing help us.' It was obviously wrong of me to use such bad language and I am very sorry about it and grateful to the police officer for accepting my apology."));

    }

}