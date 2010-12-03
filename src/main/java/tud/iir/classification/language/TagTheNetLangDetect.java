package tud.iir.classification.language;

import java.io.UnsupportedEncodingException;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import tud.iir.helper.StringHelper;
import tud.iir.web.HTTPPoster;

/**
 * Language detection via tagthe.net
 * 
 * http://www.tagthe.net/
 * http://www.tagthe.net/fordevelopers
 * 
 * @author Philipp Katz
 * 
 */
public class TagTheNetLangDetect extends LanguageClassifier {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(TagTheNetLangDetect.class);

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

        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e);
        } catch (JSONException e) {
            LOGGER.error(e);
        }

        return result;

    }

    public static void main(String[] args) {

        LanguageClassifier lc = new TagTheNetLangDetect();
        System.out.println(lc.classify("olala, mademoiselle. c'est la vie."));

    }

}
