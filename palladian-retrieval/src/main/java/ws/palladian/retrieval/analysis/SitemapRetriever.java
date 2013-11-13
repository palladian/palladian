package ws.palladian.retrieval.analysis;

import java.util.List;
import java.util.regex.Pattern;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;

/**
 * <p>
 * Read the sitemap and visit every page.
 * </p>
 * 
 * @author David Urbansky
 */
public class SitemapRetriever {

    private final static Pattern LOC_PATTERN = Pattern.compile("(?<=loc\\>).*?(?=\\</loc)", Pattern.CASE_INSENSITIVE);

    public List<String> getUrls(String sitemapIndexUrl) {
        List<String> pageUrls = CollectionHelper.newArrayList();

        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        DocumentRetriever documentRetriever = new DocumentRetriever(httpRetriever);

        // get sitemap index page
        String sitemapIndex = documentRetriever.getText(sitemapIndexUrl);

        List<String> urls = StringHelper.getRegexpMatches(LOC_PATTERN, sitemapIndex);

        int i = 1;
        for (String sitemapUrl : urls) {

            // clean url
            sitemapUrl = normalizeUrl(sitemapUrl);

            // download
            String downloadPath = "data/temp/sitemap" + i + ".xml.compressed";
            String unzippedPath = downloadPath.replace(".compressed", "");
            httpRetriever.downloadAndSave(sitemapUrl, downloadPath);

            // unzip
            FileHelper.ungzipFile(downloadPath, unzippedPath);

            // read
            String sitemapText = FileHelper.tryReadFileToString(unzippedPath);
            List<String> sitemapUrls = StringHelper.getRegexpMatches(LOC_PATTERN, sitemapText);

            // clean
            List<String> cleanSitemapUrls = CollectionHelper.newArrayList();
            for (String url : sitemapUrls) {
                cleanSitemapUrls.add(normalizeUrl(url));
            }

            pageUrls.addAll(cleanSitemapUrls);

            i++;
        }

        return pageUrls;
    }

    protected String normalizeUrl(String url) {
        return url.replace("<![CDATA[", "").replace("]]>", "").trim();
    }
}
