package ws.palladian.extraction.date.searchengine;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.RegExp;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.search.SearcherException;
import ws.palladian.retrieval.search.SearcherFactory;
import ws.palladian.retrieval.search.web.HakiaNewsSearcher;
import ws.palladian.retrieval.search.web.WebResult;
import ws.palladian.retrieval.search.web.WebSearcher;

public class HakiaDateGetter {
    
    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(HakiaDateGetter.class);

    private String url;
    private String title = null;
    private DocumentRetriever documentRetriever = new DocumentRetriever();
    private HttpRetriever httpRetriever = new HttpRetriever();

    private void setTitle() {
        Document doc = documentRetriever.getWebDocument(url);
        if (doc != null) {
            NodeList titleList = doc.getElementsByTagName("title");
            String title = "";
            if (titleList.getLength() > 0) {
                title = titleList.item(0).getTextContent();
                title = title.replaceAll("\\s", "%20");
            }
            this.title = title;
        }
    }

    private ExtractedDate getDateFromHakia() {
        ExtractedDate date = null;
        // modified for new Palladian's new search API and untested -- 2011-11-26, Philipp
        WebSearcher<WebResult> webSearcher = SearcherFactory.createSearcher(HakiaNewsSearcher.class, ConfigHolder.getInstance().getConfig());
        try {
            List<WebResult> webResults = webSearcher.search(title, 100);

            for (int i = 0; i < webResults.size(); i++) {
                WebResult result = webResults.get(i);
                String requestUrl = result.getUrl();
                List<String> redirectUrls = httpRetriever.getRedirectUrls(requestUrl);
                if (!redirectUrls.isEmpty()){
                    requestUrl = CollectionHelper.getLast(redirectUrls);
                }
                if (requestUrl != null && requestUrl.equalsIgnoreCase(url)) {
                    DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
                    String dateString = dateFormat.format(result.getDate());
                    date = DateParser.findDate(dateString, RegExp.DATE_USA_MM_D_Y_T_SEPARATOR);
                    break;
                }

            }
        } catch (SearcherException e) {
            LOGGER.error(e);
        } catch (HttpException e) {
            LOGGER.error(e);
        }
        return date;
    }

    public ExtractedDate getHakiaDate(String url) {
        this.url = url;
        setTitle();
        ExtractedDate date = null;
        if (title != null) {
            date = getDateFromHakia();
        }
        return date;
    }

}
