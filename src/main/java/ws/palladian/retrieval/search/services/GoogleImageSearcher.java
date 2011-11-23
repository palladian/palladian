package ws.palladian.retrieval.search.services;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.preprocessing.multimedia.ExtractedImage;
import ws.palladian.retrieval.search.WebResult;
import ws.palladian.retrieval.search.WebSearcher;


public final class GoogleImageSearcher extends BaseGoogleSearcher implements WebSearcher {
    
    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(GoogleImageSearcher.class);

    @Override
    protected String getBaseUrl() {
        return "http://ajax.googleapis.com/ajax/services/search/images";
    }
    
    @Override
    protected WebResult parseResult(JSONObject resultData) throws JSONException {
        
        String url = resultData.getString("unescapedUrl");

        // only accept jpg and png images
        if (url.indexOf(".jpg") == -1 && url.indexOf(".png") == -1) {
            return null;
        }

//        String imageCaption = resultData.getString("content");
        int width = resultData.getInt("width");
        int height = resultData.getInt("height");
        
        LOGGER.debug("google retrieved url " + url);

        // all match content keywords must appear in the caption
        // of the image
//        int matchCount = 0;
//        for (String element : matchContent) {
//            if (imageCaption.toLowerCase().indexOf(element.toLowerCase()) > -1) {
//                matchCount++;
//            }
//        }
//        if (matchCount < matchContent.length) {
//            continue;
//        }

        ExtractedImage image = new ExtractedImage();
        image.setURL(url);
        image.setWidth(width);
        image.setHeight(height);
//        image.setRankCount(j * (i + 1) + 1);
        
        System.out.println(image);
        
        return null;
    }

    @Override
    public String getName() {
        return "Google Images";
    }


}
