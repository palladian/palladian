package ws.palladian.retrieval.analysis;

import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.PatternHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * <p>
 * Read the sitemap and visit every page.
 * </p>
 *
 * @author David Urbansky
 * @see https://www.sitemaps.org/protocol.html
 */
public class SitemapRetriever {

    private final static Pattern LOC_PATTERN = Pattern.compile("(?<=>)[^>]+?(?=</loc)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public Set<String> getUrls(String sitemapIndexUrl) {
        LinkedHashSet<String> pageUrls = new LinkedHashSet<>();

        HttpRetriever httpRetriever = new HttpRetrieverFactory(true).create();
        DocumentRetriever documentRetriever = new DocumentRetriever(httpRetriever);

        String sitemapIndex;

        // is the sitemap gzipped?
        if (FileHelper.getFileType(sitemapIndexUrl).equalsIgnoreCase("gz")) {

            String tempPath = "data/temp/sitemapIndex.xml";
            httpRetriever.downloadAndSave(sitemapIndexUrl, tempPath + ".gzipped");
            FileHelper.ungzipFile(tempPath + ".gzipped", tempPath);
            sitemapIndex = documentRetriever.getText(tempPath);
            FileHelper.delete(tempPath);
            FileHelper.delete(tempPath + ".gzipped");

        } else {

            // get sitemap index page
            sitemapIndex = documentRetriever.getText(sitemapIndexUrl);

        }

        List<String> urls = StringHelper.getRegexpMatches(LOC_PATTERN, sitemapIndex);

        ProgressMonitor sitemapRetriever = new ProgressMonitor(urls.size(), 0.1, "SitemapRetriever");
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
            sitemapText = PatternHelper.compileOrGet("\\n</loc>", Pattern.CASE_INSENSITIVE).matcher(sitemapText).replaceAll("</loc>");
            sitemapText = PatternHelper.compileOrGet("<loc>\\n", Pattern.CASE_INSENSITIVE).matcher(sitemapText).replaceAll("<loc>");
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
            LinkedHashSet<String> cleanSitemapUrls = new LinkedHashSet<>();
            for (String url : sitemapUrls) {
                cleanSitemapUrls.add(normalizeUrl(url));
            }

            pageUrls.addAll(cleanSitemapUrls);

            // clean up files
            FileHelper.delete(downloadPath);
            FileHelper.delete(unzippedPath);

            i++;

            sitemapRetriever.incrementAndPrintProgress();

        }

        return pageUrls;
    }

    public List<String> readSitemap(String sitemapUrl) {
        return readSitemap(sitemapUrl, ".");
    }

    public List<String> readSitemap(String sitemapUrl, String goalNodeRegexp) {
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
        HttpRetriever httpRetriever = new HttpRetrieverFactory(true).create();
        String sitemapText = new DocumentRetriever(httpRetriever).getText(sitemapUrl);
        List<String> sitemapUrls = StringHelper.getRegexpMatches(LOC_PATTERN, sitemapText);

        // clean
        List<String> cleanSitemapUrls = new ArrayList<>();
        for (String url : sitemapUrls) {
            cleanSitemapUrls.add(normalizeUrl(url));
        }

        pageUrls.addAll(cleanSitemapUrls);

        return pageUrls;
    }

    /**
     * Normalize a URL by taking care of CDATA text and HTML entity escaping.
     * 
     * @param url A sitemap conform URL.
     * @see https://www.sitemaps.org/protocol.html#escaping
     * @return The normalized URL.
     */
    protected String normalizeUrl(String url) {
        url = url.replace("<![CDATA[", "").replace("]]>", "").trim();

        url = url.replace("&amp;", "&");
        url = url.replace("&apos;", "'");
        url = url.replace("&quot;", "\"");
        url = url.replace("&gt;", ">");
        url = url.replace("&lt;", "<");

        return url;
    }

}
