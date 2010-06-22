package tud.iir.extraction.entity;

import java.util.HashSet;
import java.util.Iterator;

import org.w3c.dom.Document;

import tud.iir.extraction.ExtractionType;
import tud.iir.knowledge.Concept;
import tud.iir.web.Crawler;

/**
 * The FocusedCrawlExtractor technique.
 * 
 * @author David Urbansky
 */
class FocusedCrawlExtractor extends EntityExtractionTechnique {

    private EntityExtractor ee = null;
    private XPathWrapperInductor xwi = null;

    public FocusedCrawlExtractor(EntityExtractor entityExtractor) {
        ee = entityExtractor;
        setExtractionTechnique(ExtractionType.ENTITY_FOCUSED_CRAWL);
    }

    @Override
    public Integer[] getPatterns() {
        Integer[] patterns = { EntityQueryFactory.TYPE_LIST_OF_XP, EntityQueryFactory.TYPE_XS_LIST, EntityQueryFactory.TYPE_BROWSE_XP,
                EntityQueryFactory.TYPE_INDEX_OF_XP, EntityQueryFactory.TYPE_XS_INDEX };
        return patterns;
    }

    @Override
    public EntityQuery getEntityQuery(Concept concept, int entityQueryType) {
        return EntityQueryFactory.getInstance().createFocusedCrawlQuery(concept, entityQueryType);
    }

    @Override
    public void extract(String url, EntityQuery eq, Concept concept) {

        if (url.endsWith(".xml"))
            return;

        // do not extract from the same url twice
        if (!urlProcessed.add(Crawler.getCleanURL(url)))
            return;

        Crawler crawler = new Crawler();
        Document document = crawler.getWebDocument(url);
        if (document == null)
            return;

        ListDiscoverer ld = new ListDiscoverer();

        ld.findPaginationURLs(document);

        String entityXPath = "";

        xwi = new XPathWrapperInductor(ee);

        // pagination found
        if (ld.getPaginationXPath().length() > 0) {

            ee.getLogger().info("pagination found: " + ld.getPaginationXPath() + ", get pagination path and try to extract");

            HashSet<String> urlsVisited = new HashSet<String>();
            HashSet<String> listURLs = ld.getPaginationURLs();

            while (urlsVisited.size() < listURLs.size()) {

                Iterator<String> listURLIterator = listURLs.iterator();
                while (listURLIterator.hasNext()) {
                    String currentURL = listURLIterator.next();
                    if (!urlsVisited.add(currentURL))
                        continue;

                    ee.getLogger().info("process url: " + currentURL);
                    document = crawler.getWebDocument(currentURL);
                    if (document == null)
                        return;

                    // detect list and extract
                    if (entityXPath.length() == 0) {
                        entityXPath = ld.discoverEntityXPath(document);
                    }

                    if (entityXPath.length() > 0) {
                        ee.getLogger().info("use entity xpath: " + entityXPath);
                        xwi.setEntityXPath(entityXPath);
                        xwi.extract(document, eq, concept);
                        if (!xwi.hasExtractedFromCurrentURL()) {
                            return;
                        }
                    } else {
                        return;
                    }

                    ld.findPaginationURLs(document);
                    listURLs = ld.getPaginationURLs();
                    listURLIterator = listURLs.iterator();
                }

            }

            // no pagination found
        } else {

            ee.getLogger().info("no pagination found, try to extract from current page");

            // detect list and try to extract
            entityXPath = ld.discoverEntityXPath(document);
            if (entityXPath.length() > 0) {
                xwi.setEntityXPath(entityXPath);
                xwi.extract(document, eq, concept);
            }
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // KnowledgeManager km = DatabaseManager.getInstance().loadOntology();

        // EntityExtractor.getInstance().setKnowledgeManager(km);

        Concept concept = new Concept("Mobile Phone");
        String url = "http://www.gsmarena.com/motorola-phones-4.php";
        url = "http://www.imdb.com/Sections/Years/2008/";
        concept.setName("Movie");
        // url = "http://www.radioreloaded.com/actor/?tag=A"; concept.setName("Actor");
        // url = "http://www.actorscelebs.com/browse/e/"; concept.setName("Actor");
        url = "http://www.hollywood.com/movies";
        // url = "http://www.hollywood.com/a-z_index/celebrities_I";
        // km.addConcept(concept);
        url = "http://www.raaga.com/channels/telugu/movies.asp";
        // url = "http://localhost:8001";

        FocusedCrawlExtractor fce = new FocusedCrawlExtractor(EntityExtractor.getInstance());
        EntityQuery eq = fce.getEntityQuery(concept, EntityQueryFactory.TYPE_BROWSE_XP);
        fce.extract(url, eq, concept);

        fce.ee.printExtractions();

        // String[] urls = {}
        // Crawler c = new Crawler();
        // for (int i = 0; i < urls.length; i++) {
        // Document document = c.getDocument(urls[i]);
        //			
        // }

    }
}