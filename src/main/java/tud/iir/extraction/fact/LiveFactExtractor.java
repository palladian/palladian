package tud.iir.extraction.fact;

import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import tud.iir.extraction.ExtractionType;
import tud.iir.extraction.PageAnalyzer;
import tud.iir.helper.CollectionHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StringHelper;
import tud.iir.knowledge.Attribute;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.Fact;
import tud.iir.knowledge.FactValue;
import tud.iir.knowledge.KnowledgeManager;
import tud.iir.knowledge.Source;
import tud.iir.persistence.DatabaseManager;
import tud.iir.web.Crawler;
import tud.iir.web.SourceRetriever;
import tud.iir.web.SourceRetrieverManager;

/**
 * The LiveFactExtractor manages fact extraction for entity names of unknown concepts. Only the names of the entities are known.
 * 
 * @author David Urbansky
 */
public class LiveFactExtractor {

    private KnowledgeManager km = null;
    private Concept concept = null;
    private Entity entity = null;
    private Source source = null;
    private String entityName = "";

    private static final Logger logger = Logger.getLogger(LiveFactExtractor.class);

    // TODO add reference to caller so that source for entity can be saved
    public LiveFactExtractor(String entityName) {
        km = new KnowledgeManager();
        setEntityName(entityName);
        concept = new Concept("unknown");
        km.addConcept(concept);
        entity = new Entity(getEntityName(), concept);
        source = new Source("http://www.webknox.com", 0.0, ExtractionType.USER_INPUT);
        entity.addSource(source);
        concept.addEntity(entity);
    }

    /**
     * Extract facts for the entity name.
     * 
     * @param numberOfPages The number of pages that are searched through for facts.
     * @return An array of extracted facts.
     */
    public ArrayList<Fact> extractFacts(int numberOfPages) {

        ArrayList<Fact> facts = new ArrayList<Fact>();

        // build queries to retrieve web pages
        String query = getEntityName();

        // retrieve web pages with entity name mentions
        SourceRetriever sr = new SourceRetriever();
        sr.setSource(SourceRetrieverManager.GOOGLE);
        sr.setResultCount(numberOfPages);

        ArrayList<String> urls = sr.getURLs(query, true);
        for (String url : urls) {
            // url =
            // "http://www2.panasonic.com/consumer-electronics/shop/Cameras-Camcorders/Digital-Cameras/Lumix-Digital-Cameras/model.DMC-FX35S.S_11002_7000000000000005702#tabsection";
            // url =
            // "http://www2.panasonic.com/consumer-electronics/shop/Cameras-Camcorders/Digital-Cameras/Lumix-Digital-Cameras/model.DMC-FX35S.S_11002_7000000000000005702#tabsection";

            facts.addAll(extractFacts(url));
        }

        // saveExtractionsToDatabase();

        // return (Fact[]) facts.toArray(new Fact[0]);
        return entity.getFacts();
    }

    private void saveExtractionsToDatabase() {
        // check whether query was directed towards an existing entity (some facts must have been found)
        if (entity.getFacts().size() > 4) {
            logger.info("facts are being saved!");
            DatabaseManager.getInstance().saveExtractions(km);
        } else {
            logger.info("less than 5 facts found, facts are not saved in database");
        }

    }

