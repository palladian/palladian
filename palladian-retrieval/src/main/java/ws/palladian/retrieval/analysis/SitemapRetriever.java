package ws.palladian.retrieval.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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

    private final static Pattern LOC_PATTERN = Pattern.compile("(?<=loc>).*?(?=</loc)", Pattern.CASE_INSENSITIVE);

    public List<String> getUrls(String sitemapIndexUrl) {
        List<String> pageUrls = new ArrayList<>();

        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        DocumentRetriever documentRetriever = new DocumentRetriever(httpRetriever);

        // get sitemap index page
        String sitemapIndex = documentRetriever.getText(sitemapIndexUrl);

        List<String> urls = StringHelper.getRegexpMatches(LOC_PATTERN, sitemapIndex);

        int i = 1;
        for (String sitemapUrl : urls) {

            // clean url
            sitemapUrl = normalizeUrl(sitemapUrl);

            // is it gzipped?
            boolean gzipped = false;
            if (FileHelper.getFileType(sitemapUrl).equalsIgnoreCase("gz")) {
                gzipped = true;
            }

            // download
            String downloadPath = "data/temp/sitemap" + i + ".xml.gzipped";
            String unzippedPath = downloadPath.replace(".gzipped", "");
            httpRetriever.downloadAndSave(sitemapUrl, downloadPath);

            // unzip
            if (gzipped) {
                FileHelper.ungzipFile(downloadPath, unzippedPath);
            } else {
                FileHelper.copyFile(downloadPath, unzippedPath);
            }

            // read
            String sitemapText = FileHelper.tryReadFileToString(unzippedPath);
            if (sitemapText == null) {
                continue;
            }

            String[] lines = sitemapText.split("\n");
            List<String> sitemapUrls = new ArrayList<>();
            for (String line : lines) {
                List<String> regexpMatches = StringHelper.getRegexpMatches(LOC_PATTERN, line);
                sitemapUrls.addAll(regexpMatches);
            }

            // clean
            List<String> cleanSitemapUrls = new ArrayList<>();
            for (String url : sitemapUrls) {
                cleanSitemapUrls.add(normalizeUrl(url));
            }

            pageUrls.addAll(cleanSitemapUrls);

            // clean up files
            FileHelper.delete(downloadPath);
            FileHelper.delete(unzippedPath);

            i++;
        }

        return pageUrls;
    }

    protected List<String> readSitemap(String sitemapUrl, String goalNodeRegexp) {
        Pattern goalRegexp = Pattern.compile(goalNodeRegexp, Pattern.CASE_INSENSITIVE);
        List<String> urls = getUrlsFromSitemap(sitemapUrl);
        List<String> goalUrls = new ArrayList<>();
        for (String url : urls) {
            if (goalRegexp.matcher(url).find()) {
                goalUrls.add(url);
            }
        }

        return goalUrls;
    }

    private List<String> getUrlsFromSitemap(String sitemapUrl) {
        List<String> pageUrls = new ArrayList<>();

        // read
        String sitemapText = new DocumentRetriever().getText(sitemapUrl);
        List<String> sitemapUrls = StringHelper.getRegexpMatches(LOC_PATTERN, sitemapText);

        // clean
        List<String> cleanSitemapUrls = new ArrayList<>();
        for (String url : sitemapUrls) {
            cleanSitemapUrls.add(normalizeUrl(url));
        }

        pageUrls.addAll(cleanSitemapUrls);

        return pageUrls;
    }

    protected String normalizeUrl(String url) {
        return url.replace("<![CDATA[", "").replace("]]>", "").trim();
    }
}
