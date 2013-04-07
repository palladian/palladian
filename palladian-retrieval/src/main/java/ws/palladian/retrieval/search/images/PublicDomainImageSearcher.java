package ws.palladian.retrieval.search.images;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.search.License;
import ws.palladian.retrieval.search.SearcherException;
import ws.palladian.retrieval.search.web.WebSearcher;

/**
 * <p>
 * Search for public domain images on <a href="http://www.public-domain-image.com">Public Domain Image</a>.
 * </p>
 * 
 * @author David Urbansky
 */
public class PublicDomainImageSearcher extends WebSearcher<WebImageResult> {

    @Override
    public List<WebImageResult> search(String query, int resultCount, Language language) throws SearcherException {
        List<WebImageResult> results = CollectionHelper.newArrayList();

        resultCount = Math.min(1000, resultCount);

        DocumentRetriever documentRetriever = new DocumentRetriever();

        ol: for (int page = 1; page <= 100; page++) {

            String requestUrl = buildRequest(query, page);

            Document webDocument = documentRetriever.getWebDocument(requestUrl);
            List<Node> imageNodes = XPathHelper.getXhtmlNodes(webDocument, "//div[@class='imagethumb']/a/img");

            if (imageNodes.isEmpty()) {
                break;
            }

            for (Node node : imageNodes) {

                String summary = node.getAttributes().getNamedItem("alt").getTextContent();
                String imageUrl = node.getAttributes().getNamedItem("src").getTextContent();
                String thumbImageUrl = imageUrl;
                imageUrl = imageUrl.replace("cache/", "public-domain-images-pictures-free-stock-photos/");
                imageUrl = imageUrl.replace("_85_thumb", "");
                imageUrl = "http://www.public-domain-image.com" + imageUrl;
                thumbImageUrl = "http://www.public-domain-image.com" + thumbImageUrl;

                WebImageResult webImageResult = new WebImageResult(imageUrl, imageUrl, summary, summary, -1, -1, null,
                        null);
                webImageResult.setThumbImageUrl(thumbImageUrl);

                webImageResult.setLicense(License.PUBLIC_DOMAIN);
                webImageResult.setLicenseLink("http://creativecommons.org/publicdomain/zero/1.0/deed.en");
                webImageResult.setImageType(ImageType.PHOTO);

                results.add(webImageResult);

                if (results.size() >= resultCount) {
                    break ol;
                }
            }

        }

        return results;
    }

    private String buildRequest(String searchTerms, int page) {
        searchTerms = UrlHelper.encodeParameter(searchTerms);
        String url = "http://www.public-domain-image.com/page/search/" + searchTerms + "/" + page;
        return url;
    }

    @Override
    public String getName() {
        return "PublicDomainImage";
    }

    /**
     * @param args
     * @throws SearcherException
     */
    public static void main(String[] args) throws SearcherException {
        PublicDomainImageSearcher searcher = new PublicDomainImageSearcher();
        List<WebImageResult> results = searcher.search("car", 10);
        CollectionHelper.print(results);
    }
}
