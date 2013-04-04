package ws.palladian.retrieval.datasets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;

/**
 * <p>The DeliciousCrawler creates a data set of web pages with delicious tags. This data set can then be used as training data for the web page classifier.</p>
 * 
 * @author David Urbansky
 * 
 */
public class DeliciousCrawler {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DeliciousCrawler.class);

    /** The document retriever. */
    private DocumentRetriever documentRetriever = null;

    /** The stack holding the URLs. */
    private HashSet<String> urlStack =  null;

    public DeliciousCrawler() {
        documentRetriever = new DocumentRetriever();

        // //////////////////////////////////////////////////
        // documentRetriever.setSwitchProxyRequests(15);

        // http://www.proxy-list.org/en/index.php
        // Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("87.106.143.132", 3128));
        // Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("187.84.240.134", 8080));
        // crawler.setProxy(proxy);

        urlStack = FileHelper.deserialize("data/benchmarkSelection/page/urlStack.ser");
        if (urlStack == null) {
            urlStack = new HashSet<String>();
        }
    }

    private Node getNextChildNode(Node startNode, String nodeName, String classNames) {
        Node targetNode = startNode;

        NodeList childNodes = startNode.getChildNodes();
        for (int j = 0; j < childNodes.getLength(); j++) {
            if (childNodes.item(j).getNodeName().equalsIgnoreCase(nodeName)) {

                Node n = childNodes.item(j);

                // check if class names match
                if (classNames.length() > 0) {
                    Node classNode = n.getAttributes().getNamedItem("class");
                    if (classNode != null && classNode.getTextContent().equalsIgnoreCase(classNames)) {
                        targetNode = n;
                        break;
                    }
                } else {
                    targetNode = n;
                    break;
                }

            }
        }

        return targetNode;
    }

    private ArrayList<String> getTagList(String detailURL) {
        ArrayList<String> tagList = new ArrayList<String>();

        Document detailPage = documentRetriever.getWebDocument(detailURL);

        List<Node> tagNodes = XPathHelper.getNodes(detailPage, "/html/body/div/div/div/div/ul/li/a/span".toUpperCase());

        for (Node n : tagNodes) {
            String htmlText = n.getFirstChild().getTextContent();
            htmlText = StringHelper.trim(htmlText);
            if (htmlText.length() > 1) {
                tagList.add(htmlText);
            }
        }

        try {
            Thread.sleep((int) (Math.random() * 2000));
        } catch (InterruptedException e) {
            LOGGER.warn(e.getMessage());
        }

        return tagList;
    }

    public void crawl() {

        long t1 = System.currentTimeMillis();

        StringBuilder content = new StringBuilder();

        for (int p = 1; p <= 1; p++) {

            String url = "http://delicious.com/recent?page=" + p + "&setcount=100";
            LOGGER.info("check: " + url);

            Document document = documentRetriever.getWebDocument(url);

            List<Node> nodeList = XPathHelper.getNodes(document, "//ul/li/div".toUpperCase());

            for (Node node : nodeList) {

                ArrayList<String> tags = new ArrayList<String>();

                NodeList childNodes = node.getChildNodes();

                Node linkNode = null;

                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node childNode = childNodes.item(i);

                    String detailURL = "";

                    // find link node
                    if (childNode != null && childNode.getAttributes() != null && childNode.getAttributes().getNamedItem("class") != null
                            && childNode.getAttributes().getNamedItem("class").getTextContent().equalsIgnoreCase("data")) {

                        NodeList childNodes0 = childNode.getChildNodes();
                        for (int j = 0; j < childNodes0.getLength(); j++) {
                            if (childNodes0.item(j).getNodeName().equalsIgnoreCase("h4")) {
                                linkNode = childNodes0.item(j).getChildNodes().item(1);
                                break;
                            }
                        }

                        Node n = getNextChildNode(childNode, "div", "actions");
                        n = getNextChildNode(n, "div", "");
                        n = getNextChildNode(n, "a", "delNav");

                        Node hrefNode = n.getAttributes().getNamedItem("href");
                        if (hrefNode != null) {
                            detailURL = "http://delicious.com" + hrefNode.getTextContent();
                        }

                        // get detail page with information about tags
                        tags = getTagList(detailURL);
                    }

                }

                String deliciousLink = linkNode.getAttributes().getNamedItem("href").getNodeValue();
                if (!urlStack.contains(deliciousLink) && tags.size() > 0) {

                    content.append(deliciousLink).append(" ");
                    StringBuilder logLine = new StringBuilder(" ");
                    for (String tag : tags) {
                        tag = StringHelper.trim(tag).toLowerCase();
                        content.append(tag).append(" ");
                        logLine.append(tag + " ");
                    }
                    content.append("\n");

                    LOGGER.info("added: " + deliciousLink + logLine);
                    urlStack.add(deliciousLink);
                }

            }

            try {
                Thread.sleep((int) (Math.random() * 5000));
            } catch (InterruptedException e) {
                LOGGER.warn(e.getMessage());
                return;
            }
        }

        LOGGER.info("serializing results...");
        FileHelper.appendFile("data/benchmarkSelection/page/deliciouspages.txt", content);
        FileHelper.serialize(urlStack, "data/benchmarkSelection/page/urlStack.ser");
        LOGGER.info("runtime: " + DateHelper.getRuntime(t1));
    }

    /**
     * Read the data set, clean it and write the output to a new file.
     * 
     * @param minAppearance Number of times a tag must appear in order to keep it.
     */
    @SuppressWarnings("unchecked")
    public static void cleanDataSet(int minAppearance) {

        long t1 = System.currentTimeMillis();

        final Object[] obj2 = new Object[1];
        obj2[0] = new HashMap<String, Integer>(); // tags and counts

        LineAction la = new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {

                String[] lineParts = line.split(" ");

                // add tags
                HashMap<String, Integer> tagMap = (HashMap<String, Integer>) obj2[0];
                for (int i = 1; i < lineParts.length; i++) {
                    String tag = DeliciousCrawler.cleanTag(lineParts[i]);
                    if (tag.length() < 2) {
                        continue;
                    }

                    tag = normalizeTag(tag);

                    if (tagMap.containsKey(tag)) {
                        tagMap.put(tag, tagMap.get(tag) + 1);
                    } else {
                        tagMap.put(tag, 1);
                    }

                }
            }
        };

        FileHelper.performActionOnEveryLine("data/benchmarkSelection/page/deliciouspages.txt", la);

        final Object[] obj = new Object[3];
        obj[0] = new StringBuilder(); // the cleansed output
        obj[1] = minAppearance; // minimal appearance of a tag to be kept
        obj[2] = obj2[0]; // tags and counts

        la = new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {

                String[] lineParts = line.split(" ");

                StringBuilder fileString = (StringBuilder) obj[0];
                StringBuilder lineString = new StringBuilder();

                for (int i = 1; i < lineParts.length; i++) {

                    // trim tags and remove special chars
                    String tag = lineParts[i];
                    tag = DeliciousCrawler.cleanTag(tag);

                    // delete all tags with a length shorter or equal 1
                    if (tag.length() < 2) {
                        continue;
                    }

                    // remove all tags that are just numbers
                    boolean onlyNumber = false;
                    try {
                        Integer.valueOf(tag);
                        onlyNumber = true;
                    } catch (NumberFormatException e) {
                        onlyNumber = false;
                    }
                    if (onlyNumber) {
                        continue;
                    }

                    // remove all tags that appear less then minAppearance times
                    HashMap<String, Integer> tagMap = (HashMap<String, Integer>) obj[2];
                    if (tagMap.get(tag) == null || tagMap.get(tag) < (Integer) obj[1]) {
                        continue;
                    }

                    lineString.append(tag).append(" ");
                }

                // delete all URLs that do not have any tags assigned
                if (lineString.length() > 0) {
                    fileString.append(lineParts[0]).append(" ").append(lineString).append("\n");
                }

            }
        };

        FileHelper.performActionOnEveryLine("data/benchmarkSelection/page/deliciouspages.txt", la);

        StringBuilder cleansedDataSet = (StringBuilder)obj[0];
        FileHelper.writeToFile("data/benchmarkSelection/page/deliciouspages_cleansed_" + minAppearance + ".txt", cleansedDataSet);

        System.out.println("data set cleansed and saved in " + DateHelper.getRuntime(t1));
    }

    @SuppressWarnings("unchecked")
    public static void analyzeDataSet(String suffix) {

        long t1 = System.currentTimeMillis();
        StringBuilder s = new StringBuilder();

        final Object[] obj = new Object[4];
        obj[0] = 0; // total number of lines
        obj[1] = new HashMap<String, Integer>(); // tags and counts
        obj[2] = 0; // average URL length
        obj[3] = 0; // tags per URL

        LineAction la = new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {

                // save total number of lines
                obj[0] = lineNumber;

                String[] lineParts = line.split(" ");

                // add tags
                HashMap<String, Integer> tagMap = (HashMap<String, Integer>) obj[1];
                for (int i = 1; i < lineParts.length; i++) {
                    String tag = DeliciousCrawler.cleanTag(lineParts[i]);
                    if (tag.length() < 2) {
                        continue;
                    }

                    if (tagMap.containsKey(tag)) {
                        tagMap.put(tag, tagMap.get(tag) + 1);
                    } else {
                        tagMap.put(tag, 1);
                    }

                }

                // add URL length
                obj[2] = (Integer) obj[2] + lineParts[0].length();

                // add number of tags
                obj[3] = (Integer) obj[3] + lineParts.length - 1;

                if (lineNumber % 1000 == 0) {
                    System.out.println(lineNumber + " lines processed");
                }
            }
        };

        String location = "data/benchmarkSelection/page/deliciouspages";
        if (suffix.length() > 0) {
            location += suffix;
        }
        location += ".txt";

        FileHelper.performActionOnEveryLine(location, la);

        Integer totalURLs = (Integer)obj[0];
        double avgURLLength = MathHelper.round(Double.valueOf((Integer)obj[2]) / Double.valueOf((Integer)obj[0]), 4);
        double avgTagsPerURL = MathHelper.round(Double.valueOf((Integer)obj[3]) / Double.valueOf((Integer)obj[0]), 4);
        HashMap<String, Integer> tagMap = (HashMap<String, Integer>)obj[1];
        tagMap = CollectionHelper.sortByValue(tagMap, false);

        s.append("URLs in data set:     	" + totalURLs + "\n");
        s.append("Number of distinct tags:	" + tagMap.keySet().size() + "\n");
        s.append("Average URL length:   	" + avgURLLength + "\n");
        s.append("Average Tags per URL: 	" + avgTagsPerURL + "\n");
        s.append("Most common tags:\n");
        int c = 0;
        for (Entry<String, Integer> t : tagMap.entrySet()) {
            s.append("\t" + t.getKey() + ":\t\t" + t.getValue() + " (" + MathHelper.round((double) t.getValue() * 100.0 / (double) totalURLs, 2)
                    + "% of all URLs)\n");
            if (c >= 50) {
                break;
            }
            c++;
        }

        // print a list of all tags sorted by name
        TreeSet<String> tagSorted = new TreeSet<String>();
        for (Entry<String, Integer> t : tagMap.entrySet()) {
            tagSorted.add(t.getKey());
        }

        s.append("Tags alphabetically (top 500):\n");
        c = 0;
        for (String tag : tagSorted) {
            s.append("\t" + tag + "\n");
            if (c >= 500) {
                break;
            }
            c++;
        }

        System.out.println(s);
        System.out.println("data set analyzed in " + DateHelper.getRuntime(t1));
    }

    /**
     * Normalize vocabulary. For example, blogs => blogs / musica, musik => music / e-learning, learning => learn
     * 
     * @param tag The tag that should be normalized.
     * @return The normalized tag.
     */
    public static String normalizeTag(String tag) {

        // map tag to another tag
        HashMap<String, String> mapping = new HashMap<String, String>();
        mapping.put("2read", "unread");
        mapping.put("_unread", "unread");
        mapping.put("academia", "academic");
        mapping.put("access", "accessibility");
        mapping.put("accounts", "account");
        mapping.put("ad", "advertising");
        mapping.put("ads", "advertising");
        mapping.put("addons", "addon");
        mapping.put("admin", "administration");
        mapping.put("agencies", "agency");
        mapping.put("algorithms", "algorithm");
        mapping.put("american", "america");
        mapping.put("analysis", "analytics");
        mapping.put("app", "application");
        mapping.put("apps", "application");
        mapping.put("applications", "application");
        mapping.put("archives", "archive");
        mapping.put("articles", "article");
        mapping.put("arts", "art");
        mapping.put("arte", "art");
        mapping.put("artists", "artist");
        mapping.put("asian", "asia");
        mapping.put("asp.net", "asp");
        mapping.put("authors", "author");
        mapping.put("backgrounds", "background");
        mapping.put("banking", "bank");
        mapping.put("beautiful", "beauty");
        mapping.put("bestof", "best");
        mapping.put("blogs", "blog");
        mapping.put("blogger", "blog");
        mapping.put("blogging", "blog");
        mapping.put("bookmarking", "bookmark");
        mapping.put("bookmarks", "bookmark");
        mapping.put("bookmarksbar", "bookmark");
        mapping.put("bookmarksmenu", "bookmark");
        mapping.put("books", "book");
        mapping.put("branding", "brand");
        mapping.put("brasil", "brazil");
        mapping.put("browsers", "browser");
        mapping.put("building", "build");
        mapping.put("biz", "business");
        mapping.put("cameras", "camera");
        mapping.put("cards", "card");
        mapping.put("careers", "career");
        mapping.put("cars", "car");
        mapping.put("cartoons", "cartoon");
        mapping.put("casa", "house");
        mapping.put("cell", "cellphone");
        mapping.put("charts", "chart");
        mapping.put("christian", "christianity");
        mapping.put("cine", "cinema");
        mapping.put("cities", "city");
        mapping.put("classes", "class");
        mapping.put("classroom", "class");
        mapping.put("clothing", "clothes");
        mapping.put("coding", "code");
        mapping.put("colors", "color");
        mapping.put("colour", "color");
        mapping.put("comics", "comic");
        mapping.put("communications", "communication");
        mapping.put("companies", "company");
        mapping.put("computers", "computer");
        mapping.put("computing", "computer");
        mapping.put("conferences", "conference");
        mapping.put("converter", "convert");
        mapping.put("courses", "class");
        mapping.put("course", "class");
        mapping.put("cursos", "class");
        mapping.put("crafts", "craft");
        mapping.put("crafty", "craft");
        mapping.put("create", "creativity");
        mapping.put("creative", "creativity");
        mapping.put("cultura", "culture");
        mapping.put("cycling", "bycicle");
        mapping.put("databases", "database");
        mapping.put("db", "database");
        mapping.put("debugging", "debug");
        mapping.put("decor", "decorating");
        mapping.put("designer", "design");
        mapping.put("designers", "design");
        mapping.put("diseño", "design");
        mapping.put("dev", "development");
        mapping.put("develop", "development");
        mapping.put("developer", "development");
        mapping.put("distributed", "distribution");
        mapping.put("docs", "document");
        mapping.put("documents", "document");
        mapping.put("documentation", "documentary");
        mapping.put("downloads", "download");
        mapping.put("dev", "development");
        mapping.put("ebook", "e-book");
        mapping.put("ebooks", "e-book");
        mapping.put("e-books", "e-book");
        mapping.put("eco", "ecology");
        mapping.put("economics", "economy");
        mapping.put("editor", "editing");
        mapping.put("editorial", "editing");
        mapping.put("educación", "education");
        mapping.put("educacion", "education");
        mapping.put("educational", "education");
        mapping.put("effects", "effect");
        mapping.put("elearning", "learning");
        mapping.put("electronics", "electronic");
        mapping.put("english", "england");
        mapping.put("enterpreneur", "entrepreneurship");
        mapping.put("entrepreneur", "entrepreneurship");
        mapping.put("españa", "spain");
        mapping.put("español", "spain");
        mapping.put("events", "event");
        mapping.put("examples", "example");
        mapping.put("extensions", "extension");
        mapping.put("fanfic", "fanfiction");
        mapping.put("favorites", "favorite");
        mapping.put("feeds", "feed");
        mapping.put("fic", "fiction");
        mapping.put("files", "file");
        mapping.put("films", "film");
        mapping.put("finances", "finance");
        mapping.put("financial", "finance");
        mapping.put("first-time", "firsttime");
        mapping.put("fonts", "font");
        mapping.put("board", "forum");
        mapping.put("forums", "forum");
        mapping.put("fotografia", "foto");
        mapping.put("fotografie", "foto");
        mapping.put("fotos", "foto");
        mapping.put("photo", "foto");
        mapping.put("photos", "foto");
        mapping.put("photographer", "foto");
        mapping.put("photographers", "foto");
        mapping.put("photography", "foto");
        mapping.put("frameworks", "framework");
        mapping.put("freebies", "free");
        mapping.put("freeware", "free");
        mapping.put("french", "france");
        mapping.put("funny", "fun");
        mapping.put("gadgets", "gadget");
        mapping.put("galleries", "gallery");
        mapping.put("games", "game");
        mapping.put("gaming", "game");
        mapping.put("gardening", "garden");
        mapping.put("gen", "general");
        mapping.put("geo", "geography");
        mapping.put("deutsch", "germany");
        mapping.put("german", "germany");
        mapping.put("gifts", "gift");
        mapping.put("graphicdesign", "graphic");
        mapping.put("graphics", "graphic");
        mapping.put("gratis", "free");
        mapping.put("guidelines", "guide");
        mapping.put("guides", "guide");
        mapping.put("hacking", "hack");
        mapping.put("hacks", "hack");
        mapping.put("healthcare", "health");
        mapping.put("healthy", "health");
        mapping.put("holidays", "holiday");
        mapping.put("homepage", "home");
        mapping.put("hotels", "hotel");
        mapping.put("housing", "house");
        mapping.put("how", "howto");
        mapping.put("how-to", "howto");
        mapping.put("html5", "html");
        mapping.put("humor", "fun");
        mapping.put("humour", "fun");
        mapping.put("icons", "icon");
        mapping.put("ideas", "idea");
        mapping.put("ipodtouch", "ipod");
        mapping.put("illustrator", "illustration");
        mapping.put("illustracion", "illustration");
        mapping.put("ilustração", "illustration");
        mapping.put("images", "image");
        mapping.put("informatica", "info");
        mapping.put("information", "info");
        mapping.put("install", "installation");
        mapping.put("investing", "investment");
        mapping.put("italian", "italy");
        mapping.put("japanese", "japan");
        mapping.put("jobs", "job");
        mapping.put("jobsearch", "job");
        mapping.put("journal", "journalism");
        mapping.put("journals", "journalism");
        mapping.put("kids", "children");
        mapping.put("languages", "language");
        mapping.put("languagearts", "language");
        mapping.put("learn", "learning");
        mapping.put("lessons", "lesson");
        mapping.put("lessonplans", "lesson");
        mapping.put("libraries", "library");
        mapping.put("libros", "library");
        mapping.put("lifehacker", "lifehack");
        mapping.put("lifehacks", "lifehack");
        mapping.put("lifestyle", "lifehack");
        mapping.put("lighting", "light");
        mapping.put("links", "link");
        mapping.put("lists", "list");
        mapping.put("literacy", "literature");
        mapping.put("logos", "logo");
        mapping.put("macintosh", "mac");
        mapping.put("macosx", "mac");
        mapping.put("magazines", "magazine");
        mapping.put("email", "mail");
        mapping.put("manager", "management");
        mapping.put("mapping", "map");
        mapping.put("maps", "map");
        mapping.put("marketing", "market");
        mapping.put("mashable", "mashup");
        mapping.put("mathematics", "math");
        mapping.put("maths", "math");
        mapping.put("medicine", "medical");
        mapping.put("microblogging", "blog");
        mapping.put("moda", "mode");
        mapping.put("model", "modeling");
        mapping.put("models", "modeling");
        mapping.put("modules", "module");
        mapping.put("movie", "film");
        mapping.put("movies", "film");
        mapping.put("musica", "music");
        mapping.put("música", "music");
        mapping.put("musik", "music");
        mapping.put("musique", "music");
        mapping.put("net", "network");
        mapping.put("networking", "network");
        mapping.put("networks", "network");
        mapping.put("newspaper", "news");
        mapping.put("newspapers", "news");
        mapping.put("noticias", "notes");
        mapping.put("nyc", "newyork");
        mapping.put("ny", "newyork");
        mapping.put("open-source", "opensource");
        mapping.put("patterns", "pattern");
        mapping.put("pics", "image");
        mapping.put("pictures", "image");
        mapping.put("planning", "plan");
        mapping.put("plans", "plan");
        mapping.put("plugins", "plugin");
        mapping.put("podcasting", "podcast");
        mapping.put("podcasts", "podcast");
        mapping.put("portfolios", "portfolio");
        mapping.put("posters", "poster");
        mapping.put("presentations", "presentation");
        mapping.put("printing", "print");
        mapping.put("process", "processing");
        mapping.put("products", "product");
        mapping.put("programacion", "program");
        mapping.put("projects", "project");
        mapping.put("rating:pg-13", "pg-13");
        mapping.put("read", "unread");
        mapping.put("recipes", "recipe");
        mapping.put("resources", "resource");
        mapping.put("restaurants", "restaurant");
        mapping.put("reviews", "review");
        mapping.put("rubyonrails", "ruby");
        mapping.put("schools", "schoool");
        mapping.put("scripts", "script");
        mapping.put("searchengine", "search");
        mapping.put("searchengines", "search");
        mapping.put("services", "service");
        mapping.put("porn", "sex");
        mapping.put("share", "sharing");
        mapping.put("sharepoint", "share");
        mapping.put("shopping", "shop");
        mapping.put("shops", "shop");
        mapping.put("sites", "site");
        mapping.put("social-media", "social");
        mapping.put("social_media", "social");
        mapping.put("socialmedia", "social");
        mapping.put("socialnetwork", "social");
        mapping.put("socialnetworking", "social");
        mapping.put("socialnetworks", "social");
        mapping.put("socialsoftware", "social");
        mapping.put("socialstudies", "social");
        mapping.put("spanish", "spain");
        mapping.put("sports", "sport");
        mapping.put("startups", "startup");
        mapping.put("stocks", "stock");
        mapping.put("stories", "story");
        mapping.put("stream", "streaming");
        mapping.put("students", "student");
        mapping.put("tag", "tagging");
        mapping.put("tags", "tagging");
        mapping.put("teacher", "teaching");
        mapping.put("teachers", "teaching");
        mapping.put("tecnologia", "technology");
        mapping.put("testing", "test");
        mapping.put("teacher", "teaching");
        mapping.put("themes", "theme");
        mapping.put("templates", "template");
        mapping.put("to-read", "unread");
        mapping.put("to_read", "unread");
        mapping.put("toread", "unread");
        mapping.put("tools", "tool");
        mapping.put("bittorrent", "torrent");
        mapping.put("torrents", "torrent");
        mapping.put("tutorials", "tutorial");
        mapping.put("tutoriais", "tutorial");
        mapping.put("tutoriales", "tutorial");
        mapping.put("utilities", "utility");
        mapping.put("videos", "video");
        mapping.put("videogames", "game");
        mapping.put("visualisation", "visualization");
        mapping.put("wallpapers", "wallpaper");
        mapping.put("web-design", "webdesign");
        mapping.put("web_design", "webdesign");
        mapping.put("webapps", "webapp");
        mapping.put("web2.0", "web");
        mapping.put("webdev", "webdevelopment");
        mapping.put("weblog", "blog");
        mapping.put("webservices", "webservice");
        mapping.put("websites", "website");
        mapping.put("widgets", "widget");
        mapping.put("wikis", "wiki");
        mapping.put("windows7", "windows");
        mapping.put("windowsmobile", "windows");
        mapping.put("words", "word");
        mapping.put("xhtml", "html");

        tag = tag.toLowerCase();
        String normalizedTag = tag;
        if (mapping.containsKey(tag)) {
            normalizedTag = mapping.get(tag);
        }

        return normalizedTag;
    }

    private static String cleanTag(String tag) {
        tag = tag.replaceAll("\\$", "");
        tag = tag.replaceAll("\\%", "");
        tag = tag.replaceAll("\\&", "");
        tag = tag.replaceAll("\\(", "");
        tag = tag.replaceAll("\\)", "");
        tag = tag.replaceAll("\"", "");

        tag = StringHelper.trim(tag);

        return tag;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        // DeliciousCrawler.cleanDataSet(500);
        // DeliciousCrawler.analyzeDataSet("_cleansed_500");
        // DeliciousCrawler.analyzeDataSet("_10k_500_n");
        // System.exit(0);

        DeliciousCrawler c = new DeliciousCrawler();
        while (true) {
            c.crawl();
            // DeliciousCrawler.analyzeDataSet("");
            try {
                Thread.sleep(2 * DateHelper.MINUTE_MS);
            } catch (InterruptedException e) {
                LOGGER.warn(e.getMessage());
                break;
            }
        }
    }

}