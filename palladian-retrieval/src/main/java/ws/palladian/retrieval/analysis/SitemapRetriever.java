package ws.palladian.retrieval.analysis;

import java.util.List;

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

    public List<String> getUrls(String sitemapIndexUrl) {
        List<String> pageUrls = CollectionHelper.newArrayList();

        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        DocumentRetriever documentRetriever = new DocumentRetriever(httpRetriever);

        // get sitemap index page
        String sitemapIndex = documentRetriever.getText(sitemapIndexUrl);

        String locRegexp = "(?<=loc\\>).*?(?=\\</loc)";
        List<String> urls = StringHelper.getRegexpMatches(locRegexp, sitemapIndex);

        int i = 1;
        for (String sitemapUrl : urls) {

            // download
            String downloadPath = "data/temp/sitemap" + i + ".xml.compressed";
            String unzippedPath = downloadPath.replace(".compressed", "");
            httpRetriever.downloadAndSave(sitemapUrl, downloadPath);

            // unzip
            FileHelper.ungzipFile(downloadPath, unzippedPath);

            // read
            String sitemapText = FileHelper.tryReadFileToString(unzippedPath);
            List<String> sitemapUrls = StringHelper.getRegexpMatches(locRegexp, sitemapText);
            pageUrls.addAll(sitemapUrls);

            i++;
        }

        return pageUrls;
    }

}
