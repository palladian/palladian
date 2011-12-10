package ws.palladian.extraction.date.searchengine;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import ws.palladian.extraction.date.DateGetterHelper;
import ws.palladian.extraction.date.dates.ExtractedDate;
import ws.palladian.helper.RegExp;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.search.web.HakiaNewsSearcher;
import ws.palladian.retrieval.search.web.WebResult;
import ws.palladian.retrieval.search.web.WebSearcher;

public class HakiaDateGetter {

    private String url;
    private String title = null;
    private DocumentRetriever documentRetriever = new DocumentRetriever();

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
        WebSearcher<WebResult> webSearcher = new HakiaNewsSearcher();
        List<WebResult> webResults = webSearcher.search(title, 100);

        for (int i = 0; i < webResults.size(); i++) {
            WebResult result = webResults.get(i);
            String tempUrl = result.getUrl();
            String requestUrl = documentRetriever.getRedirectUrl(tempUrl);
            if (requestUrl != null && requestUrl.equalsIgnoreCase(url)) {
                DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
                String dateString = dateFormat.format(result.getDate());
                date = DateGetterHelper.getDateFromString(dateString, RegExp.DATE_USA_MM_D_Y_T_SEPARATOR);
                break;
            }

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
