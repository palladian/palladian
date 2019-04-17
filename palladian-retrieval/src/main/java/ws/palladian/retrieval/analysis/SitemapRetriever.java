package ws.palladian.retrieval.analysis;

import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.collection.MapBuilder;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.PatternHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonObject;

import java.util.*;
import java.util.regex.Pattern;

/**
 * <p>
 * Read a sitemap or sitemap index.
 * </p>
 *
 * @author David Urbansky
 * @link https://www.sitemaps.org/protocol.html
 */
public class SitemapRetriever {
    private DocumentRetriever documentRetriever;
    private final static Pattern LOC_PATTERN = Pattern.compile("(?<=>)[^>]+?(?=</loc)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private final static Pattern PRIORITY_PATTERN = Pattern.compile("(?<=>)[0-9.]+?(?=</priority)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private final static Pattern ALL = Pattern.compile(".");

    public SitemapRetriever() {
        HttpRetriever httpRetriever = new HttpRetrieverFactory(true).create();
        documentRetriever = new DocumentRetriever(httpRetriever);
        documentRetriever.setGlobalHeaders(MapBuilder.createPut("Cookie","euConsent=true").create());
    }
    public SitemapRetriever(DocumentRetriever documentRetriever) {
        this.documentRetriever = documentRetriever;
        Map<String, String> stringStringMap = Optional.ofNullable(documentRetriever.getGlobalHeaders()).orElse(new HashMap<>());
        stringStringMap.put("Cookie","euConsent=true");
    }

    /** Get all urls from the sitemap. If it is a sitemap index, get all urls from all sitemaps linked in the index. */
    public Set<String> getUrls(String sitemapUrl) {
        return getUrls(sitemapUrl, new HashMap<>());
    }

    /** Get all urls from the sitemap. If it is a sitemap index, get all urls from all sitemaps linked in the index. Fill the url priority map. */
    public Set<String> getUrls(String sitemapUrl, Map<String, Double> urlToPriorityMap) {
        return getUrls(sitemapUrl, urlToPriorityMap, ALL, true);
    }

    /** Get all urls from the sitemap that match the pattern (or exclude those). If it is a sitemap index, get all urls from all sitemaps linked in the index. Fill the url priority map. */
    public Set<String> getUrls(String sitemapUrl, Map<String, Double> urlToPriorityMap, Pattern goalNodePattern, boolean include) {
        LinkedHashSet<String> pageUrls = new LinkedHashSet<>();

        String sitemapContent;

        // is the sitemap gzipped?
        if (FileHelper.getFileType(sitemapUrl).equalsIgnoreCase("gz")) {
            String tempPath = "data/temp/sitemapIndex.xml";
            documentRetriever.getHttpRetriever().downloadAndSave(sitemapUrl, tempPath + ".gzipped");
            FileHelper.ungzipFile(tempPath + ".gzipped", tempPath);
            sitemapContent = documentRetriever.getText(tempPath);
            FileHelper.delete(tempPath);
            FileHelper.delete(tempPath + ".gzipped");
        } else {
            // get sitemap index page
            sitemapContent = documentRetriever.getText(sitemapUrl);
        }

        sitemapContent = cleanUpSitemap(sitemapContent);
        SitemapType sitemapType = getSitemapType(sitemapContent);
        if (sitemapType == null) {
            return pageUrls;
        }

        switch (sitemapType) {
            case LIST:
                List<String> cleanListUrls = getUrlsFromSitemap(sitemapContent, urlToPriorityMap, goalNodePattern, include);
                pageUrls.addAll(cleanListUrls);
                break;
            case INDEX:
                List<String> urls = StringHelper.getRegexpMatches(LOC_PATTERN, sitemapContent);
                ProgressMonitor sitemapRetriever = new ProgressMonitor(urls.size(), 0.1, "SitemapRetriever");
                int i = 1;
                for (String sitemapLinkUrl : urls) {
                    // clean url
                    sitemapLinkUrl = normalizeUrl(sitemapLinkUrl);

                    // is it gzipped?
                    boolean gzipped = false;
                    if (FileHelper.getFileType(sitemapLinkUrl).equalsIgnoreCase("gz")) {
                        gzipped = true;
                    }

                    // download
                    String downloadPath = "data/temp/sitemap" + i + ".xml.gzipped";
                    String unzippedPath = downloadPath.replace(".gzipped", "");
                    documentRetriever.getHttpRetriever().downloadAndSave(sitemapLinkUrl, downloadPath);

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
                    List<String> cleanUrls = getUrlsFromSitemap(sitemapText, urlToPriorityMap, goalNodePattern, include);
                    pageUrls.addAll(cleanUrls);

                    // clean up files
                    FileHelper.delete(downloadPath);
                    FileHelper.delete(unzippedPath);

                    i++;

                    sitemapRetriever.incrementAndPrintProgress();
                }
                break;
        }

        return pageUrls;
    }

    private String cleanUpSitemap(String sitemapText) {
        // check for and remove namespaces
        List<String> namespaces = StringHelper.getRegexpMatches(PatternHelper.compileOrGet("(?<=xmlns:)([a-z0-9]+)(?=[=])"), sitemapText);
        for (String namespace : namespaces) {
            sitemapText = sitemapText.replace(namespace+":","");
        }

        // remove <![CDATA[ and ]]
        sitemapText = sitemapText.replace("<![CDATA[","").replace("]]>","");

        sitemapText = PatternHelper.compileOrGet("\\n</loc>", Pattern.CASE_INSENSITIVE).matcher(sitemapText).replaceAll("</loc>");
        sitemapText = PatternHelper.compileOrGet("<loc>\\n", Pattern.CASE_INSENSITIVE).matcher(sitemapText).replaceAll("<loc>");

        return sitemapText;
    }

    private SitemapType getSitemapType(String sitemapText) {
        try {
            SitemapType sitemapType = SitemapType.LIST;

            if (sitemapText.contains("<sitemapindex") || sitemapText.contains(":sitemapindex ")) {
                sitemapType = SitemapType.INDEX;
            }

            return sitemapType;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<String> getUrlsFromSitemap(String sitemapText, Map<String, Double> urlToPriorityMap, Pattern goalNodePattern, boolean include) {
        sitemapText = cleanUpSitemap(sitemapText);

        String[] lines = sitemapText.split("\n");
        List<String> sitemapUrls = new ArrayList<>();
        List<String> priorityStrings = new ArrayList<>();
        for (String line : lines) {
            List<String> regexpMatches = StringHelper.getRegexpMatches(LOC_PATTERN, line);
            sitemapUrls.addAll(regexpMatches);
            List<String> priorityMatches = StringHelper.getRegexpMatches(PRIORITY_PATTERN, line);
            priorityStrings.addAll(priorityMatches);
        }

        // clean and check for include
        LinkedHashSet<String> cleanSitemapUrls = new LinkedHashSet<>();
        for (String url : sitemapUrls) {
            boolean matchedPattern = goalNodePattern.matcher(url).find();
            if ((matchedPattern && include) || (!matchedPattern && !include)) {
                cleanSitemapUrls.add(normalizeUrl(url));
            }
        }

        // get all priority tags, only if number of priority tags = number of URLs we can map them
        try {
            if (sitemapUrls.size() == priorityStrings.size()) {
                for (int k = 0; k < sitemapUrls.size(); k++) {
                    urlToPriorityMap.put(sitemapUrls.get(k), Double.valueOf(priorityStrings.get(k)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<>(cleanSitemapUrls);
    }

    /**
     * Normalize a URL by taking care of CDATA text and HTML entity escaping.
     * 
     * @param url A sitemap conform URL.
     * @link https://www.sitemaps.org/protocol.html#escaping
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