    public ArrayList<Fact> extractFacts(String url) {
        ArrayList<Fact> facts = new ArrayList<Fact>();

        // TODO: clean up entity name and try to find concept

        PageAnalyzer pa = new PageAnalyzer();

        pa.setDocument(url);

        logger.info("Current URL: " + url);

        // try to find facts in a table
        String[] tableParameters = pa.detectFactTable();

        logger.info("XPath: " + tableParameters[0]);

        // TODO ../testing/website24_notebook.html TABLE[0] ?
        // TODO ../testing/website28_city.html table should have two columns only!
        if (tableParameters[0].length() > 0 && Integer.valueOf(tableParameters[2]) > 6) {

            logger.info("Extract from Table, expecting " + tableParameters[2] + " facts");

            String currentXPath = pa.getFirstTableCell(tableParameters[0]);
            // iterate through all rows and extract attribute and value
            int expectedRows = Integer.valueOf(tableParameters[2]);
            int expectedRowsAdded = 0;
            for (int i = 1; i <= expectedRows; i++) {

                String rowXPath = currentXPath;
                for (int j = 0; j < i; j++) {
                    rowXPath = pa.getNextTableRow(rowXPath);
                }

                String cellContent1 = StringHelper.trim(pa.getTextByXPath(rowXPath));
                rowXPath = pa.getNextTableCell(rowXPath);
                String cellContent2 = StringHelper.trim(pa.getTextByXPath(rowXPath).replaceAll("(\\?\\s)|((\\s\\?)|(\\?))", ""));

                if (cellContent1.length() == 0 || cellContent2.length() == 0) {
                    if (expectedRowsAdded < 10) {
                        expectedRowsAdded++;
                        expectedRows++;
                    }
                    continue;
                }

                Attribute attribute = new Attribute(cellContent1, Attribute.VALUE_STRING, entity.getConcept());
                attribute.addSource(source);
                attribute.setExtractedAt(new Date());
                FactValue value = new FactValue(cellContent2, new Source(url), ExtractionType.TABLE_CELL);
                Fact f = new Fact(attribute, value);
                facts.add(f);
                entity.addFactAndValue(f, value);

                logger.info("  " + cellContent1 + " : " + cellContent2);
            }
        }

        // if no table was detected or not enough was extracted from a table, search for attribute:value structure
        if (facts.size() < 4) {

            logger.info("Extract from Colon");

            Crawler crawler = new Crawler();
            String pageContent = crawler.download(url, false, true, true, true);

            // Pattern pattern = Pattern.compile(RegExp.COLON_FACT_REPRESENTATION);
            String followedByTagCheck = "\\>[^>]*?:\\s?\\<";
            Pattern pattern = Pattern.compile(followedByTagCheck);
            Matcher matcher = pattern.matcher(pageContent);
            // System.out.println(pageContent);

            if (matcher.find()) {
                // format of colon pattern is supposed to be one of the following:
                // <tag>attribute: </tag><tag2>value</tag2>
                // <tag>attribute: <tag2>value</tag2></tag>
                pattern = Pattern.compile("\\>[^>]*?:\\s?\\<.*?\\>");

            } else {
                // format of colon pattern is supposed to be one of the following:
                // <tag>attribute: value</tag>
                // <tag>attribute: value<br />attribute: value</tag>
                pattern = Pattern.compile("\\>.*?:\\s?");
            }

            matcher = pattern.matcher(pageContent);
            while (matcher.find()) {
                String attributeString = matcher.group().substring(1).replaceAll("<.*?>", "");

                // take all text after attribute until next closing tag TODO jump over several closing tags after attribute if no text was in between (as in
                // ../testing/website9_movie.html)
                int startIndex = matcher.end();
                int endIndex = pageContent.indexOf("</", startIndex);
                if (pageContent.toLowerCase().indexOf("<br", startIndex) > -1 && pageContent.toLowerCase().indexOf("<br", startIndex) < endIndex) {
                    endIndex = pageContent.toLowerCase().indexOf("<br", startIndex);
                }

                String valueString = "";
                try {
                    valueString = pageContent.substring(startIndex, endIndex).replaceAll("<.*?>", "");
                } catch (IndexOutOfBoundsException e) {
                    logger.error(valueString, e);
                    return facts;
                }

                attributeString = StringHelper.trim(attributeString);
                valueString = StringHelper.trim(valueString.replaceAll("(\\?\\s)|((\\s\\?)|(\\?))", ""));

                if (attributeString.length() == 0 || attributeString.length() > 30 || valueString.length() == 0 || attributeString.endsWith("http")
                        || attributeString.endsWith("\"javascript") || attributeString.endsWith("http") || attributeString.indexOf("style=") > -1)
                    continue;

                Attribute attribute = new Attribute(attributeString, Attribute.VALUE_STRING, concept);
                attribute.addSource(source);
                attribute.setExtractedAt(new Date());
                FactValue value = new FactValue(valueString, new Source(url), ExtractionType.COLON_PHRASE);
                Fact f = new Fact(attribute, value);
                facts.add(f);
                entity.addFactAndValue(f, value);

                logger.info("  " + attributeString + " : " + valueString);
            }

        }

        return facts;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public static void main(String[] args) {
        LiveFactExtractor lfe = new LiveFactExtractor("DMC-FX35");
        ArrayList<Fact> facts = lfe.extractFacts(3);
        CollectionHelper.print(facts);
        
        System.exit(0);

        // ArrayList<Fact> facts = lfe.extractFacts("http://www.alibaba.com/product-free/11472106/30_x_Nokia_N73_cell_phone.html");
        // ArrayList<Fact> facts = lfe.extractFacts("http://www.notebookcheck.net/Dell-Vostro-1710.9697.0.html");
        // ArrayList<Fact> facts = lfe.extractFacts("http://www.cellphonevibes.com/nokia/n77.html");

        // Crawler c = new Crawler();
        // Document document = c.getDocument("data/benchmarkSelection/facts/liveFactExtraction/training/website1.html", true);
        // String contentString = document.getLastChild().getTextContent();
        // String contentString = FileHelper.readFileToString("data/benchmarkSelection/facts/liveFactExtraction/training/website1.html");
        // System.out.println(contentString);
        // contentString = contentString.replaceAll("\\<(.|\n)*?>","");

        // ArrayList<Fact> facts = lfe.extractFacts("data/benchmarkSelection/facts/liveFactExtraction/training/website2.html");
        ArrayList<Fact> facts2 = lfe.extractFacts("data/benchmarkSelection/facts/liveFactExtraction/testing/website3_car.html");
        CollectionHelper.print(facts2);

        System.exit(0);

        String text = "<!-- This website is powered by TYPO3\n - inspiring people to share! TYPO3 is a free open source Content Management Framework initially created by Kasper Skaarhoj and licensed under GNU/GPL.	TYPO3 is copyright 1998-2008 of Kasper Skaarhoj. Extensions are copyright of their respective owners.	Information and contribution at http://typo3.com/ and http://typo3.org/ -->";
        // text = "<!--abc\ndef-->";
        // System.out.println(text);
        // System.out.println(text.replaceAll("a(.|\n)*?f", ""));
        // System.out.println(text.replaceAll("\\<!--.*?-->", ""));

        // Pattern pattern = Pattern.compile(RegExp.COLON_FACT_REPRESENTATION);
        text = FileHelper.readFileToString("data/benchmarkSelection/facts/liveFactExtraction/training/website1.html");
        Pattern pattern = Pattern.compile("((\\<!--.*?-->)|(\\<style.*?>.*?\\</style>)|(\\<script.*?>.*?\\</script>)|(\\<.*?>))", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            // System.out.println(matcher.group());
            text = text.replace(matcher.group(), "");
        }

        // System.out.println(text);
        System.exit(0);

        text = "<li>Volume: 96 cc</li> <li>Weight: 128 g</li>";
        text = "Volume: 96 ccWeight: 128 g";
        // text = "Volume: 96 cc Weight: 128 g";
        text = "General InfoNetwork:&nbsp;GSM 1900Dimensions:&nbsp;111 x 50 x 18.8 mmScreen Size:&nbsp;240 x 320 pixelsColor Depth:&nbsp;16M colors, TFTWeight:&nbsp;114 gAvailable Color(s):&nbsp;Black";
        System.out.println(text);
        text = StringHelper.unescapeHTMLEntities(text);
        System.out.println(text);

        /*
         * Pattern pattern = Pattern.compile(RegExp.COLON_FACT_REPRESENTATION); Matcher matcher = pattern.matcher(text); while (matcher.find()) {
         * System.out.println(matcher.group()); } text = StringHelper.concatMatchedString(text,"||",RegExp.COLON_FACT_REPRESENTATION); text += "";
         */

    }

}