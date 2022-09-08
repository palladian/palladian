package ws.palladian.retrieval.analysis;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.collection.MapBuilder;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.StringInputStream;
import ws.palladian.helper.nlp.PatternHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>
 * Read a sitemap or sitemap index.
 * </p>
 *
 * @author David Urbansky
 * @link https://www.sitemaps.org/protocol.html
 */
public class SitemapRetriever {
    private boolean parseXml = false;
    private final DocumentRetriever documentRetriever;
    private final static Pattern LOC_PATTERN = Pattern.compile("(?<=>)[^>]+?(?=</loc)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private final static Pattern PRIORITY_PATTERN = Pattern.compile("(?<=>)[0-9.]+?(?=</priority)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private final static Pattern LAST_MOD_PATTERN = Pattern.compile("(?<=>)[^>]+?(?=</lastmod)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private final static Pattern ALL = Pattern.compile(".");

    public SitemapRetriever() {
        HttpRetriever httpRetriever = new HttpRetrieverFactory(true).create();
        documentRetriever = new DocumentRetriever(httpRetriever);
        documentRetriever.setGlobalHeaders(MapBuilder.createPut("Cookie", "euConsent=true").create());
    }

    public SitemapRetriever(DocumentRetriever documentRetriever) {
        this.documentRetriever = documentRetriever;
        Map<String, String> stringStringMap = Optional.ofNullable(documentRetriever.getGlobalHeaders()).orElse(new HashMap<>());
        String cookieString = stringStringMap.get("Cookie");
        if (cookieString != null) {
            cookieString += ";euConsent=true";
        } else {
            cookieString = "euConsent=true";
        }
        stringStringMap.put("Cookie", cookieString);
    }

    /**
     * Get all urls from the sitemap. If it is a sitemap index, get all urls from all sitemaps linked in the index.
     */
    public Set<String> getUrls(String sitemapUrl) {
        return getUrls(sitemapUrl, new HashMap<>());
    }

    /**
     * Get all urls from the sitemap. If it is a sitemap index, get all urls from all sitemaps linked in the index. Fill the url priority map.
     */
    public Set<String> getUrls(String sitemapUrl, Map<String, Double> urlToPriorityMap) {
        return getUrls(sitemapUrl, urlToPriorityMap, ALL, true);
    }

    /**
     * Get all urls from the sitemap that match the pattern (or exclude those). If it is a sitemap index, get all urls from all sitemaps linked in the index. Fill the url priority map.
     */
    public Set<String> getUrls(String sitemapUrl, Map<String, Double> urlToPriorityMap, Pattern goalNodePattern, boolean include) {
        return getSitemap(sitemapUrl, urlToPriorityMap, goalNodePattern, include).getUrlSet().stream().map(Sitemap.Entry::getLocation).collect(Collectors.toSet());
    }

    public Sitemap getSitemap(String sitemapUrl, Map<String, Double> urlToPriorityMap, Pattern goalNodePattern, boolean include) {
        Sitemap sitemap = new Sitemap();

        String sitemapContent;

        // is the sitemap gzipped?
        if (FileHelper.getFileType(sitemapUrl).equalsIgnoreCase("gz")) {
            String tempPath = "data/temp/sitemapIndex.xml";
            documentRetriever.getHttpRetriever().downloadAndSave(sitemapUrl, tempPath + ".gzipped", Optional.ofNullable(documentRetriever.getGlobalHeaders()).orElse(new HashMap<>()), false);
            FileHelper.ungzipFile(tempPath + ".gzipped", tempPath);
            sitemapContent = documentRetriever.getText(tempPath);
            if (sitemapContent == null) {
                // sometimes websites call the file .gz but it's just a plain text file, in which case we can't unzip and simply read the "zipped" file
                sitemapContent = documentRetriever.getText(tempPath + ".gzipped");
            }
            FileHelper.delete(tempPath);
            FileHelper.delete(tempPath + ".gzipped");
        } else {
            // get sitemap index page
            sitemapContent = documentRetriever.getText(sitemapUrl);
        }

        if (sitemapContent == null) {
            return sitemap;
        }

        boolean needsCleaning = true;
        if (!parseXml) {
            sitemapContent = cleanUpSitemap(sitemapContent);
            needsCleaning = false;
        }
        SitemapType sitemapType = getSitemapType(sitemapContent);
        if (sitemapType == null) {
            return sitemap;
        }

        switch (sitemapType) {
            case LIST:
                Sitemap sitemap1;
                if (parseXml) {
                    sitemap1 = getUrlsFromSitemapParsed(sitemapContent, goalNodePattern, include);
                } else {
                    sitemap1 = getUrlsFromSitemap(sitemapContent, urlToPriorityMap, goalNodePattern, include, needsCleaning);
                }

                sitemap.addUrls(sitemap1.getUrlSet());
                break;
            case INDEX:
                List<String> urls = StringHelper.getRegexpMatches(LOC_PATTERN, sitemapContent);
                ProgressMonitor sitemapRetriever = new ProgressMonitor(urls.size(), 0.1, "SitemapRetriever (" + sitemapUrl + ")");
                for (String sitemapLinkUrl : urls) {
                    // clean url
                    sitemapLinkUrl = normalizeUrl(sitemapLinkUrl);

                    // is it gzipped?
                    boolean gzipped = FileHelper.getFileType(sitemapLinkUrl).equalsIgnoreCase("gz");

                    // download
                    String downloadPath = "data/temp/sitemap" + System.currentTimeMillis() + ".xml.gzipped";
                    String unzippedPath = downloadPath.replace(".gzipped", "");
                    documentRetriever.getHttpRetriever().downloadAndSave(sitemapLinkUrl, downloadPath, Optional.ofNullable(documentRetriever.getGlobalHeaders()).orElse(new HashMap<>()), false);

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
                    Sitemap sitemap2;
                    if (parseXml) {
                        sitemap2 = getUrlsFromSitemapParsed(sitemapText, goalNodePattern, include);
                    } else {
                        sitemap2 = getUrlsFromSitemap(sitemapText, urlToPriorityMap, goalNodePattern, include);
                    }
                    sitemap.addUrls(sitemap2.getUrlSet());

                    // clean up files
                    FileHelper.delete(downloadPath);
                    FileHelper.delete(unzippedPath);

                    sitemapRetriever.incrementAndPrintProgress();
                }
                break;
        }

        return sitemap;
    }

    private String cleanUpSitemap(String sitemapText) {
        // check for and remove namespaces
        List<String> namespaces = StringHelper.getRegexpMatches(PatternHelper.compileOrGet("(?<=xmlns:)([a-z0-9]+)(?=[=])"), sitemapText);
        for (String namespace : namespaces) {
            // we do not want to remove "image" otherwise we might end up with "image:loc" => "loc" and think these are "real" HTML content URLs (e.g. https://www.healthymummy.com/recipe-sitemap1.xml)
            if (!namespace.equalsIgnoreCase("image")) {
                sitemapText = sitemapText.replace(namespace + ":", "");
            }
        }

        // remove <![CDATA[ and ]]
        sitemapText = sitemapText.replace("<![CDATA[", "").replace("]]>", "");

        sitemapText = PatternHelper.compileOrGet("(\\n+\\s*)</loc>", Pattern.CASE_INSENSITIVE).matcher(sitemapText).replaceAll("</loc>");
        sitemapText = PatternHelper.compileOrGet("<loc>(\\n+\\s*)", Pattern.CASE_INSENSITIVE).matcher(sitemapText).replaceAll("<loc>");

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

    private Sitemap getUrlsFromSitemapParsed(String sitemapText, Pattern goalNodePattern, boolean include) {
        Pattern pattern = PatternHelper.compileOrGet("<!\\[CDATA\\[([^<>]+)]\\]>"); // CDATA can result in the extracted urls being empty, remove it in advance
        Matcher matcher = pattern.matcher(sitemapText);
        sitemapText = matcher.replaceAll("$1");

        List<Sitemap.Entry> entries = new ArrayList<>();
        try {
            Document xmlDocument;
            List<Node> urlNodes = Collections.emptyList();
            try {
                xmlDocument = ParserFactory.createXmlParser().parse(new StringInputStream(sitemapText));
                urlNodes = XPathHelper.getXhtmlNodes(xmlDocument, "//url");
            } catch (Exception e) {
                // ccl
            }

            if (urlNodes.isEmpty()) { // fallback to forgiving html parser
                xmlDocument = ParserFactory.createHtmlParser().parse(new StringInputStream(sitemapText));
                urlNodes = XPathHelper.getXhtmlNodes(xmlDocument, "//url");
            }


            for (Node urlNode : urlNodes) {
                Node locationNode = null;
                Node lastModNode = null;
                Node priorityNode = null;
                for (int i = 0; i < urlNode.getChildNodes().getLength(); i++) {
                    Node child = urlNode.getChildNodes().item(i);
                    if (child.getNodeName().equalsIgnoreCase("loc")) {
                        locationNode = child;
                    } else if (child.getNodeName().equalsIgnoreCase("lastmod")) {
                        lastModNode = child;
                    } else if (child.getNodeName().equalsIgnoreCase("priority")) {
                        priorityNode = child;
                    }
                }

                if (locationNode == null) {
                    continue;
                }

                String url = locationNode.getTextContent();
                boolean matchedPattern = goalNodePattern.matcher(url).find();
                if ((matchedPattern && include) || (!matchedPattern && !include)) {
                    String location = normalizeUrl(url);

                    String lastModString = lastModNode != null ? lastModNode.getTextContent() : null;
                    ExtractedDate lastMod = lastModString != null ? DateParser.findDate(lastModString) : null;

                    Double priority = null;
                    if (priorityNode != null) {
                        try {
                            priority = Double.valueOf(priorityNode.getTextContent());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    entries.add(new Sitemap.Entry(location, lastMod != null ? lastMod.getNormalizedDate() : null, priority));
                }
            }
        } catch (ParserException e) {
            e.printStackTrace();
        }

        return new Sitemap(new LinkedHashSet<>(entries));
    }

    private Sitemap getUrlsFromSitemap(String sitemapText, Map<String, Double> urlToPriorityMap, Pattern goalNodePattern, boolean include) {
        return getUrlsFromSitemap(sitemapText, urlToPriorityMap, goalNodePattern, include, true);
    }

    private Sitemap getUrlsFromSitemap(String sitemapText, Map<String, Double> urlToPriorityMap, Pattern goalNodePattern, boolean include, boolean needsCleaning) {
        if (needsCleaning) {
            sitemapText = cleanUpSitemap(sitemapText);
        }

        String[] lines = sitemapText.split("\n");
        List<String> sitemapUrls = new ArrayList<>();
        List<String> priorityStrings = new ArrayList<>();
        List<String> lastModStrings = new ArrayList<>();
        for (String line : lines) {
            List<String> regexpMatches = StringHelper.getRegexpMatches(LOC_PATTERN, line);
            sitemapUrls.addAll(regexpMatches);
            List<String> priorityMatches = StringHelper.getRegexpMatches(PRIORITY_PATTERN, line);
            priorityStrings.addAll(priorityMatches);
            List<String> lastModMatches = StringHelper.getRegexpMatches(LAST_MOD_PATTERN, line);
            lastModStrings.addAll(lastModMatches);
        }

        // clean and check for include
        LinkedHashSet<Sitemap.Entry> cleanSitemapEntries = new LinkedHashSet<>();
        boolean skipMatching = false;
        if (goalNodePattern.pattern().equals(".*")) {
            skipMatching = true;
        }
        boolean hasValidPriorities = priorityStrings.size() == sitemapUrls.size();
        boolean hasValidLastMods = lastModStrings.size() == sitemapUrls.size();

        for (int i = 0; i < sitemapUrls.size(); i++) {
            String url = sitemapUrls.get(i);
            boolean matchedPattern = true;
            if (!skipMatching) {
                matchedPattern = goalNodePattern.matcher(url).find();
            }
            if ((matchedPattern && include) || (!matchedPattern && !include)) {
                String location = normalizeUrl(url);
                String priorityString = hasValidPriorities ? priorityStrings.get(i) : null;
                String lastModString = hasValidLastMods ? lastModStrings.get(i) : null;
                ExtractedDate lastMod = lastModString != null ? DateParser.findDate(lastModString) : null;
                Double priority = null;
                if (priorityString != null) {
                    try {
                        priority = Double.valueOf(priorityString);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                cleanSitemapEntries.add(new Sitemap.Entry(location, lastMod != null ? lastMod.getNormalizedDate() : null, priority));
            }
        }

        // get all priority tags, only if number of priority tags = number of URLs we can map them
        try {
            if (hasValidPriorities) {
                for (int k = 0; k < sitemapUrls.size(); k++) {
                    urlToPriorityMap.put(sitemapUrls.get(k), Double.valueOf(priorityStrings.get(k)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new Sitemap(cleanSitemapEntries);
    }

    /**
     * Normalize a URL by taking care of CDATA text, HTML entity escaping, and trimming.
     *
     * @param url A sitemap conform URL.
     * @return The normalized URL.
     * @link https://www.sitemaps.org/protocol.html#escaping
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

    public boolean isParseXml() {
        return parseXml;
    }

    public void setParseXml(boolean parseXml) {
        this.parseXml = parseXml;
    }
}
