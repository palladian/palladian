package tud.iir.extraction.object;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.ho.yaml.Yaml;
import org.w3c.dom.Document;

import tud.iir.extraction.PageAnalyzer;
import tud.iir.web.Crawler;
import tud.iir.web.CrawlerCallback;

public class ObjectExtractor implements CrawlerCallback {

    private static ObjectExtractor INSTANCE = new ObjectExtractor();

    private static final Logger LOGGER = Logger.getLogger(ObjectExtractor.class);

    private ArrayList<HashMap<String, Object>> objectDescription = null;
    private HashMap<String, Object> extractionResults = null;

    private ObjectExtractor() {
        initialize();
    }

    /**
     * Get the instance of the ObjectExtractor, which itself is singleton.
     * 
     * @return The ObjectExtractor instance.
     */
    public static ObjectExtractor getInstance() {
        return INSTANCE;
    }

    private void initialize() {
    }

    @SuppressWarnings("unchecked")
    public void loadObjectDescription(boolean created) {
        try {
            String suffix = "";
            if (created)
                suffix = "_created";
            Object object = Yaml.load(new File("data/knowledgeBase/extractionTemplateSmall" + suffix + ".yml"));
            HashMap<String, ArrayList<HashMap<String, Object>>> siteMap = (HashMap<String, ArrayList<HashMap<String, Object>>>) object;

            // input:
            // objects:
            // - url: "http://www.abb.com/product/seitp322/4bd491f50ac01a9fc1256dcc0046a220.aspx?productLanguage=us&country=00"
            // properties:
            // - name: "DC motor, DMI 180-400"
            // description: "Thanks to the precise optimization of the electrical and mechanical"

            // output:
            // objects:
            // - url: "http://www.abb.com/product/"
            // properties:
            // - name: /html/h1
            // description: /html/p/div

            objectDescription = siteMap.get("objects");

            // QASite qaSite = Yaml.loadType(new File("data/knowledgeBase/qaSiteConfigurations.yml"),QASite.class);
            LOGGER.info("description loaded...");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void saveObjectTemplate(HashMap<String, Object> template) {
        try {
            Yaml.dump(template, new File("data/knowledgeBase/extractionTemplateSmall_created.yml"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        LOGGER.info("template saved");
    }

    private void saveExtractions() {
        try {
            Yaml.dump(extractionResults, new File("data/knowledgeBase/extractionTemplateSmall_results.yml"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        LOGGER.info("results saved");
    }

    @SuppressWarnings("unchecked")
    public void createTemplate() {
        PageAnalyzer pa = new PageAnalyzer();
        HashMap<String, Object> template = new HashMap<String, Object>();
        ArrayList<HashMap<String, Object>> urlList = new ArrayList<HashMap<String, Object>>();

        for (HashMap<String, Object> descriptionPart : objectDescription) {

            HashMap<String, Object> urlObject = new HashMap<String, Object>();

            String url = (String) descriptionPart.get("url");
            pa.setDocument(url);
            urlObject.put("url", url);

            System.out.println("creating description format from URL: " + url);

            ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
            for (HashMap<String, String> properties : ((ArrayList<HashMap<String, String>>) descriptionPart.get("properties"))) {

                HashMap<String, String> propertyMap = new HashMap<String, String>();

                if (properties.get("type") != null && properties.get("type").equals("image")) {
                    // TODO find xPath for image with given src
                    continue;
                }

                for (Entry<String, String> nameValue : properties.entrySet()) {

                    System.out.println(nameValue.getKey() + "," + nameValue.getValue());
                    if (nameValue.getKey().equals("type")) {
                        propertyMap.put("type", nameValue.getValue());
                        continue;
                    }

                    LinkedHashSet<String> xPaths = pa.constructAllXPaths(nameValue.getValue());
                    if (xPaths.size() == 0) {
                        LOGGER.error(nameValue.getValue() + " could not be found to create an xPath", null);
                        System.exit(1);
                    }

                    propertyMap.put(nameValue.getKey(), xPaths.iterator().next());
                }

                list.add(propertyMap);
            }
            urlObject.put("properties", list);
            urlList.add(urlObject);
        }

        template.put("objects", urlList);
        saveObjectTemplate(template);
    }

    public void startCrawl() {
        // create empty result structure
        extractionResults = new HashMap<String, Object>();
        ArrayList<HashMap<String, Object>> urlList = new ArrayList<HashMap<String, Object>>();
        extractionResults.put("objects", urlList);

        Crawler crawler = new Crawler();
        crawler.addCrawlerCallback(this);
        crawler.setStopCount(500);

        HashSet<String> urlStack = new HashSet<String>();

        // collect urls from description as starting points
        for (HashMap<String, Object> descriptionPart : objectDescription) {
            String url = (String) descriptionPart.get("url");
            urlStack.add(url);
        }

        urlStack = new HashSet<String>();
        urlStack.add("http://www.abb.com/ProductGuide/");

        crawler.startCrawl(urlStack, true, false);
        saveExtractions();
    }

    public void crawlerCallback(Document document) {
        applyExtractionTemplate(document);
    }

    @SuppressWarnings("unchecked")
    public void applyExtractionTemplate(Document document) {

        if (document == null)
            return;

        PageAnalyzer pa = new PageAnalyzer();

        for (HashMap<String, Object> descriptionPart : objectDescription) {

            HashMap<String, Object> urlObject = new HashMap<String, Object>();
            urlObject.put("url", document.getDocumentURI());
            pa.setDocument(document);

            System.out.println("trying to extract from URL: " + document.getDocumentURI());

            ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
            for (HashMap<String, String> properties : ((ArrayList<HashMap<String, String>>) descriptionPart.get("properties"))) {

                HashMap<String, String> propertyMap = new HashMap<String, String>();

                if (properties.get("type") != null && properties.get("type").equals("image")) {
                    // TODO find xPath for image with given src
                    continue;
                }

                for (Entry<String, String> nameValue : properties.entrySet()) {

                    System.out.println(nameValue.getKey() + "," + nameValue.getValue());
                    if (nameValue.getKey().equals("type")) {
                        continue;
                    }

                    // find value by xPath and create attribute
                    String value = pa.getTextByXPath(nameValue.getValue());
                    if (value.length() > 0) {
                        propertyMap.put(nameValue.getKey(), value);
                    }
                }

                if (propertyMap.size() > 0) {
                    list.add(propertyMap);
                }
            }
            if (list.size() > 0) {
                urlObject.put("properties", list);
                ((ArrayList<HashMap<String, Object>>) extractionResults.get("objects")).add(urlObject);
            }
        }
    }

    public static void main(String[] args) {
        ObjectExtractor oe = new ObjectExtractor();
        // oe.loadObjectDescription(false);
        // oe.createTemplate();
        oe.loadObjectDescription(true);
        oe.startCrawl();
    }
}