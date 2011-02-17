package ws.palladian.classification.language;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.StringHelper;
import ws.palladian.web.HTTPPoster;

/**
 * <p>
 * Language detection via tagthe.net
 * </p>
 * 
 * <a href="http://www.tagthe.net/">http://www.tagthe.net/</a>
 * <a href="http://www.tagthe.net/fordevelopers">http://www.tagthe.net/fordevelopers</a>
 * 
 * @author Philipp Katz
 * 
 */
public class TagTheNetLangDetect extends LanguageClassifier {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(TagTheNetLangDetect.class);

    /** tagthe.net provides no information, which languages it supports, so we record all detected ones here in the set. */
    private Set<String> detectedLanguages = new HashSet<String>();

    @Override
    public String classify(String text) {

        String result = "";

        HTTPPoster poster = new HTTPPoster();
        PostMethod postMethod = new PostMethod("http://tagthe.net/api");
        postMethod.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        try {

            postMethod.setRequestEntity(new StringRequestEntity("text=" + StringHelper.urlEncode(text) + "&view=json",
                    "text/raw", "UTF-8"));

            String response = poster.handleRequest(postMethod);
            JSONObject json = new JSONObject(response);

            String language = json.getJSONArray("memes").getJSONObject(0).getJSONObject("dimensions")
                    .getJSONArray("language").getString(0);

            result = mapLanguage(language);
            
            detectedLanguages.add(result);

        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e);
        } catch (JSONException e) {
            LOGGER.error(e);
        }

        return result;

    }
    
    public Set<String> getDetectedLanguages() {
        return detectedLanguages;
    }

    public static void main(String[] args) {

        LanguageClassifier lc = new TagTheNetLangDetect();
        for (int i = 0; i < 1000; i++) {
            System.out.println(lc.classify("olala, mademoiselle. c'est la vie."));
        }

    }

}
